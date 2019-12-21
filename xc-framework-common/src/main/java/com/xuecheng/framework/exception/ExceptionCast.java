package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;

/**
 * 抛出自定义异常类工具
 */
public class ExceptionCast {

    public static void cast(ResultCode resultCode){
        throw new CustomerException(resultCode);
    }
}
