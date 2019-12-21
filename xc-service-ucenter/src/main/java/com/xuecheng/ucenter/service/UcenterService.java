package com.xuecheng.ucenter.service;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UcenterService {

    @Autowired
    private XcUserRepository xcUserRepository;
    @Autowired
    private XcCompanyUserRepository xcCompanyUserRepository;

    public XcUserExt getUserExt(String username) {

        XcUser user = xcUserRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        XcCompanyUser companyUser = xcCompanyUserRepository.findByUserId(xcUserExt.getId());
        if (companyUser != null){
            xcUserExt.setCompanyId(companyUser.getCompanyId());
        }

        return xcUserExt;
    }
}
