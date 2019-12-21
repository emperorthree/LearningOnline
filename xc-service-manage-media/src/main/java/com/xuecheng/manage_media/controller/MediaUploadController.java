package com.xuecheng.manage_media.controller;

import com.xuecheng.api.media.MediaUploadControllerApi;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.service.MediaUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media/upload/")
public class MediaUploadController implements MediaUploadControllerApi {

    @Autowired
    private MediaUploadService mediaUploadService;

    @PostMapping("register")
    @Override
    public ResponseResult register(@RequestParam("fileMd5") String fileMd5,
                                   @RequestParam("fileName") String fileName,
                                   @RequestParam("fileSize") String fileSize,
                                   @RequestParam("mimetype") String mimetype,
                                   @RequestParam("fileExt") String fileExt) {
        return mediaUploadService.register(fileMd5, fileName, fileSize, mimetype, fileExt);
    }

    @PostMapping("checkChunk")
    @Override
    public ResponseResult checkChunk(@RequestParam("fileMd5") String fileMd5, @RequestParam("chunk") Integer chunk,
                                     @RequestParam("chunkSize") Integer chunkSize) {
        return mediaUploadService.checkChunk(fileMd5, chunk, chunkSize);
    }

    @PostMapping("uploadChunk")
    @Override
    public ResponseResult uploadChunk(@RequestBody MultipartFile file, @RequestParam("chunk") Integer chunk,
                                      @RequestParam("fileMd5") String fileMd5) {
        return mediaUploadService.uploadChunk(file, chunk, fileMd5);
    }

    @PostMapping("mergeFile")
    @Override
    public ResponseResult mergeFile(@RequestParam("fileName") String fileName, @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("mimetype") String mimetype, @RequestParam("fileSize") Long fileSize,
                                    @RequestParam("fileExt") String fileExt) {
        return mediaUploadService.mergeFile(fileName, fileMd5, mimetype, fileSize, fileExt);
    }
}
