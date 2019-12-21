package com.xuecheng.manage_cms;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RestTemplateTest {

    @Autowired
    GridFsTemplate gridFsTemplate;
    @Autowired
    GridFSBucket gridFSBucket;
    @Autowired
    RestTemplate restTemplate;

    @Test
    public void testRestTemplate(){
        final ResponseEntity<Map> forEntity = restTemplate.getForEntity("www.baidu.com/ss", Map.class);
        System.out.println(forEntity);
    }

    //存文件
    @Test
    public void testGridfs() throws FileNotFoundException {
        File file = new File("e:/index_banner.ftl");
        FileInputStream in = new FileInputStream(file);
        ObjectId objectId = gridFsTemplate.store(in, "测试模板0001");
        String fileid = objectId.toString();
        System.out.println(fileid);

    }
    //取文件
    @Test
    public void testgridsBucket() throws IOException {
        String fileId = "5d47b59d5bf6130f8c00e24c";
        //根据id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        //打开下载流对象
        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //获取流对象
        GridFsResource resource = new GridFsResource(gridFSFile, downloadStream);
        String s = IOUtils.toString(resource.getInputStream(), "utf-8");
        System.out.println(s);
    }
}
