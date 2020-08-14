package com.pingjin.oss.core.usermgr.service.impl;

import com.google.common.base.Strings;
import com.pingjin.oss.core.usermgr.dao.UserInfoMapper;
import com.pingjin.oss.core.usermgr.model.UserInfo;
import com.pingjin.oss.core.usermgr.service.UserInfoService;
import com.pingjin.oss.core.utils.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Transactional
@Service
public class UserInfoServiceImpl implements UserInfoService {

    //set expireTime is better
    private long LONG_REFRESH_TIME = 4670409600000L;
    private int LONG_EXPIRE_TIME = 36500;

    @Autowired
    private UserInfoMapper userInfoMapper;


    @Override
    public boolean addUser(UserInfo userInfo) {
        userInfoMapper.addUser(userInfo);
        //todo add token

        return true;
    }

    @Override
    public boolean updateUserInfo(String userId, String password, String detail) {
        userInfoMapper
            .updateUserInfo(userId,
                Strings.isNullOrEmpty(password) ? null : Md5Util.getMd5(password),
                Strings.emptyToNull(detail));
        return true;
    }

    @Override
    public boolean deleteUser(String userId) {
        userInfoMapper.deleteUser(userId);

        return true;
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        return userInfoMapper.getUserInfo(userId);
    }

    @Override
    public UserInfo checkPassword(String userName, String password) {
        return userInfoMapper.checkPassword(userName, password);
    }

    @Override
    public UserInfo getUserInfoByName(String userName) {
        return userInfoMapper.getUserInfoByName(userName);
    }
}
