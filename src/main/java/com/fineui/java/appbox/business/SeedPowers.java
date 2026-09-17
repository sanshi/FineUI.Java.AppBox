package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Power;

import java.util.ArrayList;
import java.util.List;

/** 权限种子：按功能模块分组的操作点（{@code name} 是代码里 {@code checkPower} 用的标识，{@code title} 是给人看的说明）。 */
final class SeedPowers {

    private SeedPowers() {
    }

    static List<Power> powers() {
        List<Power> list = new ArrayList<>();
        add(list, "CoreUserView", "浏览用户列表", "CoreUser");
        add(list, "CoreUserNew", "新增用户", "CoreUser");
        add(list, "CoreUserEdit", "编辑用户", "CoreUser");
        add(list, "CoreUserDelete", "删除用户", "CoreUser");
        add(list, "CoreUserChangePassword", "修改用户登陆密码", "CoreUser");
        add(list, "CoreRoleView", "浏览角色列表", "CoreRole");
        add(list, "CoreRoleNew", "新增角色", "CoreRole");
        add(list, "CoreRoleEdit", "编辑角色", "CoreRole");
        add(list, "CoreRoleDelete", "删除角色", "CoreRole");
        add(list, "CoreRoleUserView", "浏览角色用户列表", "CoreRoleUser");
        add(list, "CoreRoleUserNew", "向角色添加用户", "CoreRoleUser");
        add(list, "CoreRoleUserDelete", "从角色中删除用户", "CoreRoleUser");
        add(list, "CoreOnlineView", "浏览在线用户列表", "CoreOnline");
        add(list, "CoreConfigView", "浏览全局配置参数", "CoreConfig");
        add(list, "CoreConfigEdit", "修改全局配置参数", "CoreConfig");
        add(list, "CoreMenuView", "浏览菜单列表", "CoreMenu");
        add(list, "CoreMenuNew", "新增菜单", "CoreMenu");
        add(list, "CoreMenuEdit", "编辑菜单", "CoreMenu");
        add(list, "CoreMenuDelete", "删除菜单", "CoreMenu");
        add(list, "CoreLogView", "浏览日志列表", "CoreLog");
        add(list, "CoreLogDelete", "删除日志", "CoreLog");
        add(list, "CoreTitleView", "浏览职务列表", "CoreTitle");
        add(list, "CoreTitleNew", "新增职务", "CoreTitle");
        add(list, "CoreTitleEdit", "编辑职务", "CoreTitle");
        add(list, "CoreTitleDelete", "删除职务", "CoreTitle");
        add(list, "CoreTitleUserView", "浏览职务用户列表", "CoreTitleUser");
        add(list, "CoreTitleUserNew", "向职务添加用户", "CoreTitleUser");
        add(list, "CoreTitleUserDelete", "从职务中删除用户", "CoreTitleUser");
        add(list, "CoreDeptView", "浏览部门列表", "CoreDept");
        add(list, "CoreDeptNew", "新增部门", "CoreDept");
        add(list, "CoreDeptEdit", "编辑部门", "CoreDept");
        add(list, "CoreDeptDelete", "删除部门", "CoreDept");
        add(list, "CoreDeptUserView", "浏览部门用户列表", "CoreDeptUser");
        add(list, "CoreDeptUserNew", "向部门添加用户", "CoreDeptUser");
        add(list, "CoreDeptUserDelete", "从部门中删除用户", "CoreDeptUser");
        add(list, "CorePowerView", "浏览权限列表", "CorePower");
        add(list, "CorePowerNew", "新增权限", "CorePower");
        add(list, "CorePowerEdit", "编辑权限", "CorePower");
        add(list, "CorePowerDelete", "删除权限", "CorePower");
        add(list, "CoreRolePowerView", "浏览角色权限列表", "CoreRolePower");
        add(list, "CoreRolePowerEdit", "编辑角色权限", "CoreRolePower");
        add(list, "TestPage1View", "浏览测试页面一", "Test");
        add(list, "TestPage2View", "浏览测试页面二", "Test");
        return list;
    }

    private static void add(List<Power> list, String name, String title, String groupName) {
        Power p = new Power();
        p.setName(name);
        p.setTitle(title);
        p.setGroupName(groupName);
        list.add(p);
    }
}
