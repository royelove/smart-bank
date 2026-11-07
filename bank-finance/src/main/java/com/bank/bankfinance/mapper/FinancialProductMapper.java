package com.bank.bankfinance.mapper;

import com.bank.bankfinance.entity.FinancialProduct;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface FinancialProductMapper extends BaseMapper<FinancialProduct> {

    /**
     * 原子占用理财额度，返回 0 表示剩余额度不足。
     *
     * <p>新增原因：原实现是「查出来 → 内存里减 → updateById 整行写回」，
     * 并发购买时两个请求都读到同一个 remaining_amount，各自减完再写回，
     * 后写的覆盖先写的，于是产品被<b>超卖</b>（剩余额度永远减不到位）。
     * 数据库层条件更新才能保证额度不被突破。
     */
    @Update("UPDATE t_financial_product SET remaining_amount = remaining_amount - #{amount} " +
            "WHERE id = #{id} AND status = 1 AND remaining_amount >= #{amount}")
    int reserveAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
