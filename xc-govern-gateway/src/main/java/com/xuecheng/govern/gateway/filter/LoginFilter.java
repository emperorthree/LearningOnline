package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginFilter extends ZuulFilter {

    @Autowired
    AuthService authService;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        //上下文对象
        final RequestContext requestContext = RequestContext.getCurrentContext();
        final HttpServletRequest request = requestContext.getRequest();
        final String access_token = authService.getTokenFromCookie(request);
        if (access_token==null){
            access_denied();
        }
        final long expire = authService.getExpire(access_token);
        if (expire<=0)
            access_denied();
        final String jwt = authService.getJwtFromHeader(request);
        if (jwt == null){
            access_denied();
        }
        return null;
    }

    private void access_denied() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletResponse response = requestContext.getResponse();
        requestContext.setSendZuulResponse(false);//拒绝访问
        ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
        final String s = JSON.toJSONString(responseResult);
        requestContext.setResponseBody(s);
        response.setContentType("application/json;charset=utf-8");
    }
}
