package com.fineui.java.appbox.business;

import jakarta.servlet.http.HttpServletRequest;

/** 从请求 URI 取页面路由（去掉上下文路径与首尾斜杠）；站点根 {@code /} 视为首页路由。 */
public final class RouteUtil {

    private RouteUtil() {
    }

    public static String routeOf(HttpServletRequest request, String homeRoute) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
        int start = 0;
        int end = path.length();
        while (start < end && path.charAt(start) == '/') {
            start++;
        }
        while (end > start && path.charAt(end - 1) == '/') {
            end--;
        }
        String route = path.substring(start, end);
        return route.isEmpty() ? homeRoute : route;
    }
}
