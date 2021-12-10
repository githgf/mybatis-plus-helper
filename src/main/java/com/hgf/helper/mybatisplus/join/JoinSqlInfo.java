package com.hgf.helper.mybatisplus.join;

import lombok.Getter;
import lombok.Setter;

/**
 * 多表连接信息
 */
@Setter
@Getter
public class JoinSqlInfo {
    /**
     * 连接类型
     */
    String joinType;

    /**
     * 主表连接字段名
     */
    String mainTableColumn;

    /**
     * 附表连接字段名
     */
    String joinTableColumn;

    /**
     * 附表名
     */
    String joinTableName;

    /***
     * 主表名
     */
    String mainTableName;

    public String sql(){
        return String.format("%s join %s on %s.%s = %s.%s", joinType, joinTableName, mainTableName, mainTableColumn, joinTableName, joinTableColumn);
    }

}
