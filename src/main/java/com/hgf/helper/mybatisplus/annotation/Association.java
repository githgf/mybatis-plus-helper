package com.hgf.helper.mybatisplus.annotation;


import java.lang.annotation.*;

/**
 * 连接注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Association {
    /**
     * 表别称前缀
     */
    String aliasPrefix() default "";
}
