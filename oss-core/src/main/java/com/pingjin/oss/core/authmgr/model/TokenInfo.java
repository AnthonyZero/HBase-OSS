package com.pingjin.oss.core.authmgr.model;

import com.pingjin.oss.core.common.SysConstant;
import com.pingjin.oss.core.utils.UUIDUtil;

import java.util.Date;

public class TokenInfo {

    private String token;
    private int expireTime;
    private Date refreshTime;
    private Date createTime;
    private boolean active;
    private String creator;

    public TokenInfo() {

    }

    public TokenInfo(String creator) {
        this.token = UUIDUtil.getUUID();
        this.expireTime = SysConstant.DEFAULT_EXPIRE_TIME;
        Date date = new Date();
        this.refreshTime = date;
        this.createTime = date;
        this.active = true;
        this.creator = creator;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public int getExpireTime() {
      return expireTime;
    }

    public void setExpireTime(int expireTime) {
      this.expireTime = expireTime;
    }

    public Date getRefreshTime() {
      return refreshTime;
    }

    public void setRefreshTime(Date refreshTime) {
      this.refreshTime = refreshTime;
    }

    public Date getCreateTime() {
      return createTime;
    }

    public void setCreateTime(Date createTime) {
      this.createTime = createTime;
    }

    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    public String getCreator() {
      return creator;
    }

    public void setCreator(String creator) {
      this.creator = creator;
    }
}
