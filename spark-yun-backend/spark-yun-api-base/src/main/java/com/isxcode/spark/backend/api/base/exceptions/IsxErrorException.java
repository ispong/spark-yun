package com.isxcode.spark.backend.api.base.exceptions;

public class IsxErrorException extends AbstractIsxAppException {

    public IsxErrorException(AbstractIsxAppExceptionEnum abstractSparkYunExceptionEnum) {
        super(abstractSparkYunExceptionEnum);
    }

    public IsxErrorException(String code, String msg, String err) {
        super(code, msg, err);
    }

    public IsxErrorException(String code, String messageKey, Object[] args, String fallbackMsg) {
        super(code, messageKey, args, fallbackMsg);
    }

    public IsxErrorException(String messageKey, Object[] args, String fallbackMsg) {
        super(messageKey, args, fallbackMsg);
    }

    public IsxErrorException(String code, String msg) {
        super(code, msg);
    }

    public IsxErrorException(String msg) {
        super(msg);
    }
}
