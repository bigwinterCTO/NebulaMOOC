package com.nebula.mooc.ssoserver.controller;

import com.nebula.mooc.core.entity.Return;
import com.nebula.mooc.core.entity.User;
import com.nebula.mooc.core.login.SsoTokenLoginHelper;
import com.nebula.mooc.core.store.SsoLoginStore;
import com.nebula.mooc.core.store.SsoSessionIdHelper;
import com.nebula.mooc.ssoserver.core.model.UserInfo;
import com.nebula.mooc.ssoserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

/**
 * sso server (for app)
 *
 * @author xuxueli 2018-04-08 21:02:54
 */
@Controller
@RequestMapping("/app")
public class AppController {

    @Autowired
    private UserService userService;


    /**
     * Login
     *
     * @param username
     * @param password
     * @return
     */
    @RequestMapping("/login")
    @ResponseBody
    public Return<String> login(String username, String password) {

        // valid login
        Return<UserInfo> result = userService.findUser(username, password);
        if (result.getCode() != Return.SUCCESS_CODE) {
            return new Return<String>(result.getCode(), result.getMsg());
        }

        // 1、make xxl-sso user
        User xxlUser = new User();
        xxlUser.setUserid(String.valueOf(result.getData().getUserid()));
        xxlUser.setUsername(result.getData().getUsername());
        xxlUser.setVersion(UUID.randomUUID().toString().replaceAll("-", ""));
        xxlUser.setExpireMinite(SsoLoginStore.getRedisExpireMinite());
        xxlUser.setExpireFreshTime(System.currentTimeMillis());


        // 2、generate sessionId + storeKey
        String sessionId = SsoSessionIdHelper.makeSessionId(xxlUser);

        // 3、login, store storeKey
        SsoTokenLoginHelper.login(sessionId, xxlUser);

        // 4、return sessionId
        return new Return<String>(sessionId);
    }


    /**
     * Logout
     *
     * @param sessionId
     * @return
     */
    @RequestMapping("/logout")
    @ResponseBody
    public Return<String> logout(String sessionId) {
        // logout, remove storeKey
        SsoTokenLoginHelper.logout(sessionId);
        return Return.SUCCESS;
    }

    /**
     * logincheck
     *
     * @param sessionId
     * @return
     */
    @RequestMapping("/logincheck")
    @ResponseBody
    public Return<User> logincheck(String sessionId) {

        // logout
        User xxlUser = SsoTokenLoginHelper.loginCheck(sessionId);
        if (xxlUser == null) {
            return new Return<User>(Return.ERROR_CODE, "sso not login.");
        }
        return new Return<User>(xxlUser);
    }

}