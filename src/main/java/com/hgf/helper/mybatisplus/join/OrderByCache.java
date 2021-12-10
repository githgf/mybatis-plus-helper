package com.hgf.helper.mybatisplus.join;

import java.util.Objects;

/**
 * 排序缓存信息
 */
public class OrderByCache{
    /**
     * 是否是降序
     */
    boolean isDesc;

    /***
     * 字段
     */
    String column;

    public OrderByCache(boolean isDesc, String column) {
        this.isDesc = isDesc;
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderByCache that = (OrderByCache) o;
        return isDesc == that.isDesc && Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isDesc, column);
    }
}
