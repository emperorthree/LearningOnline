package com.xuecheng.api.filesystem;

import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

@Api(value = "文件服务管理接口", description = "提供文件的增删改查")
public interface FileSystemControllerApi {

    /**
     * 上传文件
     * @param multipartFile 文件信息
     * @param filetag 文件标签
     * @param businesskey 业务key
     * @param metadata 元信息,json格式
     * @return 结果
     */
    @ApiOperation("文件上传")
    public UploadFileResult uploadFile(MultipartFile multipartFile,
                                       String filetag,
                                       String businesskey,
                                       String metadata);
}
