package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.support.SerializedLambda;
import com.hgf.helper.mybatisplus.annotation.Association;
import com.hgf.helper.mybatisplus.annotation.MpJoinColumn;
import com.hgf.helper.mybatisplus.helper.MyLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.baomidou.mybatisplus.core.toolkit.StringPool.COMMA;

/**
 * @Author: zhengcan
 * @Date: 2022/5/22
 * @Description: 关联附表的wrapper
 * @Version: 1.0.0 创建
 */
@Setter
@Getter
public class JoinTableLambdaQueryWrapper<K> extends MyLambdaQueryWrapper<K> {

    /**
     * 连接类型
     */
    private String joinFlag;
    /**
     * 表别名
     */
    private String tableName;
    /**
     * 别名
     */
    private String aliasPrefix;
    /**
     * 自身实例缓存
     */
    protected final JoinTableLambdaQueryWrapper<K> typedThis = this;
    /**
     * 缓存的列集合
     */
    private Map<String, ColumnCache> columnMap = null;
    /**
     * 是否初始化缓存列集合
     */
    private boolean initColumnMap = false;
    /**
     * 连接sql
     */
    private String joinSql;
    /**
     * 是否已经初始化 isInitJoinSql
     */
    private boolean isInitJoinSql;
    /**
     * 查询column sql 是否已经缓存
     */
    private boolean isCachedSelectSql;
    /**
     * 缓存的查询column sql
     */
    private String selectSqlCache;
    /**
     * 缓存的group条件
     */
    private Set<String> groupByColumns = new HashSet<>();
    /**
     * 缓存的排序条件
     */
    private Set<OrderByCache> orderByColumns = new HashSet<>();
    /**
     * 查询结果类型是否是指定的类型
     */
    private boolean isQueryTargetEntityResult;
    /**
     * 查询返回结果
     */
    private Class<?> queryType;
    /**
     * joinQuerySpec
     */
    private JoinTableLambdaQueryWrapper<K>.JoinQuerySpec<K, ?, ?> joinQuerySpec = null;

    public JoinTableLambdaQueryWrapper(Class<K> entityClass, Class<?> queryType) {
        super(entityClass);
        this.queryType = queryType;
        this.isQueryTargetEntityResult = !ObjectUtils.isEmpty(queryType);
        initTableName();
    }

    public JoinTableLambdaQueryWrapper(Class<K> entityClass, Class<?> queryType, AtomicInteger paramNameSeq, Map<String, Object> paramNameValuePairs) {
        this(entityClass, queryType);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
    }

