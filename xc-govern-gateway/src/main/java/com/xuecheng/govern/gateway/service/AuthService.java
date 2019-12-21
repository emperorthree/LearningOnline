package com.xuecheng.govern.gateway.service;

import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //查询身份令牌
    public String getTokenFromCookie(HttpServletRequest request){
        final Map<String, String> uid = CookieUtil.readCookie(request, "uid");
        final String access_token = uid.get("uid");
        if (StringUtils.isEmpty(access_token))
            return null;
        return access_token;
    }

    //从header中查询jwt令牌
    public String getJwtFromHeader(HttpServletRequest request){
        final String authorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authorization)){
            return null;
        }
        if (!authorization.startsWith("Bearer")){
            return null;
        }
        return authorization;
    }

    //查询令牌有效期
    public long getExpire(String access_token){
        String key = "user_token:"+access_token;
        return stringRedisTemplate.getExpire(key);
    }
}
