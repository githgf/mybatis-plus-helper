package com.hgf.helper.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.*;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.hgf.helper.mybatisplus.helper.MyLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.helper.MyLambdaUpdateWrapper;
import com.hgf.helper.mybatisplus.join.JoinLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import com.hgf.helper.mybatisplus.utils.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;


/**
 * 自定义 mybatis-plus db基础服务
 * @author hgf
 */
@SuppressWarnings("unchecked")
@Slf4j
public class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity> extends ServiceImpl<M,T> {

    @Override
    protected Class<T> currentModelClass() {
        Type[] genericParamClassArray = ReflectUtil.getGenericParamClassArray(getClass());
        if (genericParamClassArray == null || genericParamClassArray.length < 2) {
            log.warn("Warn: {} not set the actual class on superclass generic parameter", getClass().getSimpleName());
            return null;
        }

        return (Class<T>) genericParamClassArray[1];
    }

    @Override
    protected Class<T> currentMapperClass() {
        Type[] genericParamClassArray = ReflectUtil.getGenericParamClassArray(getClass());
        if (genericParamClassArray == null || genericParamClassArray.length < 2) {
            log.warn("Warn: {} not set the actual class on superMapperClass generic parameter", getClass().getSimpleName());
            return null;
        }

        return (Class<T>) genericParamClassArray[0];
    }

    /**
     * 查询指定的属性eq的数据
     */
    public List<T> listByTargetField(SFunction<T, ?> fieldFunc, Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        lambdaQuery.eq(fieldFunc, value);
        return baseMapper.selectList(lambdaQuery);
    }

