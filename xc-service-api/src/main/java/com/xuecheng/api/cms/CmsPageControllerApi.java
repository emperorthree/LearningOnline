package com.xuecheng.api.cms;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.ext.CmsPostPageResult;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value = "cms页面管理接口", description = "提供页面的增删改查")
public interface CmsPageControllerApi {
    @ApiOperation("分页查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name="page",value="页码",required = true,paramType = "path",dataType = "int"),
            @ApiImplicitParam(name="size",value="每页记录",required = true,paramType = "path",dataType = "int")
    })
    //页面查询
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest);

    //页面添加
    @ApiOperation("页面添加")
    public CmsPageResult add(CmsPage cmsPage);

    //页面修改
    @ApiOperation("页面修改")
    public CmsPageResult edit(String pageId, CmsPage cmsPage);

    @ApiOperation("通过pageID查询")
    CmsPage findById(String pageId);

    @ApiOperation("页面删除")
    CmsPageResult delete(String pageId);

    @ApiOperation("页面发布")
    ResponseResult post(String pageId);

    @ApiOperation("页面保存")
    CmsPageResult save(CmsPage cmsPage);

    @ApiOperation("一键发布课程页面")
    CmsPostPageResult postPageQuick(CmsPage cmsPage);
}
