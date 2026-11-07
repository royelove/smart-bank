package com.bank.bankfinance.mapper;

import com.bank.bankfinance.entity.Position;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    /** 卖出委托冻结可卖股数，返回 0 表示可用股数不足 */
    @Update("UPDATE t_position SET frozen_quantity = frozen_quantity + #{qty} " +
            "WHERE account_number = #{accountNumber} AND stock_code = #{stockCode} " +
            "AND quantity - frozen_quantity >= #{qty}")
    int freezeShares(@Param("accountNumber") String accountNumber,
                     @Param("stockCode") String stockCode,
                     @Param("qty") int qty);

    /**
     * 撤单释放冻结股数。
     *
     * <p>修复记录：原为 {@code GREATEST(frozen_quantity - #{qty}, 0)}，与账户冻结资金同样的问题——
     * 超额释放被静默压成 0，重复撤单不会报错，还会把同一笔冻结股数释放两次。
     * 现改为强守卫，超额释放返回 0 行，由调用方回滚。
     */
    @Update("UPDATE t_position SET frozen_quantity = frozen_quantity - #{qty} " +
            "WHERE account_number = #{accountNumber} AND stock_code = #{stockCode} " +
            "AND frozen_quantity >= #{qty}")
    int unfreezeShares(@Param("accountNumber") String accountNumber,
                       @Param("stockCode") String stockCode,
                       @Param("qty") int qty);

    /**
     * 成交扣减持仓并同步扣减冻结股数，返回 0 表示持仓或冻结股数不足。
     *
     * <p>修复记录：原 {@code GREATEST(frozen_quantity - qty, 0)} 的写法同样掩盖了冻结股数不足；
     * 现要求冻结股数也足够，否则返回 0 行。
     */
    @Update("UPDATE t_position SET quantity = quantity - #{qty}, " +
            "frozen_quantity = frozen_quantity - #{qty} " +
            "WHERE account_number = #{accountNumber} AND stock_code = #{stockCode} " +
            "AND quantity >= #{qty} AND frozen_quantity >= #{qty}")
    int settleSell(@Param("accountNumber") String accountNumber,
                   @Param("stockCode") String stockCode,
                   @Param("qty") int qty);
}
