package com.bank.bankfinance.mapper;

import com.bank.bankfinance.entity.OrderBookEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderBookEntryMapper extends BaseMapper<OrderBookEntry> {

}