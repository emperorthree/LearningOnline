package com.xuecheng.manage_cms;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {

    @Autowired
    private CmsPageRepository cmsPageRepository;
    @Autowired
    private GridFsTemplate gridFsTemplate;

    //分页查询
    @Test
    public void testFindPage(){
        int page= 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<CmsPage> all = cmsPageRepository.findAll(pageable);
        System.out.println(all);
    }

    @Test
    public void testFindAll(){
        List<CmsPage> all = cmsPageRepository.findAll();
        System.out.println(all);
    }

    //修改测试
    @Test
    public void update(){
        //查询出对象
        Optional<CmsPage> cmsPage = cmsPageRepository.findById("5abefd525b05aa293098fca6");
        CmsPage page = new CmsPage();
        if(cmsPage.isPresent()){
            page = cmsPage.get();
            page.setPageAliase("test001");
            CmsPage save = cmsPageRepository.save(page);
            System.out.println(save);
        }
    }

    //自定义条件查询
    @Test
    public void testFindAll1(){
        //条件匹配器
        ExampleMatcher matching = ExampleMatcher.matching();
        matching = matching.withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
//        ExampleMatcher.GenericPropertyMatchers.exact();
        //页面别名模糊查询,需要自定义字符串的匹配器实现模糊查询
//        ExampleMatcher.GenericPropertyMatchers.之后包含很多条件(以xx开头,结尾,忽略大小写)
        CmsPage cmsPage = new CmsPage();
        cmsPage.setPageAliase("课程预览页面");
//        cmsPage.setPageWebPath("/coursepre/");
        Example<CmsPage> example = Example.of(cmsPage,matching);
        Pageable pageable = new PageRequest(0,10);
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        System.out.println(all);
    }



/*  3 3
    1 100
    10 1000
    1000000000 1001
    9 10 1000000000*/
    @Test
    public void testOut(){
        Scanner in = new Scanner(System.in);
        int works = in.nextInt();
        int boys = in.nextInt();
        while (works > 0) {
            
        }
    }
    @Test
    public void test2() throws FileNotFoundException {
        File file = new File("e:/course.ftl");
        FileInputStream inputStream = new FileInputStream(file);
        final ObjectId objectId = gridFsTemplate.store(inputStream, "课程详情模板", "");
        String fileId = objectId.toHexString();
        System.out.println(fileId);//5d85cce33e5e900d3cb48f37
    }

}