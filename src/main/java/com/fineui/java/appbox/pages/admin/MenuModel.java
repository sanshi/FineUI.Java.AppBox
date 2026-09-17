package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Menu;
import com.fineui.java.appbox.repository.MenuRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.Grid;

import java.util.List;

/** 菜单管理（路由 {@code admin/menu}）：树形表格展示全部菜单，行内编辑/删除（弹窗）；有子菜单的不能删。 */
@FineUIPage("admin/menu")
@CheckPower("CoreMenuView")
public class MenuModel extends AppBoxAdminPageBase {

    Grid Grid1;
    Button btnNew;

    private final MenuRepository menuRepository;

    private List<Menu> menus;

    public List<Menu> getMenus() {
        return menus;
    }

    public MenuModel(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            btnNew.setEnabled(checkPower("CoreMenuNew"));
            Grid1.findCommand("Edit").setEnabled(checkPower("CoreMenuEdit"));
            Grid1.findCommand("Delete").setEnabled(checkPower("CoreMenuDelete"));
            loadData();
        }
    }

    private void loadData() {
        menus = menuRepository.findAllByOrderBySortIndexAsc();
        Grid1.setDataSource(menus);
        Grid1.dataBind();
    }

    public void Window1_Close(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId == null) {
                return;
            }
            if (!checkPower("CoreMenuDelete")) {
                checkPowerFailWithAlert();
                return;
            }
            if (!menuRepository.findByParentIdOrderBySortIndexAsc(rowId).isEmpty()) {
                showAlertInTop("删除失败！请先删除子菜单！", "", MessageBoxIcon.Warning);
                return;
            }
            menuRepository.deleteById(rowId);
            loadData();
        }
    }
}
