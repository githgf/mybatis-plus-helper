package com.hgf.helper.mybatisplus.annotation;


import java.lang.annotation.*;

/**
 * 查询指定类结果
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SelectTargetResult {

    /**
     * 真实的类型，避免泛型引起的错误解析
     */
    Class<?> value() default Class.class;


}
