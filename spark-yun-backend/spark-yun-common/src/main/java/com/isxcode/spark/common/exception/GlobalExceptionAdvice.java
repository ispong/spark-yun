package com.isxcode.spark.common.exception;

import com.isxcode.spark.backend.api.base.exceptions.AbstractIsxAppException;
import com.isxcode.spark.backend.api.base.exceptions.IsxErrorException;
import com.isxcode.spark.backend.api.base.exceptions.SuccessResponseException;
import com.isxcode.spark.backend.api.base.pojos.BaseResponse;
import java.nio.file.AccessDeniedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
@ResponseBody
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GlobalExceptionAdvice extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @ExceptionHandler(AbstractIsxAppException.class)
    public ResponseEntity<BaseResponse<?>> customException(AbstractIsxAppException abstractSparkYunException) {

        BaseResponse<?> errorResponse = new BaseResponse<>();
        errorResponse.setMessageKey(abstractSparkYunException.getMessageKey());
        errorResponse.setArgs(abstractSparkYunException.getArgs());
        errorResponse.setMsg(resolveMessage(abstractSparkYunException.getMessageKey(),
            abstractSparkYunException.getArgs(), abstractSparkYunException.getMsg()));
        errorResponse.setCode(
            abstractSparkYunException.getCode() == null ? String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())
                : abstractSparkYunException.getCode());
        errorResponse.setErr(abstractSparkYunException.getErr() == null ? null : abstractSparkYunException.getErr());

        if ("401".equals(abstractSparkYunException.getCode())) {
            return new ResponseEntity<>(errorResponse, jsonHeaders(), HttpStatus.UNAUTHORIZED);
        }

        if ("403".equals(abstractSparkYunException.getCode())) {
            return new ResponseEntity<>(errorResponse, jsonHeaders(), HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(errorResponse, jsonHeaders(), HttpStatus.OK);
    }

    @ExceptionHandler(IsxErrorException.class)
    public ResponseEntity<BaseResponse<?>> customException(IsxErrorException isxErrorException) {

        BaseResponse<?> errorResponse = new BaseResponse<>();
        errorResponse.setMessageKey(isxErrorException.getMessageKey());
        errorResponse.setArgs(isxErrorException.getArgs());
        errorResponse.setMsg(
            resolveMessage(isxErrorException.getMessageKey(), isxErrorException.getArgs(), isxErrorException.getMsg()));
        errorResponse.setCode("500");
        return new ResponseEntity<>(errorResponse, jsonHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SuccessResponseException.class)
    public ResponseEntity<BaseResponse<Object>> successException(SuccessResponseException successException) {

        return new ResponseEntity<>(successException.getBaseResponse(), jsonHeaders(), HttpStatus.OK);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Object>> accessDeniedException(AccessDeniedException accessDeniedException) {

        BaseResponse<Object> baseResponse = new BaseResponse<>();
        baseResponse.setCode("401");
        baseResponse.setMessageKey("error.forbidden");
        baseResponse.setMsg(resolveMessage("error.forbidden", null, "当前用户没有权限"));
        baseResponse.setErr(accessDeniedException.getMessage());

        return new ResponseEntity<>(baseResponse, jsonHeaders(), HttpStatus.OK);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Object>> accessDeniedException(
        org.springframework.security.access.AccessDeniedException accessDeniedException) {

        BaseResponse<Object> baseResponse = new BaseResponse<>();
        baseResponse.setCode("401");
        baseResponse.setMessageKey("error.forbidden");
        baseResponse.setMsg(resolveMessage("error.forbidden", null, "当前用户没有权限"));
        baseResponse.setErr(accessDeniedException.getMessage());

        return new ResponseEntity<>(baseResponse, jsonHeaders(), HttpStatus.OK);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<BaseResponse<Object>> emptyResultDataAccessException(
        EmptyResultDataAccessException emptyResultDataAccessException) {

        BaseResponse<Object> baseResponse = new BaseResponse<>();
        baseResponse.setCode("55500");
        baseResponse.setMessageKey("error.retry_later");
        baseResponse.setMsg(resolveMessage("error.retry_later", null, "请稍后再试"));
        baseResponse.setErr(emptyResultDataAccessException.getMessage());

        return new ResponseEntity<>(baseResponse, jsonHeaders(), HttpStatus.OK);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<Object>> objectOptimisticLockingFailureException(
        ObjectOptimisticLockingFailureException objectOptimisticLockingFailureException) {

        BaseResponse<Object> baseResponse = new BaseResponse<>();
        baseResponse.setCode("55500");
        baseResponse.setMessageKey("error.retry_later");
        baseResponse.setMsg(resolveMessage("error.retry_later", null, "请稍后再试"));
        baseResponse.setErr(objectOptimisticLockingFailureException.getMessage());

        return new ResponseEntity<>(baseResponse, jsonHeaders(), HttpStatus.OK);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> allException(Exception exception) {

        BaseResponse<?> baseResponse = new BaseResponse<>();
        baseResponse.setCode(String.valueOf(HttpStatus.OK.value()));
        baseResponse.setMsg(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
        exception.printStackTrace();
        return new ResponseEntity<>(baseResponse, jsonHeaders(), HttpStatus.OK);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        ObjectError objectError = ex.getBindingResult().getAllErrors().get(0);
        BaseResponse<Object> baseResponse = new BaseResponse<>();
        baseResponse.setCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
        baseResponse.setMessageKey(objectError.getDefaultMessage());
        baseResponse.setMsg(resolveMessage(objectError.getDefaultMessage(), null, objectError.getDefaultMessage()));
        baseResponse.setErr(resolveMessage("validation.request.invalid", null, "请求参数不合法"));
        return new ResponseEntity<>(baseResponse, HttpStatus.OK);
    }

    private String resolveMessage(String messageKey, Object[] args, String fallback) {

        if (messageKey == null || messageKey.isBlank()) {
            return fallback;
        }

        try {
            return messageSource.getMessage(messageKey, args, fallback, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return fallback;
        }
    }
}
