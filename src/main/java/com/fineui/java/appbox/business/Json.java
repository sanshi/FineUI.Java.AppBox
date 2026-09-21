package com.fineui.java.appbox.business;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 全站共用：把自定义事件回带的 JSON 字符串参数解析成 {@code JsonNode}（供 {@code Page_CustomEvent} 读取字段）。
 *
 * <p>用的是 Boot 4 应用自带的 Jackson 3（{@code tools.jackson}），所以 pom 里不需要声明任何 Jackson 依赖。
 * java.time（LocalDate / LocalDateTime 等）由 Jackson 3 自带支持、默认按 ISO 字符串输出，
 * 不用再注册什么模块——这正是应用侧该走的路；FineUI 库内部的 JSON 收敛固定在 Jackson 2，
 * 那是库自己的实现细节，与应用代码无关。
 */
public final class Json {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json == null || json.isEmpty() ? "{}" : json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("解析自定义事件 JSON 参数失败: " + json, e);
        }
    }

    /**
     * 把 JSON 字符串反序列化成<b>强类型</b>对象（{@link #parse(String)} 是弱类型读法：直接当 JsonNode 按名字取字段）。
     * 典型用法：{@code Json.parse(e.getArgument(), new TypeReference<List<NodeInfo>>() {})}——
     * 之后按属性访问，字段名写错在编译期就能发现。
     */
    public static <T> T parse(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("解析自定义事件 JSON 参数失败: " + json, e);
        }
    }

    /**
     * 把任意数据序列化为缩进 JSON 并做 HTML 转义，供安全放进 {@code <pre>} 或文本域展示。
     * 典型用法：{@code labResult.setText("用户修改的数据：<pre>" + Json.encode(grid.getModifiedData()) + "</pre>")}。
     */
    public static String encode(Object data) {
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        return json.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
