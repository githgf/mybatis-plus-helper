package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.support.SerializedLambda;
import com.hgf.helper.mybatisplus.helper.MyLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.baomidou.mybatisplus.core.enums.SqlKeyword.ASC;
import static com.baomidou.mybatisplus.core.enums.SqlKeyword.DESC;
import static com.baomidou.mybatisplus.core.toolkit.StringPool.COMMA;

/**
 * @Author: zhengcan
 * @Date: 2022/5/22
 * @Description: 关联主表的wrapper
 * @Version: 1.0.0 创建
 */
@Setter
@Getter
public class FromTableLambdaQueryWrapper<T> extends MyLambdaQueryWrapper<T> {

    public static final String PARAM_NAME_VALUE_PAIRS_FIELD = "paramNameValuePairs";
    public static final String PARAM_MAP_FIELD = "paramValMap";

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
    protected final FromTableLambdaQueryWrapper<T> typedThis = this;
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
     * 缓存 的 查询column sql
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
     * group、order 条件sql片端合并工具
     */
    MergeSegments otherSqlMergeSegments = null;
    /**
     * 查询结果类型是否是指定的类型
     */
    private boolean isQueryTargetEntityResult;
    /**
     * 查询返回结果
     */
    private Class<?> queryType;
    /**
     * join table的wrapper信息集合
     */
    Map<JoinTableLambdaQueryWrapper<?>, JoinSqlInfo> wrapperMap = new HashMap<>();
    /**
     * 附表集合,可有多个附表拼接
     */
    Map<String, JoinTableLambdaQueryWrapper<?>> paramValMap = new HashMap<>();

    public FromTableLambdaQueryWrapper(Class<T> entityClass, Class<?> queryType) {
        super(entityClass);
        this.queryType = queryType;
        this.isQueryTargetEntityResult = !ObjectUtils.isEmpty(queryType);
        initTableName();
    }

    public FromTableLambdaQueryWrapper(Class<T> entityClass, Class<?> queryType, AtomicInteger paramNameSeq, Map<String, Object> paramNameValuePairs) {
        this(entityClass, queryType);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
    }

