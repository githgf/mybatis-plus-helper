package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

public class SelectJoinOne extends AbstractJoinSelectMethod {
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        //JoinTableHelper.createJoinTableIn(tableInfo);
        String sql = String.format(sqlScriptTemp(),
                sqlFirst(),
                sqlSelectColumns(tableInfo, true),
                tableInfo.getTableName(),
                getJoinSql(),
                sqlWhereEntityWrapper(true, tableInfo),
                sqlComment()
        );

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForTable(mapperClass, getMethodName(), sqlSource, tableInfo);
    }


    public String getMethodName(){
        return "selectJoinOne";
    }

}
