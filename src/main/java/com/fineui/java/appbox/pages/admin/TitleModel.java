package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/** 职称列表（路由 {@code admin/title}）：搜索 + 数据库分页排序 + 每页条数切换 + 行内编辑/删除（弹窗）。 */
@FineUIPage("admin/title")
@CheckPower("CoreTitleView")
public class TitleModel extends AppBoxAdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;
    Button btnNew;

    private final TitleRepository titleRepository;
    private final UserRepository userRepository;

    private List<Title> titles;

    public List<Title> getTitles() {
        return titles;
    }

    public TitleModel(TitleRepository titleRepository, UserRepository userRepository) {
        this.titleRepository = titleRepository;
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 根据用户权限控制页面控件的可用状态
            btnNew.setEnabled(checkPower("CoreTitleNew"));
            Grid1.findCommand("Edit").setEnabled(checkPower("CoreTitleEdit"));
            Grid1.findCommand("Delete").setEnabled(checkPower("CoreTitleDelete"));

            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());

            loadData();
        }
    }

    private void loadData() {
        String searchText = ttbSearchMessage.getValue().trim();
        Specification<Title> spec = (root, query, cb) -> searchText.isEmpty()
                ? cb.conjunction()
                : cb.like(root.get("name"), "%" + searchText + "%");

        Page<Title> page = loadPage(Grid1, pageable -> titleRepository.findAll(spec, pageable));
        titles = page.getContent();
        Grid1.setDataSource(titles);
        Grid1.dataBind();
    }

    public void Window1_Close(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_Sort(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_PageIndexChanged(Object sender, EventArgs e) {
        loadData();
    }

    public void ddlGridPageSize_SelectedIndexChanged(Object sender, EventArgs e) {
        Grid1.setPageSize(intOrDefault(ddlGridPageSize.getSelectedValue(), configService.getPageSize()));
        loadData();
    }

    public void ttbSearchMessage_Trigger1Click(Object sender, EventArgs e) {
        ttbSearchMessage.setValue("");
        ttbSearchMessage.setShowTrigger1(false);
        loadData();
    }

    public void ttbSearchMessage_Trigger2Click(Object sender, EventArgs e) {
        ttbSearchMessage.setShowTrigger1(true);
        loadData();
    }

    public void Grid1_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId == null) {
                return;
            }
            // 在操作之前进行权限检查
            if (!checkPower("CoreTitleDelete")) {
                checkPowerFailWithAlert();
                return;
            }
            if (userRepository.countByTitlesId(rowId) > 0) {
                showAlertInTop("删除失败！需要先清空属于此职称的用户！", "", MessageBoxIcon.Warning);
                return;
            }
            titleRepository.deleteById(rowId);
            loadData();
        }
    }
}
