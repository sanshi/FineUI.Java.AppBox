package com.fineui.java.appbox.pages.pub;

import com.fineui.java.binding.Display;
import com.fineui.java.binding.DisplayFormat;

import java.time.LocalDateTime;

/**
 * 公告行模型：公开页的表格行与详情内容。
 *
 * <p>本示例的公告数据由 {@link NoticeData} 提供的内存列表充当，所以这里是普通 POJO 而不是数据库实体
 * （示例的目的是演示「免登录页面」，不想让读者为了跑通它先去建一张表）。真实项目把它换成实体、
 * 由仓库接口查库即可，公开页读数据库与需要登录的页面没有任何区别。
 */
public class Notice {

    @Display(name = "标题")
    private String title;

    @Display(name = "发布时间")
    @DisplayFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime publishTime;

    @Display(name = "发布部门")
    private String department;

    /** 正文：只在详情页显示，列表页不取此列。 */
    private String content;

    private Integer id;

    public Notice() {
    }

    public Notice(Integer id, String title, LocalDateTime publishTime, String department, String content) {
        this.id = id;
        this.title = title;
        this.publishTime = publishTime;
        this.department = department;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
