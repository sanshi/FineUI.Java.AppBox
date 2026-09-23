package com.fineui.java.appbox.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/** 静态帮助页不经过 FineUI 页面渲染，错误响应也需要单独设置脚本策略。 */
@Configuration(proxyBeanMethods = false)
public class StrictCspWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HelpPageCspInterceptor())
                // Spring 静态资源处理器也接受文件名后的斜杠等别名，按整个目录匹配。
                .addPathPatterns("/help/**");
        registry.addInterceptor(new ErrorResponseCspInterceptor());
    }

    private static final class HelpPageCspInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
            response.setHeader("Content-Security-Policy", "script-src 'self';");
            String requestPath = request.getRequestURI().substring(request.getContextPath().length());
            for (String pagePath : new String[] { "/help/jisuanqi.html", "/help/wannianli.html" }) {
                if (requestPath.startsWith(pagePath + "/")) {
                    // 静态处理器会把尾斜杠别名当成页面；转回规范地址才能正确加载相对路径的脚本。
                    response.sendRedirect(request.getContextPath() + pagePath);
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ErrorResponseCspInterceptor implements HandlerInterceptor {

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                               Object handler, ModelAndView modelAndView) {
            applyErrorPolicy(response);
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception exception) {
            // 未登记的页面路由可能以异常结束，完成请求时再检查一次。
            applyErrorPolicy(response);
        }

        private void applyErrorPolicy(HttpServletResponse response) {
            if (response.getStatus() < 400) {
                return;
            }

            String currentPolicy = response.getHeader("Content-Security-Policy");
            if (currentPolicy != null && !"script-src 'self';".equals(currentPolicy)) {
                return;
            }

            // 帮助目录的不存在路径已在请求前设为 self；HTML 错误页改用 none。
            response.setHeader("Content-Security-Policy", "script-src 'none';");
        }
    }
}
