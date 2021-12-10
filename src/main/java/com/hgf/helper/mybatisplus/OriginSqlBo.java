
package com.hgf.helper.mybatisplus;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 原始sql构建器
 * @param <T>
 */
@Setter
@Getter
@Builder
public class OriginSqlBo<T> {
    /***
     * lambda 表达式
     */
    SFunction<T, ?> sFunction;

    /**
     * 数据
     */
    Object object;

    /**
     * 是否是字段
     */
    boolean isColumn;

    /**
     * 是否是动态参数
     */
    boolean isPreParam;

    public OriginSqlBo(SFunction<T, ?> sFunction, Object object, boolean isColumn, boolean isPreParam) {
        this.sFunction = sFunction;
        this.object = object;
        this.isColumn = isColumn;
        this.isPreParam = isPreParam;
    }
}