package com.isxcode.spark.backend.api.base.exceptions;

import lombok.Getter;

/** 异常抽象类. */
@Getter
public abstract class AbstractIsxAppException extends RuntimeException {

    /**
     * 异常编码.
     */
    private final String code;

    /**
     * 异常的中文信息.
     */
    private final String msg;

    /**
     * 国际化消息key.
     */
    private final String messageKey;

    /**
     * 国际化消息参数.
     */
    private final Object[] args;

    /**
     * 异常的英文信息.
     */
    private final String err;

    public AbstractIsxAppException(AbstractIsxAppExceptionEnum abstractSparkYunExceptionEnum) {

        super(abstractSparkYunExceptionEnum.getMsg());
        this.code = abstractSparkYunExceptionEnum.getCode();
        this.msg = abstractSparkYunExceptionEnum.getMsg();
        this.messageKey = null;
        this.args = null;
        this.err = null;
    }

    public AbstractIsxAppException(String code, String msg, String err) {

        super(msg);
        this.code = code;
        this.msg = msg;
        this.messageKey = null;
        this.args = null;
        this.err = err;
    }

    public AbstractIsxAppException(String code, String messageKey, Object[] args, String fallbackMsg) {

        super(fallbackMsg);
        this.code = code;
        this.msg = fallbackMsg;
        this.messageKey = messageKey;
        this.args = args;
        this.err = null;
    }

    public AbstractIsxAppException(String messageKey, Object[] args, String fallbackMsg) {

        super(fallbackMsg);
        this.code = null;
        this.msg = fallbackMsg;
        this.messageKey = messageKey;
        this.args = args;
        this.err = null;
    }

    public AbstractIsxAppException(String code, String msg) {

        super(msg);
        this.code = code;
        this.msg = msg;
        this.messageKey = null;
        this.args = null;
        this.err = null;
    }

    public AbstractIsxAppException(String msg) {

        super(msg);
        this.code = null;
        this.msg = msg;
        this.messageKey = null;
        this.args = null;
        this.err = null;
    }
}
