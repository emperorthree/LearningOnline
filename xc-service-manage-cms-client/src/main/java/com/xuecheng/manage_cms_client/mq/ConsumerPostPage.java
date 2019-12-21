package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.service.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ConsumerPostPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerPostPage.class);
    @Autowired
    private CmsPageRepository cmsPageRepository;
    @Autowired
    private PageService pageService;

    @RabbitListener(queues = {"${xuecheng.mq.queue}"})
    public void postPage(String msg){
        //解析消息
        Map map = JSON.parseObject(msg, Map.class);
        if (map == null) {
            LOGGER.info("receive cms post page:{}"+msg);
        }
        String pageId = (String)map.get("pageId");
        Optional<CmsPage> op = cmsPageRepository.findById(pageId);
        if (!op.isPresent()){
            LOGGER.error("receive cms post page, page is null:{}" +msg);
            return ;
        }
        //将页面保存到服务器
        pageService.savePageToServicePath(pageId);
    }
}
