
package com.hgf.helper.mybatisplus.helper;

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
    SFunction<T, ?> sFunction;

    Object object;

    boolean isColumn;

    boolean isPreParam;

    public OriginSqlBo(SFunction<T, ?> sFunction, Object object, boolean isColumn, boolean isPreParam) {
        this.sFunction = sFunction;
        this.object = object;
        this.isColumn = isColumn;
        this.isPreParam = isPreParam;
    }
}