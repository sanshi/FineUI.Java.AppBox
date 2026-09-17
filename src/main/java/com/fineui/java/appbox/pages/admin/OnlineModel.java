package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Online;
import com.fineui.java.appbox.repository.OnlineRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 在线统计（路由 {@code admin/online}）：最近 2 小时内有操作的用户，可按用户名搜索、分页排序，行内查看用户信息。 */
@FineUIPage("admin/online")
@CheckPower("CoreOnlineView")
public class OnlineModel extends AppBoxAdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;

    private final OnlineRepository onlineRepository;

    private List<Online> onlines;

    public List<Online> getOnlines() {
        return onlines;
    }

    public OnlineModel(OnlineRepository onlineRepository) {
        this.onlineRepository = onlineRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());
            loadData();
        }
    }

    private void loadData() {
        String searchText = ttbSearchMessage.getValue().trim();
        LocalDateTime since = LocalDateTime.now().minusHours(2);
        Specification<Online> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!searchText.isEmpty()) {
                predicates.add(cb.like(root.get("user").get("name"), "%" + searchText + "%"));
            }
            predicates.add(cb.greaterThan(root.get("updateTime"), since));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // 表格列里的用户名 / 中文名是由 user 导航派生的只读属性，排序时映射回真实的关联属性路径
        Sort sort = sortOf(Grid1);
        String sortField = Grid1.getSortField();
        if ("userName".equals(sortField) || "userChineseName".equals(sortField)) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(Grid1.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, "userName".equals(sortField) ? "user.name" : "user.chineseName");
        }
        Page<Online> page = loadPage(Grid1, sort, pageable -> onlineRepository.findAll(spec, pageable));
        onlines = page.getContent();
        Grid1.setDataSource(onlines);
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
}
