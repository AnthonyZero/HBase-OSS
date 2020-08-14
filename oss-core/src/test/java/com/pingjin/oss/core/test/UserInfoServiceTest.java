package com.pingjin.oss.core.test;

import com.pingjin.oss.core.usermgr.model.SystemRole;
import com.pingjin.oss.core.usermgr.model.UserInfo;
import com.pingjin.oss.core.usermgr.service.UserInfoService;
import com.pingjin.oss.mybatis.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 测试
 */
public class UserInfoServiceTest extends BaseTest {

    @Autowired
    private UserInfoService userInfoService;

    @Test
    public void addUser() {
        UserInfo userInfo = new UserInfo("anthonyzero", "123456", SystemRole.ADMIN, "no desc");
        userInfoService.addUser(userInfo);
    }

    @Test
    public void getUserInfo() {
        UserInfo userInfo = userInfoService.getUserInfoByName("anthonyzero");
        System.out.println(
                userInfo.getUserId() + "|" + userInfo.getUserName() + "|" + userInfo.getPassword());
    }

    @Test
    public void deleteUser() {
        UserInfo userInfo = userInfoService.getUserInfoByName("anthonyzero");
        userInfoService.deleteUser(userInfo.getUserId());
    }
}
