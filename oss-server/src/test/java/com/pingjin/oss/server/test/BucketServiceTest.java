package com.pingjin.oss.server.test;

import com.pingjin.oss.common.BucketModel;
import com.pingjin.oss.core.authmgr.model.ServiceAuth;
import com.pingjin.oss.core.authmgr.service.AuthService;
import com.pingjin.oss.core.usermgr.model.SystemRole;
import com.pingjin.oss.core.usermgr.model.UserInfo;
import com.pingjin.oss.core.usermgr.service.UserInfoService;
import com.pingjin.oss.mybatis.test.BaseTest;
import com.pingjin.oss.server.dao.BucketMapper;
import com.pingjin.oss.server.service.BucketService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public class BucketServiceTest extends BaseTest {

    @Autowired
    private BucketMapper bucketMapper;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserInfoService userService;
    @Autowired
    private BucketService bucketService;

    @Test
    public void addBucket() {
        BucketModel bucketModel = new BucketModel("test1", "anthonyzero", "bucker detail");
        bucketMapper.addBucket(bucketModel);
        UserInfo userInfo = new UserInfo("pj", "123456", SystemRole.ADMIN, "");
        userService.addUser(userInfo);
        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setTargetToken(userInfo.getUserId());
        serviceAuth.setBucketName(bucketModel.getBucketName());
        authService.addAuth(serviceAuth);
    }

    @Test
    public void getBucket() {
        BucketModel bucketModel = bucketMapper.getBucketByName("test1");
        System.out.println(bucketModel.getBucketId() + "|" + bucketModel.getBucketName());
    }

    @Test
    public void getUserAuthorizedBuckets() {
        UserInfo userInfo = userService.getUserInfoByName("pj");
        List<BucketModel> bucketModels = bucketMapper.getUserAuthorizedBuckets(userInfo.getUserId());
        bucketModels.forEach(bucketModel -> {
            System.out.println(bucketModel.getBucketId() + "|" + bucketModel.getBucketName());
        });
    }

    @Test
    public void deleteBucket() {
        UserInfo userInfo = userService.getUserInfoByName("pj");
        List<BucketModel> bucketModels = bucketMapper.getUserAuthorizedBuckets(userInfo.getUserId());
        bucketModels.forEach(bucketModel -> {
            bucketService.deleteBucket(bucketModel.getBucketName());
        });
    }
}
