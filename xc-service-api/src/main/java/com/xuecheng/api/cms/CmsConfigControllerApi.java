package com.xuecheng.api.cms;

import com.xuecheng.framework.domain.cms.CmsConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "cms配置管理接口")
public interface CmsConfigControllerApi {

    @ApiOperation("根据Id查询页面配置信息")
    CmsConfig getCmsConfig(String id);
}
