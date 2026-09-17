package com.fineui.java.appbox.business;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 页面级权限声明：标在页面类上，值是浏览该页所需的权限名（如 {@code CoreUserView}）。
 * 由 {@link AppBoxInterceptor} 在首屏 GET 与回发 POST 两条路径上统一校验；不通过时首屏输出提示页、回发返回 403
 * （客户端 {@code common.js} 据此弹出「您无权进行此操作！」）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckPower {

    String value();
}
