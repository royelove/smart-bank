package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.dto.support.IdCardEnrollDTO;
import com.bank.bankfinance.entity.IdCardLib;
import com.bank.bankfinance.mapper.IdCardLibMapper;
import com.bank.bankfinance.service.FaceService;
import com.bank.bankfinance.service.IdCardLibService;
import com.bank.bankfinance.vo.IdCardLibVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.bank.bankfinance.common.constant.ResponseCode.ID_CARD_EXISTS;

@Service
public class IdCardLibServiceImpl extends ServiceImpl<IdCardLibMapper, IdCardLib> implements IdCardLibService {

    @Autowired
    private IdCardLibMapper idCardLibMapper;

    @Autowired
    private FaceService faceService;

    @Override
    public void enroll(IdCardEnrollDTO dto) {
        Long count = idCardLibMapper.selectCount(new LambdaQueryWrapper<IdCardLib>()
                .eq(IdCardLib::getIdCard, dto.getIdCard()));
        if (count > 0) {
            throw new BusinessException(ID_CARD_EXISTS);
        }
        List<Double> feature = faceService.extractFeature(dto.getFaceImage());
        IdCardLib lib = new IdCardLib();
        lib.setIdCard(dto.getIdCard());
        lib.setName(dto.getName());
        lib.setFaceFeature(feature.stream()
                .map(v -> String.format("%.6f", v))
                .collect(Collectors.joining(",")));
        idCardLibMapper.insert(lib);
    }

    @Override
    public List<IdCardLibVO> listAll() {
        return idCardLibMapper.selectList(null).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private IdCardLibVO toVO(IdCardLib lib) {
        IdCardLibVO vo = new IdCardLibVO();
        vo.setId(lib.getId());
        vo.setIdCard(lib.getIdCard());
        vo.setName(lib.getName());
        vo.setCreationDate(lib.getCreationDate());
        return vo;
    }
}