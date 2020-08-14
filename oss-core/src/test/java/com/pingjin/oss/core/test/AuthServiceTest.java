package com.pingjin.oss.core.test;

import com.pingjin.oss.core.authmgr.model.ServiceAuth;
import com.pingjin.oss.core.authmgr.model.TokenInfo;
import com.pingjin.oss.core.authmgr.service.AuthService;
import com.pingjin.oss.mybatis.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;


public class AuthServiceTest extends BaseTest {

    @Autowired
    AuthService authService;

    @Test
    public void addToken() {
        TokenInfo tokenInfo = new TokenInfo("anthonyzero");
        authService.addToken(tokenInfo);
    }

    @Test
    public void refreshToken() {
        List<TokenInfo> tokenInfos = authService.getTokenInfos("anthonyzero");
        tokenInfos.forEach(tokenInfo -> {
            authService.refreshToken(tokenInfo.getToken());
        });
    }

    @Test
    public void deleteToken() {
        List<TokenInfo> tokenInfos = authService.getTokenInfos("anthonyzero");
        if (tokenInfos.size() > 0) {
            authService.deleteToken(tokenInfos.get(0).getToken());
        }
    }

    @Test
    public void addAuth() {
        List<TokenInfo> tokenInfos = authService.getTokenInfos("anthonyzero");
        if (tokenInfos.size() > 0) {
            ServiceAuth serviceAuth = new ServiceAuth();
            serviceAuth.setAuthTime(new Date());
            serviceAuth.setBucketName("testBucket");
            serviceAuth.setTargetToken(tokenInfos.get(0).getToken());
            authService.addAuth(serviceAuth);
        }
    }

    @Test
    public void deleteAuth() {
        List<TokenInfo> tokenInfos = authService.getTokenInfos("anthonyzero");
        if (tokenInfos.size() > 0) {
            authService.deleteAuth("testBucket", tokenInfos.get(0).getToken());
        }
    }
}
