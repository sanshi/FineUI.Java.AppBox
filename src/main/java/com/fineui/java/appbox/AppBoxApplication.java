package com.fineui.java.appbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FineUI.Java.AppBox 通用权限管理系统启动类。
 *
 * <p>UI 由 fineui-java 库承担：方言 / 回发端点 / 页面路由扫描 / 内嵌 F.js 运行时全部经自动配置装配；
 * {@code @FineUIPage} 页面类在 {@code pages} 包下，由框架扫描器登记路由。数据访问用 Spring Data JPA + H2，
 * 登录认证用 Spring Security（会话模式），业务权限由本项目自建的 {@code @CheckPower} 三层校验负责。
 */
@SpringBootApplication
public class AppBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppBoxApplication.class, args);
    }
}
