package com.xuecheng.manage_course.controller;

import com.xuecheng.api.course.SysDictionaryControllerApi;
import com.xuecheng.framework.domain.system.SysDictionary;
import com.xuecheng.manage_course.dao.SysDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys/dictionary")
public class SysDictionaryController implements SysDictionaryControllerApi {

    @Autowired
    private SysDictionaryRepository sysDictionaryRepository;

    @Override
    @GetMapping("/get/{dType}")
    public SysDictionary findSysDictionaryByTYpe(@PathVariable("dType") String dType) {
        return sysDictionaryRepository.findBydType(dType);
    }
}
