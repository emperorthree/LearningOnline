package com.xuecheng.manage_cms.dao;

import com.xuecheng.framework.domain.cms.CmsPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

//使用spring-data-mongodb提供的一套快捷操作完成mongodb数据库的使用,继承MongoRepository并指定实体类和主键类型
public interface CmsPageRepository extends MongoRepository<CmsPage, String> {
    //自定义一些方法
    //同Spring Data JPA一样Spring Data mongodb也提供自定义方法的规则
    // 如下： 按照ﬁndByXXX，ﬁndByXXXAndYYY、countByXXXAndYYY等规则定义方法，实现查询操作。

    //根据页面名称查询
    CmsPage findByPageName(String pageName);
    //根据页面名称和页面类型查询
    CmsPage findByPageNameAndSiteIdAndPageWebPath(String pageName, String siteId, String pageWebPath);
    //根据站点id和页面类型查询记录数
    int countBySiteIdAndPageType(String siteId, String pageType);
    //分页查询
    Page<CmsPage> findBySiteIdAndPageType(String siteId, String pageType, Pageable pageable);
}
