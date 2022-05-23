package com.hgf.helper.mybatisplus.join;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * @Author: zhengcan
 * @Date: 2022/5/22
 * @Description:
 * @Version: 1.0.0 创建
 */
public interface JoinQueryBase<K, T, E> {

    JoinEnum getJoinEnum();

    SFunction<T, ?> getMainJoinField();

    SFunction<K, ?> getJoinField();

    SFunction<E, K> getAssociationAnnField();

    SFunction<E, K> getJoinColumnAnnFiled();

}
