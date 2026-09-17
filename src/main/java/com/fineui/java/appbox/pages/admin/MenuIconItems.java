package com.fineui.java.appbox.pages.admin;

import com.fineui.java.core.IconHelper;
import com.fineui.java.core.RawHtml;
import com.fineui.java.core.controls.RadioButtonList;

/** 菜单新增/编辑页共用：把预置的一组图标填进单选列表（每项显示图标图片 + 名称，值为图标地址）。 */
final class MenuIconItems {

    private static final String[] ICONS = { "tag_yellow", "tag_red", "tag_purple", "tag_pink", "tag_orange", "tag_green", "tag_blue" };

    private MenuIconItems() {
    }

    static void fill(RadioButtonList list) {
        for (String icon : ICONS) {
            String value = "/res/icon/" + icon + ".png";
            RawHtml text = new RawHtml("<img style=\"vertical-align:bottom;\" src=\"" + IconHelper.resolveUrl(value) + "\" />&nbsp;" + icon);
            list.addRadioItem(value, text, true, false);
        }
    }
}
