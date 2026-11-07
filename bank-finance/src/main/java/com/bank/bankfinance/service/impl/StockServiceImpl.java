package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.provider.StockQuoteProvider;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.TicketNoUtil;
import com.bank.bankfinance.dto.trade.StockOrderDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.OrderBookEntry;
import com.bank.bankfinance.entity.Position;
import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.entity.StockOrder;
import com.bank.bankfinance.entity.StockTrade;
import com.bank.bankfinance.entity.User;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.OrderBookEntryMapper;
import com.bank.bankfinance.mapper.PositionMapper;
import com.bank.bankfinance.mapper.StockMapper;
import com.bank.bankfinance.mapper.StockOrderMapper;
import com.bank.bankfinance.mapper.UserMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.MatchingEngine;
import com.bank.bankfinance.service.StockService;
import com.bank.bankfinance.vo.PositionVO;
import com.bank.bankfinance.vo.StockOrderVO;
import com.bank.bankfinance.vo.StockQuoteVO;
import com.bank.bankfinance.vo.StockVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class StockServiceImpl implements StockService {

    private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

    private static final Set<String> DIRECTIONS = Set.of("BUY", "SELL");
    private static final Set<String> PRICE_TYPES = Set.of("MARKET", "LIMIT");

    /** 股票池为空时自动初始化（代码示例股票，避免依赖手工建数） */
    private static final String[][] SAMPLE_STOCKS = {
            {"000001", "平安银行", "深", "11.20"},
            {"000002", "万科A", "深", "7.35"},
            {"600036", "招商银行", "沪", "34.20"},
            {"600519", "贵州茅台", "沪", "1480.00"},
            {"601318", "中国平安", "沪", "45.10"},
            {"002594", "比亚迪", "深", "245.60"},
            {"300750", "宁德时代", "深", "182.30"},
            {"688981", "中芯国际", "沪", "53.80"},
    };

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StockOrderMapper stockOrderMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private List<StockQuoteProvider> quoteProviders;

    @Autowired
    private OrderBookEntryMapper orderBookMapper;

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Value("${stock.quote-provider:simulated}")
    private String configuredProvider;

    private volatile boolean seeded = false;

    @Override
    public List<StockVO> listStocks() {
        ensureSeedData();
        return stockMapper.selectList(new LambdaQueryWrapper<Stock>().orderByAsc(Stock::getStockCode))
                .stream().map(this::toStockVO).toList();
    }

    @Override
    public List<StockQuoteVO> listQuotes() {
        ensureSeedData();
        StockQuoteProvider provider = resolveProvider();
        List<Stock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1));
        if (stocks.isEmpty()) {
            return List.of();
        }
        List<StockQuoteVO> quotes = provider.batchQuote(stocks);
        if (quotes.isEmpty()) {
            log.warn("行情获取失败, provider={}, 股票数={}", provider.name(), stocks.size());
            return List.of();
        }
        // 行情源解析时已把最新价/成交量回填到 Stock 实体，这里统一落库，保证榜单与 K 线用到的行情一致
        for (Stock stock : stocks) {
            stockMapper.updateById(stock);
        }
        return quotes;
    }

    @Override
    public String quoteProvider() {
        return configuredProvider;
    }

    /**
     * 加锁在事务之外：先在锁内提交整个下单+撮合事务，再释放锁，
     * 否则会出现「锁已释放、事务尚未提交」，并发线程读到旧订单簿导致重复撮合。
     *
     * <p>修复记录：原 {@code lock.tryLock(5, 10, TimeUnit.SECONDS)} 显式给了 10 秒租约。
     * Redisson 在<b>显式指定 leaseTime 时会关闭看门狗自动续期</b>，于是只要
     * 「下单 + 全量撮合 + 库存回写」超过 10 秒（撮合是 O(订单簿) 的多表写入，行情抖动或
     * 慢 SQL 时很容易超），锁会自己过期，第二个线程进来读到同一份订单簿重复撮合，
     * 造成重复成交、重复冻结释放。改为不传 leaseTime，交给看门狗持有到 unlock 为止。
     */
    @Override
    public void placeOrder(StockOrderDTO dto) {
        String lockKey = "match:" + (dto.getStockCode() == null ? "" : dto.getStockCode().trim());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(FAIL);
            }
            transactionTemplate.executeWithoutResult(status -> doPlaceOrder(dto));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(FAIL);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doPlaceOrder(StockOrderDTO dto) {
        Account account = validateAccountAccess(dto.getAccountNumber());
        if (!passwordEncoder.matches(dto.getPassword(), account.getPassword())) {
            throw new BusinessException(PASSWORD_INVALID);
        }
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getStockCode, dto.getStockCode()));
        if (stock == null || stock.getStatus() != 1) {
            throw new BusinessException(STOCK_CODE_INVALID);
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0 || dto.getQuantity() % 100 != 0) {
            throw new BusinessException(STOCK_QTY_INVALID);
        }
        String direction = dto.getDirection().toUpperCase();
        if (!DIRECTIONS.contains(direction)) {
            throw new BusinessException(STOCK_DIRECTION_INVALID);
        }
        String priceType = dto.getPriceType().toUpperCase();
        if (!PRICE_TYPES.contains(priceType)) {
            throw new BusinessException(STOCK_PRICE_TYPE_INVALID);
        }
        BigDecimal price;
        if ("MARKET".equals(priceType)) {
            price = stock.getCurrentPrice() != null ? stock.getCurrentPrice() : stock.getPrevClose();
        } else {
            if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(STOCK_PRICE_INVALID);
            }
            /*
             * 修复：下单前把委托价规整到与 t_order_book.price DECIMAL(12,3) 相同的精度。
             * 否则客户端传 11.2345 时：冻结资金按 11.2345×数量 计算并存入 DECIMAL(18,2)，
             * 而订单簿里读到的是四舍五入后的 11.235，成交结算要释放的 11.235×数量 会略大于
             * 实际冻结额，触发冻结额守卫失败 → 本来能成交的单子直接回滚。
             */
            price = dto.getPrice().setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal amount = price.multiply(BigDecimal.valueOf(dto.getQuantity()));

        // 下单即冻结：买入冻结资金，卖出冻结股数；成交/撤单时释放（与订单插入同事务，异常自动回滚）
        if ("BUY".equals(direction)) {
            if (!accountService.freezeBuyFunds(dto.getAccountNumber(), amount)) {
                throw new BusinessException(STOCK_FUND_INSUFFICIENT);
            }
        } else {
            if (positionMapper.freezeShares(dto.getAccountNumber(), stock.getStockCode(), dto.getQuantity()) == 0) {
                throw new BusinessException(STOCK_SHARE_INSUFFICIENT);
            }
        }

        String orderNo = TicketNoUtil.orderNo();
        OrderBookEntry entry = new OrderBookEntry();
        entry.setAccountNumber(dto.getAccountNumber());
        entry.setOrderNo(orderNo);
        entry.setStockCode(stock.getStockCode());
        entry.setStockName(stock.getStockName());
        entry.setDirection(direction);
        entry.setPrice(price);
        entry.setQuantity(BigDecimal.valueOf(dto.getQuantity()));
        entry.setFilledQuantity(0);
        entry.setRemainingQuantity(dto.getQuantity());
        entry.setStatus(0);
        orderBookMapper.insert(entry);

        StockOrder order = new StockOrder();
        order.setOrderNo(orderNo);
        order.setAccountNumber(dto.getAccountNumber());
        order.setStockCode(stock.getStockCode());
        order.setStockName(stock.getStockName());
        order.setDirection(direction);
        order.setPriceType(priceType);
        order.setOrderPrice(price);
        order.setOrderQuantity(dto.getQuantity());
        order.setFrozenAmount("BUY".equals(direction) ? amount : BigDecimal.ZERO);
        order.setFilledQuantity(0);
        order.setFilledAmount(BigDecimal.ZERO);
        order.setStatus(0);
        stockOrderMapper.insert(order);

        List<StockTrade> trades = matchingEngine.match(stock.getStockCode());

        /*
         * 修复：原来无论是否成交，都把这只股票的「最新价」改成本次委托价
         * （stock.setCurrentPrice(price)）。一笔远离市价的限价单（例如市价 11.20 时挂 1.00 的买单，
         * 或挂 9999 的卖单）会立刻把行情、涨跌幅、持仓市值、盈亏全部带偏，而它其实一手都没成交。
         * 现在只有真正产生成交时才用最后一笔成交价更新行情。
         */
        if (!trades.isEmpty()) {
            BigDecimal lastPrice = trades.get(trades.size() - 1).getTradePrice();
            stock.setCurrentPrice(lastPrice);
            stock.setChangePercent(computeChange(lastPrice, stock.getPrevClose()));
            stock.setUpdateDate(LocalDateTime.now());
            stockMapper.updateById(stock);
        }
    }

    @Override
    public List<StockOrderVO> myOrders(String accountNumber) {
        validateAccountAccess(accountNumber);
        return orderBookMapper.selectList(new LambdaQueryWrapper<OrderBookEntry>()
                        .eq(OrderBookEntry::getAccountNumber, accountNumber)
                        .orderByDesc(OrderBookEntry::getCreateTime))
                .stream().map(this::toOrderBookVO).toList();
    }

    @Override
    public List<PositionVO> myPositions(String accountNumber) {
        validateAccountAccess(accountNumber);
        List<Position> positions = positionMapper.selectList(new LambdaQueryWrapper<Position>()
                .eq(Position::getAccountNumber, accountNumber)
                .orderByAsc(Position::getStockCode));
        return positions.stream().map(this::toPositionVO).toList();
    }

    /**
     * 撤单。
     *
     * <p>修复记录（原实现有四处会直接造成资损的问题）：
     * <ol>
     *   <li><b>没有拿撮合锁。</b>下单走 {@code match:<code>} 分布式锁，撤单完全不加锁，
     *       于是撤单可以插在「撮合事务已改订单簿、但尚未提交」的中间，读到旧状态后释放冻结。</li>
     *   <li><b>锁在事务之外。</b>锁必须在事务外获取、在事务提交后才能释放，
     *       所以这里与 {@link #placeOrder} 一样改用 {@code transactionTemplate}，
     *       而不是给方法加 {@code @Transactional}（那会让事务包住加锁，锁先于提交释放）。</li>
     *   <li><b>状态更新忽略结果。</b>原来 {@code entry.setStatus(3); updateById(entry)}，
     *       完全无视影响行数与乐观锁冲突。两个并发撤单请求都能"成功"，
     *       于是同一笔冻结资金/股数被释放两次 —— 凭空多出可用资金。</li>
     *   <li><b>只能撤 status==0。</b>部分成交的订单（status==1）永远无法撤销，
     *       剩余冻结资金被永久锁死。现在 status 0 和 1 都可撤。</li>
     * </ol>
     */
    @Override
    public void cancelOrder(Long orderId, String accountNumber) {
        validateAccountAccess(accountNumber);
        // 先取一次订单，只为拿到股票代码来定位锁；状态与剩余量在锁内重新读取
        OrderBookEntry snapshot = orderBookMapper.selectById(orderId);
        if (snapshot == null || !accountNumber.equals(snapshot.getAccountNumber())) {
            throw new BusinessException(STOCK_ORDER_NOT_CANCELLABLE);
        }
        RLock lock = redissonClient.getLock("match:" + snapshot.getStockCode());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(FAIL);
            }
            transactionTemplate.executeWithoutResult(status -> doCancelOrder(orderId, accountNumber));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(FAIL);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doCancelOrder(Long orderId, String accountNumber) {
        OrderBookEntry entry = orderBookMapper.selectById(orderId);
        Integer currentStatus = entry == null ? null : entry.getStatus();
        if (entry == null || !accountNumber.equals(entry.getAccountNumber())
                || currentStatus == null || (currentStatus != 0 && currentStatus != 1)) {
            throw new BusinessException(STOCK_ORDER_NOT_CANCELLABLE);
        }

        // 条件更新 + 检查影响行数：只有「仍然是可撤状态」的那一次请求能拿到 1 行，
        // 并发/重复撤单的另一次拿到 0 行并回滚，从而保证冻结只被释放一次。
        int rows = orderBookMapper.update(null, new LambdaUpdateWrapper<OrderBookEntry>()
                .eq(OrderBookEntry::getId, orderId)
                .in(OrderBookEntry::getStatus, 0, 1)
                .set(OrderBookEntry::getStatus, 3));
        if (rows == 0) {
            throw new BusinessException(STOCK_ORDER_NOT_CANCELLABLE);
        }

        int remaining = entry.getRemainingQuantity() == null ? 0 : entry.getRemainingQuantity();
        if (remaining <= 0) {
            return;
        }
        // 撤单释放剩余冻结：买单释放资金，卖单释放股数；释放失败说明冻结记账已不一致 → 回滚并暴露
        if ("BUY".equals(entry.getDirection())) {
            BigDecimal release = entry.getPrice().multiply(BigDecimal.valueOf(remaining));
            if (!accountService.releaseBuyFunds(accountNumber, release)) {
                throw new IllegalStateException(String.format(
                        "撤单释放冻结资金失败（释放额超过实际冻结额）: account=%s order=%s release=%s",
                        accountNumber, entry.getOrderNo(), release));
            }
        } else if (positionMapper.unfreezeShares(accountNumber, entry.getStockCode(), remaining) == 0) {
            throw new IllegalStateException(String.format(
                    "撤单释放冻结股数失败（释放量超过实际冻结量）: account=%s stock=%s qty=%d",
                    accountNumber, entry.getStockCode(), remaining));
        }
    }

    private Account validateAccountAccess(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new BusinessException(NO_LOGIN);
        }
        String currentAccount = LoginContext.getAccount();
        if (currentAccount != null) {
            if (!currentAccount.equals(accountNumber)) {
                throw new BusinessException(NO_LOGIN);
            }
            return accountService.getAccountEntity(accountNumber);
        }
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new BusinessException(NO_LOGIN);
        }
        Account account = accountService.getAccountEntity(accountNumber);
        if (!userId.equals(account.getUserId())) {
            throw new BusinessException(NO_LOGIN);
        }
        return account;
    }

    private void mergePosition(String accountNumber, Stock stock, int quantity, BigDecimal price) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getAccountNumber, accountNumber)
                .eq(Position::getStockCode, stock.getStockCode()));
        if (position == null) {
            Position p = new Position();
            p.setAccountNumber(accountNumber);
            p.setStockCode(stock.getStockCode());
            p.setStockName(stock.getStockName());
            p.setQuantity(quantity);
            p.setAvgCost(price);
            positionMapper.insert(p);
            return;
        }
        int newQuantity = position.getQuantity() + quantity;
        BigDecimal newAvg = position.getAvgCost()
                .multiply(BigDecimal.valueOf(position.getQuantity()))
                .add(price.multiply(BigDecimal.valueOf(quantity)))
                .divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);
        position.setQuantity(newQuantity);
        position.setAvgCost(newAvg);
        positionMapper.updateById(position);
    }

    private BigDecimal computeChange(BigDecimal price, BigDecimal prevClose) {
        if (prevClose == null || prevClose.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(prevClose).multiply(BigDecimal.valueOf(100))
                .divide(prevClose, 2, RoundingMode.HALF_UP);
    }

    private StockQuoteProvider resolveProvider() {
        for (StockQuoteProvider provider : quoteProviders) {
            if (provider.name().equalsIgnoreCase(configuredProvider)) {
                return provider;
            }
        }
        return quoteProviders.stream()
                .filter(p -> "simulated".equalsIgnoreCase(p.name()))
                .findFirst().orElseThrow();
    }

    private void ensureSeedData() {
        if (seeded) {
            return;
        }
        synchronized (this) {
            if (seeded) {
                return;
            }
            if (stockMapper.selectCount(null) == 0) {
                LocalDateTime now = LocalDateTime.now();
                for (String[] s : SAMPLE_STOCKS) {
                    Stock stock = new Stock();
                    stock.setStockCode(s[0]);
                    stock.setStockName(s[1]);
                    stock.setMarket(s[2]);
                    stock.setStatus(1);
                    BigDecimal price = new BigDecimal(s[3]);
                    stock.setPrevClose(price);
                    stock.setCurrentPrice(price);
                    stock.setHighPrice(price);
                    stock.setLowPrice(price);
                    stock.setVolume(500000L);
                    stock.setAmount(price.multiply(BigDecimal.valueOf(500000)));
                    stock.setChangePercent(BigDecimal.ZERO);
                    stock.setTotalShares(BigDecimal.valueOf(1000000000));
                    stock.setTurnoverRate(BigDecimal.ZERO);
                    stock.setCreationDate(now);
                    stock.setUpdateDate(now);
                    stockMapper.insert(stock);
                }
            }
            if (positionMapper.selectCount(null) == 0) {
                initTestPositions();
            }
            seeded = true;
        }
    }

    private void initTestPositions() {
        String[][] testAccounts = {
            {"622200001", "100000"},
            {"622200002", "200000"},
            {"622200003", "150000"},
        };
        for (String[] acc : testAccounts) {
            Account account = accountService.getAccountEntity(acc[0]);
            if (account == null) {
                Account newAcc = new Account();
                newAcc.setAccountNumber(acc[0]);
                newAcc.setAccountHolderName("做市商" + acc[0].substring(acc[0].length() - 3));
                newAcc.setAccountType("储蓄");
                newAcc.setAccountStatus(1);
                newAcc.setAccountBalance(new BigDecimal(acc[1]));
                newAcc.setPassword(passwordEncoder.encode("123456"));
                newAcc.setUserIdCard("110101199001011234");
                newAcc.setUserPhoneNumber("1380000" + acc[0].substring(acc[0].length() - 4));
                accountMapper.insert(newAcc);
                account = newAcc;
            }
        }
        String[][] seedPositions = {
            {"622200001", "000001", "平安银行", "5000", "11.20"},
            {"622200001", "600519", "贵州茅台", "200",  "1480.00"},
            {"622200001", "601318", "中国平安", "3000", "45.10"},
            {"622200002", "000002", "万科A",    "8000", "7.35"},
            {"622200002", "002594", "比亚迪",   "1000", "245.60"},
            {"622200002", "600036", "招商银行", "2000", "34.20"},
            {"622200003", "300750", "宁德时代", "1500", "182.30"},
            {"622200003", "688981", "中芯国际", "2000", "53.80"},
            {"622200003", "000001", "平安银行", "4000", "11.20"},
        };
        for (String[] pos : seedPositions) {
            Position p = new Position();
            p.setAccountNumber(pos[0]);
            p.setStockCode(pos[1]);
            p.setStockName(pos[2]);
            p.setQuantity(Integer.parseInt(pos[3]));
            p.setAvgCost(new BigDecimal(pos[4]));
            positionMapper.insert(p);
        }
    }

    private StockVO toStockVO(Stock stock) {
        StockVO vo = new StockVO();
        vo.setId(stock.getId());
        vo.setStockCode(stock.getStockCode());
        vo.setStockName(stock.getStockName());
        vo.setMarket(stock.getMarket());
        vo.setStatus(stock.getStatus());
        vo.setPrevClose(stock.getPrevClose());
        vo.setCurrentPrice(stock.getCurrentPrice());
        vo.setChangePercent(stock.getChangePercent());
        return vo;
    }

    private StockOrderVO toOrderVO(StockOrder order) {
        StockOrderVO vo = new StockOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setAccountNumber(order.getAccountNumber());
        vo.setStockCode(order.getStockCode());
        vo.setStockName(order.getStockName());
        vo.setDirection(order.getDirection());
        vo.setPriceType(order.getPriceType());
        vo.setOrderPrice(order.getOrderPrice());
        vo.setOrderQuantity(order.getOrderQuantity());
        vo.setFilledAmount(order.getFilledAmount());
        vo.setFilledQuantity(order.getFilledQuantity());
        vo.setStatus(order.getStatus());
        vo.setCreationDate(order.getCreationDate());
        return vo;
    }

    private StockOrderVO toOrderBookVO(OrderBookEntry entry) {
        StockOrderVO vo = new StockOrderVO();
        vo.setId(entry.getId().longValue());
        vo.setOrderNo(entry.getOrderNo());
        vo.setAccountNumber(entry.getAccountNumber());
        vo.setStockCode(entry.getStockCode());
        vo.setStockName(entry.getStockName());
        vo.setDirection(entry.getDirection());
        vo.setPriceType("LIMIT");
        vo.setOrderPrice(entry.getPrice());
        vo.setOrderQuantity(entry.getQuantity().intValue());
        vo.setFilledQuantity(entry.getFilledQuantity());
        vo.setFilledAmount(entry.getPrice().multiply(BigDecimal.valueOf(entry.getFilledQuantity())));
        vo.setStatus(entry.getStatus());
        vo.setCreationDate(entry.getCreateTime());
        return vo;
    }

    private PositionVO toPositionVO(Position position) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getStockCode, position.getStockCode()));
        BigDecimal currentPrice = stock != null && stock.getCurrentPrice() != null
                ? stock.getCurrentPrice() : position.getAvgCost();
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()));
        BigDecimal profitLoss = currentPrice.subtract(position.getAvgCost())
                .multiply(BigDecimal.valueOf(position.getQuantity()));
        BigDecimal profitLossPercent = position.getAvgCost().signum() > 0
                ? currentPrice.subtract(position.getAvgCost()).multiply(BigDecimal.valueOf(100))
                        .divide(position.getAvgCost(), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        PositionVO vo = new PositionVO();
        vo.setId(position.getId());
        vo.setAccountNumber(position.getAccountNumber());
        vo.setStockCode(position.getStockCode());
        vo.setStockName(position.getStockName());
        vo.setQuantity(position.getQuantity());
        vo.setAvgCost(position.getAvgCost());
        vo.setCurrentPrice(currentPrice);
        vo.setMarketValue(marketValue);
        vo.setProfitLoss(profitLoss);
        vo.setProfitLossPercent(profitLossPercent);
        return vo;
    }
}
