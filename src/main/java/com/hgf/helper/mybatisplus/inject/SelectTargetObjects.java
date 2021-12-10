package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import com.hgf.helper.mybatisplus.helper.ResultMapperBuilder;
import com.hgf.helper.mybatisplus.annotation.SelectTargetResult;
import com.hgf.helper.mybatisplus.join.JoinHelperStartHook;
import com.hgf.helper.mybatisplus.join.JoinLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.utils.ReflectUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.*;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


/**
 * 多表联查返回指定对象（非数据表映射实体类） mapper 方法注入
 *
 * mapper 中需要加上 {@link SelectTargetResult} 注解
 *
 * 如下所示
 * <p>
 *     @SelectTargetResult
 *     ResultVo getResultVo(@Param(Constants.WRAPPER) Wrappers<T> wrappers);
 * </p>
 *
 */
@Slf4j
public class SelectTargetObjects extends AbstractJoinSelectMethod {

    private static List<Cache> cacheList = new ArrayList<>();

    private ResultMapperBuilder resultMapperBuilder;

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {

        cacheList.add(new Cache(mapperClass, modelClass, builderAssistant));

        JoinHelperStartHook.setSelectJoinTargetInject(this);

        if (resultMapperBuilder == null) {
            resultMapperBuilder = new ResultMapperBuilder(configuration);
        }

        return null;
        /*JoinTableHelper.createJoinTableIn(tableInfo);


        String sql = String.format(sqlScriptTemp(),
                sqlFirst(),
                sqlSelectColumns(tableInfo, true),
                tableInfo.getTableName(),
                getJoinSql(),
                sqlWhereEntityWrapper(true, tableInfo),
                sqlComment()
        );

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForOther(mapperClass, getMethodName(), sqlSource, Object.class);*/
    }

    public void autoAddMappedStatements(){
        cacheList.forEach(t -> {
            super.builderAssistant = t.builderAssistant;
            autoAddMappedStatements(t.getMapperClass(), t.getModelClass());
        });
    }

    /**
     * 将 mapper 中有 {@link SelectTargetResult} 注解的方法构建为MappedStatements 并自动加入容器
     * @param mapperClass       mapper 类
     * @param modelClass        mapper中实体类
     */
    protected void autoAddMappedStatements(Class<?> mapperClass, Class<?> modelClass){
        Method[] declaredMethods = mapperClass.getDeclaredMethods();
        Arrays.stream(declaredMethods)
                .filter(this::verifyHandler)
                .forEach(t -> addMappedStatement(t, mapperClass, modelClass));

    }

    protected boolean verifyHandler(Method method) {
            return method.isAnnotationPresent(SelectTargetResult.class);
    }


    /**
     * 将指定方法构建为MappedStatements 并自动加入容器
     * @param method                方法类
     * @param mapperClass           mapper 类
     * @param mainEntityClass       主表类
     */
    protected void addMappedStatement(Method method, Class<?> mapperClass, Class<?> mainEntityClass) {


        TableInfo mainTableInfo = TableInfoHelper.getTableInfo(mainEntityClass);
        if (mainTableInfo == null) {
            throw new RuntimeException("mainTableInfo not found");
        }

        Class<?> resultClass = null;
        SelectTargetResult selectTargetResult = method.getAnnotation(SelectTargetResult.class);
        Class<?> value = selectTargetResult.value();
        if (value.equals(Class.class)) {
            resultClass = ReflectUtil.getMethodRealReturnType(method);
        }else {
            resultClass = value;
        }



        ResultMap resultMap = resultMapperBuilder.wrapperResultMap(resultClass, mainEntityClass);


        String sql = String.format(sqlScriptTemp(),
                sqlFirst(),
                sqlSelectColumns(),
                mainTableInfo.getTableName(),
                getJoinSql(),
                sqlWhereEntityWrapper(true, mainTableInfo),
                sqlComment()
        );

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, mainEntityClass);

        log.info("add SelectMappedStatement ===> {} {}", mapperClass, method.getName());
        this.addSelectMappedStatementForResultMap(mapperClass, method.getName(), sqlSource, resultMap.getId());
    }

    protected MappedStatement addSelectMappedStatementForResultMap(Class<?> mapperClass, String id, SqlSource sqlSource,String resultMap) {
        return addMappedStatement(mapperClass, id, sqlSource, SqlCommandType.SELECT, null,
                resultMap, null, new NoKeyGenerator(), null, null);
    }


    /**
     * SQL 查询所有表字段
     * 这里不做拼接全部由 {@link JoinLambdaQueryWrapper}
     * 动态拼接
     *
     */
    protected String sqlSelectColumns() {
        return SqlScriptUtils.unSafeParam(Q_WRAPPER_SQL_SELECT);
    }



    @Setter
    @Getter
    public static class Cache{
        Class<?> mapperClass;
        Class<?> modelClass;
        MapperBuilderAssistant builderAssistant;

        public Cache(Class<?> mapperClass, Class<?> modelClass, MapperBuilderAssistant builderAssistant) {
            this.builderAssistant = builderAssistant;
            this.mapperClass = mapperClass;
            this.modelClass = modelClass;
        }
    }

}
