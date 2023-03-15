package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.support.SerializedLambda;
import com.hgf.helper.mybatisplus.annotation.Association;
import com.hgf.helper.mybatisplus.annotation.MpJoinColumn;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import com.hgf.helper.mybatisplus.utils.ReflectUtil;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinColumnParseHelper {

    /**
     * 封装 JoinColumnInfo
     */
    public static JoinColumnInfo wrapperJoinColumnInfo(JoinQueryBuilder<?, ?, ?> builder, Class<?> wrapperClass, boolean isQueryTargetEntityResult, Class<?> mainEntityClass) {

        if (builder.getJoinColumnAnnFiled() != null) {
            return parseByJoinColumnAnnFieldFunc(builder.getJoinColumnAnnFiled(), mainEntityClass);
        } else {
            return parseByQueryJoinWrapperField(builder, wrapperClass, isQueryTargetEntityResult, mainEntityClass);
        }
    }


    /**
     * MpJoinColumn 注解解析
     * @param joinColumn                    注解信息
     * @param queryJoinWrapperField         附表实体类在查询结果中属性
     */
    public static JoinColumnInfo parse(MpJoinColumn joinColumn, Field queryJoinWrapperField, Class<?> mainEntityClass){
        // 主表属性
        String mainFieldName = joinColumn.mainFieldName();
        // 附表实体类属性名
        String refFieldName = joinColumn.refFieldName();

        if (StringUtils.isEmpty(mainFieldName) || StringUtils.isEmpty(refFieldName) || mainEntityClass.equals(Void.class)) {
            throw new RuntimeException("annotation parma error");
        }

        ColumnCache mainColumnCache = getColumnByFieldName(mainFieldName, mainEntityClass);
        if (mainColumnCache == null) {
            throw new RuntimeException(String.format("%s not found column in %s", mainFieldName, mainEntityClass.getName()));
        }

        JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
        joinColumnInfo.setMainTableColumn(mainColumnCache.getColumn());

        Class<?> joinEntityClass = ReflectUtil.getFieldRealType(queryJoinWrapperField);

        ColumnCache refColumnCache = getColumnByFieldName(refFieldName, joinEntityClass);

        if (refColumnCache == null) {
            throw new RuntimeException(String.format("%s not found column  in %s", refFieldName, joinEntityClass.getName()));
        }

        String joinTableAlias = joinColumn.joinTableAlias();
        if (StringUtils.isEmpty(joinTableAlias)) {
            Association annotation = queryJoinWrapperField.getAnnotation(Association.class);
            if (annotation != null) {
                joinTableAlias = annotation.aliasPrefix();
            }

        }


        if (StringUtils.isEmpty(joinTableAlias)) {
            joinTableAlias = queryJoinWrapperField.getName() + "_";
        }

        joinColumnInfo.setJoinTableColumn(refColumnCache.getColumn());
        joinColumnInfo.setJoinEntityClass(joinEntityClass);
        joinColumnInfo.setMainEntityClass(mainEntityClass);
        joinColumnInfo.setQueryTypeJoinField(queryJoinWrapperField);
        joinColumnInfo.setJoinTableAlias(joinTableAlias);

        return joinColumnInfo;

    }

    /**
     * 获取指定class中指定属性名对应的column缓存
     */
    public static ColumnCache getColumnByFieldName(String fieldName, Class<?> aClass) {
        Map<String, ColumnCache> columnMap = LambdaUtils.getColumnMap(aClass);
        if (columnMap == null) {
            throw new RuntimeException("annotation parma error");
        }
        return columnMap.get(LambdaUtils.formatKey(fieldName));
    }

    public static Field getQueryJoinWrapperField(JoinQueryBuilder<?, ?, ?> builder, Class<?> wrapperClass, boolean isQueryTargetEntityResult, Class<?> mainEntityClass) {
        SFunction<?, ?> mainJoinFieldFunc = builder.getMainJoinField();
        SFunction<?, ?> joinFieldFunc = builder.getJoinField();
        if (joinFieldFunc == null || mainJoinFieldFunc == null) {
            throw new RuntimeException("param error");
        }
        SFunction<?, ?> associationAnnField = builder.getAssociationAnnField();

        LambdaMeta resolve = LambdaUtils.extract(joinFieldFunc);
        // 附表实体类class
        Class<?> joinEntity = resolve.getInstantiatedClass();


        // 主表实体类
        TableInfo tableInfo = TableInfoHelper.getTableInfo(mainEntityClass);
        // 附表查询结果封装属性
        Field field = null;

        // 自动从 lambdaQueryWrapper 推测链接附表的实体属性
        String joinWrapperFieldName = Optional.ofNullable(associationAnnField).map(t -> PropertyNamer.methodToProperty(LambdaUtils.extract(associationAnnField).getImplMethodName())).orElse(null);;

        if (!StringUtils.isEmpty(joinWrapperFieldName)) {
            try {
                field = wrapperClass.getDeclaredField(joinWrapperFieldName);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        if (field == null) {
            List<Field> fields = null;
            if (!isQueryTargetEntityResult) {
                fields = CollectionUtil.mapFieldsToList(tableInfo.getFieldList(), TableFieldInfo::getField);
            } else {
                Field[] declaredFields = wrapperClass.getDeclaredFields();
                fields = Stream.of(declaredFields).collect(Collectors.toList());
            }
            if (CollectionUtil.isNotEmpty(fields)) {
                field = fields
                        .stream()
                        .filter(t -> {
                            Class<?> aClass = ReflectUtil.getFieldRealType(t);
                            return aClass != null && aClass.equals(joinEntity);
                        }).findFirst()
                        .orElse(null);
            }
        }

        return field;

    }

    public static JoinColumnInfo parseByJoinColumnAnnFieldFunc(SFunction<?, ?> function,Class<?> mainEntityClass) {

        Field field = getFieldByJoinColumnAnnFieldFunc(function);
        if (field == null) {
            throw new RuntimeException("join field not found");
        }

        MpJoinColumn joinColumn = field.getAnnotation(MpJoinColumn.class);
        if (joinColumn == null) {
            throw new RuntimeException("annotation MpJoinColumn not found");
        }

        return parse(joinColumn, field, mainEntityClass);
    }

    /**
     * 封装 JoinColumnInfo
     */
    public static JoinColumnInfo parseByQueryJoinWrapperField(JoinQueryBuilder<?, ?, ?> builder, Class<?> wrapperClass, boolean isQueryTargetEntityResult, Class<?> mainEntityClass) {

        JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
        LambdaMeta mainLambda = LambdaUtils.extract(builder.getMainJoinField());

        String mainFieldName = PropertyNamer.methodToProperty(mainLambda.getImplMethodName());

        ColumnCache columnCache = getColumnByFieldName(mainFieldName, mainEntityClass);
        if (columnCache == null) {
            throw new RuntimeException(String.format("%s not found column in %s", mainFieldName, mainEntityClass.getName()));
        }

        joinColumnInfo.setMainTableColumn(columnCache.getColumn());

        LambdaMeta joinLambda = LambdaUtils.extract(builder.getJoinField());

        String joinFieldName = PropertyNamer.methodToProperty(joinLambda.getImplMethodName());

//        Class<?> joinEntityClass = ReflectUtil.getFieldRealType(field);

        SFunction<?, ?> joinFieldFunc = builder.getJoinField();
        if (joinFieldFunc == null || builder.getMainJoinField() == null) {
            throw new RuntimeException("param error");
        }

        LambdaMeta resolve = LambdaUtils.extract(joinFieldFunc);
        // 附表实体类class
        Class<?> joinEntityClass = resolve.getInstantiatedClass();



        columnCache = getColumnByFieldName(joinFieldName, joinEntityClass);
        if (columnCache == null) {
            throw new RuntimeException(String.format("%s not found column in %s", joinFieldName, joinEntityClass.getName()));
        }
        joinColumnInfo.setJoinTableColumn(columnCache.getColumn());
        joinColumnInfo.setJoinEntityClass(joinEntityClass);
        joinColumnInfo.setMainEntityClass(mainEntityClass);


        // 设置表别名
        if (!StringUtils.isEmpty(builder.getJoinTableAlias())) {
            joinColumnInfo.setJoinTableAlias(builder.getJoinTableAlias());
        }else {
            Field field = getQueryJoinWrapperField(builder, wrapperClass, isQueryTargetEntityResult, mainEntityClass);
            if (field != null) {
                String prefix = JoinTableHelper.getJoinTableColumnPrefix(field);
                joinColumnInfo.setJoinTableAlias(prefix);
            }
        }

        if (StringUtils.isEmpty(joinColumnInfo.getJoinTableAlias())) {
            joinColumnInfo.setJoinTableAlias(TableInfoHelper.getTableInfo(joinEntityClass).getTableName());
        }



        return joinColumnInfo;
    }

    /**
     * 有 {@link MpJoinColumn} 注解属性的lambda 对应的属性
     * @param function
     * @return
     */
    public static Field getFieldByJoinColumnAnnFieldFunc(SFunction<?, ?> function) {
        try {
            LambdaMeta lambda = LambdaUtils.extract(function);
            String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
            Class<?> aClass = lambda.getInstantiatedClass();
            return aClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException e) {
            return null;
        }
    }
}
