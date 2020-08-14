package com.pingjin.oss.core.authmgr.service;

import com.pingjin.oss.core.authmgr.model.ServiceAuth;
import com.pingjin.oss.core.authmgr.model.TokenInfo;

import java.util.List;

public interface AuthService {

    boolean addAuth(ServiceAuth auth);

    boolean deleteAuth(String bucketName, String token);

    boolean deleteAuthByBucket(String bucketName);

    boolean deleteAuthByToken(String token);

    ServiceAuth getServiceAuth(String bucketName, String token);

    boolean addToken(TokenInfo tokenInfo);

    boolean updateToken(String token, int expireTime, boolean isActive);

    boolean refreshToken(String token);

    boolean deleteToken(String token);

    boolean checkToken(String token);

    TokenInfo getTokenInfo(String token);

    List<TokenInfo> getTokenInfos(String creator);
}
