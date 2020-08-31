package com.pingjin.oss.web;

import com.pingjin.oss.core.common.OssConfiguration;
import com.pingjin.oss.server.service.OssStoreService;
import com.pingjin.oss.server.service.impl.HdfsServiceImpl;
import com.pingjin.oss.server.service.impl.OssStoreServiceImpl;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OssServerBeanConfiguration {


  /**
   * create hbase client connection.
   *
   * @return conn
   * @throws IOException ioe.
   */
    @Bean
    public Connection getConnection() throws IOException {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        OssConfiguration confUtil = OssConfiguration.getConfiguration();

        config.set("hbase.zookeeper.quorum", confUtil.getString("hbase.zookeeper.quorum"));
        config.set("hbase.zookeeper.property.clientPort", confUtil.getString("hbase.zookeeper.port"));
        config.set(HConstants.HBASE_RPC_TIMEOUT_KEY, "3600000");

        return ConnectionFactory.createConnection(config);
    }

    //实例化OssStoreService示例
    @Bean(name = "ossStoreService")
    public OssStoreService getOssStore(@Autowired Connection connection) throws Exception {
        OssConfiguration confUtil = OssConfiguration.getConfiguration();
        String zkHosts = confUtil.getString("hbase.zookeeper.quorum");
        OssStoreServiceImpl store = new OssStoreServiceImpl(connection, new HdfsServiceImpl(), zkHosts);
        return store;
    }

}
