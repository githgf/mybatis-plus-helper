package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.hgf.helper.mybatisplus.annotation.Association;
import com.hgf.helper.mybatisplus.inject.SelectTargetObjects;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import com.hgf.helper.mybatisplus.utils.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  多表联查初始化
 */
@Slf4j
public class JoinHelperStartHook implements ApplicationRunner {



    static SelectTargetObjects selectJoinTargetInject;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        handlerTableInfo();
        if (selectJoinTargetInject != null) {
            selectJoinTargetInject.autoAddMappedStatements();
        }
    }



    public void handlerTableInfo(){
        List<TableInfo> tableInfos = TableInfoHelper.getTableInfos();
        if (CollectionUtil.isEmpty(tableInfos)) {
            return;
        }

        for (TableInfo tableInfo : tableInfos) {
            try {
                handlerTableInfo(tableInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void handlerTableInfo(TableInfo tableInfo) throws Exception{
        Configuration configuration = tableInfo.getConfiguration();

        Field[] fields = tableInfo.getEntityType().getDeclaredFields();

        List<Field> associationFields = Stream.of(fields).filter(t -> t.isAnnotationPresent(Association.class)).collect(Collectors.toList());

        if (CollectionUtil.isEmpty(associationFields)) {
            return;
        }

        String resultMapId = tableInfo.getResultMap();
        ResultMap resultMap = configuration.getResultMap(resultMapId);
        Field declaredField = resultMap.getClass().getDeclaredField("hasNestedResultMaps");
        declaredField.setAccessible(true);
        declaredField.set(resultMap, true);

        Map<String, Field> fieldInfoMap = CollectionUtil.convertToMapByFunc(associationFields, Field::getName);


        List<ResultMapping> resultMappings = resultMap.getResultMappings();
        for (ResultMapping resultMapping : resultMappings) {
            String property = resultMapping.getProperty();
            Field fieldInfo = fieldInfoMap.get(property);
            if (fieldInfo == null) {
                continue;
            }

            if (!fieldInfo.isAnnotationPresent(Association.class)) {
                continue;
            }

            Class<?> joinEntityClass = fieldInfo.getType();

            if (ReflectUtil.isList(joinEntityClass)) {
                joinEntityClass = ReflectUtil.getRealListClass(fieldInfo.getGenericType());
            }

            TableInfo associationTableInfo = TableInfoHelper.getTableInfo(joinEntityClass);
            if (associationTableInfo == null) {
                continue;
            }

            log.info("修改前：：{}", resultMapping);

            Class<ResultMapping> resultMappingClass = ResultMapping.class;
            Field columnField = resultMappingClass.getDeclaredField("column");
            columnField.setAccessible(true);
            columnField.set(resultMapping, null);


            Field nestedResultMapIdField = resultMappingClass.getDeclaredField("nestedResultMapId");
            nestedResultMapIdField.setAccessible(true);
            nestedResultMapIdField.set(resultMapping, associationTableInfo.getResultMap());

            Field columnPrefixField = resultMappingClass.getDeclaredField("columnPrefix");
            columnPrefixField.setAccessible(true);
            columnPrefixField.set(resultMapping, JoinTableHelper.getJoinTableColumnPrefix(fieldInfo));

            log.info("修改后：：{}", resultMapping);

        }

    }

    public static void setSelectJoinTargetInject(SelectTargetObjects selectJoinTargetInject) {
        JoinHelperStartHook.selectJoinTargetInject = selectJoinTargetInject;
    }


}
