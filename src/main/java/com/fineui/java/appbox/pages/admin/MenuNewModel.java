package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Menu;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.repository.MenuRepository;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.DropDownBox;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.RadioButtonList;
import com.fineui.java.core.controls.TextBox;

import java.util.Optional;

/** 新增菜单（路由 {@code admin/menu-new}，弹窗内打开）：上级菜单从下拉树表格选择，浏览权限按权限名输入并校验存在。 */
@FineUIPage("admin/menu-new")
@CheckPower("CoreMenuNew")
public class MenuNewModel extends AppBoxAdminPageBase {

    DropDownBox ddbParent;
    Grid Grid1;
    TextBox tbxViewPower;
    RadioButtonList rblIconList;

    @BindProperty
    private Menu menu;

    public Menu getMenu() {
        return menu;
    }

    private final MenuRepository menuRepository;
    private final PowerRepository powerRepository;

    public MenuNewModel(MenuRepository menuRepository, PowerRepository powerRepository) {
        this.menuRepository = menuRepository;
        this.powerRepository = powerRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            MenuIconItems.fill(rblIconList);
            Grid1.setDataSource(menuRepository.findAllByOrderBySortIndexAsc());
            Grid1.dataBind();
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        if (!isSafeUrl(menu.getNavigateUrl()) || !isSafeIconUrl(menu.getImageUrl())) {
            showAlert("链接与图标只能填站内路径（以 / 开头）或 http(s) 地址！");
            return;
        }
        // 上级菜单（未选择则为顶级菜单）
        String parentValue = ddbParent.getValue();
        menu.setParentId(intOrNull(parentValue));
        // 浏览权限：按权限名解析为主键，填了却不存在则提示
        if (!resolveViewPower(menu, tbxViewPower.getValue().trim(), powerRepository)) {
            showAlert("浏览权限 " + tbxViewPower.getValue().trim() + " 不存在！");
            return;
        }
        menuRepository.save(menu);
        ActiveWindow.hidePostBack();
    }

    /** 把权限名写成菜单的 viewPowerId：空 → 不限权限；不存在 → 返回 false。 */
    static boolean resolveViewPower(Menu menu, String viewPowerName, PowerRepository powerRepository) {
        if (viewPowerName.isEmpty()) {
            menu.setViewPowerId(null);
            return true;
        }
        Optional<Power> power = powerRepository.findByName(viewPowerName);
        if (power.isEmpty()) {
            return false;
        }
        menu.setViewPowerId(power.get().getId());
        return true;
    }
}
