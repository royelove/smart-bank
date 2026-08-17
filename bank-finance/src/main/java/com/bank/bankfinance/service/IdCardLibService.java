package com.bank.bankfinance.service;

import com.bank.bankfinance.dto.support.IdCardEnrollDTO;
import com.bank.bankfinance.entity.IdCardLib;
import com.bank.bankfinance.vo.IdCardLibVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IdCardLibService extends IService<IdCardLib> {
    void enroll(IdCardEnrollDTO dto);
    List<IdCardLibVO> listAll();
}