package com.hgf.helper.mybatisplus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.hgf.helper.mybatisplus.join.JoinLambdaQueryWrapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author hgf
 */
public interface HBaseMapper<T> extends BaseMapper<T> {
    /**
     * 批量插入
     */
    Integer insertBatchSomeColumn(Collection<T> entityList);

    /**
     * 多表联查 单个记录
     */
    T selectJoinOne(@Param(Constants.WRAPPER) JoinLambdaQueryWrapper<T> joinLambdaQueryWrapper);

    /**
     * 多表联查多个记录
     */
    List<T> selectJoinList(@Param(Constants.WRAPPER) JoinLambdaQueryWrapper<T> queryWrapper);

    /**
     * 根据 Wrapper 条件，多表联查查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    List<Map<String, Object>> selectJoinMaps(@Param(Constants.WRAPPER) JoinLambdaQueryWrapper<T> queryWrapper);

    /**
     * 根据 Wrapper 条件，多表联查查询全部记录
     * 注意： 只返回第一个字段的值
     * Params:
     * queryWrapper – 实体对象封装操作类（可以为 null）
     */
    List<Object> selectJoinObjects(@Param(Constants.WRAPPER) JoinLambdaQueryWrapper<T> queryWrapper);


    /**
     * 根据 entity 条件，多表联查查询全部记录（并翻页）
     *
     * @param page         分页查询条件（可以为 RowBounds.DEFAULT）
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    <E extends IPage<T>> E selectJoinPage(E page, @Param(Constants.WRAPPER) JoinLambdaQueryWrapper<T> queryWrapper);

}