    /**
     * 查询指定的属性eq的数据
     */
    public List<T> listByTargetFieldIn(SFunction<T, ?> fieldFunc, Collection<?> valueArray) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        lambdaQuery.in(fieldFunc, valueArray);
        return baseMapper.selectList(lambdaQuery);
    }

    public LambdaQueryWrapper<T> getLambdaQuery() {
        return (LambdaQueryWrapper<T>) Wrappers.lambdaQuery(super.entityClass);
    }

    public LambdaUpdateWrapper<T> getLambdaUpdate() {
        return (LambdaUpdateWrapper<T>) Wrappers.lambdaUpdate(entityClass);
    }

    public MyLambdaUpdateWrapper<T> getMyLambdaUpdate() {
        return new MyLambdaUpdateWrapper<>(entityClass);
    }

    public MyLambdaQueryWrapper<T> getMyLambdaQuery() {
        return new MyLambdaQueryWrapper<>(entityClass);
    }

    public JoinLambdaQueryWrapper<T> joinQueryWrapperForOther(Class<?> queryType){
        return new JoinLambdaQueryWrapper<>(entityClass, queryType);
    }

    public JoinLambdaQueryWrapper<T> joinQueryWrapperForTable(){
        return new JoinLambdaQueryWrapper<>(entityClass, null);
    }

    public <K> JoinLambdaQueryWrapper<K> joinQueryWrapperForTable(Class<K> kClass) {

        return new JoinLambdaQueryWrapper<>(kClass, null);
    }

    /**
     * 增加getJoinOne方法，防止直接调用baseMapper方法时查询多条结果异常
     * @param queryWrapper
     * @param throwEx
     * @return
     */
    public T getJoinOne(JoinLambdaQueryWrapper<T> queryWrapper, boolean throwEx) {
        if (baseMapper instanceof HBaseMapper) {
            return throwEx ? ((HBaseMapper<T>)baseMapper).selectJoinOne(queryWrapper) : SqlHelper.getObject(super.log, ((HBaseMapper<T>)baseMapper).selectJoinList(queryWrapper));
        }
        throw new RuntimeException("the entity mapper is not extends HBaseMapper");
    }

    /**
     * 分页查询
     * @param page
     * @param queryWrapper
     * @param <E>
     * @return
     */
    protected <E extends IPage<T>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public boolean update(Wrapper<T> updateWrapper) {
        // 如果 update_time 没有手动赋值

        if (StringUtils.isEmpty(updateWrapper.getSqlSet())) {
            return false;
        }

        if (!updateWrapper.getSqlSet().contains("update_time")) {
            if (updateWrapper instanceof LambdaUpdateWrapper) {
                LambdaUpdateWrapper<T> lambdaUpdateWrapper = (LambdaUpdateWrapper<T>) updateWrapper;
                lambdaUpdateWrapper.set(T::getUpdateTime, new Date());
            } else if (updateWrapper instanceof MyLambdaUpdateWrapper) {
                MyLambdaUpdateWrapper<T> lambdaUpdateWrapper = (MyLambdaUpdateWrapper<T>) updateWrapper;
                lambdaUpdateWrapper.set(T::getUpdateTime, new Date());
            }

        }
        return super.update(null, updateWrapper);
    }

    @Override
    public boolean saveBatch(Collection<T> collection) {
        if (CollectionUtil.isEmpty(collection)) {
            return false;
        }
        if (baseMapper instanceof HBaseMapper) {
            return ((HBaseMapper<T>) baseMapper).insertBatchSomeColumn(collection) == collection.size();
        }else {
            return super.saveBatch(collection);
        }
    }

    /**
     * 修改非空字段
     */
    public boolean updateNotNull(T entity) {
        return update(entity, getLambdaUpdate().eq(BaseEntity::getId, entity.getId()));
    }

    public T getByEq(SFunction<T, ?> columns,Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        return baseMapper.selectOne(lambdaQuery.eq(columns, value).last(" LIMIT 1"));
    }

    public boolean delByEq(SFunction<T, ?> columns,Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        return retBool(baseMapper.delete(lambdaQuery.eq(columns, value)));
    }

    public boolean delByEqIn(SFunction<T, ?> columns, Collection<?> value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        return retBool(baseMapper.delete(lambdaQuery.in(columns, value)));
    }

    public List<T> listByEq(SFunction<T, ?> columns, Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        return baseMapper.selectList(lambdaQuery.eq(columns, value));
    }
    public List<T> listByNotEq(SFunction<T, ?> columns, Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        return baseMapper.selectList(lambdaQuery.ne(columns, value));
    }

    /**
     * 是否存在
     */
    public boolean existsQuery(LambdaQueryWrapper<T> queryWrapper) {
        queryWrapper.last(" limit 1");
        return retBool(baseMapper.selectCount(queryWrapper));
    }
    public boolean existsByEq(SFunction<T, ?> columns, Object value) {
        return existsQuery(getLambdaQuery().eq(columns, value));
    }


    public Integer countByTargetFieldEq(SFunction<T, ?> columns, Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        lambdaQuery.eq(columns, value);
        return count(lambdaQuery);
    }

    public boolean updateByFieldEq(T t, SFunction<T, ?> columns,Object value) {
        LambdaUpdateWrapper<T> lambdaUpdate = getLambdaUpdate();
        lambdaUpdate.eq(columns, value);
        return update(t, lambdaUpdate);
    }

    public boolean saveOrUpdateNotNull(T entity) {
        if (entity.getId() == null) {
            return save(entity);
        }
        return updateNotNull(entity);
    }

    /**
     * 自动分页查询，通过 {@link PageParam#getEntity()} 不为空的数据库字段查询
     */
    public Page<T> autoSelectPage(PageParam<T> page) {

        T entity = page.getEntity();
        if (entity == null) {
            throw new RuntimeException("entity not allowed empty");
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        if (tableInfo == null) {
            throw new RuntimeException("tableInfo not found");
        }

        QueryWrapper<T> query = Wrappers.query(entity);

        try {
            for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
                String column = fieldInfo.getColumn();
                Field field = fieldInfo.getField();
                Object value = field.get(entity);

                if (value == null) {
                    continue;
                }

                query.eq(column, value);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return selectPage(page, query);
    }

    public boolean delById(Serializable id) {

        return retBool(baseMapper.deleteById(id));
    }

    public boolean updateTargetFieldById(SFunction<T, ?> columns, Object value, Serializable id) {
        LambdaUpdateWrapper<T> lambdaUpdate = getLambdaUpdate();
        lambdaUpdate.eq(T::getId, id);
        lambdaUpdate.set(columns, value);
        return update(lambdaUpdate);

    }

    @Override
    public Class<T> getEntityClass(){
        return super.entityClass;
    }

    public OriginSqlBo.OriginSqlBoBuilder<T> getOriginSqlBuilder() {
        return OriginSqlBo.builder();
    }


}
