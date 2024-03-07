package com.easy.query.plugin.core.entity;

import java.util.Objects;

/**
 * create time 2024/2/26 21:43
 * 文件说明
 *
 * @author xuejiaming
 */
public class ClassNodePropPath {
    private final String from;
    private final String to;
    private final String property;

    public ClassNodePropPath(String from, String to, String property){

        this.from = from;
        this.to = to;
        this.property = property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassNodePropPath that = (ClassNodePropPath) o;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, property);
    }

    public String getTo() {
        return to;
    }
}
