package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.config.FaceProperties;
import com.bank.bankfinance.service.FaceService;
import com.bank.bankfinance.vo.FaceVerifyResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FaceServiceImpl implements FaceService {

    private static final Logger log = LoggerFactory.getLogger(FaceServiceImpl.class);

    @Autowired
    private FaceProperties faceProperties;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(faceProperties.getTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(faceProperties.getTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(faceProperties.getBaseUrl())
                .requestFactory(factory)
                .build();
        log.info("人脸识别服务已接入: {}", faceProperties.getBaseUrl());
    }

    @Override
    public List<Double> extractFeature(String base64Image) {
        Map<String, Object> body = new HashMap<>();
        body.put("image", base64Image);
        Map<String, Object> data = post("/api/face/feature", body);
        Object feature = data.get("feature");
        if (!(feature instanceof List<?> list) || list.size() != 512) {
            throw new BusinessException(ResponseCode.FACE_SERVICE_ERROR);
        }
        return list.stream().map(v -> ((Number) v).doubleValue()).toList();
    }

    @Override
    public FaceVerifyResult verify(String base64Image, List<Double> storedFeature) {
        Map<String, Object> body = new HashMap<>();
        body.put("image", base64Image);
        body.put("feature", storedFeature);
        Map<String, Object> data = post("/api/face/compare", body);
        boolean pass = Boolean.TRUE.equals(data.get("pass"));
        double score = data.get("score") instanceof Number n ? n.doubleValue() : 0.0;
        return new FaceVerifyResult(pass, score);
    }

    /**
     * 调用人脸微服务并统一解析 code，非 0 统一转业务异常
     */
    private Map<String, Object> post(String uri, Map<String, Object> body) {
        ResponseEntity<Map> response;
        try {
            response = restClient.post().uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);
        } catch (ResourceAccessException e) {
            log.error("人脸服务连接失败 {}: {}", uri, e.getMessage());
            throw new BusinessException(ResponseCode.FACE_SERVICE_ERROR);
        }
        Map<String, Object> data = response.getBody();
        if (data == null) {
            throw new BusinessException(ResponseCode.FACE_SERVICE_ERROR);
        }
        Integer code = data.get("code") instanceof Number n ? n.intValue() : null;
        if (code != null && code == 0) {
            return data;
        }
        String msg = data.get("msg") == null ? "人脸服务异常" : String.valueOf(data.get("msg"));
        if (Integer.valueOf(404).equals(code)) {
            throw new BusinessException(ResponseCode.FACE_NO_FACE);
        }
        log.warn("人脸服务业务错误 {}: code={}, msg={}", uri, code, msg);
        throw new BusinessException(ResponseCode.FACE_SERVICE_ERROR.getCode(), msg);
    }
}
