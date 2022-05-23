package com.hgf.helper.mybatisplus.helper;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.hgf.helper.mybatisplus.OriginSqlBo;
import com.hgf.helper.mybatisplus.join.JoinQueryBase;
import com.hgf.helper.mybatisplus.join.JoinQueryBuilder;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.baomidou.mybatisplus.core.enums.SqlKeyword.*;

/**
 * 自定义的查询wrapper
 * @param <T>
 */
public class MyLambdaQueryWrapper<T> extends AbstractLambdaWrapper<T, MyLambdaQueryWrapper<T>>
        implements Query<MyLambdaQueryWrapper<T>, T, SFunction<T, ?>> {

    private SelectBuilder sqlSelect = new SelectBuilder();

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public MyLambdaQueryWrapper() {
        this((T) null);
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public MyLambdaQueryWrapper(T entity) {
        super.setEntity(entity);
        super.initNeed();
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public MyLambdaQueryWrapper(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(...)
     */
    MyLambdaQueryWrapper(T entity, Class<T> entityClass, SelectBuilder sqlSelect, AtomicInteger paramNameSeq,
                         Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments,
                         SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.sqlSelect = sqlSelect;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    /**
     * SELECT 部分 SQL 设置
     *
     * @param columns 查询字段
     */
    @Override
    public MyLambdaQueryWrapper<T> select(SFunction<T, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            Stream.of(columns).forEach(t -> this.sqlSelect.getParts().add(columnToString(t,false)));
        }
        return typedThis;
    }

    /**
     * 过滤查询的字段信息(主键除外!)
     * <p>例1: 只要 java 字段名以 "test" 开头的             -> select(i -&gt; i.getProperty().startsWith("test"))</p>
     * <p>例2: 只要 java 字段属性是 CharSequence 类型的     -> select(TableFieldInfo::isCharSequence)</p>
     * <p>例3: 只要 java 字段没有填充策略的                 -> select(i -&gt; i.getFieldFill() == FieldFill.DEFAULT)</p>
     * <p>例4: 要全部字段                                   -> select(i -&gt; true)</p>
     * <p>例5: 只要主键字段                                 -> select(i -&gt; false)</p>
     *
     * @param predicate 过滤方式
     * @return this
     */
    @Override
    public MyLambdaQueryWrapper<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        if (entityClass == null) {
            entityClass = getEntityClass();
        } else {
            setEntityClass(entityClass);
        }
        Assert.notNull(entityClass, "entityClass can not be null");
        this.sqlSelect.getParts().add(TableInfoHelper.getTableInfo(entityClass).chooseSelect(predicate));
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 用于生成嵌套 sql
     * <p>故 sqlSelect 不向下传递</p>
     */
    @Override
    protected MyLambdaQueryWrapper<T> instance() {
        return new MyLambdaQueryWrapper<>(getEntity(), getEntityClass(), null, paramNameSeq, paramNameValuePairs,
                new MergeSegments(), SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
    }


    public MyLambdaQueryWrapper<T> min(SFunction<T, ?> column){
        if (column != null) {
            this.sqlSelect.getParts().add(String.format("min(%s)", columnToString(column)));
        }
        return typedThis;
    }

    public MyLambdaQueryWrapper<T> max(SFunction<T, ?> column){
        if (column != null) {
            this.sqlSelect.getParts().add(String.format("max(%s)", columnToString(column)));
        }
        return typedThis;
    }

    public MyLambdaQueryWrapper<T> sum(SFunction<T, ?> column){
        if (column != null) {
            this.sqlSelect.getParts().add(String.format("sum(%s)", columnToString(column)));
        }
        return typedThis;
    }

    public MyLambdaQueryWrapper<T> sum(String sql){
        if (!StringUtils.isEmpty(sql)) {
            this.sqlSelect.getParts().add(String.format("sum(%s)", sql));
        }
        return typedThis;
    }

    public MyLambdaQueryWrapper<T> count(SFunction<T, ?> column){
        if (column != null) {
            this.sqlSelect.getParts().add(String.format("count(%s)", columnToString(column)));
        }
        return typedThis;
    }

    public MyLambdaQueryWrapper<T> count(){
        this.sqlSelect.getParts().add("count(0)");
        return typedThis;
    }

    public MyLambdaQueryWrapper<T> as(String asName){
        if (StringUtils.isEmpty(asName)) {
            return typedThis;
        }

        List<String> parts = this.sqlSelect.getParts();

        if (CollectionUtil.isEmpty(parts)) {
            return typedThis;
        }

        String lastPart = parts.remove(parts.size() - 1);
        lastPart += " as " + asName;

        parts.add(lastPart);


        return typedThis;
    }


    public MyLambdaQueryWrapper<T> orderBy(boolean isAsc, String column) {
        if (StringUtils.isEmpty(column)) {
            return typedThis;
        }
        SqlKeyword mode = isAsc ? ASC : DESC;
        doIt(true, ORDER_BY, () -> column, mode);
        return typedThis;
    }

    /**
     * 原始 where sql
     */
    @SafeVarargs
    public final MyLambdaQueryWrapper<T> originWhereSql(String sqlFormat, OriginSqlBo<T>... sqlBos) {

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

        return typedThis;
    }

    /**
     * 原始 select sql
     */
    @SafeVarargs
    public final MyLambdaQueryWrapper<T> originSelectSql(String sqlFormat, SFunction<T,?>... sFunctions) {

        Object[] objects = new Object[sFunctions.length];

        for (int i = 0; i < sFunctions.length; i++) {
            String column = columnToString(sFunctions[i]);

            if (column != null) {
                objects[i] = column;
            }
        }

        if (!CollectionUtil.isEmpty(objects)) {
            String format = String.format(sqlFormat, objects);
            this.sqlSelect.getParts().add(format);
        }

        return typedThis;
    }

    @Override
    public String columnToString(SFunction<T, ?> column) {
        return super.columnToString(column);
    }

    protected SelectBuilder getSelectBuilder() {
        return this.sqlSelect;
    }

}
