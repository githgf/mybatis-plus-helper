package com.hgf.helper.mybatisplus.salves;

import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 读写分离数据源实现
 * 主库配置和默认spring 配置一样
 * <p>
 * 从库配置格式如下
 * <p>
 * spring:
 * datasource:
 * salves:
 * url: jdbc:mysql:/XXXX
 * username: XXXX
 * password: XXXX
 *
 *
 * </p>
 */
@Slf4j
@Component
//@Import(value = {MoreSalvesDataSourceConfig.class})
//@AutoConfigureAfter(MoreSalvesDataSourceConfig.class)
@ConfigurationProperties(prefix = "spring.datasource.hikari")
public class DyDataSource extends HikariDataSource{

    /**
     * 当前线程使用数据库租户信息
     */
    private static ThreadLocal<String> threadLocal = new InheritableThreadLocal<>();

    /**
     * 租户信息对应数据库连接池映射
     */
    private static ConcurrentMap<String, HikariDataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    /**
     * 数据源锁map
     */
    private static ConcurrentSkipListSet<String> LOCK_DB_CACHE = new ConcurrentSkipListSet<>();

    /**
     * 属性map
     */
    private static ThreadLocal<Map<String, Object>> attrMap = new InheritableThreadLocal<>();

    /**
     * 主库数据源
     */
    private DataSourceProperties masterDb;

    /**
     * 当前sql为查询，属性
     */
    public static final String QUERY_FLAG = "QUERY";

    /**
     * 主库标记
     */
    public static final String MASTER_KEY = "MASTER";
    /**
     * 在从库中随机找一个
     */
    public static final String RANDOM_SALVE = "RANDOM_SALVE";

    /**
     * 所有的从库数据源
     */
    public MoreSalvesDataSourceConfig salvesDataSourceConfig;

    public DyDataSource(DataSourceProperties dataSourceProperties, MoreSalvesDataSourceConfig moreSalvesDataSourceConfig) {
        super();
        masterDb = dataSourceProperties;
        salvesDataSourceConfig = moreSalvesDataSourceConfig;
    }


    @Override
    public Connection getConnection() throws SQLException {

        return getDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getDataSource().getConnection(username, password);
    }

    /**
     * 设置当前线程数据库租户
     */
    public static void setDbKey(Object dbKey) {
        if (dbKey == null) {
            return;
        }
        threadLocal.set(String.valueOf(dbKey));
    }

    public static void removeDbKey() {
        threadLocal.remove();
    }

    /**
     * 获取当前数据源
     */
    @Override
    public DataSource getDataSource() {

        // 如果没有指定默认就是主库
        String dbKey = threadLocal.get();
        if (StringUtils.isEmpty(dbKey)) {
            dbKey = MASTER_KEY;
        }

        if (dbKey.equals(RANDOM_SALVE)) {
            // 随机找一个不是主库的数据源
            if (CollectionUtil.isNotEmpty(salvesDataSourceConfig.salves)) {
                dbKey = salvesDataSourceConfig.salves.keySet().stream().filter(t -> !t.equals(MASTER_KEY)).findAny().orElse(null);
            }
        }

//        log.info("当前运行 {} ", dbKey);
        HikariDataSource dataSource = DATA_SOURCE_MAP.get(dbKey);
        if (dataSource == null) {
            // 初始化
            synchronized (dbKey.intern()) {
                dataSource = DATA_SOURCE_MAP.get(dbKey);
                if (dataSource == null) {
                    dataSource = initDataSource(dbKey);
                    if (dataSource != null) {
                        DATA_SOURCE_MAP.put(dbKey, dataSource);
                    }
                }
            }
        }

        if (dataSource == null) {
            throw new RuntimeException("dataSource is null");
        }

        return dataSource;

    }

    /**
     * 初始化数据源
     */
    private HikariDataSource initDataSource(String dbKey) {

        DataSourceProperties dataSourceProperties = Optional.ofNullable(salvesDataSourceConfig.getSalves()).map(t -> t.get(dbKey)).orElse(null);

        if (dataSourceProperties == null) {
            dataSourceProperties = masterDb;
        }

        // 从源码中抄的，大概就是绑定数据
        return buildByDataSourceProperties(dataSourceProperties);
    }


    private HikariDataSource buildByDataSourceProperties(DataSourceProperties dataSourceProperties) {
        HikariDataSource build = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        build.setConnectionInitSql(super.getConnectionInitSql());
        // 设置最大空闲连接数
//        build.setMinimumIdle(super.getMinimumIdle());

        return build;
    }


    /**
     * 释放数据源
     */
    public void releaseDataSource(String dbKey) {
        boolean containsKey = DATA_SOURCE_MAP.containsKey(dbKey);
        if (!containsKey) {
            return;
        }
        synchronized (dbKey.intern()) {
            HikariDataSource dataSource = DATA_SOURCE_MAP.get(dbKey);
            if (dataSource == null) {
                return;
            }
            try {
                dataSource.close();
                DATA_SOURCE_MAP.remove(dbKey);
                dataSource = null;
            } finally {
                LOCK_DB_CACHE.remove(dbKey);
            }
        }
    }

    public static String getThreadLocalDbKey() {
        return threadLocal.get();
    }


    public static void putAttr(String key, Object value) {
        Map<String, Object> map = attrMap.get();
        if (map == null) {
            map = new HashMap<>();
            attrMap.set(map);
        }

        map.put(key, value);
    }

    public static Object getAttr(String key) {
        Map<String, Object> map = attrMap.get();
        return Optional.ofNullable(map).map(t -> t.get(key)).orElse(null);
    }

    @PostConstruct
    public void init() {
        log.info("DyDataSource init ...........");
    }

}
