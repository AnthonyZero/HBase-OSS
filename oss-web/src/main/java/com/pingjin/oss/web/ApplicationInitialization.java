package com.pingjin.oss.web;

import com.pingjin.oss.core.common.SysConstant;
import com.pingjin.oss.core.usermgr.model.SystemRole;
import com.pingjin.oss.core.usermgr.model.UserInfo;
import com.pingjin.oss.core.usermgr.service.UserInfoService;
import com.pingjin.oss.server.service.OssStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInitialization implements ApplicationRunner {

    @Autowired
    private OssStoreService ossStoreService;

    @Autowired
    private UserInfoService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        UserInfo userInfo = userService.getUserInfoByName(SysConstant.SYSTEM_USER);
        if (userInfo == null) {
            UserInfo userInfo1 = new UserInfo(SysConstant.SYSTEM_USER, "admin", SystemRole.SUPERADMIN,
              "this is superadmin");
            userService.addUser(userInfo1);
        }
        ossStoreService.createSeqTable();
    }
}
