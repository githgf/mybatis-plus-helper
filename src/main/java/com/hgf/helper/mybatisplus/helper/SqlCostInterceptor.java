package com.hgf.helper.mybatisplus.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.sql.Statement;
import java.util.Properties;

/**
 * Sql执行时间记录拦截器
 */
@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "batch", args = { Statement.class })})
public class SqlCostInterceptor implements Interceptor {

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
    public void printSqlLog(long startTime, StatementHandler statementHandler) {
        Boolean enable = environment.getProperty("mp.printQuery", Boolean.class);
        SqlPrintUtils.printSqlLog(startTime, statementHandler.getBoundSql(), enable);

    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }



}