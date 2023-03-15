package com.hgf.helper.mybatisplus.join;

//@Getter
public class JoinTableInfo{
  /*  *//**
     * 主类所在tableInfo
     *//*
    TableInfo mainTable;

    *//**
     * 连接的表映射字段
     *//*
    List<TableFieldInfo> joinFields;

    String allSqlSelect;

    public JoinTableInfo(TableInfo mainTable) {
        this.mainTable = mainTable;
        this.joinFields = mainTable.getFieldList().stream().filter(t -> t.getField().isAnnotationPresent(Association.class)).collect(Collectors.toList());
    }*/

}
