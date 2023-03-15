package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

public class SelectJoinObjects extends AbstractJoinSelectMethod {
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
        return this.addSelectMappedStatementForOther(mapperClass, getMethodName(), sqlSource, Object.class);
    }


    public String getMethodName(){
        return "selectJoinObjects";
    }

}
