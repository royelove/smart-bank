package com.bank.bankfinance.common.exception;

import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>修复记录：原实现所有分支都返回 {@code Result}（即始终 HTTP 200），
 * 于是限流（429000）、未登录（401000）、无权限（700001）以及未捕获的服务端异常
 * 在网关上全都表现为 200。网关、客户端 SDK、熔断器与监控**无法感知任何错误**，
 * 这个缺陷会放大系统内每一个其他问题。
 *
 * <p>当前策略（刻意保持向后兼容）：
 * <ul>
 *   <li>普通业务错误（余额不足、密码错误等）<b>仍返回 HTTP 200</b>，仅通过响应体
 *       {@code code} 区分——这是既有前端的契约，不改。</li>
 *   <li>需要中间件/客户端采取行动的错误返回真实状态码：401 未认证、403 无权限、
 *       429 限流、500 服务端异常。</li>
 * </ul>
 * 如需回退到「全部 200」的旧行为，设置 {@code app.error.expose-http-status=false}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 需要映射为非 200 状态的业务错误码 */
    private static final String CODE_RATE_LIMITED = "429000";
    private static final String CODE_UNAUTHORIZED = "401000";
    private static final Set<String> UNAUTHORIZED_CODES =
            Set.of("401000", ResponseCode.NO_LOGIN.getCode());
    private static final Set<String> FORBIDDEN_CODES =
            Set.of(ResponseCode.NO_ADMIN_PERMISSION.getCode(),
                    ResponseCode.STAFF_REGISTER_FORBIDDEN.getCode());

    @Value("${app.error.expose-http-status:true}")
    private boolean exposeHttpStatus;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        Result<?> body = (e.getResponseCode() != null)
                ? Result.fail(e.getResponseCode())
                : Result.fail(e.getCode(), e.getMessage());
        return respond(body, httpStatusFor(e.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidationException(MethodArgumentNotValidException e) {
        return respond(Result.fail("400000", joinFieldErrors(e.getBindingResult().getFieldErrors())),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<?>> handleBindException(BindException e) {
        return respond(Result.fail("400000", joinFieldErrors(e.getBindingResult().getFieldErrors())),
                HttpStatus.BAD_REQUEST);
    }

    /** 方法参数级校验（如 @Validated + @Min/@Max 作用在 @RequestParam 上） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<?>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return respond(Result.fail("400000", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<?>> handleMissingParam(MissingServletRequestParameterException e) {
        return respond(Result.fail("400000", "缺少必要参数: " + e.getParameterName()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(Result.fail("400000", "参数类型错误: " + e.getName()),
                HttpStatus.BAD_REQUEST);
    }

    /** 请求体不是合法 JSON / 字段类型无法反序列化 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<?>> handleNotReadable(HttpMessageNotReadableException e) {
        return respond(Result.fail("400000", "请求体格式错误"), HttpStatus.BAD_REQUEST);
    }

    /**
     * 唯一索引冲突。
     *
     * <p>并发下的 check-then-insert（注册、自助开户等）会走到这里。原实现落到
     * {@code RuntimeException} 分支返回「系统繁忙」，用户看到的是误导性的服务端故障；
     * 这里改为明确的业务语义。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<?>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMostSpecificCause().getMessage());
        return respond(Result.fail(ResponseCode.DUPLICATE_RECORD), HttpStatus.OK);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<?>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return respond(Result.fail(ResponseCode.FAIL), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("未知异常: ", e);
        return respond(Result.fail(ResponseCode.FAIL), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String joinFieldErrors(java.util.List<FieldError> errors) {
        return errors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
    }

    private HttpStatus httpStatusFor(String code) {
        if (code == null) {
            return HttpStatus.OK;
        }
        if (CODE_RATE_LIMITED.equals(code)) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (UNAUTHORIZED_CODES.contains(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (FORBIDDEN_CODES.contains(code)) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.OK;
    }

    /** 统一出口：{@code exposeHttpStatus=false} 时回退为历史行为（HTTP 200 + 响应体 code） */
    private ResponseEntity<Result<?>> respond(Result<?> body, HttpStatus status) {
        HttpStatus effective = exposeHttpStatus ? status : HttpStatus.OK;
        return ResponseEntity.status(effective).body(body);
    }
}
