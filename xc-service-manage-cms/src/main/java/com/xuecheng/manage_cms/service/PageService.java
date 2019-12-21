package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.ext.CmsPostPageResult;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFSBucket gridFSBucket;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CmsSiteRepository cmsSiteRepository;
    /**
     * 分页查询
     * @param page 页数
     * @param size 每页显示
     * @param request 查询条件
     * @return 页面列表
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest request){
        if(request == null){
            request = new QueryPageRequest();
        }
        //条件匹配器
        //页面名称模糊查询需要自定义匹配器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
        //条件值
        CmsPage cmsPage = new CmsPage();
        if (StringUtils.isNotEmpty(request.getPageId())){
            cmsPage.setPageId(request.getPageId());
        }
        if (StringUtils.isNotEmpty(request.getPageAliase())){
            cmsPage.setPageAliase(request.getPageAliase());
        }
        if (StringUtils.isNotEmpty(request.getSiteId())){
            cmsPage.setSiteId(request.getSiteId());
        }
        //创建条件实例
        Example<CmsPage> example = Example.of(cmsPage, matcher);

        if(page <=0 ){
            page = 1;
        }
        page = page - 1;
        if(size <= 0 ){
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<CmsPage> list = cmsPageRepository.findAll(example, pageable);
        QueryResult<CmsPage> pageQueryResult = new QueryResult<>();
        pageQueryResult.setList(list.getContent());
        pageQueryResult.setTotal(list.getTotalPages());

        return new QueryResponseResult(CommonCode.SUCCESS, pageQueryResult);
    }

    /**
     * 页面添加
     * @param cmsPage 待添加页面
     * @return 基本数据加cmspage
     */
    public CmsPageResult add(CmsPage cmsPage){
        //检查页面是否已经存在
        CmsPage queryCms = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(),
                cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (queryCms != null){

            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);

//            return new CmsPageResult(CommonCode.FAIL,null);
        }
        //页面不存在加入数据库
        CmsPage savePage = cmsPageRepository.save(cmsPage);
        CmsPageResult result = new CmsPageResult(CommonCode.SUCCESS, savePage);
        return result;
    }

    /**
     * 根据id查询cmsPage
     * @param pageId id
     * @return cmsPage
     */
    public CmsPage findById(String pageId){
        Optional<CmsPage> byId = cmsPageRepository.findById(pageId);
        if (byId.isPresent()){
            return byId.get();
        }
        return null;
    }

    /**
     * 页面修改
     * @param pageId 修改页面id
     * @param cmsPage 页面对象
     * @return .
     */
    public CmsPageResult edit(String pageId, CmsPage cmsPage){
        CmsPage byId = this.findById(pageId);
        if (byId != null) {
            byId.setTemplateId(cmsPage.getTemplateId());
            byId.setPageAliase(cmsPage.getPageAliase());
            byId.setPageWebPath(cmsPage.getPageWebPath());
            byId.setSiteId(cmsPage.getSiteId());
            byId.setPageName(cmsPage.getPageName());
            byId.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            byId.setDataUrl(cmsPage.getDataUrl());
            CmsPage save = cmsPageRepository.save(byId);
            if (save != null) {
                return new CmsPageResult(CommonCode.SUCCESS, save);
            }
        }
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    public CmsPageResult delete(String pageId){
        cmsPageRepository.deleteById(pageId);
        return new CmsPageResult(CommonCode.SUCCESS,null);
    }

    /**
     * 页面静态化
     * @param pageId 页面ID
     * @return .
     */
    public String getPageHtml(String pageId){
        //获取页面模板数据
        Map pageDataById = this.getPageDataById(pageId);
        //获取页面模板
        String ftlTem = this.getTempolateById(pageId);
        //执行静态化
        String htmlTem = this.genHtml(ftlTem, pageDataById);
        return htmlTem;
    }

    /**
     * 获取模板需要的数据
     * @param pageId id
     * @return map
     */
    public Map getPageDataById(String pageId){

        CmsPage byId = this.findById(pageId);
        if (byId == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String dataUrl = byId.getDataUrl();
        if(!StringUtils.isNotEmpty(dataUrl)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        //请求url接口得到数据
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;
    }

    /**
     * 获取模板
     * @return
     */
    public String getTempolateById(String pageId)  {
        //通过页面id查出页面对象后获得模板id
        CmsPage byId = this.findById(pageId);
        if (byId == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String templateId = byId.getTemplateId();
        //通过模板id查出模板对象得到模板文件id
        if (org.springframework.util.StringUtils.isEmpty(templateId)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> templateById = cmsTemplateRepository.findById(templateId);
        //通过模板文件id找到文件
        if (!templateById.isPresent()){
            ExceptionCast.cast(CmsCode.CMS_TEMPLATEINFO_NOTEXISTS);
        }
        String templateFileId = templateById.get().getTemplateFileId();
        //根据id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
        //打开下载流对象
        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //获取流对象
        GridFsResource resource = new GridFsResource(gridFSFile, downloadStream);
        try {
            String html = IOUtils.toString(resource.getInputStream(), "utf-8");
            return html;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 执行静态化
     * @param html
     * @param model
     * @return
     */
    public String genHtml(String html, Map model){

        try {
            //生成配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", html);
            //配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template newTemplate = configuration.getTemplate("template");
            String htmlTemp = FreeMarkerTemplateUtils.processTemplateIntoString(newTemplate, model);
            return htmlTemp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发布页面
     * @param pageId 页面id
     * @return res
     */
    public ResponseResult postPage(String pageId) {
        //执行静态化
        String pageHtml = this.getPageHtml(pageId);
        if (StringUtils.isEmpty(pageHtml)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        //保存静态化文件
        CmsPage cmsPage = this.saveHtml(pageId, pageHtml);
        //mq发送消息
        this.sendPostPage(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 向mq发送消息
     * @param pageId
     */
    private void sendPostPage(String pageId){
        Optional<CmsPage> op = cmsPageRepository.findById(pageId);
        if (!op.isPresent()) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        CmsPage cmsPage = op.get();
        Map<String, String> map = new HashMap<>();
        map.put("pageId", pageId);
        String msg = JSON.toJSONString(map);
        //获取站点id作为routingkey
        String routingKey = cmsPage.getSiteId();
        //发布消息
        this.rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE, routingKey, msg);
    }

    /**
     * 保存静态页面内容
     * @param pageId
     * @param content
     * @return
     */
    private CmsPage saveHtml(String pageId, String content) {
        //查询页面
        Optional<CmsPage> op = cmsPageRepository.findById(pageId);
        if (!op.isPresent()) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        CmsPage cmsPage = op.get();
        //存储之前先删除
        String htmlFileId = cmsPage.getHtmlFileId();
        if (StringUtils.isNotEmpty(htmlFileId)) {
            gridFsTemplate.delete(Query.query(Criteria.where("_id").is(htmlFileId)));
        }
        //保存HTML文件到gridfs
        InputStream inputStream = IOUtils.toInputStream(content);
        ObjectId objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        String fileId = objectId.toHexString();
        //将文件id存储到cmspage中
        cmsPage.setHtmlFileId(fileId);
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }
    //添加页面，如果已存在则更新页面 
    public CmsPageResult save(CmsPage cmsPage) {
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (cmsPage1 != null) {
            return this.edit(cmsPage1.getPageId(), cmsPage);
        }else {
            return this.add(cmsPage);
        }

    }

    //一键发布
    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {

        CmsPageResult save = this.save(cmsPage);
        if (!save.isSuccess()) {
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        CmsPage cmsPage1 = save.getCmsPage();
        ResponseResult result = this.postPage(cmsPage1.getPageId());
        if (!result.isSuccess()) {
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }

        //拼接url
        Optional<CmsSite> op = cmsSiteRepository.findById(cmsPage1.getSiteId());
        CmsSite cmsSite = op.orElse(null);
        String pageUrl = cmsSite.getSiteDomain() + cmsSite.getSiteWebPath() + cmsPage1.getPageWebPath() + cmsPage1.getPageName();

        return new CmsPostPageResult(CommonCode.SUCCESS, pageUrl);

    }

}
