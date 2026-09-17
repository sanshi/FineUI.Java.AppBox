package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Menu;
import com.fineui.java.appbox.repository.MenuRepository;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridRowDataBoundEventArgs;
import com.fineui.java.core.controls.DropDownBox;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.RadioButtonList;
import com.fineui.java.core.controls.TextBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 编辑菜单（路由 {@code admin/menu-edit}，弹窗内打开）：本菜单及其子菜单不可选为上级；保存走 read-first。 */
@FineUIPage("admin/menu-edit")
@CheckPower("CoreMenuEdit")
public class MenuEditModel extends AppBoxAdminPageBase {

    DropDownBox ddbParent;
    Grid Grid1;
    TextBox tbxViewPower;
    RadioButtonList rblIconList;

    @HiddenProperty
    private int menuId;

    @BindProperty
    private Menu menu;

    public Menu getMenu() {
        return menu;
    }

    private final MenuRepository menuRepository;
    private final PowerRepository powerRepository;

    private final Map<Integer, Menu> menusById = new HashMap<>();

    public MenuEditModel(MenuRepository menuRepository, PowerRepository powerRepository) {
        this.menuRepository = menuRepository;
        this.powerRepository = powerRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        menuId = getQueryInt("id", 0);
        menu = menuRepository.findById(menuId).orElse(null);
        if (menu == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            tbxViewPower.setValue(menu.getViewPowerName() == null ? "" : menu.getViewPowerName());
            MenuIconItems.fill(rblIconList);
            if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty()) {
                rblIconList.setSelectedValue(menu.getImageUrl());
            }
            List<Menu> all = menuRepository.findAllByOrderBySortIndexAsc();
            for (Menu m : all) {
                menusById.put(m.getId(), m);
            }
            Grid1.setDataSource(all);
            Grid1.dataBind();
            if (menu.getParent() != null) {
                ddbParent.setValue(String.valueOf(menu.getParentId()));
                ddbParent.setText(menu.getParent().getName());
            }
        }
    }

    /** 行绑定：本菜单及其子菜单不可选为上级。 */
    public void Grid1_RowDataBound(Object sender, GridRowDataBoundEventArgs e) {
        Menu row = (Menu) e.getDataItem();
        e.setRowSelectable(!isOrChildOfCurrent(row.getId()));
    }

    /** 保存时重新读全表判断：候选上级是不是本菜单或它的下级（回发不跑 Page_Load，索引是空的）。 */
    private boolean isSelfOrDescendant(Integer candidateId) {
        Map<Integer, Menu> all = new HashMap<>();
        for (Menu m : menuRepository.findAllByOrderBySortIndexAsc()) {
            all.put(m.getId(), m);
        }
        Integer id = candidateId;
        int guard = 0;
        while (id != null) {
            if (guard++ >= 100) {
                return true;   // 层级异常（超深或库里本来就有环）：保守拒绝，不把环坐实
            }
            if (id == menuId) {
                return true;
            }
            Menu m = all.get(id);
            id = m == null ? null : m.getParentId();
        }
        return false;
    }

    private boolean isOrChildOfCurrent(Integer id) {
        int guard = 0;
        while (id != null && guard++ < 100) {
            if (id == menuId) {
                return true;
            }
            Menu m = menusById.get(id);
            id = m == null ? null : m.getParentId();
        }
        return false;
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        Menu stored = menuRepository.findById(menuId).orElse(null);
        if (stored == null) {
            showAlert("该菜单不存在或已被删除！");
            return;
        }
        if (!isSafeUrl(menu.getNavigateUrl()) || !isSafeIconUrl(menu.getImageUrl())) {
            showAlert("链接与图标只能填站内路径（以 / 开头）或 http(s) 地址！");
            return;
        }
        String parentValue = ddbParent.getValue();
        Integer parentId = intOrNull(parentValue);
        if (parentId != null && isSelfOrDescendant(parentId)) {
            showAlert("上级菜单不能是本菜单或其下级菜单！");
            return;
        }
        String viewPowerName = tbxViewPower.getValue().trim();
        if (!MenuNewModel.resolveViewPower(stored, viewPowerName, powerRepository)) {
            showAlert("浏览权限 " + viewPowerName + " 不存在！");
            return;
        }
        stored.setName(menu.getName());
        stored.setSortIndex(menu.getSortIndex());
        stored.setNavigateUrl(menu.getNavigateUrl());
        stored.setImageUrl(menu.getImageUrl());
        stored.setRemark(menu.getRemark());
        stored.setParentId(parentId);
        menuRepository.save(stored);
        ActiveWindow.hidePostBack();
    }
}
