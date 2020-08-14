package com.pingjin.oss.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * 数据源配置
 */
@Configuration
@MapperScan(basePackages = OssDataSourceConfig.PACKAGE,
    sqlSessionFactoryRef = "OssSqlSessionFactory")
public class OssDataSourceConfig {

    //只扫描指定路径的dao目录（下面的mapper接口）
    //注意不要扫描其他比如普通interface 会导致注入到ioc形成mapperProxy对象
    static final String PACKAGE = "com.pingjin.oss.**.dao";

    /**
     * ossDataSource.
     *
     * @return DataSource DataSource
     * @throws IOException IOException
     */
    @Bean(name = "OssDataSource")
    @Primary
    public DataSource ossDataSource() throws IOException {
        //获取数据源相关信息
        ResourceLoader loader = new DefaultResourceLoader();
        InputStream inputStream = loader.getResource("classpath:application.properties")
            .getInputStream();
        Properties properties = new Properties();
        properties.load(inputStream);
        Set<Object> keys = properties.keySet();
        Properties dsproperties = new Properties();
        for (Object key : keys) {
            if (key.toString().startsWith("datasource")) {
                dsproperties.put(key.toString().replace("datasource.", ""), properties.get(key));
            }
        }
        //通过HikariDataSourceFactory 构建数据源
        HikariDataSourceFactory factory = new HikariDataSourceFactory();
        factory.setProperties(dsproperties);
        inputStream.close();
        return factory.getDataSource();
    }

    /**
    * OssSqlSessionFactory.
    */
    @Bean(name = "OssSqlSessionFactory")
    @Primary
    public SqlSessionFactory ossSqlSessionFactory(
        @Qualifier("OssDataSource") DataSource phoenixDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(phoenixDataSource);
        ResourceLoader loader = new DefaultResourceLoader();
        String resource = "classpath:mybatis-config.xml";
        factoryBean.setConfigLocation(loader.getResource(resource));
        factoryBean.setSqlSessionFactoryBuilder(new SqlSessionFactoryBuilder());
        return factoryBean.getObject();
    }
}
