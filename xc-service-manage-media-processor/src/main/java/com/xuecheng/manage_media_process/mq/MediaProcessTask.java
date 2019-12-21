package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessTask.class);

    @Value("${xc-service-manage-media.video-location}")
    private String serverPath;
    @Value("${xc-service-manage-media.ffmpeg-path}")
    private String ffmpegPath;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}",
    containerFactory = "customContainFactory")
    public void receiveMediaProcessTask(String msg){
        Map map = JSON.parseObject(msg, Map.class);
        LOGGER.info("receive mdeia msg:{}",msg);
        String mediaId = (String) map.get("mediaId");
        //判断数据库中是否有这条记录
        Optional<MediaFile> mediaFileOptional = mediaFileRepository.findById(mediaId);
        if (!mediaFileOptional.isPresent()) {
            return;
        }

        MediaFile mediaFile = mediaFileOptional.get();
        //媒资文件类型
        String fileType = mediaFile.getFileType();
        if (fileType == null || !fileType.equals("avi")){
            mediaFile.setFileStatus("303004");//无需处理
            mediaFileRepository.save(mediaFile);
        }else {
            mediaFile.setFileStatus("303001");//未处理
            mediaFileRepository.save(mediaFile);
        }
        //生成MP4
        String mp4_name = mediaFile.getFileId()+".mp4";
        String mp4folder_path = serverPath + mediaFile.getFilePath();
        String videoPath = serverPath + mediaFile.getFilePath() +mediaFile.getFileName();
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegPath, videoPath, mp4_name, mp4folder_path);
        String res = mp4VideoUtil.generateMp4();
        if ( res==null || !res.equals("success")) {
            //生成MP4失败,写入日志
            mediaFile.setFileStatus("303003");//操作失败
            MediaFileProcess_m3u8 m3u8 = new MediaFileProcess_m3u8();
            m3u8.setErrormsg(res);
            mediaFile.setMediaFileProcess_m3u8(m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //生成m3u8文件
        videoPath = serverPath + mediaFile.getFilePath() + mp4_name;
        String m3u8_name = mediaFile.getFileId()+".m3u8";
        String m3u8folder_path = serverPath+mediaFile.getFilePath()+"hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpegPath, videoPath, m3u8_name, m3u8folder_path);
        res = hlsVideoUtil.generateM3u8();
        if ( res==null || !res.equals("success")) {
            //生成MP4失败,写入日志
            mediaFile.setFileStatus("303003");//操作失败
            MediaFileProcess_m3u8 m3u8 = new MediaFileProcess_m3u8();
            m3u8.setErrormsg(res);
            mediaFile.setMediaFileProcess_m3u8(m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        mediaFile.setFileStatus("303002");//操作失败
        MediaFileProcess_m3u8 m3u8 = new MediaFileProcess_m3u8();
        m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(m3u8);
        //
        mediaFile.setFileUrl(mediaFile.getFilePath()+"hls/"+m3u8_name);
        mediaFileRepository.save(mediaFile);
    }

}
