package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.hgf.helper.mybatisplus.annotation.Association;
import com.hgf.helper.mybatisplus.annotation.MpJoinColumn;
import lombok.Builder;
import lombok.Getter;

/**
 * 连接查询条件构建
 * @param <K>           附表实体类
 * @param <T>           主表实体类
 * @param <E>           查询结果
 */
@Getter
@Builder
public class JoinQueryBuilder<K,T,E> {
    /**
     * 连接类型
     */
    JoinEnum joinEnum;
    /**
     * 主表连接属性
     */
    SFunction<T, ?> mainJoinField;
    /**
     * 附表连接属性
     */
    SFunction<K, ?> joinField;

    /**
     * 被 {@link Association} 标记的属性，
     */
    SFunction<E, K> associationAnnField;

    /**
     * 同时 被
     * {@link MpJoinColumn}
     * {@link Association}
     * 标记的属性
     */
    SFunction<E, K> joinColumnAnnFiled;


}
