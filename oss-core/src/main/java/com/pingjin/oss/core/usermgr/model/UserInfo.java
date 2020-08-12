package com.pingjin.oss.core.usermgr.model;


import com.pingjin.oss.core.utils.Md5Util;
import com.pingjin.oss.core.utils.UUIDUtil;

import java.util.Date;

public class UserInfo {

    private String userId;
    private String userName;
    private String password;
    private String detail;
    private SystemRole systemRole;
    private Date createTime;

    public UserInfo(String userName, String password, SystemRole systemRole, String detail) {
        this.userId = UUIDUtil.getUUID();
        this.userName = userName;
        this.password = Md5Util.getMd5(password);
        this.systemRole = systemRole;
        this.detail = detail;
        this.createTime = new Date();
    }

    public UserInfo() {

    }

    public SystemRole getSystemRole() {
      return systemRole;
    }

    public void setSystemRole(SystemRole systemRole) {
      this.systemRole = systemRole;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }

    public Date getCreateTime() {
      return createTime;
    }

    public void setCreateTime(Date createTime) {
      this.createTime = createTime;
    }
}
