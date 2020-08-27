package com.pingjin.oss.server.service.impl;


import com.google.common.base.Strings;
import com.pingjin.oss.common.ObjectListResult;
import com.pingjin.oss.common.ObjectMetaData;
import com.pingjin.oss.common.OssObject;
import com.pingjin.oss.common.OssObjectSummary;
import com.pingjin.oss.common.utils.JsonUtil;
import com.pingjin.oss.server.service.HdfsService;
import com.pingjin.oss.server.service.OssStoreService;
import com.pingjin.oss.server.utils.OssUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.apache.hadoop.hbase.io.ByteBufferInputStream;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * 存储服务
 */
public class OssStoreServiceImpl implements OssStoreService {

    private static Logger logger = Logger.getLogger(OssStoreServiceImpl.class);
    private Connection connection = null;
    private HdfsService fileStore;
    private String zkUrls;
    private CuratorFramework zkClient; //zk客户端

    public OssStoreServiceImpl(Connection connection, HdfsService fileStore, String zkurls)
        throws IOException {
        this.connection = connection;
        this.fileStore = fileStore;
        this.zkUrls = zkurls;
        zkClient = CuratorFrameworkFactory.newClient(zkUrls, new ExponentialBackoffRetry(20, 5));
        zkClient.start();
    }

    @Override
    public void createSeqTable() throws IOException {
        Admin admin = connection.getAdmin();
        TableName tableName = TableName.valueOf(OssUtil.BUCKET_DIR_SEQ_TABLE);
        if (admin.tableExists(tableName)) {
            return;
        }
        HBaseService.createTable(connection, OssUtil.BUCKET_DIR_SEQ_TABLE, new String[]{OssUtil.BUCKET_DIR_SEQ_CF});
    }

