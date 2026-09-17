package com.fineui.java.appbox.model;

/** 所有实体的公共契约：整型自增主键。供泛型工具按主键定位实体。 */
public interface KeyId {

    Integer getId();

    void setId(Integer id);
}
