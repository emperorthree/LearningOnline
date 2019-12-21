package com.xuecheng.api.course;

import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CoursePublishResult;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.ApiOperation;

public interface CourseControllerApi {
    @ApiOperation("课程计划查询")
    TeachplanNode findTeachPlanList(String courseId);
    @ApiOperation("添加课程计划")
    ResponseResult addTeachplan(Teachplan teachplan);
    @ApiOperation("查询我的课程")
    QueryResponseResult getMyCourse(int page, int size, CourseListRequest courseListRequest);
    @ApiOperation("新增课程")
    ResponseResult addCourse(CourseBase courseBase);
    @ApiOperation("课程查询通过id")
    CourseBase getCourseById(String id);
    @ApiOperation("更新课程信息")
    ResponseResult updateCourse(String courseId, CourseBase courseBase);
    @ApiOperation("根据课程id查询课程营销信息")
    CourseMarket getCourseMarketById(String courseId);
    @ApiOperation("更新课程营销信息")
    ResponseResult updateCourseMarket(String courseId, CourseMarket courseMarket);
    @ApiOperation("添加课程图片")
    ResponseResult addCoursePic(String courseId, String pic);
    @ApiOperation("删除课程图片")
    ResponseResult deleteCoursePic(String courseId);
    @ApiOperation("查询课程图片")
    CoursePic findCoursePic(String courseId);
    @ApiOperation("课程视图的查询")
    CourseView findCourseView(String id);
    @ApiOperation("预览课程")
    public CoursePublishResult preview(String id);
    @ApiOperation("发布课程")
    public CoursePublishResult publish(String id);
    @ApiOperation("保存媒资信息")
    public ResponseResult saveMedia(TeachplanMedia teachplanMedia);

}
