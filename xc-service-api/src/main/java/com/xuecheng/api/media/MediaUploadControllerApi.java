package com.xuecheng.api.media;

import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

@Api(value = "媒体资源管理接口", description = "提供媒体资源的增删改查")
public interface MediaUploadControllerApi {

    @ApiOperation("文件上传注册")
    public ResponseResult register(String fileMd5,
                                   String fileName,
                                   String fileSize,
                                   String mimetype,
                                   String fileExt);

    @ApiOperation("分块检查")
    public ResponseResult checkChunk(String fileMd5, Integer chunk, Integer chunkSize);

    @ApiOperation("上传分块")
    public ResponseResult uploadChunk(MultipartFile file, Integer chunk, String fileMd5);

    @ApiOperation("合并文件")
    public ResponseResult mergeFile(String fileName,
                                    String fileMd5,
                                    String mimetype,
                                    Long fileSize,
                                    String fileExt);

}
