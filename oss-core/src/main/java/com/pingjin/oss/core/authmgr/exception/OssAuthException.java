package com.pingjin.oss.core.authmgr.exception;


import com.pingjin.oss.core.common.OssException;

public class OssAuthException extends OssException {

    private int code;
    private String message;

    public OssAuthException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public OssAuthException(int code, String message) {
        super(message, null);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public int errorCode() {
      return this.code;
    }
}