    @Override
    public void createBucketStore(String bucket) throws IOException {
        //创建目录表
        HBaseService.createTable(connection, OssUtil.getDirTableName(bucket),
                OssUtil.getDirColumnFamily());
        //创建文件表
        HBaseService.createTable(connection, OssUtil.getObjTableName(bucket),
                OssUtil.getObjColumnFamily(), OssUtil.OBJ_REGIONS);
        //将其 添加到seq 表
        Put put = new Put(Bytes.toBytes(bucket));
        put.addColumn(OssUtil.BUCKET_DIR_SEQ_CF_BYTES, OssUtil.BUCKET_DIR_SEQ_QUALIFIER, Bytes.toBytes(0L));
        HBaseService.putRow(connection, OssUtil.BUCKET_DIR_SEQ_TABLE, put);
        //创建HDFS目录
        this.fileStore.mikDir(OssUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void deleteBucketStore(String bucket) throws IOException {
        //删除目录表 文件表
        HBaseService.deleteTable(connection, OssUtil.getDirTableName(bucket));
        HBaseService.deleteTable(connection, OssUtil.getObjTableName(bucket));
        //删除seq 表中的记录
        HBaseService.delete(connection, OssUtil.BUCKET_DIR_SEQ_TABLE, bucket);
        //删除HDFS上目录
        this.fileStore.deleteDir(OssUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void put(String bucket, String key, ByteBuffer input, long length, String mediaType,
        Map<String, String> properties) throws Exception {
        InterProcessMutex lock = null;
        try {
            if (key.endsWith("/")) { //判断是否创建目录
                //put dir object
                putDir(bucket, key);
                return;
            }

            //目录全路径
            String dir = key.substring(0, key.lastIndexOf("/") + 1);
            String hash = null;
            while (hash == null) {
                if (!dirExist(bucket, dir)) {
                    //当前目录不存在 创建
                    hash = putDir(bucket, dir);
                } else {
                    //否则 直接返回当前目录的seqid
                    hash = this.getDirSeqId(bucket, dir);
                }
            }
            String lockey = key.replaceAll("/", "_");
            //获取锁
            lock = new InterProcessMutex(this.zkClient, "/oss/" + bucket + "/" + lockey);
            lock.acquire();
            //文件的rowkey
            String fileKey = hash + "_" + key.substring(key.lastIndexOf("/") + 1);

            Put contentPut = new Put(fileKey.getBytes());
            if (!Strings.isNullOrEmpty(mediaType)) {
                //cf:m
                contentPut.addColumn(OssUtil.OBJ_META_CF_BYTES, OssUtil.OBJ_MEDIATYPE_QUALIFIER, mediaType.getBytes());
            }
            if (properties != null) {
                String props = JsonUtil.toJson(properties);
                contentPut.addColumn(OssUtil.OBJ_META_CF_BYTES, OssUtil.OBJ_PROPS_QUALIFIER, props.getBytes());
            }
            //cf:l 文件大小
            contentPut.addColumn(OssUtil.OBJ_META_CF_BYTES, OssUtil.OBJ_LEN_QUALIFIER, Bytes.toBytes((long) length));

            //如果文件大小 <= 20m 存储hbase 否则存在hdfs
            if (length <= OssUtil.FILE_STORE_THRESHOLD) {
                ByteBuffer qualifierBuffer = ByteBuffer.wrap(OssUtil.OBJ_CONT_QUALIFIER);
                contentPut.addColumn(OssUtil.OBJ_CONT_CF_BYTES, qualifierBuffer, System.currentTimeMillis(), input);
                qualifierBuffer.clear();
            } else {
                String fileDir = OssUtil.FILE_STORE_ROOT + "/" + bucket + "/" + hash;
                String name = key.substring(key.lastIndexOf("/") + 1);
                InputStream inputStream = new ByteBufferInputStream(input);
                this.fileStore.saveFile(fileDir, name, inputStream, length, getBucketReplication(bucket));
            }
            HBaseService.putRow(connection, OssUtil.getObjTableName(bucket), contentPut);
        } finally {
            if (lock != null) {
              lock.release();
            }
        }
    }

    @Override
    public OssObjectSummary getSummary(String bucket, String key) throws IOException {
      if (key.endsWith("/")) {
        Result result = HBaseService
            .getRow(connection, OssUtil.getDirTableName(bucket), key);
        if (!result.isEmpty()) {
          return this.dirObjectToSummary(result, bucket, key);
        } else {
          return null;
        }
      }
      String dir = key.substring(0, key.lastIndexOf("/") + 1);
      String seq = this.getDirSeqId(bucket, dir);
      if (seq == null) {
        return null;
      }
      String objKey = seq + "_" + key.substring(key.lastIndexOf("/") + 1);
      Result result = HBaseService
          .getRow(connection, OssUtil.getObjTableName(bucket), objKey);
      if (result.isEmpty()) {
        return null;
      }
      return this.resultToObjectSummary(result, bucket, dir);
    }

    @Override
    public List<OssObjectSummary> list(String bucket, String startKey, String endKey)
        throws IOException {

      String dir1 = startKey.substring(0, startKey.lastIndexOf("/") + 1).trim();
      if (dir1.length() == 0) {
        dir1 = "/";
      }
      String dir2 = endKey.substring(0, startKey.lastIndexOf("/") + 1).trim();
      if (dir2.length() == 0) {
        dir2 = "/";
      }
      String name1 = startKey.substring(startKey.lastIndexOf("/") + 1);
      String name2 = endKey.substring(startKey.lastIndexOf("/") + 1);
      String seqId = this.getDirSeqId(bucket, dir1);
      //查询dir1中大于name1的全部文件
      List<OssObjectSummary> keys = new ArrayList<>();
      if (seqId != null && name1.length() > 0) {
        byte[] max = Bytes.createMaxByteArray(100);
        byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
        if (dir1.equals(dir2)) {
          tail = (seqId + "_" + name2).getBytes();
        }
        byte[] start = (seqId + "_" + name1).getBytes();
        ResultScanner scanner1 = HBaseService
            .scanner(connection, OssUtil.getObjTableName(bucket), start, tail);
        Result result = null;
        while ((result = scanner1.next()) != null) {
          OssObjectSummary summary = this.resultToObjectSummary(result, bucket, dir1);
          keys.add(summary);
        }
        if (scanner1 != null) {
          scanner1.close();
        }
      }
      //startkey~endkey之间的全部目录
      ResultScanner scanner2 = HBaseService
          .scanner(connection, OssUtil.getDirTableName(bucket), startKey, endKey);
      Result result = null;
      while ((result = scanner2.next()) != null) {
        String seqId2 = Bytes.toString(result.getValue(OssUtil.DIR_META_CF_BYTES,
            OssUtil.DIR_SEQID_QUALIFIER));
        if (seqId2 == null) {
          continue;
        }
        String dir = Bytes.toString(result.getRow());
        keys.add(dirObjectToSummary(result, bucket, dir));
        getDirAllFiles(bucket, dir, seqId2, keys, endKey);
      }
      if (scanner2 != null) {
        scanner2.close();
      }
      Collections.sort(keys);
      return keys;
    }

    @Override
    public ObjectListResult listDir(String bucket, String dir, String start, int maxCount)
        throws IOException {
      if (start == null) {
        start = "";
      }
      Get get = new Get(Bytes.toBytes(dir));
      get.addFamily(OssUtil.DIR_SUBDIR_CF_BYTES);
      if (start.length() > 0) {
        get.setFilter(new QualifierFilter(CompareOp.GREATER_OR_EQUAL,
            new BinaryComparator(Bytes.toBytes(start))));
      }
      int maxCount1 = maxCount + 2;
      Result dirResult = HBaseService
          .getRow(connection, OssUtil.getDirTableName(bucket), get);
      List<OssObjectSummary> subDirs = null;
      if (!dirResult.isEmpty()) {
        subDirs = new ArrayList<>();
        for (Cell cell : dirResult.rawCells()) {
          OssObjectSummary summary = new OssObjectSummary();
          byte[] qualifierBytes = new byte[cell.getQualifierLength()];
          CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
          String name = Bytes.toString(qualifierBytes);
          summary.setKey(dir + name + "/");
          summary.setName(name);
          summary.setLastModifyTime(cell.getTimestamp());
          summary.setMediaType("");
          summary.setBucket(bucket);
          summary.setLength(0);
          subDirs.add(summary);
          if (subDirs.size() >= maxCount1) {
            break;
          }
        }
      }

      String dirSeq = this.getDirSeqId(bucket, dir);
      byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
      Scan objScan = new Scan();
      objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_"));
      objScan.setFilter(new PageFilter(maxCount + 1));
      objScan.setStartRow(objStart);
      objScan.setMaxResultsPerColumnFamily(maxCount1);
      objScan.addFamily(OssUtil.OBJ_META_CF_BYTES);
      logger.info("scan start: " + Bytes.toString(objStart) + " - ");
      ResultScanner objScanner = HBaseService
          .scanner(connection, OssUtil.getObjTableName(bucket), objScan);
      List<OssObjectSummary> objectSummaryList = new ArrayList<>();
      Result result = null;
      while (objectSummaryList.size() < maxCount1 && (result = objScanner.next()) != null) {
        OssObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
        objectSummaryList.add(summary);
      }
      if (objScanner != null) {
        objScanner.close();
      }
      logger.info("scan complete: " + Bytes.toString(objStart) + " - ");
      if (subDirs != null && subDirs.size() > 0) {
        objectSummaryList.addAll(subDirs);
      }
      Collections.sort(objectSummaryList);
      ObjectListResult listResult = new ObjectListResult();
      OssObjectSummary nextMarkerObj =
          objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1)
              : null;
      if (nextMarkerObj != null) {
        listResult.setNextMarker(nextMarkerObj.getKey());
      }
      if (objectSummaryList.size() > maxCount) {
        objectSummaryList = objectSummaryList.subList(0, maxCount);
      }
      listResult.setMaxKeyNumber(maxCount);
      if (objectSummaryList.size() > 0) {
        listResult.setMinKey(objectSummaryList.get(0).getKey());
        listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
      }
      listResult.setObjectCount(objectSummaryList.size());
      listResult.setObjectList(objectSummaryList);
      listResult.setBucket(bucket);

      return listResult;
    }

    @Override
    public ObjectListResult listByPrefix(String bucket, String dir, String keyPrefix, String start,
        int maxCount) throws IOException {
      if (start == null) {
        start = "";
      }
      FilterList filterList = new FilterList(Operator.MUST_PASS_ALL);
      filterList.addFilter(new ColumnPrefixFilter(keyPrefix.getBytes()));
      if (start.length() > 0) {
        filterList.addFilter(new QualifierFilter(CompareOp.GREATER_OR_EQUAL,
            new BinaryComparator(Bytes.toBytes(start))));
      }
      int maxCount1 = maxCount + 2;
      Result dirResult = HBaseService
          .getRow(connection, OssUtil.getDirTableName(bucket), dir, filterList);
      List<OssObjectSummary> subDirs = null;
      if (!dirResult.isEmpty()) {
        subDirs = new ArrayList<>();
        for (Cell cell : dirResult.rawCells()) {
          OssObjectSummary summary = new OssObjectSummary();
          byte[] qualifierBytes = new byte[cell.getQualifierLength()];
          CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
          String name = Bytes.toString(qualifierBytes);
          summary.setKey(dir + name + "/");
          summary.setName(name);
          summary.setLastModifyTime(cell.getTimestamp());
          summary.setMediaType("");
          summary.setBucket(bucket);
          summary.setLength(0);
          subDirs.add(summary);
          if (subDirs.size() >= maxCount1) {
            break;
          }
        }
      }

      String dirSeq = this.getDirSeqId(bucket, dir);
      byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
      Scan objScan = new Scan();
      objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_" + keyPrefix));
      objScan.setFilter(new PageFilter(maxCount + 1));
      objScan.setStartRow(objStart);
      objScan.setMaxResultsPerColumnFamily(maxCount1);
      objScan.addFamily(OssUtil.OBJ_META_CF_BYTES);
      logger.info("scan start: " + Bytes.toString(objStart) + " - ");
      ResultScanner objScanner = HBaseService
          .scanner(connection, OssUtil.getObjTableName(bucket), objScan);
      List<OssObjectSummary> objectSummaryList = new ArrayList<>();
      Result result = null;
      while (objectSummaryList.size() < maxCount1 && (result = objScanner.next()) != null) {
        OssObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
        objectSummaryList.add(summary);
      }
      if (objScanner != null) {
        objScanner.close();
      }
      logger.info("scan complete: " + Bytes.toString(objStart) + " - ");
      if (subDirs != null && subDirs.size() > 0) {
        objectSummaryList.addAll(subDirs);
      }
      Collections.sort(objectSummaryList);
      ObjectListResult listResult = new ObjectListResult();
      OssObjectSummary nextMarkerObj =
          objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1)
              : null;
      if (nextMarkerObj != null) {
        listResult.setNextMarker(nextMarkerObj.getKey());
      }
      if (objectSummaryList.size() > maxCount) {
        objectSummaryList = objectSummaryList.subList(0, maxCount);
      }
      listResult.setMaxKeyNumber(maxCount);
      if (objectSummaryList.size() > 0) {
        listResult.setMinKey(objectSummaryList.get(0).getKey());
        listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
      }
      listResult.setObjectCount(objectSummaryList.size());
      listResult.setObjectList(objectSummaryList);
      listResult.setBucket(bucket);

      return listResult;
    }

    @Override
    public OssObject getObject(String bucket, String key) throws IOException {
      if (key.endsWith("/")) {
        Result result = HBaseService
            .getRow(connection, OssUtil.getDirTableName(bucket), key);
        if (result.isEmpty()) {
          return null;
        }
        ObjectMetaData metaData = new ObjectMetaData();
        metaData.setBucket(bucket);
        metaData.setKey(key);
        metaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
        metaData.setLength(0);
        OssObject object = new OssObject();
        object.setMetaData(metaData);
        return object;
      }
      String dir = key.substring(0, key.lastIndexOf("/") + 1);
      String name = key.substring(key.lastIndexOf("/") + 1);
      String seq = this.getDirSeqId(bucket, dir);
      String objKey = seq + "_" + name;
      Result result = HBaseService
          .getRow(connection, OssUtil.getObjTableName(bucket), objKey);
      if (result.isEmpty()) {
        return null;
      }
      OssObject object = new OssObject();
      if (result.containsNonEmptyColumn(OssUtil.OBJ_CONT_CF_BYTES,
          OssUtil.OBJ_CONT_QUALIFIER)) {
        ByteArrayInputStream bas = new ByteArrayInputStream(
            result
                .getValue(OssUtil.OBJ_CONT_CF_BYTES,
                    OssUtil.OBJ_CONT_QUALIFIER));
        object.setContent(bas);
      } else {
        String fileDir = OssUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seq;
        InputStream inputStream = this.fileStore.openFile(fileDir, name);
        object.setContent(inputStream);
      }
      long len = Bytes.toLong(result.getValue(OssUtil.OBJ_META_CF_BYTES,
          OssUtil.OBJ_LEN_QUALIFIER));
      ObjectMetaData metaData = new ObjectMetaData();
      metaData.setBucket(bucket);
      metaData.setKey(key);
      metaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
      metaData.setLength(len);
      metaData.setMediaType(Bytes.toString(result.getValue(OssUtil.OBJ_META_CF_BYTES,
          OssUtil.OBJ_MEDIATYPE_QUALIFIER)));
      byte[] b = result
          .getValue(OssUtil.OBJ_META_CF_BYTES, OssUtil.OBJ_PROPS_QUALIFIER);
      if (b != null) {
        metaData.setAttrs(JsonUtil.fromJson(Map.class, Bytes.toString(b)));
      }
      object.setMetaData(metaData);
      return object;
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception {
      if (key.endsWith("/")) {
        //check sub dir and current dir files.
        if (!isDirEmpty(bucket, key)) {
          throw new RuntimeException("dir is not empty");
        }
        InterProcessMutex lock = null;
        try {
          String lockey = key.replaceAll("/", "_");
          lock = new InterProcessMutex(this.zkClient, "/oss/" + bucket + "/" + lockey);
          lock.acquire();
          if (!isDirEmpty(bucket, key)) {
            throw new RuntimeException("dir is not empty");
          }
          String dir1 = key.substring(0, key.lastIndexOf("/"));
          String name = dir1.substring(dir1.lastIndexOf("/") + 1);
          if (name.length() > 0) {
            String parent = key.substring(0, key.lastIndexOf(name));
            HBaseService
                .deleteQualifier(connection, OssUtil.getDirTableName(bucket), parent,
                    OssUtil.DIR_SUBDIR_CF, name);
          }
          HBaseService.delete(connection, OssUtil.getDirTableName(bucket), key);
          return;
        } finally {
          if (lock != null) {
            lock.release();
          }
        }
      }
      String dir = key.substring(0, key.lastIndexOf("/") + 1);
      String name = key.substring(key.lastIndexOf("/") + 1);
      String seqId = this.getDirSeqId(bucket, dir);
      String objKey = seqId + "_" + name;
      Result result = HBaseService
          .getRow(connection, OssUtil.getObjTableName(bucket), objKey,
              OssUtil.OBJ_META_CF_BYTES, OssUtil.OBJ_LEN_QUALIFIER);
      if (result.isEmpty()) {
        return;
      }
      long len = Bytes.toLong(result.getValue(OssUtil.OBJ_META_CF_BYTES,
          OssUtil.OBJ_LEN_QUALIFIER));
      if (len > OssUtil.FILE_STORE_THRESHOLD) {
        String fileDir = OssUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seqId;
        this.fileStore.deleteFile(fileDir, name);
      }
      HBaseService.delete(connection, OssUtil.getObjTableName(bucket), objKey);
    }

    private boolean isDirEmpty(String bucket, String dir) throws IOException {
      return listDir(bucket, dir, null, 2).getObjectList().size() == 0;
    }



    private void getDirAllFiles(String bucket, String dir, String seqId, List<OssObjectSummary> keys,
        String endKey) throws IOException {

      byte[] max = Bytes.createMaxByteArray(100);
      byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
      if (endKey.startsWith(dir)) {
        String endKeyLeft = endKey.replace(dir, "");
        String fileNameMax = endKeyLeft;
        if (endKeyLeft.indexOf("/") > 0) {
          fileNameMax = endKeyLeft.substring(0, endKeyLeft.indexOf("/"));
        }
        tail = Bytes.toBytes(seqId + "_" + fileNameMax);
      }

      Scan scan = new Scan(Bytes.toBytes(seqId), tail);
      scan.setFilter(OssUtil.OBJ_META_SCAN_FILTER);
      ResultScanner scanner = HBaseService
          .scanner(connection, OssUtil.getObjTableName(bucket), scan);
      Result result = null;
      while ((result = scanner.next()) != null) {
        OssObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
        keys.add(summary);
      }
      if (scanner != null) {
        scanner.close();
      }
    }

    private OssObjectSummary resultToObjectSummary(Result result, String bucket, String dir)
        throws IOException {
      OssObjectSummary summary = new OssObjectSummary();
      long timestamp = result.rawCells()[0].getTimestamp();
      summary.setLastModifyTime(timestamp);
      String id = new String(result.getRow());
      summary.setId(id);
      String name = id.split("_", 2)[1];
      String key = dir + name;
      summary.setKey(key);
      summary.setName(name);
      summary.setBucket(bucket);
      String s = Bytes.toString(result.getValue(OssUtil.OBJ_META_CF_BYTES,
          OssUtil.OBJ_PROPS_QUALIFIER));
      if (s != null) {
        summary.setAttrs(JsonUtil.fromJson(Map.class, s));
      }
      summary.setLength(Bytes.toLong(result.getValue(OssUtil.OBJ_META_CF_BYTES,
          OssUtil.OBJ_LEN_QUALIFIER)));
      summary
          .setMediaType(Bytes.toString(result.getValue(OssUtil.OBJ_META_CF_BYTES,
              OssUtil.OBJ_MEDIATYPE_QUALIFIER)));

      return summary;
    }

    private OssObjectSummary dirObjectToSummary(Result result, String bucket, String dir) {
      OssObjectSummary summary = new OssObjectSummary();
      String id = Bytes.toString(result.getRow());
      summary.setId(id);
      summary.setAttrs(new HashMap<>(0));
      if (dir.length() > 1) {
        summary.setName(dir.substring(dir.lastIndexOf("/") + 1));
      } else {
        summary.setName("");
      }
      summary.setBucket(bucket);
      summary.setKey(dir);
      summary.setLastModifyTime(result.rawCells()[0].getTimestamp());
      summary.setLength(0);
      summary.setMediaType("");
      return summary;
    }

    //副本数量
    private short getBucketReplication(String bucket) {
        return 2;
    }

    //创建目录
    private String putDir(String bucket, String dir) throws Exception {
        if (dirExist(bucket, dir)) {
            return null;
        }
        InterProcessMutex lock = null;
        try {
            String lockey = dir.replaceAll("/", "_");
            lock = new InterProcessMutex(this.zkClient, "/oss/" + bucket + "/" + lockey);//从zk获取锁
            lock.acquire();
            String dir1 = dir.substring(0, dir.lastIndexOf("/"));
            //获取目录的名称
            String name = dir1.substring(dir1.lastIndexOf("/") + 1);
            if (name.length() > 0) {
                //父目录
                String parent = dir.substring(0, dir1.lastIndexOf("/") + 1);
                if (!this.dirExist(bucket, parent)) {
                    //父目录不存在 创建父目录 递归
                    this.putDir(bucket, parent);
                }
                //在父目录添加sub列族内  添加子项
                Put put = new Put(Bytes.toBytes(parent));
                //sub: 目录名称
                put.addColumn(OssUtil.DIR_SUBDIR_CF_BYTES, Bytes.toBytes(name), Bytes.toBytes('1'));
                HBaseService.putRow(connection, OssUtil.getDirTableName(bucket), put);
            }
            //添加目录表
            String seqId = this.getDirSeqId(bucket, dir);
            String hash = seqId == null ? makeDirSeqId(bucket) : seqId;
            Put hashPut = new Put(dir.getBytes());
            //cf:u
            hashPut.addColumn(OssUtil.DIR_META_CF_BYTES, OssUtil.DIR_SEQID_QUALIFIER, Bytes.toBytes(hash));
            HBaseService.putRow(connection, OssUtil.getDirTableName(bucket), hashPut);
            return hash;
        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    //生成一个目录seqId
    private String makeDirSeqId(String bucket) throws IOException {
        long v = HBaseService
            .incrementColumnValue(connection, OssUtil.BUCKET_DIR_SEQ_TABLE, bucket,
                OssUtil.BUCKET_DIR_SEQ_CF_BYTES, OssUtil.BUCKET_DIR_SEQ_QUALIFIER,
                1);
        return String.format("%da%d", v % 64, v);
    }

    //判断目录是否存在（目录表是否存在此行）
    private boolean dirExist(String bucket, String dir) throws IOException {
        return HBaseService.existsRow(connection, OssUtil.getDirTableName(bucket), dir);
    }

    //获取目录seqId
    private String getDirSeqId(String bucket, String dir) throws IOException {
        Result result = HBaseService.getRow(connection, OssUtil.getDirTableName(bucket), dir);
        if (result.isEmpty()) {
            return null;
        }
        //存在 cf:u
        String dirSeqId = Bytes.toString(result.getValue(OssUtil.DIR_META_CF_BYTES, OssUtil.DIR_SEQID_QUALIFIER));
        return dirSeqId;
    }
}
