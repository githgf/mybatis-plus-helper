package com.hgf.helper.mybatisplus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MpJoinColumn {

    /**
     * 主表实体类属性名
     */
    String mainFieldName();

    /**
     * 附表实体类属性名
     */
    String refFieldName();

}