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
import com.hgf.helper.mybatisplus.helper.SelectBuilder;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.baomidou.mybatisplus.core.enums.SqlKeyword.ASC;
import static com.baomidou.mybatisplus.core.enums.SqlKeyword.DESC;
import static com.baomidou.mybatisplus.core.toolkit.StringPool.COMMA;

/**
 * 多表联查
 *
 * @param <T>
 */

@Setter
@Getter
public class JoinLambdaQueryWrapper<T> extends MyLambdaQueryWrapper<T> {
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


    protected final JoinLambdaQueryWrapper<T> typedThis = this;


    private Map<String, ColumnCache> columnMap = null;
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
    private Set<String> groupByColumns;

    /**
     * 缓存的排序条件
     */
    private Set<OrderByCache> orderByColumns;

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

    Map<JoinLambdaQueryWrapper<?>, JoinSqlInfo> wrapperMap = new HashMap<>();

    Map<String, JoinLambdaQueryWrapper<?>> paramValMap = new HashMap<>();


    public boolean existsWrapper(String tableAlias) {
        return wrapperMap.keySet().stream().filter(t -> t.getTableAlias().equals(tableAlias)).findFirst().map(k -> true).orElse(false);
    }


    /**
     * 连接查询
     */
    public <K,E> JoinLambdaQueryWrapper<K> join(JoinQueryBuilder<K,T,E> builder) {
        JoinColumnInfo joinColumnInfo = JoinColumnParseHelper.wrapperJoinColumnInfo(builder, isQueryTargetEntityResult ? queryType : super.getEntityClass(), isQueryTargetEntityResult, super.getEntityClass());


        String joinTableColumnPrefix = JoinTableHelper.getJoinTableColumnPrefix(joinColumnInfo.getQueryTypeJoinField());
        // 已经存在此连接关系
        if (existsWrapper(joinTableColumnPrefix)) {
            return null;
        }

        Class<K> joinEntityClass = (Class<K>) joinColumnInfo.getJoinEntityClass();

        JoinLambdaQueryWrapper<K> lambdaQueryWrapper = new JoinLambdaQueryWrapper<>(joinEntityClass, null);
        lambdaQueryWrapper.setAliasPrefix(joinTableColumnPrefix);
        lambdaQueryWrapper.setJoinFlag(builder.getJoinEnum().getSql());

        JoinSqlInfo joinSqlInfo = new JoinSqlInfo();
        joinSqlInfo.setJoinType(builder.getJoinEnum().getSql());
        joinSqlInfo.setMainTableColumn(joinColumnInfo.getMainTableColumn());
        joinSqlInfo.setJoinTableColumn(joinColumnInfo.getJoinTableColumn());
        joinSqlInfo.setJoinTableName(lambdaQueryWrapper.getTableName());
        joinSqlInfo.setMainTableName(this.tableName);
        joinSqlInfo.setJoinTableAlias(joinTableColumnPrefix);


        wrapperMap.put(lambdaQueryWrapper, joinSqlInfo);
        paramValMap.put(lambdaQueryWrapper.getTableAlias(), lambdaQueryWrapper);


        return lambdaQueryWrapper;
    }


    public JoinLambdaQueryWrapper(Class<T> entityClass, Class<?> queryType) {
        super(entityClass);
        this.queryType = queryType;
        if (queryType != null) {
            this.isQueryTargetEntityResult = true;
        }
        initTableName();
    }

