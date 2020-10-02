package com.pingjin.oss.sdk;

import com.pingjin.oss.common.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface OssClient {

  public void createBucket(String bucketName) throws IOException;

  public void deleteBucket(String bucketName) throws IOException;

  public List<BucketModel> listBucket() throws IOException;

  public void putObject(PutRequest putRequest) throws IOException;

  public void putObject(String bucket, String key) throws IOException;

  public void putObject(String bucket, String key, byte[] content, String mediaType)
      throws IOException;

  public OssObjectSummary getObjectSummary(String bucket, String key) throws IOException;

  public void deleteObject(String bucket, String key) throws IOException;


  public void putObject(String bucket, String key, byte[] content, String mediaType,
                        String contentEncoding)
      throws IOException;

  public void putObject(String bucket, String key, File content, String mediaType)
      throws IOException;

  public void putObject(String bucket, String key, File content, String mediaType,
                        String contentEncoding)
      throws IOException;

  public void putObject(String bucket, String key, File content)
      throws IOException;




  public ObjectListResult listObject(String bucket, String startKey, String endKey)
      throws IOException;

  public ObjectListResult listObject(ListObjectRequest request) throws IOException;

  public ObjectListResult listObjectByPrefix(String bucket, String dir, String prefix,
                                             String startKey)
      throws IOException;

  public ObjectListResult listObjectByDir(String bucket, String dir, String startKey)
      throws IOException;

  public OssObject getObject(String bucket, String key) throws IOException;

  public void createBucket(String bucketName, String detail)
      throws IOException;


  public BucketModel getBucketInfo(String bucketName) throws IOException;

}
