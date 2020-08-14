package com.pingjin.oss.mybatis.test;

import com.pingjin.oss.mybatis.OssDataSourceConfig;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test基类
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Import(OssDataSourceConfig.class)
@PropertySource("classpath:application.properties")
@ComponentScan("com.pingjin.oss.*")
public class BaseTest {

}
