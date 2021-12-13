package com.hgf.helper.mybatisplus.helper;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.hgf.helper.mybatisplus.OriginSqlBo;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MyLambdaUpdateWrapper<T> extends AbstractLambdaWrapper<T, MyLambdaUpdateWrapper<T>> {

    private final List<String> sqlSet;


    public MyLambdaUpdateWrapper() {
        // 如果无参构造函数，请注意实体 NULL 情况 SET 必须有否则 SQL 异常
        this((T) null);
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaUpdate(entity)
     */
    public MyLambdaUpdateWrapper(T entity) {
        super.setEntity(entity);
        super.initNeed();
        this.sqlSet = new ArrayList<>();
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaUpdate(entity)
     */
    public MyLambdaUpdateWrapper(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
        this.sqlSet = new ArrayList<>();
    }

    MyLambdaUpdateWrapper(T entity, Class<T> entityClass, List<String> sqlSet, AtomicInteger paramNameSeq,
                        Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments,
                        SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.sqlSet = sqlSet;
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    /**
     * 指定列自增
     * @param columns           列引用
     * @param value             增长值
     */
    public MyLambdaUpdateWrapper<T> incrField(SFunction<T, ?> columns, Object value) {
        String columnsToString = super.columnToString(columns);

        String format = String.format("%s =  %s + %s", columnsToString,columnsToString, formatSql("{0}", value));

        setSql(format);

        return this;
    }

    private void setSql(String format) {
        setSql(true, format);
    }

    /**
     * 指定列自减
     * @param columns           列引用
     * @param value             减少值
     */
    public MyLambdaUpdateWrapper<T> descField(SFunction<T, ?> columns, Object value) {
        String columnsToString = super.columnToString(columns);

        String format = String.format("%s =  %s - %s", columnsToString,columnsToString, formatSql("{0}", value));

        setSql(format);

        return this;
    }

    /**
     * 原始 where sql
     */
    @SafeVarargs
    public final MyLambdaUpdateWrapper<T> originWhereSql(String sqlFormat, OriginSqlBo<T>... sqlBos) {

        Object[] objects = new Object[sqlBos.length];

        for (int i = 0; i < sqlBos.length; i++) {
            OriginSqlBo<T> sqlBo = sqlBos[i];
            Object o = null;
            if (sqlBo.isColumn() && sqlBo.getSFunction() != null) {
                o = columnToString(sqlBo.getSFunction());
            } else if (!sqlBo.isColumn() && sqlBo.getObject() != null) {

                o = sqlBo.isPreParam() ? formatSql("{0}", sqlBo.getObject()) : sqlBo.getObject();
            }

            if (o != null) {
                objects[i] = o;
            }
        }

        if (!CollectionUtil.isEmpty(objects)) {
            String format = String.format(sqlFormat, objects);
            doIt(true, () -> format, SqlKeyword.AND);
        }

        return this;
    }


    @Override
    public String columnToString(SFunction<T, ?> column) {
        return super.columnToString(column);
    }

    /**
     * 根据传入map case update 最终实现如下效果
     * <p>
     *      SET `columns` = (
     *     CASE `caseColumns` WHEN caseMap.keys[0] THEN caseMap.values[0]
     *     WHEN caseMap.keys[1] THEN caseMap.values[1]
     *     WHEN caseMap.keys[2] THEN caseMap.values[2]
     *     END)
     * </p>
     */
    public MyLambdaUpdateWrapper<T> caseSet(SFunction<T, ?> columns, Map<Object,Object> caseMap, SFunction<T, ?> caseColumns) {
        String columnsToString = super.columnToString(columns);

        List<Object> params = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        int paramIndex = 0;
        for (Object o : caseMap.keySet()) {
            stringBuilder.append(String.format(" WHEN {%d} THEN {%d}", paramIndex++, paramIndex++)).append("\n");
            params.add(o);
            params.add(caseMap.get(o));
        }

        String caseSqlFormat = String.format("(case %s %s end)", super.columnToString(caseColumns), stringBuilder.toString());


        String format = String.format("%s =  %s", columnsToString, formatSql(caseSqlFormat, params.toArray(new Object[0])));


        setSql(format);

        in(caseColumns, caseMap.keySet());

        return this;
    }

    public MyLambdaUpdateWrapper<T> caseIncrSet(SFunction<T, ?> columns, Map<Object,Object> caseMap, SFunction<T, ?> caseColumns) {
        String columnsToString = super.columnToString(columns);

        List<Object> params = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        int paramIndex = 0;
        for (Object o : caseMap.keySet()) {
            stringBuilder.append(String.format(" WHEN {%d} THEN {%d}", paramIndex++, paramIndex++)).append("\n");
            params.add(o);
            params.add(caseMap.get(o));
        }

        String caseSqlFormat = String.format("(case %s %s end)", super.columnToString(caseColumns), stringBuilder.toString());


        String format = String.format("%s = %s + %s", columnsToString, columnsToString, formatSql(caseSqlFormat, params.toArray(new Object[0])));


        setSql(format);

        return this;
    }

    public MyLambdaUpdateWrapper<T> set(boolean condition, SFunction<T, ?> column, Object val) {
        if (condition) {
            sqlSet.add(String.format("%s=%s", columnToString(column), formatSql("{0}", val)));
        }
        return typedThis;
    }

    public MyLambdaUpdateWrapper<T> set( SFunction<T, ?> column, Object val) {
        sqlSet.add(String.format("%s=%s", columnToString(column), formatSql("{0}", val)));
        return typedThis;
    }

    public MyLambdaUpdateWrapper<T> setSql(boolean condition, String sql) {
        if (condition && StringUtils.isNotBlank(sql)) {
            sqlSet.add(sql);
        }
        return typedThis;
    }

    @Override
    public String getSqlSet() {
        if (CollectionUtils.isEmpty(sqlSet)) {
            return null;
        }
        return String.join(StringPool.COMMA, sqlSet);
    }

    @Override
    protected MyLambdaUpdateWrapper<T> instance() {
        return new MyLambdaUpdateWrapper(getEntity(), getEntityClass(), null, paramNameSeq, paramNameValuePairs,
                new MergeSegments(), SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSet.clear();
    }


}
