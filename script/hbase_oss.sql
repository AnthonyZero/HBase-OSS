CREATE DATABASE IF NOT EXISTS hbase_oss
  DEFAULT CHARACTER SET UTF8
  COLLATE UTF8_GENERAL_CI;

USE hbase_oss;

--
-- Table structure for table `user_info`
--
DROP TABLE IF EXISTS user_info;

CREATE TABLE user_info
(
  user_id     VARCHAR(32) NOT NULL,
  user_name   VARCHAR(32) NOT NULL,
  password    VARCHAR(64) NOT NULL COMMENT 'password md5',
  system_role VARCHAR(32) NOT NULL COMMENT 'admin or usermgr',
  create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  detail      VARCHAR(256),
  PRIMARY KEY (user_id),
  UNIQUE KEY AK_UQ_USER_NAME (user_name)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COMMENT = '用户信息';

--
-- Table structure for table `token_info`
--

DROP TABLE IF EXISTS token_info;

CREATE TABLE token_info
(
  token        VARCHAR(32) NOT NULL,
  expire_time  INT(11)     NOT NULL,
  create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  refresh_time TIMESTAMP   NOT NULL,
  active       TINYINT     NOT NULL,
  creator      VARCHAR(32) NOT NULL,
  PRIMARY KEY (token)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COMMENT = 'token信息表';

--
-- Table structure for table `os_bucket`
--

DROP TABLE IF EXISTS os_bucket;

CREATE TABLE os_bucket (
  bucket_id  VARCHAR(32),
  bucket_name VARCHAR(32),
  create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  detail      VARCHAR(256),
  creator     VARCHAR(32) NOT NULL,
  UNIQUE KEY AK_KEY_BUCKET_NAME(BUCKET_NAME),
  PRIMARY KEY (bucket_id)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COMMENT = 'OS BUCKET';

--
-- Table structure for table SERVICE_AUTH
--

DROP TABLE IF EXISTS service_auth;

CREATE TABLE service_auth
(
  bucket_name  VARCHAR(32) NOT NULL,
  target_token VARCHAR(32) NOT NULL COMMENT '被授权对象token',
  auth_time    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (bucket_name, target_token)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COMMENT = '对象存储服务授权表';

