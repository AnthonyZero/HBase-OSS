package com.pingjin.oss.core.usermgr.service;


import com.pingjin.oss.core.usermgr.model.UserInfo;

/**
 *
 */
public interface UserInfoService {

    boolean addUser(UserInfo userInfo);

    boolean updateUserInfo(String userId, String password, String detail);

    boolean deleteUser(String userId);

    UserInfo getUserInfo(String userId);

    UserInfo checkPassword(String userName, String password);

    UserInfo getUserInfoByName(String userName);
}