    /**
     * 出初始化table信息
     */
    public void initTableName() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(super.getEntityClass());
        if (tableInfo == null) {
            throw new RuntimeException("tableInfo not found");
        }
        this.tableName = tableInfo.getTableName();
    }

    /**
     * 生成join table wrapper
     *
     * @param joinEntityClass
     * @param <K>
     * @return
     */
    public <K> JoinTableLambdaQueryWrapper<K> generateJoinTableWrapper(Class<K> joinEntityClass) {
        return new JoinTableLambdaQueryWrapper<>(joinEntityClass, null);
    }

    public FromTableLambdaQueryWrapper<T> nextJoin(){
        return typedThis;
    }

    public FromTableLambdaQueryWrapper<T> backFrom() {
        return typedThis;
    }


    /**
     * 解析join table信息
     *
     * @param queryWrapper
     * @param builder
     * @param <K>
     * @param <T>
     * @param <E>
     * @return
     */
    public <K, T, E> FromTableLambdaQueryWrapper<T> joinTable(JoinTableLambdaQueryWrapper<K> queryWrapper, JoinQueryBase<K, T, E> builder) {
        // 解析关联的列信息
        JoinColumnInfo joinColumnInfo = JoinColumnParseHelper.wrapperJoinColumnInfo(
                builder,
                isQueryTargetEntityResult ? queryType : super.getEntityClass(),
                isQueryTargetEntityResult,
                super.getEntityClass());
        // 表别名或列前缀
        String joinTableColumnPrefix = JoinTableHelper.getJoinTableColumnPrefix(joinColumnInfo.getQueryTypeJoinField());
        // 已经存在此连接关系
        if (existsWrapper(joinTableColumnPrefix)) {
            return null;
        }
        // 获取关联表的实体类型
        Class<K> joinEntityClass = (Class<K>) joinColumnInfo.getJoinEntityClass();

        JoinTableLambdaQueryWrapper<K> joinTableWrapper;
        if (ObjectUtils.isEmpty(queryWrapper)) {
            joinTableWrapper = generateJoinTableWrapper(joinEntityClass);
        } else {
            joinTableWrapper = queryWrapper;
        }

        JoinSqlInfo joinSqlInfo = new JoinSqlInfo()
                .setJoinType(builder.getJoinEnum().getSql())
                .setMainTableColumn(joinColumnInfo.getMainTableColumn())
                .setJoinTableColumn(joinColumnInfo.getJoinTableColumn())
                .setJoinTableName(joinTableWrapper.getTableName())
                .setMainTableName(this.tableName)
                .setJoinTableAlias(joinTableColumnPrefix);

        joinTableWrapper.setAliasPrefix(joinTableColumnPrefix);
        joinTableWrapper.setJoinFlag(builder.getJoinEnum().getSql());
        // 缓存wrapper相关信息
        wrapperMap.put(joinTableWrapper, joinSqlInfo);
        paramValMap.put(joinTableWrapper.getTableAlias(), joinTableWrapper);

        return (FromTableLambdaQueryWrapper<T>) typedThis;
    }

    /**
     * 获取表的别名
     *
     * @return
     */
    public String getTableAlias() {
        return StringUtils.isEmpty(aliasPrefix) ? tableName : aliasPrefix;
    }

    /**
     * 检查该附表是否已经存在
     *
     * @param tableAlias
     * @return
     */
    public boolean existsWrapper(String tableAlias) {
        return wrapperMap.keySet().stream().filter(t -> t.getTableAlias().equals(tableAlias)).findFirst().map(k -> true).orElse(false);
    }

    /**
     * where条件拼接时列一定要加上表别名
     *
     * @param column
     * @param onlyColumn
     * @return
     */
    @Override
    protected String columnToString(SFunction<T, ?> column, boolean onlyColumn) {
        return getColumn(LambdaUtils.resolve(column), false);
    }

    /**
     * 获取查询列名
     *
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
            final Class<T> entityClass = getEntityClass();
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
     *
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
     * 查询 column sql
     */
    @Override
    public String getSqlSelect() {
        if (isCachedSelectSql) {
            return selectSqlCache;
        }
        List<String> parts = super.getSelectBuilder().getParts();
        String mainSql;
        if (CollectionUtil.isEmpty(parts)) {
            mainSql = getFullColumnSqlSelect();
        } else {
            mainSql = parts.stream()
                    .map(t -> builderSelectSegment(t))
                    .collect(Collectors.joining(COMMA));
        }
        if (CollectionUtil.isEmpty(wrapperMap)) {
            return mainSql;
        }
        String joinSql = this.wrapperMap.keySet().stream()
                .map(JoinTableLambdaQueryWrapper::getSqlSelect)
                .collect(Collectors.joining(COMMA));
        String sql = String.join(COMMA, mainSql, joinSql);
        selectSqlCache = sql;
        isCachedSelectSql = true;
        return sql;

    }

    /**
     * 拼接 join sql
     */
    public String getJoinSql() {
        if (isInitJoinSql) {
            return joinSql;
        }
        joinSql = wrapperMap.values().stream().map(JoinSqlInfo::sql).collect(Collectors.joining("\n"));
        isInitJoinSql = true;
        return joinSql;
    }

    /**
     * 封装sql片段
     */
    @Override
    public String getSqlSegment() {
        String sql = this.wrapperMap.keySet().stream()
                .map(t -> formatSqlSegment(t.getExpression().getSqlSegment(), t.getTableAlias()))
                .collect(Collectors.joining(" ")).trim();
        if (!StringUtils.isEmpty(sql)) {
            sql = String.format(" %s %s ", SqlKeyword.AND.getSqlSegment(), sql);
        }
        String otherSegment = getOtherSegment();
        return String.format("%s \n %s %s", expression.getSqlSegment() + otherSegment, sql, lastSql.getStringValue());
    }

    protected String formatSqlSegment(String origin, String tableAlias) {
        return origin.replace(PARAM_NAME_VALUE_PAIRS_FIELD,
                String.format("%s.%s.%s", PARAM_MAP_FIELD, tableAlias, PARAM_NAME_VALUE_PAIRS_FIELD));
    }

    /**
     * 获取附表的 GROUP\HAVING\ORDER sql 片段
     */
    public String getOtherSegment() {
        if (otherSqlMergeSegments != null) {
            return otherSqlMergeSegments.getSqlSegment();
        }
        otherSqlMergeSegments = new MergeSegments();
        for (JoinTableLambdaQueryWrapper<?> queryWrapper : wrapperMap.keySet()) {
            if (CollectionUtil.isNotEmpty(queryWrapper.getGroupByColumns())) {
                this.groupByColumns.addAll(queryWrapper.getGroupByColumns());
            }
            if (CollectionUtil.isNotEmpty(queryWrapper.getOrderByColumns())) {
                this.orderByColumns.addAll(queryWrapper.getOrderByColumns());
            }
        }

        if (CollectionUtil.isNotEmpty(this.groupByColumns)) {
            otherSqlMergeSegments.add(
                    doIt(true, SqlKeyword.GROUP_BY, () -> String.join(COMMA, this.groupByColumns)));
        }
        if (CollectionUtil.isNotEmpty(this.orderByColumns)) {
            this.orderByColumns.forEach(t -> {
                SqlKeyword mode = t.isDesc ? DESC : ASC;
                otherSqlMergeSegments.add(
                        doIt(true, SqlKeyword.ORDER_BY, () -> t.column, mode)
                );
            });
        }
        return otherSqlMergeSegments.getSqlSegment();
    }

    /**
     * 查询条件为空(不包含entity)
     */
    @Override
    public boolean nonEmptyOfNormal() {
        return nonEmptyOfNormal() || wrapperMap.keySet().stream().filter(Wrapper::nonEmptyOfNormal).findFirst().map(Wrapper::nonEmptyOfNormal).orElse(false);
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
     * 将原有的查询sql改成有别名的
     */
    public String wrapperAliasSelect(String originSql) {
        return Arrays.stream(originSql.split(COMMA))
                .map(this::builderSelectSegment)
                .collect(Collectors.joining(","));
    }

    /**
     * 构建select查询列语句
     *
     * @param column
     * @return
     */
    public String builderSelectSegment(String column) {
        if (column.toLowerCase().contains(" as ")) {
            if (column.contains(getTableAlias() + ".")) {
                return column;
            } else {
                return getTableAlias() + "." + column;
            }
        }
        if (column.contains(getTableAlias() + ".")) {
            return String.format("%s as `%s%s`", column, StringUtils.isEmpty(aliasPrefix) ? "" : aliasPrefix, column.split("\\.")[1]);
        }
        return String.format("%s.%s as `%s%s`", getTableAlias(), column, StringUtils.isEmpty(aliasPrefix) ? "" : aliasPrefix, column);
    }

    /**
     * SELECT 部分 SQL 设置
     *
     * @param columns 查询字段
     */
    @Override
    @SafeVarargs
    public final FromTableLambdaQueryWrapper<T> select(SFunction<T, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            Stream.of(columns)
                    .forEach(t -> super.getSelectBuilder().getParts()
                            .add(getColumn(LambdaUtils.resolve(t), true)));
        }
        return typedThis;
    }

    @SafeVarargs
    @Override
    public final FromTableLambdaQueryWrapper<T> groupBy(boolean condition, SFunction<T, ?>... columns) {
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
    protected FromTableLambdaQueryWrapper<T> instance() {
        return new FromTableLambdaQueryWrapper<>(getEntityClass(), getQueryType(), paramNameSeq, paramNameValuePairs);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> groupBy(SFunction<T, ?> column) {
        return groupBy(true, column);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> groupBy(SFunction<T, ?>... columns) {
        return groupBy(true, columns);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> orderByAsc(SFunction<T, ?> column) {
        return orderBy(true, true, column);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> orderByAsc(SFunction<T, ?>... columns) {
        return orderBy(true, true, columns);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> orderByAsc(boolean condition, SFunction<T, ?>... columns) {
        return orderBy(condition, true, columns);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> orderByDesc(SFunction<T, ?> column) {
        return orderBy(true, false, column);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> orderByDesc(SFunction<T, ?>... columns) {
        return orderBy(true, false, columns);
    }

    @Override
    public FromTableLambdaQueryWrapper<T> orderByDesc(boolean condition, SFunction<T, ?>... columns) {
        return orderBy(true, false, columns);
    }

    @SafeVarargs
    @Override
    public final FromTableLambdaQueryWrapper<T> orderBy(boolean condition, boolean isAsc, SFunction<T, ?>... columns) {
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

}
