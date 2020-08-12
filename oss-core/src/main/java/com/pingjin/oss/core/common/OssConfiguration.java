package com.pingjin.oss.core.common;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置属性
 */
public class OssConfiguration {

    private static OssConfiguration configuration;
    private static Properties properties;

    static {

        PathMatchingResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver();
        configuration = new OssConfiguration();
        try {
            configuration.properties = new Properties();
            //加载所有的配置文件
            Resource[] resources = resourceLoader.getResources("classpath:*.properties");
            for (Resource resource : resources) {
                Properties props = new Properties();
                InputStream input = resource.getInputStream();
                props.load(input);
                input.close();
                configuration.properties.putAll(props);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private OssConfiguration() { }

    public static OssConfiguration getConfiguration() {
        return configuration;
    }


    public String getString(String key) {
        return properties.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }
}
