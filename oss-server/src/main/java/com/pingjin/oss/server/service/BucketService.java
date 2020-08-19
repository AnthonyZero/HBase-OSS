package com.pingjin.oss.server.service;



import com.pingjin.oss.common.BucketModel;
import com.pingjin.oss.core.usermgr.model.UserInfo;

import java.util.List;

public interface BucketService {

    boolean addBucket(UserInfo userInfo, String bucketName, String detail);

    boolean deleteBucket(String bucketName);

    boolean updateBucket(String bucketName, String detail);

    BucketModel getBucketById(String bucketId);

    BucketModel getBucketByName(String bucketName);

    List<BucketModel> getUserBuckets(String token);
}
