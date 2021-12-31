package com.hgf.helper.mybatisplus.helper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hgf.helper.mybatisplus.annotation.Association;
import com.hgf.helper.mybatisplus.join.JoinTableHelper;
import com.hgf.helper.mybatisplus.utils.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * resultMap 构建器
 */
@Slf4j
public class ResultMapperBuilder {
    private final Configuration configuration;

    public ResultMapperBuilder(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * 将mapper 中的方法自动加入 ResultMap 到容器中
     * @param resultClass               返回类
     * @param mainEntityClass           主表类
     */
    public ResultMap wrapperResultMap(Class<?> resultClass, Class<?> mainEntityClass) {


        GlobalConfig globalConfig = GlobalConfigUtils.getGlobalConfig(configuration);

        ReflectorFactory reflectorFactory = configuration.getReflectorFactory();
        Reflector reflector = reflectorFactory.findForClass(resultClass);
        GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();

        Field[] declaredFields = resultClass.getDeclaredFields();

        List<ResultMapping> resultMappings = new ArrayList<>(declaredFields.length);

        String resultMapId = String.format("%s_generate", resultClass);


        ResultMap resultMap = null;
        try {
            resultMap = configuration.getResultMap(resultMapId);
        } catch (Exception e) {

        }
        if (resultMap != null) {
            return resultMap;
        }

        for (Field declaredField : declaredFields) {

            boolean isTableField = declaredField.isAnnotationPresent(TableField.class);
            boolean isAssociationField = declaredField.isAnnotationPresent(Association.class);
            ResultMapping resultMapping = null;
            if (isTableField) {
                TableField tableField = declaredField.getAnnotation(TableField.class);
                if (tableField.exist()) {
                    resultMapping = getTableFieldResultMapping(dbConfig, reflector, declaredField);
                }

            } else if (isAssociationField) {
                resultMapping = getAssociationFieldResultMapping(declaredField, reflector, mainEntityClass.equals(declaredField.getType()));
            }

            if (resultMapping == null) {
                continue;
            }

            resultMappings.add(resultMapping);
        }

        resultMap = new ResultMap.Builder(configuration, resultMapId, resultClass, resultMappings).build();
        configuration.addResultMap(resultMap);

        log.info("add ResultMap ===> {} {}", resultMap.getId(), resultClass.getName());


        return resultMap;

    }

    /**
     * 获取连接属性resultMapping
     *
     * @param declaredField 属性
     * @param reflector     反射工具
     * @param isMainField   当前属性是否是主表属性
     */
    protected ResultMapping getAssociationFieldResultMapping(Field declaredField, Reflector reflector, boolean isMainField) {
        Class<?> joinEntityClass = declaredField.getType();

        if (ReflectUtil.isList(joinEntityClass)) {
            joinEntityClass = ReflectUtil.getRealListClass(declaredField.getGenericType());
        }


        TableInfo tableInfo = TableInfoHelper.getTableInfo(joinEntityClass);

        if (tableInfo == null) {
            return null;
        }

        String resultMapId = tableInfo.getResultMap();

        if (StringUtils.isBlank(resultMapId)) {
            return null;
        }

        ResultMap resultMap = configuration.getResultMap(resultMapId);
        if (resultMap == null) {
            return null;
        }


//        String newResultMapId = isMainField ? tableInfo.getResultMap() : String.format("%s_ass[%s]", joinEntityClass.getName(), declaredField.getName());
        String newResultMapId = tableInfo.getResultMap();

        String columnPrefix = isMainField ? null : JoinTableHelper.getJoinTableColumnPrefix(declaredField);

        // 当前属性不是主表属性时需要新建一个 ResultMap
        /*if (!isMainField) {
            List<ResultMapping> resultMappings = resultMap.getResultMappings();
            ResultMap newResultMap = new ResultMap.Builder(configuration, newResultMapId, resultMap.getType(), resultMappings).build();
            configuration.addResultMap(newResultMap);

            log.info("addResultMap ====> {} {}", newResultMap.getId(), newResultMap.getType());

        }*/

        String property = declaredField.getName();

        ResultMapping.Builder mappingBuilder = getTableFieldResultMappingBuilder(configuration, property, declaredField.getType(), null, null, null);
        mappingBuilder.columnPrefix(columnPrefix);
        mappingBuilder.nestedResultMapId(newResultMapId);
        return mappingBuilder.build();

    }

    /**
     * 将指定属性 封装 ResultMapping
     * @param dbConfig              数据库全局配置
     * @param reflector             反射工具
     * @param declaredField         类属性
     */
    protected ResultMapping getTableFieldResultMapping(GlobalConfig.DbConfig dbConfig, Reflector reflector,Field declaredField) {
        TableField tableField = declaredField.getAnnotation(TableField.class);
        String property = declaredField.getName();
        Class<?> propertyType = reflector.getGetterType(property);

        // 照抄 TableField 构造器
        String column = tableField.value();
        if (StringUtils.isBlank(column)) {
            column = property;
            if (configuration.isMapUnderscoreToCamelCase()) {
                /* 开启字段下划线申明 */
                column = StringUtils.camelToUnderline(column);
            }
            if (dbConfig.isCapitalMode()) {
                /* 开启字段全大写申明 */
                column = column.toUpperCase();
            }
        }
        String columnFormat = dbConfig.getColumnFormat();
        if (StringUtils.isNotBlank(columnFormat) && tableField.keepGlobalFormat()) {
            column = String.format(columnFormat, column);
        }


        return getTableFieldResultMappingBuilder
                (
                        configuration,
                        property, propertyType,
                        column, null,
                        (Class<? extends TypeHandler<?>>) tableField.typeHandler()
                )
                .build();
    }

    /**
     * 将指定类中属性 封装 ResultMappingBuilder
     * @param configuration     mybatis 配置
     * @param property          属性名
     * @param propertyType      属性类型
     * @param column            表字段
     * @param jdbcType          {@link JdbcType}
     * @param typeHandler       {@link TypeHandler}
     */
    protected ResultMapping.Builder getTableFieldResultMappingBuilder(Configuration configuration, String property, Class<?> propertyType, String column, JdbcType jdbcType, Class<? extends TypeHandler<?>> typeHandler) {

        ResultMapping.Builder builder = new ResultMapping.Builder(configuration, property, StringUtils.isNotBlank(column) ? StringUtils.getTargetColumn(column) : null, propertyType);
        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        if (jdbcType != null && jdbcType != JdbcType.UNDEFINED) {
            builder.jdbcType(jdbcType);
        }
        if (typeHandler != null && typeHandler != UnknownTypeHandler.class) {
            TypeHandler<?> mappingTypeHandler = registry.getMappingTypeHandler(typeHandler);
            if (mappingTypeHandler == null) {
                mappingTypeHandler = registry.getInstance(propertyType, typeHandler);
            }
            builder.typeHandler(mappingTypeHandler);
        }
        return builder;
    }
}
