package com.xinghuo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.xinghuo.guide.XingweiGuideWorkspaceService;

@Controller
public class XingweiGuidePageController {
    private final XingweiGuideWorkspaceService workspace;
    public XingweiGuidePageController(XingweiGuideWorkspaceService workspace){this.workspace=workspace;}
    @GetMapping("/admin/xingwei/guides")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:view')")
    String page(Model model,CsrfToken csrf,Authentication actor){model.addAttribute("csrf",csrf);model.addAttribute("username",actor.getName());model.addAttribute("xingweiMode",true);return "admin-guides";}
    @GetMapping("/admin/xingwei/guides/{id}/preview")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:view')")
    String preview(@PathVariable long id,Model model,Authentication actor){
        var detail=workspace.read(id,actor).detail();var guide=detail.guide();
        model.addAttribute("guide",guide);model.addAttribute("sections",detail.sections());model.addAttribute("products",detail.products());model.addAttribute("faqs",detail.faqs());model.addAttribute("relatedGuides",detail.relatedGuides());
        model.addAttribute("previewMode",true);model.addAttribute("canonicalUrl","https://wydlgf.cn/guides/"+guide.slug());model.addAttribute("pageTitle","受保护预览 · "+guide.title());model.addAttribute("pageDescription",guide.summary());model.addAttribute("ogImage",guide.coverImage());
        return "guide";
    }
}
