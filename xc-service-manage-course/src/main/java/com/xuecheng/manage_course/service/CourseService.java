package com.xuecheng.manage_course.service;

import com.alibaba.druid.support.json.JSONUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.ext.CmsPostPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CoursePublishResult;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    CourseBaseRepository courseBaseRepository;
    @Autowired
    TeachplanRepository teachplanRepository;
    @Autowired
    CourseMapper courseMapper;
    @Autowired
    CourseMarketRepository courseMarketRepository;
    @Autowired
    CoursePicRepository coursePicRepository;
    @Autowired
    CmsPageClient cmsPageClient;
    @Autowired
    CoursePubRepository coursePubRepository;
    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;
    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course‐publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course‐publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course‐publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course‐publish.siteId}")
    private String publish_siteId;
    @Value("${course‐publish.templateId}")
    private String publish_templateId;
    @Value("${course‐publish.previewUrl}")
    private String previewUrl;

    //查询课程计划
    public TeachplanNode findTeachplanList(String courseId){
        return teachplanMapper.selectList(courseId);
    }

    //得到或则创建课程根节点
    public String getTeachplanRoot(String courseId){

        //查看课程是否存在
        Optional<CourseBase> op = courseBaseRepository.findById(courseId);
        if (!op.isPresent()) {
            return null;
        }
        CourseBase courseBase = op.get();

        //取出课程计划根节点
        List<Teachplan> teachplanRoot
                = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        if (teachplanRoot == null || teachplanRoot.size() == 0) {
            //新增一个节点
            Teachplan root = new Teachplan();
            root.setCourseid(courseId);
            root.setGrade("1");
            root.setPname(courseBase.getName());
            root.setParentid("0");
            root.setStatus("0");
            teachplanRepository.save(root);
            return root.getId();
        }
        Teachplan teachplan = teachplanRoot.get(0);
        return  teachplan.getId();
    }

    /**
     * 添加课程计划
     * @param teachplan plan
     * @return res
     */
    public ResponseResult addTeachplan(Teachplan teachplan){

        if (teachplan == null ||
                StringUtils.isEmpty(teachplan.getCourseid()) ||
                StringUtils.isEmpty(teachplan.getPname())) {
            ExceptionCast.cast(CommonCode.FAIL);
        }

        String courseId = teachplan.getCourseid();
        String parentId = teachplan.getParentid();

        //如果没有填写父节点
        if (StringUtils.isEmpty(parentId)) {
            parentId = getTeachplanRoot(courseId);
        }

        //获取父节点的级别
        Optional<Teachplan> op = teachplanRepository.findById(parentId);
        if (!op.isPresent()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        Teachplan teachplanParent = op.get();
        String grade = teachplanParent.getGrade();

        //设置新增节点的内容
        teachplan.setParentid(parentId);
        teachplan.setStatus("0");

        //根据父节点层级设置当前节点级别
        if (grade.equals("1")) {
            teachplan.setGrade("2");
        }else if (grade.equals("2")) {
            teachplan.setGrade("3");
        }

        teachplan.setCourseid(teachplanParent.getCourseid());
        teachplanRepository.save(teachplan);

        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 课程查询分页
     * @param page page
     * @param size size
     * @param courseListRequest 查询条件
     * @return list
     */
    public QueryResponseResult findMyCourseList(int page, int size, CourseListRequest courseListRequest){

        if (courseListRequest == null) {
            courseListRequest = new CourseListRequest();
        }
        if (page<0) {
            page = 0;
        }
        if (size<=0) {
            size = 20;
        }
        PageHelper.startPage(page, size);
        Page<CourseInfo> myCourseList = courseMapper.findMyCourseList(courseListRequest);
        List<CourseInfo> result = myCourseList.getResult();
        long total = myCourseList.getTotal();
        QueryResult<CourseInfo> courseInfoQueryResult = new QueryResult<>();
        courseInfoQueryResult.setList(result);
        courseInfoQueryResult.setTotal(total);
        return new QueryResponseResult(CommonCode.SUCCESS, courseInfoQueryResult);
    }

    /**
     * 课程新增-未完善的
     * @param courseBase 课程信息
     * @return 结果
     */
    public ResponseResult addCourse(CourseBase courseBase){
        courseBaseRepository.save(courseBase);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 根据id查询课程
     * @param id 课程id
     * @return 课程基本信息
     */
    public CourseBase getCourseById(String id) {
        Optional<CourseBase> op = courseBaseRepository.findById(id);
        return op.orElse(null);
    }


    /**
     * 课程修改
     * @param courseId 课程号
     * @param courseBase 课程信息
     * @return 结果
     */
    @Transactional
    public ResponseResult updateCourse(String courseId, CourseBase courseBase){

        CourseBase courseById = getCourseById(courseId);
        if (courseById == null) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        courseById.setDescription(courseBase.getDescription());
        courseById.setGrade(courseBase.getGrade());
        courseById.setMt(courseBase.getMt());
        courseById.setName(courseBase.getName());
        courseById.setSt(courseBase.getSt());
        courseById.setStudymodel(courseBase.getStudymodel());
        courseById.setUsers(courseBase.getUsers());

        courseBaseRepository.save(courseById);

        return new ResponseResult(CommonCode.SUCCESS);


    }

    /**
     * 根据id查询课程营销信息
     * @param courseId 课程id
     * @return 课程营销类
     */
    public CourseMarket getCourseMarketByid(String courseId){
        Optional<CourseMarket> op = courseMarketRepository.findById(courseId);
        return op.orElse(null);
    }

    /**
     * 更新课程营销信息
     * @param courseId id
     * @param courseMarket class
     * @return res
     */
    public ResponseResult updateCourseMarket(String courseId, CourseMarket courseMarket){
        CourseMarket courseMarketByid = getCourseMarketByid(courseId);
        if (courseMarketByid == null) {
            courseMarketRepository.save(courseMarket);
        }else {
            courseMarketByid.setCharge(courseMarket.getCharge());
            courseMarketByid.setPrice(courseMarket.getPrice());
            courseMarketByid.setValid(courseMarket.getValid());
            courseMarketByid.setStartTime(courseMarket.getStartTime());
            courseMarketByid.setEndTime(courseMarket.getEndTime());
            courseMarketByid.setQq(courseMarket.getQq());

            courseMarketRepository.save(courseMarketByid);
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 保存图片id到对应课程id
     * @param courseId cid
     * @param pic pid
     * @return res
     */
    @Transactional
    public ResponseResult addCoursePic(String courseId, String pic) {

        Optional<CoursePic> op = coursePicRepository.findById(courseId);
        CoursePic coursePic = null;
        if (op.isPresent()) {
            coursePic = op.get();
        }
        if (coursePic == null) {
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);

        return new ResponseResult(CommonCode.SUCCESS);
    }
    //删除课程图片
    @Transactional
    public ResponseResult deleteCoursePic(String courseId){
        if (StringUtils.isEmpty(courseId)) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_BUSINESSISNULL);
        }
        long l = coursePicRepository.deleteByCourseid(courseId);
        if (l > 0) {
            return new ResponseResult(CommonCode.SUCCESS);
        }else {
            return new ResponseResult(CommonCode.FAIL);
        }

    }

    //查询图片
    public CoursePic findCoursePic(String courseId){

        if (StringUtils.isEmpty(courseId)) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_BUSINESSISNULL);
        }
        Optional<CoursePic> op = coursePicRepository.findById(courseId);
        return op.orElse(null);
    }

    //课程视图的查询

    public CourseView findCourseView(String id){

        CourseView courseView = new CourseView();
        if (StringUtils.isEmpty(id)) {
            return courseView;
        }
        final Optional<CoursePic> opPic = coursePicRepository.findById(id);
        if (opPic.isPresent()) {
            courseView.setCoursePic(opPic.get());
        }
        final Optional<CourseMarket> opMarket = courseMarketRepository.findById(id);
        if (opMarket.isPresent()) {
            courseView.setCourseMarket(opMarket.get());
        }
        final Optional<CourseBase> opBase = courseBaseRepository.findById(id);
        if (opBase.isPresent()) {
            courseView.setCourseBase(opBase.get());
        }
        final TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplan(teachplanNode);

        return courseView;
    }

    //课程详情页预览
    public CoursePublishResult preview(String id) {
        Optional<CourseBase> op = courseBaseRepository.findById(id);
        if (!op.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        CourseBase courseBase = op.get();
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setTemplateId(publish_templateId);
        cmsPage.setDataUrl(publish_dataUrlPre + id);
        cmsPage.setPageName(id+".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //远程请求保存数据
        CmsPageResult res = cmsPageClient.save(cmsPage);
        if (!res.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        //页面id
        String pageId = res.getCmsPage().getPageId();
        //页面url
        String pageUrl = previewUrl + pageId;
        return new CoursePublishResult(CommonCode.SUCCESS, pageUrl);
    }

    //课程发布
    @Transactional
    public CoursePublishResult publish(String id){

        Optional<CourseBase> op = courseBaseRepository.findById(id);
        if (!op.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        CourseBase courseBase = op.get();

        //页面发布
        CmsPostPageResult cmsPostPageResult = this.publishPage(courseBase, id);
        if (!cmsPostPageResult.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        //修改课程发布状态
        this.updatePubState(courseBase);

        //添加课程索引表
        CoursePub coursePub = createCoursePub(id);
        final CoursePub coursePub1 = saveCoursePub(id, coursePub);
        if (coursePub1 == null) {
            ExceptionCast.cast(CourseCode.COURSE_CREATE_INDEX_FILE);
        }

        //保存课程计划媒资信息
        saveTeachplanMediaPub(id);

        return new CoursePublishResult(CommonCode.SUCCESS, cmsPostPageResult.getPageUrl());
    }

    //保存coursePub
    private CoursePub saveCoursePub(String courseId, CoursePub coursePub){

        if (StringUtils.isEmpty(courseId)) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        CoursePub newCoursePub = null;
        Optional<CoursePub> op = coursePubRepository.findById(courseId);
        if (op.isPresent()) {
            newCoursePub = op.get();
        }
        if (newCoursePub == null){
            newCoursePub = new CoursePub();
        }

        BeanUtils.copyProperties(coursePub, newCoursePub);

        newCoursePub.setId(courseId);
        newCoursePub.setTimestamp(new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        newCoursePub.setPubTime(simpleDateFormat.format(new Date()));
        coursePubRepository.save(newCoursePub);

        return newCoursePub;
    }

    //创建coursePub
    private CoursePub createCoursePub(String courseId) {

        CoursePub coursePub = new CoursePub();
        coursePub.setId(courseId);

        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(courseId);
        if (courseBaseOptional.isPresent()) {
            BeanUtils.copyProperties(courseBaseOptional.get(), coursePub);
        }

        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(courseId);
        if (courseMarketOptional.isPresent()) {
            BeanUtils.copyProperties(courseMarketOptional.get(), coursePub);
        }

        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(courseId);
        if (coursePicOptional.isPresent()) {
            BeanUtils.copyProperties(coursePicOptional.get(), coursePub);
        }

        TeachplanNode teachplanNode = teachplanMapper.selectList(courseId);
        String teachplanString = JSONUtils.toJSONString(teachplanNode);
        coursePub.setTeachplan(teachplanString);

        return coursePub;

    }

    //修改课程发布状态
    private void updatePubState(CourseBase courseBase) {

        courseBase.setStatus("202002");

        courseBaseRepository.save(courseBase);
    }


    //页面发布
    private CmsPostPageResult publishPage(CourseBase courseBase, String id){

        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setTemplateId(publish_templateId);
        cmsPage.setDataUrl(publish_dataUrlPre + id);
        cmsPage.setPageName(id+".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        return cmsPageClient.postPageQuick(cmsPage);
    }

    //保存媒资信息
    public ResponseResult saveMedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia == null){
            ExceptionCast.cast(CommonCode.INVALIDPARAM);
        }
        //课程计划
        String teachPlanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> op = teachplanRepository.findById(teachPlanId);
        if (!op.isPresent()) {
             ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_ISNULL);
        }
        Teachplan teachplan = op.get();
        //只允许为叶子节点的课程计划添加视频文件
        String grade = teachplan.getGrade();
        if (StringUtils.isEmpty(grade) || !grade.equals("3")){
            ExceptionCast.cast(CourseCode.COURSE_MEDIS_TEACHPLAN_GRADE_ERROR);
        }
        TeachplanMedia one = null;
        final Optional<TeachplanMedia> optional = teachplanMediaRepository.findById(teachPlanId);
/*        if (!optional.isPresent()){
            one = new TeachplanMedia();
        }else {
            one = optional.get();
        }*/
        one = optional.orElse(new TeachplanMedia());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setCourseId(teachplanMedia.getCourseId());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        one.setTeachplanId(teachPlanId);
        teachplanMediaRepository.save(one);

        return new ResponseResult(CommonCode.SUCCESS);
    }

    //保存课程计划媒资信息
    private void saveTeachplanMediaPub(String courseId){
        //查询媒资信息
        List<TeachplanMedia> byCourseId = teachplanMediaRepository.findByCourseId(courseId);
        //将媒资课程计划信息存储在待索引表
        List<TeachplanMediaPub> list = new ArrayList<>();
        for (TeachplanMedia teachplanMedia : byCourseId){
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia, teachplanMediaPub);
            teachplanMediaPub.setTimestamp(new Date());
            list.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(list);
    }
}
