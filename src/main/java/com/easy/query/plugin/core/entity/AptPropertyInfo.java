package com.easy.query.plugin.core.entity;

/**
 * create time 2023/9/16 12:12
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptPropertyInfo {
    /**
     * 属性名
     */
    private final String propertyName;
    /**
     * 注释内容
     */
    private final String comment;
    /**
     * 属性类型
     */
    private final String propertyType;
    /**
     * 对象名
     */
    private final String entityName;
    private final boolean valueObject;
    private final String owner;
    private final String includePlaceholder;
    private final String sqlColumn;

    public AptPropertyInfo(String propertyName, String propertyType, String comment, String entityName,boolean valueObject,String owner,boolean includeProperty){

        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.comment = comment;
        this.entityName = entityName;
        this.valueObject = valueObject;
        this.owner = owner;
        this.includePlaceholder = includeProperty?", "+propertyType+".class":"";
        this.sqlColumn = includeProperty?"SQLNavigateColumn":"SQLColumn";
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getComment() {
        return comment;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public String getEntityName() {
        return entityName;
    }

    public boolean isValueObject() {
        return valueObject;
    }

    public String getOwner() {
        return owner;
    }

    public String getIncludePlaceholder() {
        return includePlaceholder;
    }

    public String getSqlColumn() {
        return sqlColumn;
    }
}