    public void initTableName() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(super.getEntityClass());
        if (tableInfo == null) {
            throw new RuntimeException("tableInfo not found");
        }
        this.tableName = tableInfo.getTableName();
    }

    public <K, T, E> FromTableLambdaQueryWrapper<T> joinTable(FromTableLambdaQueryWrapper<T> queryWrapper){
        Assert.notNull(this.joinQuerySpec, "the joinQuerySpec is null");
        return joinTable(queryWrapper, (JoinTableLambdaQueryWrapper<K>.JoinQuerySpec<K, T, E>)this.joinQuerySpec);
    }

    public <K, T, E> FromTableLambdaQueryWrapper<T> joinTable(FromTableLambdaQueryWrapper<T> queryWrapper, JoinQueryBase<K, T, E> builder) {
        return queryWrapper.joinTable((JoinTableLambdaQueryWrapper<K>) this, builder);
    }

    public String getTableAlias() {
        return StringUtils.isEmpty(aliasPrefix) ? tableName : aliasPrefix;
    }

    /**
     * 查询 column sql
     */
    @Override
    public String getSqlSelect() {
        if (isCachedSelectSql) {
            return selectSqlCache;
        }
        List<String> parts = super.getSelectBuilder().getParts();
        String joinSql;
        if (CollectionUtil.isEmpty(parts)) {
            joinSql = getFullColumnSqlSelect();
        } else {
            joinSql = parts.stream()
                    .map(t -> builderSelectSegment(t))
                    .collect(Collectors.joining(COMMA));
        }
        selectSqlCache = joinSql;
        isCachedSelectSql = true;
        return joinSql;
    }

    /**
     * 拼接全部字段的sql 查询
     */
    public String getFullColumnSqlSelect() {
        TableInfo mainTableInfo = TableInfoHelper.getTableInfo(super.getEntityClass());
        String mainSql = mainTableInfo.getAllSqlSelect();
        return wrapperAliasSelect(mainSql);
    }

    /**
     * 构建select查询列语句
     * @param column
     * @return
     */
    public String builderSelectSegment(String column) {
        if (column.toLowerCase().contains(" as ")) {
            if (column.contains(getTableAlias() + ".")) {
                return column;
            }else {
                return getTableAlias() + "." + column;
            }
        }
        if (column.contains(getTableAlias() + ".")) {
            return String.format("%s as `%s%s`", column, StringUtils.isEmpty(aliasPrefix) ? "" : aliasPrefix, column.split("\\.")[1]);
        }
        return String.format("%s.%s as `%s%s`", getTableAlias(), column, StringUtils.isEmpty(aliasPrefix) ? "" : aliasPrefix, column);
    }

    /**
     * 将原有的查询sql改成有别名的
     */
    public String wrapperAliasSelect(String originSql) {
        return Arrays.stream(originSql.split(COMMA))
                .map(this::builderSelectSegment)
                .collect(Collectors.joining(","));
    }

    /**
     * SELECT 部分 SQL 设置
     * @param columns 查询字段
     */
    @Override
    @SafeVarargs
    public final JoinTableLambdaQueryWrapper<K> select(SFunction<K, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            Stream.of(columns)
                    .forEach(t -> super.getSelectBuilder().getParts()
                            .add(getColumn(LambdaUtils.resolve(t), true)));
        }
        return typedThis;
    }

    @SafeVarargs
    @Override
    public final JoinTableLambdaQueryWrapper<K> groupBy(boolean condition, SFunction<K, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            this.groupByColumns.addAll(
                    Stream.of(columns)
                            .map(this::columnToString)
                            .collect(Collectors.toList())
            );
        }
        return typedThis;
    }

    @Override
    protected JoinTableLambdaQueryWrapper<K> instance() {
        return new JoinTableLambdaQueryWrapper<>(getEntityClass(), getQueryType(), paramNameSeq, paramNameValuePairs);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> groupBy(SFunction<K, ?> column) {
        return groupBy(true, column);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> groupBy(SFunction<K, ?>... columns) {
        return groupBy(true, columns);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> orderByAsc(SFunction<K, ?> column) {
        return orderBy(true, true, column);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> orderByAsc(SFunction<K, ?>... columns) {
        return orderBy(true, true, columns);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> orderByAsc(boolean condition, SFunction<K, ?>... columns) {
        return orderBy(condition, true, columns);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> orderByDesc(SFunction<K, ?> column) {
        return orderBy(true, false, column);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> orderByDesc(SFunction<K, ?>... columns) {
        return orderBy(true, false, columns);
    }

    @Override
    public JoinTableLambdaQueryWrapper<K> orderByDesc(boolean condition, SFunction<K, ?>... columns) {
        return orderBy(true, false, columns);
    }

    @SafeVarargs
    @Override
    public final JoinTableLambdaQueryWrapper<K> orderBy(boolean condition, boolean isAsc, SFunction<K, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            this.orderByColumns.addAll(
                    Stream.of(columns)
                            .map(t -> new OrderByCache(!isAsc, columnToString(t)))
                            .collect(Collectors.toSet()));
        }
        return typedThis;
    }

    public Set<String> getGroupByColumns() {
        return groupByColumns;
    }

    public Set<OrderByCache> getOrderByColumns() {
        return orderByColumns;
    }

    /**
     * 获取查询列名
     * @param lambda
     * @param onlyColumn
     * @return
     */
    private String getColumn(SerializedLambda lambda, boolean onlyColumn) {
        Class<?> aClass = lambda.getInstantiatedType();
        tryInitCache(aClass);
        String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
        ColumnCache columnCache = getColumnCache(fieldName, aClass);
        return onlyColumn ? columnCache.getColumn() : columnCache.getColumnSelect();
    }

    /**
     * 初始化查询列名：“表别名.原列名”
     */
    private void tryInitCache(Class<?> lambdaClass) {
        if (!initColumnMap) {
            final Class<K> entityClass = getEntityClass();
            if (entityClass != null) {
                lambdaClass = entityClass;
            }
            this.columnMap = LambdaUtils.getColumnMap(lambdaClass);
            for (String field : this.columnMap.keySet()) {
                ColumnCache columnCache = this.columnMap.get(field);
                columnCache.setColumnSelect(getTableAlias() + "." + columnCache.getColumn());
            }
            initColumnMap = true;
        }
        Assert.notNull(this.columnMap, "can not find lambda cache for this entity [%s]", lambdaClass.getName());
    }

    /**
     * 获取已经加上别名的查询列名
     * @param fieldName
     * @param lambdaClass
     * @return
     */
    private ColumnCache getColumnCache(String fieldName, Class<?> lambdaClass) {
        ColumnCache columnCache = this.columnMap.get(LambdaUtils.formatKey(fieldName));
        Assert.notNull(columnCache, "can not find lambda cache for this property [%s] of entity [%s]",
                fieldName, lambdaClass.getName());
        return columnCache;
    }

    /**
     * 创建连接查询条件实体类
     * @param <K>
     * @param <T>
     * @param <E>
     * @return
     */
    public <K, T, E> JoinTableLambdaQueryWrapper<K>.JoinQuerySpec<K, T, E> buildJoinQuerySpec() {
        this.joinQuerySpec = new JoinQuerySpec<>();
        return (JoinTableLambdaQueryWrapper<K>.JoinQuerySpec<K, T, E>) joinQuerySpec;
    }

    /**
     * 连接查询条件
     * @param <K>           附表实体类
     * @param <T>           主表实体类
     * @param <E>           查询结果
     */
    @Getter
    public class JoinQuerySpec<K, T, E> implements JoinQueryBase<K, T, E>{
        /**
         * 连接类型
         */
        JoinEnum joinEnum;
        /**
         * 主表连接属性
         */
        SFunction<T, ?> mainJoinField;
        /**
         * 附表连接属性
         */
        SFunction<K, ?> joinField;
        /**
         * 被 {@link Association} 标记的属性，
         */
        SFunction<E, K> associationAnnField;
        /**
         * 同时被
         * {@link MpJoinColumn}
         * {@link Association}
         * 标记的属性
         */
        SFunction<E, K> joinColumnAnnFiled;

        public JoinQuerySpec<K, T, E> joinEnum(JoinEnum joinEnum) {
            this.joinEnum = joinEnum;
            return this;
        }

        public JoinQuerySpec<K, T, E> mainJoinField(SFunction<T, ?> mainJoinField) {
            this.mainJoinField = mainJoinField;
            return this;
        }

        public JoinQuerySpec<K, T, E> joinField(SFunction<K, ?> joinField) {
            this.joinField = joinField;
            return this;
        }

        public JoinQuerySpec<K, T, E> associationAnnField(SFunction<E, K> associationAnnField) {
            this.associationAnnField = associationAnnField;
            return this;
        }

        public JoinQuerySpec<K, T, E> joinColumnAnnFiled(SFunction<E, K> joinColumnAnnFiled) {
            this.joinColumnAnnFiled = joinColumnAnnFiled;
            return this;
        }

        public JoinTableLambdaQueryWrapper<K> build() {
            return (JoinTableLambdaQueryWrapper<K>) JoinTableLambdaQueryWrapper.this;
        }
    }
}
