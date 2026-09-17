package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Menu;
import com.fineui.java.appbox.model.Power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 菜单种子：两棵树（系统管理 / 测试菜单）。叶子节点的 {@code navigateUrl} 是本应用的页面路由（小写连字符），
 * {@code viewPowerId} 由权限名经已入库的权限表解析；节点的 {@code children} 只在种子阶段临时承载子节点，由初始化器递归入库。
 */
final class SeedMenus {

    private SeedMenus() {
    }

    static List<Menu> menus(Map<String, Power> powersByName) {
        Menu system = folder("系统管理", 10, "顶级菜单", "/res/icon/cog.png",
                leaf("用户管理", 10, "二级菜单", "/admin/user-list", "/res/icon/tag_blue.png", power(powersByName, "CoreUserView")),
                leaf("职称管理", 20, "二级菜单", "/admin/title", "/res/icon/tag_blue.png", power(powersByName, "CoreTitleView")),
                leaf("职称用户管理", 30, "二级菜单", "/admin/title-user", "/res/icon/tag_blue.png", power(powersByName, "CoreTitleUserView")),
                leaf("部门管理", 40, "二级菜单", "/admin/dept", "/res/icon/tag_blue.png", power(powersByName, "CoreDeptView")),
                leaf("部门用户管理", 50, "二级菜单", "/admin/dept-user", "/res/icon/tag_blue.png", power(powersByName, "CoreDeptUserView")),
                leaf("角色管理", 60, "二级菜单", "/admin/role", "/res/icon/tag_blue.png", power(powersByName, "CoreRoleView")),
                leaf("角色用户管理", 70, "二级菜单", "/admin/role-user", "/res/icon/tag_blue.png", power(powersByName, "CoreRoleUserView")),
                leaf("权限管理", 80, "二级菜单", "/admin/power", "/res/icon/tag_blue.png", power(powersByName, "CorePowerView")),
                leaf("角色权限管理", 90, "二级菜单", "/admin/role-power", "/res/icon/tag_blue.png", power(powersByName, "CoreRolePowerView")),
                leaf("菜单管理", 100, "二级菜单", "/admin/menu", "/res/icon/tag_blue.png", power(powersByName, "CoreMenuView")),
                leaf("在线统计", 110, "二级菜单", "/admin/online", "/res/icon/tag_blue.png", power(powersByName, "CoreOnlineView")),
                leaf("系统配置", 120, "二级菜单", "/admin/config", "/res/icon/tag_blue.png", power(powersByName, "CoreConfigView")),
                leaf("修改密码", 130, "二级菜单", "/admin/change-password", "/res/icon/tag_blue.png", null));

        Menu test = folder("测试菜单", 20, "顶级菜单", "/res/icon/folder.png",
                folder("测试目录1", 10, "二级菜单", "/res/icon/folder.png",
                        leaf("测试页面1", 10, "三级菜单", "/test/test1", "/res/icon/page.png", power(powersByName, "TestPage1View"))),
                leaf("测试页面2", 20, "二级菜单", "/test/test2", "/res/icon/page.png", power(powersByName, "TestPage2View")));

        List<Menu> roots = new ArrayList<>();
        roots.add(system);
        roots.add(test);
        return roots;
    }

    private static Integer power(Map<String, Power> powersByName, String name) {
        Power p = powersByName.get(name);
        if (p == null) {
            throw new IllegalStateException("菜单种子引用了不存在的权限：" + name);
        }
        return p.getId();
    }

    private static Menu folder(String name, int sortIndex, String remark, String imageUrl, Menu... children) {
        Menu m = new Menu();
        m.setName(name);
        m.setSortIndex(sortIndex);
        m.setRemark(remark);
        m.setImageUrl(imageUrl);
        m.setChildren(new ArrayList<>(Arrays.asList(children)));
        return m;
    }

    private static Menu leaf(String name, int sortIndex, String remark, String navigateUrl, String imageUrl, Integer viewPowerId) {
        Menu m = new Menu();
        m.setName(name);
        m.setSortIndex(sortIndex);
        m.setRemark(remark);
        m.setNavigateUrl(navigateUrl);
        m.setImageUrl(imageUrl);
        m.setViewPowerId(viewPowerId);
        return m;
    }
}
