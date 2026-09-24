package com.fineui.java.appbox.business;

import com.fineui.java.core.PageRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 页面请求拦截器（首屏 GET 与回发 POST 都经过）：
 * <ol>
 *   <li>已登录用户手工访问登录页 → 直接跳首页；</li>
 *   <li>已登录用户的每次请求 → 刷新在线记录（按节流间隔写库）；</li>
 *   <li>页面类上有 {@link CheckPower} → 校验当前用户权限：首屏不通过输出提示页，回发不通过返回 403
 *       （客户端 {@code common.js} 的 {@code F.beforeAjaxError} 钩子据此弹出「您无权进行此操作！」）。</li>
 * </ol>
 * 页面级校验只是第一道闸；各页的按钮可用状态（控件级）与删除/保存事件开头的二次校验（操作级）仍按各自权限名判定。
 */
@Component
public class AppBoxInterceptor implements HandlerInterceptor {

    public static final String CHECK_POWER_FAIL_PAGE_MESSAGE = "您无权访问此页面！";
    public static final String CHECK_POWER_FAIL_ACTION_MESSAGE = "您无权进行此操作！";

    private final PageRegistry registry;
    private final AuthService authService;
    private final OnlineService onlineService;
    private final String homeRoute;

    public AppBoxInterceptor(PageRegistry registry, AuthService authService, OnlineService onlineService,
                             @Value("${fineui.home:index}") String homeRoute) {
        this.registry = registry;
        this.authService = authService;
        this.onlineService = onlineService;
        this.homeRoute = homeRoute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String canonical = registry.canonicalRoute(RouteUtil.routeOf(request, homeRoute));
        AppBoxUser user = AuthService.currentUser();
        boolean isGet = "GET".equalsIgnoreCase(request.getMethod());

        if ("login".equals(canonical) && user != null && isGet) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        if (user != null) {
            // 用户被禁用或删除后，其已登录的会话应立即失效（会话身份是登录那一刻的快照）
            if (!authService.isActiveUser(user)) {
                authService.logout();
                if (isGet) {
                    response.sendRedirect(request.getContextPath() + "/login");
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("账号已被禁用或删除，请重新登录！");
                }
                return false;
            }
            onlineService.update(user.getId(), request.getSession(true));
        }
        if (canonical != null) {
            Class<?> pageClass = registry.pageClass(canonical);
            CheckPower checkPower = pageClass == null ? null : pageClass.getAnnotation(CheckPower.class);
            if (checkPower != null && !authService.checkPower(checkPower.value())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                if (isGet) {
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().write("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/></head><body>"
                            + CHECK_POWER_FAIL_PAGE_MESSAGE + "</body></html>");
                } else {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write(CHECK_POWER_FAIL_ACTION_MESSAGE);
                }
                return false;
            }
        }
        return true;
    }
}
