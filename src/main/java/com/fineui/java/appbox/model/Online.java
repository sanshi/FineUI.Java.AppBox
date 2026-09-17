package com.fineui.java.appbox.model;

import com.fineui.java.binding.Display;
import com.fineui.java.binding.DisplayFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** 在线用户记录：登录时登记（IP + 登录时间），之后每次请求按节流间隔刷新最后操作时间。每个用户至多一条。 */
@Entity
@Table(name = "onlines")
public class Online implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Display(name = "IP地址")
    @Column(length = 50)
    private String ipAddress;

    @Display(name = "登录时间")
    @DisplayFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Display(name = "最后操作时间")
    @DisplayFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updateTime;

    @Display(name = "用户")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 用户被删除时随之删掉其在线记录（数据库级联），否则删用户会被外键拦住
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /** 用户名（由只读导航派生，供表格列直接绑定）。 */
    @Display(name = "用户名")
    public String getUserName() {
        return user == null ? null : user.getName();
    }

    /** 用户中文名（由只读导航派生，供表格列直接绑定）。 */
    @Display(name = "中文名")
    public String getUserChineseName() {
        return user == null ? null : user.getChineseName();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
