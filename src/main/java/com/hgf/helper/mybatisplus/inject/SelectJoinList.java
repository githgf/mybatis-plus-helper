package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 多表联查返回list mapper 方法注入
 */
public class SelectJoinList extends AbstractJoinSelectMethod {
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
        return "selectJoinList";
    }

}
