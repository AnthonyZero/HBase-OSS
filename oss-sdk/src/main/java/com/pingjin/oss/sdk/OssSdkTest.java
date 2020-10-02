package com.pingjin.oss.sdk;

import com.pingjin.oss.common.BucketModel;
import com.pingjin.oss.common.ObjectListResult;
import com.pingjin.oss.common.OssObject;
import com.pingjin.oss.common.OssObjectSummary;

import java.io.*;
import java.util.List;

/**
 * Created by pingjin
 */
public class OssSdkTest {

    private static String token = "a8b945e13f944e03b0d0ed49e89d97b4";
    private static String endPoints = "http://127.0.0.1:9080";

    public static void main(String[] args) {
        final OssClient client = OssClientFactory.getOrClient(endPoints, token);
        try {
            List<BucketModel> bucketModelList = client.listBucket();
            bucketModelList.forEach(bucketModel -> {
                System.out.println(bucketModel.getBucketName());
                try {
                    ObjectListResult result = client.listObjectByDir(bucketModel.getBucketName(), "/dir1/dir2/", null);
                    List<OssObjectSummary> objectList = result.getObjectList();
                    for(OssObjectSummary summary : objectList) {
                        System.out.println(summary.getBucket() + "->" + summary.getKey() + ":" + summary.getName());

                        //下载
                        OssObject ossObject = client.getObject(bucketModel.getBucketName(), summary.getKey());
                        InputStream inputStream = ossObject.getContent();
                        File outFile = new File("/Users/anthonyzero/logs/" + summary.getName());
                        if(!outFile.getParentFile().exists()) {
                            outFile.getParentFile().mkdirs();
                        }
                        FileOutputStream outputStream = new FileOutputStream(outFile);
                        byte[] bytes = new byte[1024];
                        int length = 0;
                        while((length = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, length);
                        }
                        outputStream.close();
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            //nothing to do
        }
    }
}
