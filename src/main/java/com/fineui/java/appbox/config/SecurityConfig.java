package com.fineui.java.appbox.config;

import com.fineui.java.appbox.business.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * 安全配置（会话模式）：除登录页、静态资源、FineUI 运行时、帮助静态页、{@code /public/} 下的公开页外一律要求已登录；
 * 登录不用框架表单登录，而是登录页的按钮事件校验密码后经 {@code AuthService.login} 写入会话上下文。
 *
 * <p>未登录时按请求类型分流：普通页面 GET 重定向到 /login；回发 POST 返回 401，
 * 由客户端 {@code common.js} 的 {@code F.beforeAjaxError} 钩子提示并跳转登录页。
 * CSRF 令牌仓库由 fineui-java 按 {@code fineui.security-csrf} 提供，FineUI 运行时会在每次回发请求头里带上令牌。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
                                                   CsrfTokenRepository fineUICsrfTokenRepository) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(antMatcher("/login"), antMatcher("/res/**"), antMatcher("/F/**"),
                                antMatcher("/help/**"), antMatcher("/favicon.ico"), antMatcher("/error")).permitAll()
                        // 公开页：/public/ 下的页面一律无需登录，新增一个公开页只要把路由起在这个前缀下，本文件不用再改。
                        // 这一条同时覆盖首屏 GET 与回发 POST——回发请求发往页面自身的地址，所以匿名会话里搜索、翻页都能用。
                        .requestMatchers(antMatcher("/public/**")).permitAll()
                        // H2 控制台能直连数据库（改口令、改权限），只允许超级管理员；生产环境应直接关掉 spring.h2.console.enabled
                        .requestMatchers(PathRequest.toH2Console()).hasRole(AuthService.ROLE_ADMIN)
                        .anyRequest().authenticated())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(new PostbackAwareEntryPoint()))
                // 主框架页以 IFrame 嵌入各功能页：允许同源嵌入，保留跨站点击劫持防护
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .csrf(csrf -> csrf.csrfTokenRepository(fineUICsrfTokenRepository)
                        .ignoringRequestMatchers(PathRequest.toH2Console()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** 未登录：页面 GET 跳登录页；回发 POST 回 401 文本，交给客户端钩子处理。 */
    static final class PostbackAwareEntryPoint implements AuthenticationEntryPoint {

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("身份验证失败，请重新登录！");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
        }
    }
}
