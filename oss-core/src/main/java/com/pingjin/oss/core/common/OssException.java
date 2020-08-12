package com.pingjin.oss.core.common;

/**
 * 异常类基类
 */
public abstract class OssException extends RuntimeException {

    protected String errorMsg;

    public OssException(String message, Throwable cause) {
        super(cause);
        this.errorMsg = message;
    }

    public abstract int errorCode();

    public String errorMsg() {
        return this.errorMsg;
    }
}
