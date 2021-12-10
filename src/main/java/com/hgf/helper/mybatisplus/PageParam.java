package com.hgf.helper.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author hgf
 */
@Setter
@Getter
@JsonIgnoreProperties({"hitCount", "searchCount", "optimizeCountSql"})
public class PageParam<T> extends Page<T> {
    /**
     * 查询对象
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    T entity;


    /**
     * 将原有的page中的记录数据替换，页码。。不变
     * @param originPageParam           元数据page
     * @param records                   新数据记录
     * @param <K>                       原来page记录泛型
     * @param <H>                       新page记录泛型
     */
    public static <K,H> IPage<K> convert(IPage<H> originPageParam, List<K> records) {
        IPage<K> newPageParam = new Page<>();
        newPageParam.setRecords(records);
        newPageParam.setTotal(originPageParam.getTotal());
        newPageParam.setSize(originPageParam.getSize());
        newPageParam.setCurrent(originPageParam.getCurrent());
        return newPageParam;
    }



}
