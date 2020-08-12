package com.pingjin.oss.core.usermgr.dao;

import com.pingjin.oss.core.usermgr.model.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;


@Mapper
public interface UserInfoMapper {

    /**
     * 添加用户
     * @param userInfo
     */
    void addUser(@Param("userInfo") UserInfo userInfo);

    /**
     * 修改用户信息
     * @param userId
     * @param password
     * @param detail
     * @return
     */
    int updateUserInfo(@Param("userId") String userId, @Param("password") String password, @Param("detail") String detail);

    /**
     * 删除用户
     * @param userId
     * @return
     */
    int deleteUser(@Param("userId") String userId);

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @ResultMap("userInfoResultMap")
    UserInfo getUserInfo(@Param("userId") String userId);

    /**
     *
     * @param userName
     * @param password
     * @return
     */
    UserInfo checkPassword(@Param("userName") String userName, @Param("password") String password);

    /**
     * 通过名称获取用户信息
     * @param userName
     * @return
     */
    @ResultMap("userInfoResultMap")
    UserInfo getUserInfoByName(@Param("userName") String userName);
}
