package com.hgf.helper.mybatisplus.salves;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConfigurationProperties(prefix = "more")
public class MoreSalvesDataSourceConfig {
    /**
     * 从库
     */
    public Map<String, DataSourceProperties> salves;

    public Map<String, DataSourceProperties> getSalves() {
        return salves;
    }

    public void setSalves(Map<String, DataSourceProperties> salves) {
        this.salves = salves;
    }
}
