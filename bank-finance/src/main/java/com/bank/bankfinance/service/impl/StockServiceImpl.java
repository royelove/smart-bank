package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.provider.StockQuoteProvider;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.TicketNoUtil;
import com.bank.bankfinance.dto.trade.StockOrderDTO;
import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.Position;
import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.entity.StockOrder;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.PositionMapper;
import com.bank.bankfinance.mapper.StockMapper;
import com.bank.bankfinance.mapper.StockOrderMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.StockService;
import com.bank.bankfinance.vo.PositionVO;
import com.bank.bankfinance.vo.StockOrderVO;
import com.bank.bankfinance.vo.StockQuoteVO;
import com.bank.bankfinance.vo.StockVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.bank.bankfinance.common.constant.ResponseCode.*;

@Service
public class StockServiceImpl implements StockService {

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
        return stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1))
                .stream().map(provider::quote).toList();
    }

    @Override
    public String quoteProvider() {
        return configuredProvider;
    }

    @Transactional
    @Override
    public void placeOrder(StockOrderDTO dto) {
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
            price = dto.getPrice();
        }
        BigDecimal amount = price.multiply(BigDecimal.valueOf(dto.getQuantity()));

        StockOrder order = new StockOrder();
        order.setOrderNo(TicketNoUtil.orderNo());
        order.setAccountNumber(dto.getAccountNumber());
        order.setStockCode(stock.getStockCode());
        order.setStockName(stock.getStockName());
        order.setDirection(direction);
        order.setPriceType(priceType);
        order.setOrderPrice(price);
        order.setOrderQuantity(dto.getQuantity());
        order.setFrozenAmount(BigDecimal.ZERO);
        order.setFilledQuantity(dto.getQuantity());
        order.setFilledAmount(amount);
        order.setStatus(1);

        if ("BUY".equals(direction)) {
            int rows = accountMapper.deductBalance(dto.getAccountNumber(), amount);
            if (rows == 0) {
                throw new BusinessException(STOCK_FUND_INSUFFICIENT);
            }
            mergePosition(dto.getAccountNumber(), stock, dto.getQuantity(), price);
        } else {
            Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                    .eq(Position::getAccountNumber, dto.getAccountNumber())
                    .eq(Position::getStockCode, stock.getStockCode()));
            if (position == null || position.getQuantity() < dto.getQuantity()) {
                throw new BusinessException(STOCK_SHARE_INSUFFICIENT);
            }
            position.setQuantity(position.getQuantity() - dto.getQuantity());
            if (position.getQuantity() == 0) {
                positionMapper.deleteById(position.getId());
            } else {
                positionMapper.updateById(position);
            }
            accountMapper.addBalance(dto.getAccountNumber(), amount);
        }
        stockOrderMapper.insert(order);

        stock.setCurrentPrice(price);
        stock.setChangePercent(computeChange(price, stock.getPrevClose()));
        stockMapper.updateById(stock);
    }

    @Override
    public List<StockOrderVO> myOrders(String accountNumber) {
        validateAccountAccess(accountNumber);
        return stockOrderMapper.selectList(new LambdaQueryWrapper<StockOrder>()
                        .eq(StockOrder::getAccountNumber, accountNumber)
                        .orderByDesc(StockOrder::getCreationDate))
                .stream().map(this::toOrderVO).toList();
    }

    @Override
    public List<PositionVO> myPositions(String accountNumber) {
        validateAccountAccess(accountNumber);
        List<Position> positions = positionMapper.selectList(new LambdaQueryWrapper<Position>()
                .eq(Position::getAccountNumber, accountNumber)
                .orderByAsc(Position::getStockCode));
        return positions.stream().map(this::toPositionVO).toList();
    }

    @Override
    public void cancelOrder(Long orderId, String accountNumber) {
        validateAccountAccess(accountNumber);
        StockOrder order = stockOrderMapper.selectById(orderId);
        if (order == null || !order.getAccountNumber().equals(accountNumber)) {
            throw new BusinessException(STOCK_ORDER_NOT_CANCELLABLE);
        }
        if (order.getStatus() == 0) {
            order.setStatus(2);
            stockOrderMapper.updateById(order);
            return;
        }
        throw new BusinessException(STOCK_ORDER_NOT_CANCELLABLE);
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
                    stock.setChangePercent(BigDecimal.ZERO);
                    stock.setCreationDate(now);
                    stock.setUpdateDate(now);
                    stockMapper.insert(stock);
                }
            }
            seeded = true;
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