    JoinLambdaQueryWrapper(Class<T> entityClass, Class<?> queryType, AtomicInteger paramNameSeq, Map<String, Object> paramNameValuePairs) {
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

    @Override
    protected String columnToString(SFunction<T, ?> column, boolean onlyColumn) {

        // 只要是查询条件拼接的，
        return getColumn(LambdaUtils.resolve(column), false);
    }

    /**
     * SELECT 部分 SQL 设置
     *
     * @param columns 查询字段
     */
    @Override
    @SafeVarargs
    public final JoinLambdaQueryWrapper<T> select(SFunction<T, ?>... columns) {
        if (ArrayUtils.isNotEmpty(columns)) {
            Stream.of(columns).forEach(t -> super.getSelectBuilder().getParts().add(getColumn(LambdaUtils.resolve(t), true)));
        }
        return typedThis;
    }


    private String getColumn(SerializedLambda lambda, boolean onlyColumn) {
        Class<?> aClass = lambda.getInstantiatedType();
        tryInitCache(aClass);
        String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
        ColumnCache columnCache = getColumnCache(fieldName, aClass);
        return onlyColumn ? columnCache.getColumn() : columnCache.getColumnSelect();
    }

    /**
     * 初始化 ColumnMap
     */
    private void tryInitCache(Class<?> lambdaClass) {
        if (!initColumnMap) {
            final Class<T> entityClass = getEntityClass();
            if (entityClass != null) {
                lambdaClass = entityClass;
            }
            columnMap = LambdaUtils.getColumnMap(lambdaClass);
            for (String field : columnMap.keySet()) {

                ColumnCache columnCache = columnMap.get(field);
                columnCache.setColumnSelect(getTableAlias() + "." + columnCache.getColumn());
            }

            initColumnMap = true;
        }
        Assert.notNull(columnMap, "can not find lambda cache for this entity [%s]", lambdaClass.getName());
    }


    private ColumnCache getColumnCache(String fieldName, Class<?> lambdaClass) {
        ColumnCache columnCache = columnMap.get(LambdaUtils.formatKey(fieldName));
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

        SelectBuilder selectBuilder = super.getSelectBuilder();
        List<String> parts = selectBuilder.getParts();

        String mainSql = null;
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
                .map(JoinLambdaQueryWrapper::getSqlSelect)
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

        String sql = this.wrapperMap.keySet().stream().map(t -> formatSqlSegment(t.getExpression().getSqlSegment(),t.getTableAlias())).collect(Collectors.joining(" ")).trim();

        if (!StringUtils.isEmpty(sql)) {
            sql = String.format(" %s %s ", SqlKeyword.AND.getSqlSegment(), sql);
        }
        String otherSegment = getOtherSegment();

        return String.format("%s \n %s %s", expression.getSqlSegment() + otherSegment, sql, lastSql.getStringValue());
    }

    protected String formatSqlSegment(String origin,String tableAlias) {

        return origin.replace(PARAM_NAME_VALUE_PAIRS_FIELD, String.format("%s.%s.%s",PARAM_MAP_FIELD, tableAlias, PARAM_NAME_VALUE_PAIRS_FIELD));
    }

    /**
     * 获取附表的 GROUP\HAVING\ORDER sql 片段
     */
    public String getOtherSegment() {
        if (otherSqlMergeSegments != null) {
            return otherSqlMergeSegments.getSqlSegment();
        }

        otherSqlMergeSegments = new MergeSegments();

        for (JoinLambdaQueryWrapper<?> joinLambdaQueryWrapper : wrapperMap.keySet()) {
            Set<String> groupByColumns = joinLambdaQueryWrapper.getGroupByColumns();

            if (CollectionUtil.isNotEmpty(groupByColumns)) {
                otherSqlMergeSegments.add(doIt(true, SqlKeyword.GROUP_BY, () -> String.join(COMMA, groupByColumns)));
            }

            Set<OrderByCache> orderByColumns = joinLambdaQueryWrapper.getOrderByColumns();

            if (CollectionUtil.isNotEmpty(orderByColumns)) {

                orderByColumns.forEach(t -> {
                    SqlKeyword mode = t.isDesc ? DESC : ASC;
                    otherSqlMergeSegments.add(
                            doIt(true, SqlKeyword.ORDER_BY, () -> t.column, mode)
                    );
                });

            }
        }

        if (CollectionUtil.isNotEmpty(this.groupByColumns)) {
            otherSqlMergeSegments.add(doIt(true, SqlKeyword.GROUP_BY, () -> String.join(COMMA, this.groupByColumns)));
        }

        if (CollectionUtil.isNotEmpty(orderByColumns)) {

            orderByColumns.forEach(t -> {
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
        /*String joinSql = this.wrapperList.keySet().stream().map(t -> {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(t.getEntityClass());

            String allSqlSelect = tableInfo.getAllSqlSelect();
            return t.wrapperAliasSelect(allSqlSelect);

        }).collect(Collectors.joining(COMMA));

        return String.join(COMMA, mainSql, joinSql);*/
    }

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

    @SafeVarargs
    @Override
    public final JoinLambdaQueryWrapper<T> groupBy(boolean condition, SFunction<T, ?>... columns) {
        if (CollectionUtil.isEmpty(columns)) {
            return typedThis;
        }


        if (this.groupByColumns == null) {
            this.groupByColumns = new HashSet<>();
        }


        this.groupByColumns.addAll(
                Stream.of(columns)
                        .map(this::columnToString)
                        .collect(Collectors.toList())
        );

        return typedThis;
    }

    @Override
    protected JoinLambdaQueryWrapper<T> instance() {
        return new JoinLambdaQueryWrapper<>(getEntityClass(), getQueryType(), paramNameSeq, paramNameValuePairs);
    }

    @Override
    public JoinLambdaQueryWrapper<T> groupBy(SFunction<T, ?> column) {
        return groupBy(true, column);
    }

    @Override
    public JoinLambdaQueryWrapper<T> groupBy(SFunction<T, ?>... columns) {
        return groupBy(true, columns);
    }

    @Override
    public JoinLambdaQueryWrapper<T> orderByAsc(SFunction<T, ?> column) {
        return orderBy(true, true, column);
    }

    @Override
    public JoinLambdaQueryWrapper<T> orderByAsc(SFunction<T, ?>... columns) {
        return orderBy(true, true, columns);
    }

    @Override
    public JoinLambdaQueryWrapper<T> orderByAsc(boolean condition, SFunction<T, ?>... columns) {
        return orderBy(condition, true, columns);
    }

    @Override
    public JoinLambdaQueryWrapper<T> orderByDesc(SFunction<T, ?> column) {
        return orderBy(true, false, column);
    }

    @Override
    public JoinLambdaQueryWrapper<T> orderByDesc(SFunction<T, ?>... columns) {
        return orderBy(true, false, columns);
    }

    @Override
    public JoinLambdaQueryWrapper<T> orderByDesc(boolean condition, SFunction<T, ?>... columns) {
        return orderBy(true, false, columns);
    }

    @SafeVarargs
    @Override
    public final JoinLambdaQueryWrapper<T> orderBy(boolean condition, boolean isAsc, SFunction<T, ?>... columns) {
        if (CollectionUtil.isEmpty(columns)) {
            return typedThis;
        }

        if (this.orderByColumns == null) {
            this.orderByColumns = new HashSet<>();
        }


        Set<OrderByCache> collect = Stream.of(columns)
                .map(t -> new OrderByCache(!isAsc, columnToString(t)))
                .collect(Collectors.toSet());
        this.orderByColumns.addAll(collect);

        return typedThis;
    }


    public Set<String> getGroupByColumns() {
        return groupByColumns;
    }

    public Set<OrderByCache> getOrderByColumns() {
        return orderByColumns;
    }

    @Override
    public void clear() {
        super.clear();
        this.joinFlag = null;
        this.tableName = null;
        this.aliasPrefix = null;
        this.columnMap = null;
        this.joinSql = null;
        this.selectSqlCache = null;
        this.groupByColumns = null;
        this.orderByColumns = null;
        this.otherSqlMergeSegments = null;
        this.queryType = null;
    }

    public String getTableAlias() {
        return StringUtils.isEmpty(aliasPrefix) ? tableName : aliasPrefix;
    }

}
