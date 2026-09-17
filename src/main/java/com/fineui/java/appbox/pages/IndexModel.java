package com.fineui.java.appbox.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineui.java.appbox.business.AppBoxPageBase;
import com.fineui.java.appbox.model.Menu;
import com.fineui.java.appbox.repository.MenuRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.MenuButton;
import com.fineui.java.core.controls.Tree;
import com.fineui.java.core.controls.TreeNode;
import com.fineui.java.core.enums.Icon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 主框架页（路由 {@code index}，站点根 {@code /} 直接渲染本页）：左侧菜单树 + 右侧选项卡工作区。
 * 菜单树从菜单表生成，只保留当前用户有浏览权限的菜单、并剔除因此变空的目录；帮助下拉菜单由全局配置 HelpList（JSON）生成。
 */
@FineUIPage("index")
public class IndexModel extends AppBoxPageBase {

    Tree treeMenu;
    Button btnUserName;
    Button btnSystemHelp;

    private final MenuRepository menuRepository;

    public IndexModel(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            loadData();
        }
    }

    private void loadData() {
        List<Menu> menus = resolveUserMenuList();
        if (menus.isEmpty()) {
            showNotify("系统管理员尚未给你配置菜单！");
            return;
        }

        List<TreeNode> nodes = new ArrayList<>();
        resolveMenuTree(menus, null, nodes);
        if (!nodes.isEmpty()) {
            nodes.get(0).setExpanded(true);   // 展开第一个树节点
        }
        for (TreeNode node : nodes) {
            treeMenu.addNode(node);
        }

        btnUserName.setText(getIdentityName());
        buildSystemHelpMenu();
    }

    /** 当前用户可见的菜单：不要求权限的菜单，或所属角色拥有其浏览权限的菜单（按排序号排列）。 */
    private List<Menu> resolveUserMenuList() {
        List<String> rolePowerNames = getRolePowerNames();
        List<Menu> menus = new ArrayList<>();
        for (Menu menu : menuRepository.findAllByOrderBySortIndexAsc()) {
            if (menu.getViewPowerId() == null
                    || (menu.getViewPower() != null && rolePowerNames.contains(menu.getViewPower().getName()))) {
                menus.add(menu);
            }
        }
        return menus;
    }

    /** 递归生成菜单树，返回本层加入的节点数；没有子节点又没有链接的目录节点被剔除。 */
    private int resolveMenuTree(List<Menu> menus, Integer parentId, List<TreeNode> nodes) {
        int count = 0;
        for (Menu menu : menus) {
            if (!Objects.equals(menu.getParentId(), parentId)) {
                continue;
            }
            TreeNode node = new TreeNode();
            node.setId("menu" + menu.getId());
            node.setText(menu.getName());
            if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty() && isSafeIconUrl(menu.getImageUrl())) {
                node.setIconUrl(menu.getImageUrl());
            }
            // 链接与图标是可编辑数据：只认站内路径或 http(s)，伪协议（javascript: 等）一律忽略
            boolean hasUrl = menu.getNavigateUrl() != null && !menu.getNavigateUrl().isEmpty() && isSafeUrl(menu.getNavigateUrl());
            if (hasUrl) {
                node.setNavigateUrl(menu.getNavigateUrl());
            }

            List<TreeNode> children = new ArrayList<>();
            int childCount = resolveMenuTree(menus, menu.getId(), children);
            if (childCount == 0) {
                if (!hasUrl) {
                    continue;   // 空目录：不加入
                }
                node.setLeaf(true);
            } else {
                for (TreeNode child : children) {
                    node.addChild(child);
                }
            }
            nodes.add(node);
            count++;
        }
        return count;
    }

    /** 帮助下拉菜单：由全局配置 HelpList 的 JSON 数组（Text / Icon / ID / URL）生成，点击在工作区新开选项卡。 */
    private void buildSystemHelpMenu() {
        com.fineui.java.core.controls.Menu menu = new com.fineui.java.core.controls.Menu();
        menu.setId("helpMenu");
        btnSystemHelp.addChild(menu, "menu");

        JsonNode items;
        try {
            items = parseJson(configService.getHelpList());
        } catch (RuntimeException e) {
            return;   // 配置里的 JSON 坏了：不生成帮助菜单，但首页要照常打开
        }
        int index = 0;
        for (JsonNode item : items) {
            String text = item.path("Text").asText("");
            String id = item.path("ID").asText("");
            String url = item.path("URL").asText("");
            if (text.isEmpty() || id.isEmpty() || url.isEmpty() || !isSafeUrl(url)) {
                continue;   // 配置里的地址不可信：伪协议（javascript: 等）不生成菜单项
            }
            MenuButton menuItem = new MenuButton();
            menuItem.setId("helpMenuItem" + (index++));
            menuItem.setText(text);
            Icon icon = iconOf(item.path("Icon").asText(""));
            if (icon != null) {
                menuItem.setIcon(icon);
            }
            // 选项卡参数挂在元素属性上，客户端 onHelpMenuClick 里经 getAttr 取出
            menuItem.setAttribute("data-id", id);
            menuItem.setAttribute("data-url", url);
            menuItem.setAttribute("data-text", text);
            menuItem.setClickHandler("onHelpMenuClick");
            menu.addChild(menuItem);
        }
    }

    private static Icon iconOf(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (Icon icon : Icon.values()) {
            if (icon.name().equalsIgnoreCase(name)) {
                return icon;
            }
        }
        return null;
    }

    public void btnSignOut_Click(Object sender, EventArgs e) {
        authService.logout();
        redirect("/login");
    }
}
