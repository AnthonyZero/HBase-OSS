package com.pingjin.oss.server.service.impl;

import com.pingjin.oss.common.BucketModel;
import com.pingjin.oss.core.authmgr.model.ServiceAuth;
import com.pingjin.oss.core.authmgr.service.AuthService;
import com.pingjin.oss.core.usermgr.model.UserInfo;
import com.pingjin.oss.server.dao.BucketMapper;
import com.pingjin.oss.server.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class BucketServiceImpl implements BucketService {

    @Autowired
    private BucketMapper bucketMapper;
    @Autowired
    private AuthService authService;

    @Override
    public boolean addBucket(UserInfo userInfo, String bucketName, String detail) {
        BucketModel bucketModel = new BucketModel(bucketName, userInfo.getUserName(), detail);
        bucketMapper.addBucket(bucketModel);
        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setBucketName(bucketName);
        serviceAuth.setTargetToken(userInfo.getUserId()); //token == userId
        authService.addAuth(serviceAuth);
        return true;
    }

    @Override
    public boolean deleteBucket(String bucketName) {
        bucketMapper.deleteBucket(bucketName);
        authService.deleteAuthByBucket(bucketName);
        return true;
    }

    @Override
    public boolean updateBucket(String bucketName, String detail) {
        bucketMapper.updateBucket(bucketName, detail);
        return true;
    }

    @Override
    public BucketModel getBucketById(String bucketId) {
        return bucketMapper.getBucket(bucketId);
    }

    @Override
    public BucketModel getBucketByName(String bucketName) {
        return bucketMapper.getBucketByName(bucketName);
    }

    @Override
    public List<BucketModel> getUserBuckets(String token) {
        return bucketMapper.getUserAuthorizedBuckets(token);
    }
}
