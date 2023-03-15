package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hgf.helper.mybatisplus.annotation.Association;

import java.lang.reflect.Field;

/**
 * 联查 表工具
 */
public class JoinTableHelper {

    /*private static Map<Class<?>, JoinTableInfo> joinTableCache = new HashMap<>();

    public static synchronized JoinTableInfo createJoinTableIn(TableInfo tableInfo) {
        Class<?> entityType = tableInfo.getEntityType();
        JoinTableInfo joinTableInfo = joinTableCache.get(entityType);
        if (joinTableInfo != null) {
            return joinTableInfo;
        }

        joinTableInfo = new JoinTableInfo(tableInfo);

        joinTableCache.put(entityType, joinTableInfo);
        return joinTableInfo;
    }
*/
    /**
     * 获取关联表前缀
     */
    public static String getJoinTableColumnPrefix(Field field) {
        if (field == null) {
            return null;
        }
        Association association = field.getAnnotation(Association.class);
        if (association != null && StringUtils.isNotBlank(association.aliasPrefix())) {
            return association.aliasPrefix();
        }
        return field.getName() + "_";
    }


}
