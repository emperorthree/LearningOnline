package com.xuecheng.api.media;

import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.QueryResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "媒体文件管理接口", description = "提供媒体文件的增删改查")
public interface MediaFileControllerApi {
    @ApiOperation("媒体文件查询")
    public QueryResponseResult findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest);
}
