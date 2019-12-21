package com.xuecheng.auth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

public class TestAuth {

    //生成JWT令牌
    @Test
    public void testCreateJwt(){
        //证书文件
        String key_location = "xc.keystore";
        String keystore_password = "xuechengkeystore";
        ClassPathResource classPathResource = new ClassPathResource(key_location);
        //密钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(classPathResource, keystore_password.toCharArray());

        //密钥的密码,此密码要和别名匹配
        String keypassword = "xuecheng";
        //密钥别名
        String alias = "xckey";
        //密钥对(公钥和私钥)
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias, keypassword.toCharArray());
        RSAPrivateKey aPrivate = (RSAPrivateKey) keyPair.getPrivate();

        Map<String, Object> map = new HashMap<>();
        map.put("id","001");
        map.put("name","tom");
        map.put("roles", "admin");
        //生成jwt令牌
        Jwt jwt = JwtHelper.encode(JSON.toJSONString(map), new RsaSigner(aPrivate));
        String token = jwt.getEncoded();
        System.out.println(token);
    }


}
