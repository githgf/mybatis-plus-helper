package com.hgf.helper.mybatisplus.helper;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.hgf.helper.mybatisplus.context.BaseSpringContext;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;

/**
 * Sql打印拦截器
 */
@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "batch", args = { Statement.class })})
public class SqlCostInterceptor implements Interceptor {

    protected static final String ENV_PRINT_QUERY = "mp.printQuery";
    protected static final String ENV_BIG_SIZE = "mp.bigListSize";
    protected static final String ENV_EXEC_TIMEOUT_WARN = "mp.execTimeOut";

    @Autowired
    Environment environment;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();

        long startTime = System.currentTimeMillis();
        StatementHandler statementHandler = (StatementHandler)target;
        try {
            return invocation.proceed();
        } finally {
            printSqlLog(startTime, statementHandler);
        }
    }

    /**
     * 打印sql 日志
     * 如果耗时还是没解决考虑异步
     */
    protected void printSqlLog(long startTime, StatementHandler statementHandler) {
        BoundSql boundSql = statementHandler.getBoundSql();
        Object parameterObject = boundSql.getParameterObject();
        String sql = boundSql.getSql().trim();

        Boolean enable = environment.getProperty(ENV_PRINT_QUERY, Boolean.class);

        // 不打印查询sql日志
        if (sql.toLowerCase().startsWith("select") && BooleanUtils.isFalse(enable)) {
            return;
        }

        long endTime = System.currentTimeMillis();
        long sqlCost = endTime - startTime;

        // 格式化Sql语句，去除换行符，替换参数
        // 推荐方案
        String allSql = null;
        // 如果当前参数是大的list不打印sql，否则会造成打印时间很长
        if (!verifyBigArray(parameterObject)) {

            try {
                allSql = getFllSql(boundSql, parameterObject);
            } catch (Exception e) {
                log.warn("getFllSql 格式化失败:{}", e.getMessage());
            }

            try {
                if (allSql == null) {
                    List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
                    // 备选方案
                    allSql = formatSql(sql, parameterObject, parameterMappingList);
                }
            } catch (Exception e) {
                log.warn("formatSql 格式化失败:{}", e.getMessage());
            }
        }


        if (allSql == null) {
            allSql = beautifySql(sql);
        }

        long printCost = System.currentTimeMillis() - endTime;

        printSql(allSql, sqlCost, printCost);
    }

    /**
     * 打印sql
     * @param allSql                sql语句
     * @param sqlCost               sql执行用时(ms)
     * @param printCost             sql打印用时(ms)
     */
    protected void printSql(String allSql, long sqlCost, long printCost) {
        int timeout = environment.getProperty(ENV_EXEC_TIMEOUT_WARN, Integer.class, 100);


        log.info("SQL：[{}]  执行耗时    [ {} ms]  {} [ {} ms] ", allSql, sqlCost, printCost > timeout ? "打印耗时长警告" : "打印耗时", printCost);

    }

    /**
     * 验证当前参数是长度很大的list
     */
    protected boolean verifyBigArray(Object paramObj) {
        if (!(paramObj instanceof DefaultSqlSession.StrictMap)) {
            return false;
        }

        int bigSize = environment.getProperty(ENV_BIG_SIZE, Integer.class, 2);


        DefaultSqlSession.StrictMap strictMap = (DefaultSqlSession.StrictMap) paramObj;
        Object collection = strictMap.get("collection");
        if (collection != null && (collection instanceof Collection)) {
            Collection collection1 = (Collection) collection;
            return collection1.size() > bigSize;
        }
        return false;


    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 格式化sql语句（备选方案）
     */
    @SuppressWarnings("unchecked")
    protected String formatSql(String sql, Object parameterObject, List<ParameterMapping> parameterMappingList) {
        // 输入sql字符串空判断
        if (sql == null || sql.length() == 0) {
            return "";
        }

        // 美化sql
        sql = beautifySql(sql);

        // 不传参数的场景，直接把Sql美化一下返回出去
        if (parameterObject == null || parameterMappingList == null || parameterMappingList.size() == 0) {
            return sql;
        }

        // 定义一个没有替换过占位符的sql，用于出异常时返回
        String sqlWithoutReplacePlaceholder = sql;

        try {
            if (parameterMappingList != null) {
                Class<?> parameterObjectClass = parameterObject.getClass();

                // 如果参数是StrictMap且Value类型为Collection，获取key="list"的属性，这里主要是为了处理<foreach>循环时传入List这种参数的占位符替换
                // 例如select * from xxx where id in <foreach collection="list">...</foreach>
                if (isStrictMap(parameterObjectClass)) {
                    DefaultSqlSession.StrictMap<Collection<?>> strictMap = (DefaultSqlSession.StrictMap<Collection<?>>)parameterObject;

                    if (isList(strictMap.get("list").getClass())) {
                        sql = handleListParameter(sql, strictMap.get("list"));
                    }
                } else if (isMap(parameterObjectClass)) {
                    // 如果参数是Map则直接强转，通过map.get(key)方法获取真正的属性值
                    // 这里主要是为了处理<insert>、<delete>、<update>、<select>时传入parameterType为map的场景
                    Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
                    sql = handleMapParameter(sql, paramMap, parameterMappingList);
                }  else {
                    // 通用场景，比如传的是一个自定义的对象或者八种基本数据类型之一或者String
                    sql = handleCommonParameter(sql, parameterMappingList, parameterObjectClass, parameterObject);
                }
            }
        } catch (Exception e) {
            // 占位符替换过程中出现异常，则返回没有替换过占位符但是格式美化过的sql，这样至少保证sql语句比BoundSql中的sql更好看
            return sqlWithoutReplacePlaceholder;
        }

        return sql;
    }

    /**
     * 美化Sql
     */
    protected String beautifySql(String sql) {
        sql = sql.replace("\n", "").replace("\t", "").replace("  ", " ").replace("( ", "(").replace(" )", ")").replace(" ,", ",");

        return sql;
    }

    /**
     * 处理参数为List的场景
     */
    protected String handleListParameter(String sql, Collection<?> col) {
        if (col != null && col.size() != 0) {
            for (Object obj : col) {
                String value = null;
                Class<?> objClass = obj.getClass();

                // 只处理基本数据类型、基本数据类型的包装类、String这三种
                // 如果是复合类型也是可以的，不过复杂点且这种场景较少，写代码的时候要判断一下要拿到的是复合类型中的哪个属性
                if (isPrimitiveOrPrimitiveWrapper(objClass)) {
                    value = obj.toString();
                } else if (objClass.isAssignableFrom(String.class)) {
                    value = "\"" + obj.toString() + "\"";
                }

                sql = sql.replaceFirst("\\?", value);
            }
        }

        return sql;
    }

    /**
     * 处理mybatis 参数
     */
    protected String handleMybatisMapParameter(String sql, MapperMethod.ParamMap paramMap, List<ParameterMapping> parameterMappingList){
        Map<String, Object> nameValuePairs = null;
        Collection values = paramMap.values();

        boolean isPlus = false;
        for (Object value : values) {
            if (value instanceof AbstractWrapper) {
                isPlus = true;
                break;
            }
        }

        for (ParameterMapping parameterMapping : parameterMappingList) {
            String propertyName = parameterMapping.getProperty();

            Object propertyValue = null;
            if (isPlus) {
                String[] split = propertyName.split("\\.");

                if (nameValuePairs == null) {
                    propertyValue = paramMap.get(split[0]);

                    if (propertyValue != null) {
                        AbstractWrapper wrapper = (AbstractWrapper) propertyValue;
                        nameValuePairs = wrapper.getParamNameValuePairs();

                    }
                }

                propertyValue = nameValuePairs.get(split[2]);
            }else {
                propertyValue = paramMap.get(propertyName);
            }

            if (propertyValue == null) {
                propertyValue = "null";
            }

            if (propertyValue.getClass().isAssignableFrom(String.class)) {
                propertyValue = "\"" + propertyValue + "\"";
            }

            sql = sql.replaceFirst("\\?", propertyValue.toString());

        }
        return sql;
    }

    /**
     * 处理参数为Map的场景
     */
    protected String handleMapParameter(String sql, Map<?, ?> paramMap, List<ParameterMapping> parameterMappingList) {

        if (paramMap instanceof MapperMethod.ParamMap) {
            return handleMybatisMapParameter(sql, (MapperMethod.ParamMap) paramMap, parameterMappingList);
        }else {
            for (ParameterMapping parameterMapping : parameterMappingList) {
                Object propertyName = parameterMapping.getProperty();
                Object propertyValue = paramMap.get(propertyName);
                if (propertyValue != null) {
                    if (propertyValue.getClass().isAssignableFrom(String.class)) {
                        propertyValue = "\"" + propertyValue + "\"";
                    }

                } else {
                    propertyValue = "null";
                }
                sql = sql.replaceFirst("\\?", propertyValue.toString());
            }
        }

        return sql;
    }

    /**
     * 处理通用的场景
     */
    protected String handleCommonParameter(String sql, List<ParameterMapping> parameterMappingList, Class<?> parameterObjectClass,
            Object parameterObject) throws Exception {
        for (ParameterMapping parameterMapping : parameterMappingList) {
            String propertyValue = null;
            // 基本数据类型或者基本数据类型的包装类，直接toString即可获取其真正的参数值，其余直接取paramterMapping中的property属性即可
            if (isPrimitiveOrPrimitiveWrapper(parameterObjectClass)) {
                propertyValue = parameterObject.toString();
            } else {
                String propertyName = parameterMapping.getProperty();

                Field field = parameterObjectClass.getDeclaredField(propertyName);
                // 要获取Field中的属性值，这里必须将私有属性的accessible设置为true
                field.setAccessible(true);
                propertyValue = String.valueOf(field.get(parameterObject));
                if (parameterMapping.getJavaType().isAssignableFrom(String.class)) {
                    propertyValue = "\"" + propertyValue + "\"";
                }
            }

            sql = sql.replaceFirst("\\?", propertyValue);
        }

        return sql;
    }

    /**
     * 是否基本数据类型或者基本数据类型的包装类
     */
    protected boolean isPrimitiveOrPrimitiveWrapper(Class<?> parameterObjectClass) {
        return parameterObjectClass.isPrimitive() ||
                (parameterObjectClass.isAssignableFrom(Byte.class) || parameterObjectClass.isAssignableFrom(Short.class) ||
                        parameterObjectClass.isAssignableFrom(Integer.class) || parameterObjectClass.isAssignableFrom(Long.class) ||
                        parameterObjectClass.isAssignableFrom(Double.class) || parameterObjectClass.isAssignableFrom(Float.class) ||
                        parameterObjectClass.isAssignableFrom(Character.class) || parameterObjectClass.isAssignableFrom(Boolean.class));
    }

    /**
     * 是否DefaultSqlSession的内部类StrictMap
     */
    protected boolean isStrictMap(Class<?> parameterObjectClass) {
        return parameterObjectClass.isAssignableFrom(DefaultSqlSession.StrictMap.class);
    }

    /**
     * 是否List的实现类
     */
    protected boolean isList(Class<?> clazz) {
        Class<?>[] interfaceClasses = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (interfaceClass.isAssignableFrom(List.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 是否Map的实现类
     */
    protected boolean isMap(Class<?> parameterObjectClass) {
        if (parameterObjectClass.getName().equals(MapperMethod.ParamMap.class.getName())) {
            return true;
        }
        Class<?>[] interfaceClasses = parameterObjectClass.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (interfaceClass.isAssignableFrom(Map.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取完整的sql（推荐方案）
     */
    protected String getFllSql(BoundSql boundSql, Object parameterObject) {
        SqlSessionFactory bean = BaseSpringContext.getBean(SqlSessionFactory.class);
        if (bean == null) {
            return null;
        }
        Configuration configuration = bean.getConfiguration();

        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

        if (CollectionUtil.isEmpty(parameterMappings)) {
            return null;
        }
        List<Object> paramList = new ArrayList<>();

        for (int i = 0; i < parameterMappings.size(); i++) {
            ParameterMapping parameterMapping = parameterMappings.get(i);
            if (parameterMapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            Object value = null;
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
                /*TypeHandler typeHandler = parameterMapping.getTypeHandler();
                JdbcType jdbcType = parameterMapping.getJdbcType();
                if (value == null && jdbcType == null) {
                    jdbcType = configuration.getJdbcTypeForNull();
                }*/
            paramList.add(value);
        }
        String sql = boundSql.getSql();
        for (Object o : paramList) {
            sql = sql.replaceFirst("\\?", formatParamValue(o));
        }

        return beautifySql(sql);
    }

    protected String formatParamValue(Object paramValue) {
        if (paramValue == null) {
            return "null";
        }
        if (paramValue instanceof String) {
            paramValue =  "'" + paramValue + "'";
        }
        if (paramValue instanceof Date) {
            paramValue =  "'" + paramValue + "'";
        }
        return paramValue.toString();
    }

}