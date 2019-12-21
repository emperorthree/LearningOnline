package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUploadService.class);

    @Value("${xc-service-manage-media.upload-location}")
    private String uploadPath;
    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    String routingkey_media_video;

    //获取上传文件路径
    private String getFilePath(String fileMd5, String fileExt){
        return uploadPath+"/"+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }
    //获得文件所在目录
    private String getFileFolder(String fileMd5){
        return uploadPath+"/"+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
    }

    //创建长传文件路径
    private boolean createFolder(String fileMd5){
        String fileFolderPath = getFileFolder(fileMd5);
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            return fileFolder.mkdirs();
        }
        return true;
    }
    //文件上传注册
    public ResponseResult register(String fileMd5, String fileName,
                                   String fileSize, String mimetype, String fileExt) {

        //检查文件是否上传
        //1.得到文件的路径
        String filePath = getFilePath(fileMd5, fileExt);
        File file = new File(filePath);
        //2.查询数据库文件是否存在
        final Optional<MediaFile> fileOptional = mediaFileRepository.findById(fileMd5);

        if (file.exists() && fileOptional.isPresent()){
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }

        boolean folder = createFolder(fileMd5);
        if (!folder) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_FAIL);
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    //检查分块
    public CheckChunkResult checkChunk(String fileMd5, Integer chunk, Integer chunkSize) {

        //获取块文件所在目录
        String chunkPath = getFileFolder(fileMd5) + "/" + "chunks" + "/";
        File chunkFile = new File(chunkPath+chunk);
        if (chunkFile.exists()) {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, true);
        }else{
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, false);
        }

    }

    //上传分块
    public ResponseResult uploadChunk(MultipartFile file, Integer chunk, String fileMd5) {
        if(file==null){
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_FAIL);
        }
        //创建块文件目录
        File chunkFolder = new File(getFileFolder(fileMd5) + "/" + "chunks" + "/");
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        String chunkPath = getFileFolder(fileMd5) + "/" + "chunks" + "/" +chunk;
        //块文件---空
        File chunkFile = new File(chunkPath);

        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = file.getInputStream();
            fileOutputStream = new FileOutputStream(chunkFile);
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("chunk file upload fail:{}",e.getMessage());
            ExceptionCast.cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //合并分块
    public ResponseResult mergeFile(String fileName, String fileMd5, String mimetype, Long fileSize, String fileExt) {
        //1.将文件块合并
        String chunkPath = getFileFolder(fileMd5) + "/" + "chunks" + "/";
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()){
            chunkFolder.mkdirs();
        }
        //合并文件路径
        File mergeFile = new File(getFilePath(fileMd5, fileExt));
        if (mergeFile.exists()){
            mergeFile.delete();
        }
        boolean newFile = false;
        try {
            newFile = mergeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        List<File> chunkFiles = getChunkFiles(chunkFolder);
        //合并文件
        mergeFile = mergeFile(mergeFile, chunkFiles);
        if (mergeFile == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //2.校验MD5
        boolean flag = checkFileMd5(mergeFile, fileMd5);
        if (!flag) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //3.向mongodb中加入信息
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        mediaFile.setFileOriginalName(fileName);
        mediaFile.setFilePath(fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/");
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);
        sendProcessVideoMsg(mediaFile.getFileId());

        return new ResponseResult(CommonCode.SUCCESS);
    }

    //按顺序获取块文件列表
    private List<File> getChunkFiles(File chunkFileFolder){

        File[] files = chunkFileFolder.listFiles();
        List<File> chunkFileList = new ArrayList<>();
        chunkFileList.addAll(Arrays.asList(files));
        Collections.sort(chunkFileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }else {
                    return -1;
                }
            }
        });
        return chunkFileList;
    }
    //校验文件MD5值
    private boolean checkFileMd5(File mergeFile, String md5){
        if (mergeFile == null || StringUtils.isEmpty(md5)){
            return false;
        }

        FileInputStream mergeInputStream = null;
        try {
            mergeInputStream = new FileInputStream(mergeFile);
            String mergeMd5 = DigestUtils.md5Hex(mergeInputStream);
            if (md5.equalsIgnoreCase(mergeMd5)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("check file md5 fail:{}",e.getMessage());
        } finally {
            try {
                mergeInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    //合并文件
    private File mergeFile(File mergeFile, List<File> chunkFiles){

        try {
            RandomAccessFile write = new RandomAccessFile(mergeFile, "rw");
            byte[] b = new byte[1024];
            for (File chunks:chunkFiles){
                RandomAccessFile read = new RandomAccessFile(chunks, "r");
                int len = -1;
                while ((len = read.read(b)) != -1){
                    write.write(b, 0, len);
                }
                read.close();
            }
            write.close();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("merge file fail:{}",e.getMessage());
            return null;
        }
        return mergeFile;
    }

    //向mq发送消息
    private ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> mediaFileOptional = mediaFileRepository.findById(mediaId);
        if (!mediaFileOptional.isPresent()) {
            return new ResponseResult(CommonCode.FAIL);
        }
        MediaFile mediaFile = mediaFileOptional.get();

        Map<String, String> mapMsg = new HashMap<>();
        mapMsg.put("mediaId", mediaId);
        String msg = JSON.toJSONString(mapMsg);
        this.rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK, routingkey_media_video, msg);
        LOGGER.info("send media process msg:{}",msg);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
