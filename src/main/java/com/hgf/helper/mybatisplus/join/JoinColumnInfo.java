package com.hgf.helper.mybatisplus.join;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

@Setter
@Getter
public class JoinColumnInfo {
    /**
     * 主表连接字段名
     */
    String mainTableColumn;

    /**
     * 附表连接字段名
     */
    String joinTableColumn;

    /**
     * 附表映射实体类
     */
    Class<?> joinEntityClass;

    /**
     * 主表映射实体类
     */
    Class<?> mainEntityClass;

    /**
     * 查询结果class中附表实体类属性
     */
    Field queryTypeJoinField;

    /**
     * 附表别称
     */
    String joinTableAlias;

}
