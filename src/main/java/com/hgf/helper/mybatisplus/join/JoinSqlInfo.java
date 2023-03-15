package com.hgf.helper.mybatisplus.join;

import lombok.Getter;
import lombok.Setter;

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

    String joinTableAlias;

    public String sql(){
        return String.format("\n %s join %s %s on %s.%s = %s.%s \n", joinType, joinTableName, joinTableAlias, mainTableName, mainTableColumn, joinTableAlias, joinTableColumn);
    }

}
