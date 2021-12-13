package com.hgf.helper.mybatisplus.salves;


import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 读写分离自动配置类
 */
@Configuration
@ConditionalOnProperty(value = "db.more")
public class SalvesReadAutoConfig {

    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties, MoreSalvesDataSourceConfig moreSalvesDataSourceConfig) {
        return new DyDataSource(dataSourceProperties, moreSalvesDataSourceConfig);
    }

    @Bean
    public MoreSalvesDataSourceConfig moreSalvesDataSourceConfig(){
        return new MoreSalvesDataSourceConfig();
    }


    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory, MybatisProperties properties) {
        ExecutorType executorType = properties.getExecutorType();
        if (executorType != null) {
            return new MySqlSessionTemplate(sqlSessionFactory, executorType);
        } else {
            return new MySqlSessionTemplate(sqlSessionFactory);
        }
    }
}
