/*
 * Copyright (c) 2011-2020, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;
import java.util.function.Predicate;

/**
 * 自定义主键
 */
@NoArgsConstructor
@AllArgsConstructor
public class InsertBatchSomeColumnWithInput extends AbstractMethod {

    /**
     * 字段筛选条件
     */
    @Setter
    @Accessors(chain = true)
    private Predicate<TableFieldInfo> predicate;

    @SuppressWarnings("Duplicates")
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        KeyGenerator keyGenerator = new NoKeyGenerator();
        SqlMethod sqlMethod = SqlMethod.INSERT_ONE;
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        String insertSqlColumn = tableInfo.getKeyColumn() + COMMA  +
            this.filterTableFieldInfo(fieldList, predicate, TableFieldInfo::getInsertSqlColumn, EMPTY);
        String columnScript = LEFT_BRACKET + insertSqlColumn.substring(0, insertSqlColumn.length() - 1) + RIGHT_BRACKET;
        String insertSqlProperty = SqlScriptUtils.safeParam(Constants.ENTITY_DOT + tableInfo.getKeyProperty()) + COMMA  +
            this.filterTableFieldInfo(fieldList, predicate, i -> i.getInsertSqlProperty(ENTITY_DOT), EMPTY);
        insertSqlProperty = LEFT_BRACKET + insertSqlProperty.substring(0, insertSqlProperty.length() - 1) + RIGHT_BRACKET;
        String valuesScript = SqlScriptUtils.convertForeach(insertSqlProperty, "list", null, ENTITY, COMMA);
        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();
        // 表包含主键处理逻辑,如果不包含主键当普通字段处理
        /*if (StringUtils.isNotBlank(tableInfo.getKeyProperty())) {
            if (tableInfo.getIdType() == IdType.AUTO) {
                *//* 自增主键 *//*
                keyGenerator = new Jdbc3KeyGenerator();
                keyProperty = tableInfo.getKeyProperty();
                keyColumn = tableInfo.getKeyColumn();
            } else {
                if (null != tableInfo.getKeySequence()) {
                    keyGenerator = TableInfoHelper.genKeyGenerator(getMethod(sqlMethod), tableInfo, builderAssistant);
                    keyProperty = tableInfo.getKeyProperty();
                    keyColumn = tableInfo.getKeyColumn();
                }
            }
        }*/
        String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), columnScript, valuesScript);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addInsertMappedStatement(mapperClass, modelClass, getMethod(sqlMethod), sqlSource, keyGenerator, keyProperty, keyColumn);
    }

    @Override
    public String getMethod(SqlMethod sqlMethod) {
        // 自定义 mapper 方法名
        return "insertBatchSomeColumnWithInput";
    }
}
