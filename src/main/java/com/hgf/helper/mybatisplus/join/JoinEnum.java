package com.hgf.helper.mybatisplus.join;

/**
 * 连接类型
 */
public enum JoinEnum {
    INNER("inner"),
    LEFT("left"),
    RIGHT("right")
    ;

    String sql;

    JoinEnum(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
