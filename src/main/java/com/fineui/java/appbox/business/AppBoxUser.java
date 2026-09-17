package com.fineui.java.appbox.business;

import java.io.Serializable;
import java.util.List;

/** 登录用户的会话身份：用户主键、用户名、所属角色主键列表（登录时一次性算好，随会话保存）。 */
public final class AppBoxUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final List<Integer> roleIds;

    public AppBoxUser(int id, String name, List<Integer> roleIds) {
        this.id = id;
        this.name = name;
        this.roleIds = List.copyOf(roleIds);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getRoleIds() {
        return roleIds;
    }

    /** 是否超级管理员（用户名 admin，拥有全部权限、不可删除）。 */
    public boolean isAdmin() {
        return "admin".equals(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
