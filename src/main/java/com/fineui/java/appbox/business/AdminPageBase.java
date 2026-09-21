package com.fineui.java.appbox.business;

/**
 * 后台功能页基类：与 {@link PageBase} 的区别只在首屏自动加页面水印（当前用户的「角色名（用户名）」），
 * 水印由 {@code AppPageManagerInitializer} 在渲染前按本类型判定后写入页面配置。主框架页、登录页不继承本类。
 */
public abstract class AdminPageBase extends PageBase {
}
