## HBase-OSS
Spring Boot+HBase分布式文件存储

## 软件环境
* SpringBoot 1.4.2.RELEASE
* MySQL
* JDK 1.8
* Hadoop 2.7.3
* HBase 1.2.4
* Zookeeper 3.4.14

## 权限管理设计
1. 用户可以添加Token,并且设定Token的过期时间
2. 用户可以将Bucket的访问权限授权给某个Token
3. 用户创建Bucket的时候，默认将自己的用户ID作为Token对自己授权

## 文件管理设计
1. Bucket与Token用户等信息存储到Mysql数据库，文件和文件夹存储到HBase
2. 文件存储基于HBase可以快速的读取指定RowKey的文件
3. 基于HBase过滤器实现前缀，起止文件名的的过滤操作
4. 文件上传
   * 判断有无需要上传到的文件夹，无需新建
   * 新建文件夹需要在目录表插入一条记录外还需要添加到父目录的列
   * 通过文件所在目录seqId_文件名 组成文件的RowKey,插入到文件表同时更新目录表（cf:u为目录表的seqId）
5. 文件下载
   * 根据目录地址和文件名可以获取到file的RowKey
   * 根据file的RowKey快速的找到这个文件,读取对应列族中存储的内容（二进制）
