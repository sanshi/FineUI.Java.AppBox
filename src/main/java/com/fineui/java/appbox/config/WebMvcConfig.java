package com.fineui.java.appbox.config;

import com.fineui.java.appbox.business.AppBoxInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册页面请求拦截器：静态资源、FineUI 运行时、帮助静态页、错误页、H2 控制台不经过它。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppBoxInterceptor appBoxInterceptor;

    public WebMvcConfig(AppBoxInterceptor appBoxInterceptor) {
        this.appBoxInterceptor = appBoxInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appBoxInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/res/**", "/F/**", "/help/**", "/error", "/h2-console/**", "/favicon.ico");
    }
}
