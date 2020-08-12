package com.pingjin.oss.core.usermgr.exception;


import com.pingjin.oss.core.common.OssException;

/**
 * 用户管理模块异常.
 */
public class OssUserException extends OssException {

    private int code;
    private String message;

    public OssUserException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public OssUserException(int code, String message) {
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
    public int errorCode() { return code; }
}
