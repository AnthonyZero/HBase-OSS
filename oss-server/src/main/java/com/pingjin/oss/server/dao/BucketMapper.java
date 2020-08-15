package com.pingjin.oss.server.dao;

import com.pingjin.oss.common.BucketModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.List;

@Mapper
public interface BucketMapper {

    void addBucket(@Param("bucket") BucketModel bucketModel);

    int updateBucket(@Param("bucketName") String bucketName, @Param("detail") String detail);

    int deleteBucket(@Param("bucketName") String bucketName);

    @ResultMap("bucketResultMap")
    BucketModel getBucket(@Param("bucketId") String bucketId);

    @ResultMap("bucketResultMap")
    BucketModel getBucketByName(@Param("bucketName") String bucketName);

    @ResultMap("bucketResultMap")
    List<BucketModel> getBucketByCreator(@Param("creator") String creator);

    @ResultMap("bucketResultMap")
    List<BucketModel> getUserAuthorizedBuckets(@Param("token") String token);
}
