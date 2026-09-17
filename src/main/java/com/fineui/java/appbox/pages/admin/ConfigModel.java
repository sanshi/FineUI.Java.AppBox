package com.fineui.java.appbox.pages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.TextArea;

/** 系统配置（路由 {@code admin/config}）：表格默认每页条数、帮助下拉菜单（JSON 数组）。保存后整站刷新以应用新配置。 */
@FineUIPage("admin/config")
@CheckPower("CoreConfigView")
public class ConfigModel extends AppBoxAdminPageBase {

    DropDownList ddlPageSize;
    TextArea tbxHelpList;
    Button btnSave;

    private final ObjectMapper objectMapper;

    public ConfigModel(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            btnSave.setEnabled(checkPower("CoreConfigEdit"));
            ddlPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            tbxHelpList.setValue(prettyJson(configService.getHelpList()));
        }
    }

    public void btnSave_Click(Object sender, EventArgs e) {
        if (!checkPower("CoreConfigEdit")) {
            checkPowerFailWithAlert();
            return;
        }
        String helpList = tbxHelpList.getValue().trim();
        try {
            JsonNode node = objectMapper.readTree(helpList);
            if (node == null || !node.isArray()) {
                throw new IllegalArgumentException("not a JSON array");
            }
        } catch (Exception ex) {
            tbxHelpList.markInvalid("格式不正确，必须是JSON字符串！");
            return;
        }
        configService.setPageSize(intOrDefault(ddlPageSize.getSelectedValue(), configService.getPageSize()));
        configService.setHelpList(helpList);
        configService.saveAll();
        // 整站刷新以应用新配置（页面脚本里定义）
        invokeClientFunction("reloadTopWindow");
    }

    /** 把配置里紧凑存放的 JSON 排版成多行，便于在文本域里编辑；不是合法 JSON 时原样返回。 */
    private String prettyJson(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json));
        } catch (Exception ex) {
            return json;
        }
    }
}
