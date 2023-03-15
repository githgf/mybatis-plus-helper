package com.hgf.helper.mybatisplus;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.hgf.helper.mybatisplus.helper.MyLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.helper.MyLambdaUpdateWrapper;
import com.hgf.helper.mybatisplus.join.JoinLambdaQueryWrapper;
import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 自定义 mybatis-plus db基础服务
 * @author hgf
 */
@SuppressWarnings("unchecked")
@Slf4j
public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M,T> {

    @Override
    protected Class<M> currentMapperClass() {
        return (Class<M>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseServiceImpl.class, 0);
    }

    @Override
    protected Class<T> currentModelClass() {
        return (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseServiceImpl.class, 1);
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
        LambdaUpdateWrapper<T> lambdaUpdate = (LambdaUpdateWrapper<T>) Wrappers.lambdaUpdate(entityClass);
//        lambdaUpdate.set(T::getUpdateTime, new Date());
        return lambdaUpdate;
    }

    @Override
    public LambdaUpdateChainWrapper<T> lambdaUpdate() {
        LambdaUpdateChainWrapper<T> lambdaUpdate = super.lambdaUpdate();
//        lambdaUpdate.set(T::getUpdateTime, new Date());
        return lambdaUpdate;
    }

    public MyLambdaUpdateWrapper<T> getMyLambdaUpdate() {
        MyLambdaUpdateWrapper<T> myLambdaUpdateWrapper = new MyLambdaUpdateWrapper(entityClass);
//        myLambdaUpdateWrapper.set(T::getUpdateTime, new Date());
        return myLambdaUpdateWrapper;
    }

    public MyLambdaQueryWrapper<T> getMyLambdaQuery() {
        return new MyLambdaQueryWrapper(entityClass);
    }

    public JoinLambdaQueryWrapper<T> joinQueryWrapperForOther(Class<?> queryType){
        return new JoinLambdaQueryWrapper(entityClass, queryType);
    }

    public JoinLambdaQueryWrapper<T> joinQueryWrapperForTable(){
        return new JoinLambdaQueryWrapper(entityClass, null);
    }

    public <K extends BaseEntity> JoinLambdaQueryWrapper<K> joinQueryWrapperForTable(Class<K> kClass) {

        return new JoinLambdaQueryWrapper(kClass, null);
    }


    /**
     * 分页查询
     * @param page
     * @param queryWrapper
     * @param <E>
     * @return
     */
    protected <E extends IPage<T>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {

        /*if (queryWrapper instanceof AbstractWrapper) {
            boolean hasOrder = !StringUtils.isEmpty(queryWrapper.getSqlSegment()) && queryWrapper.getSqlSegment().toUpperCase().contains("ORDER");
            if (!hasOrder) {
                AbstractWrapper wrapper = (AbstractWrapper) queryWrapper;
                if (wrapper instanceof QueryWrapper) {
                    QueryWrapper<T> lambdaQueryWrapper = (QueryWrapper<T>) wrapper;
                    lambdaQueryWrapper.orderBy(true, false, "create_time");
                }
            }
        }*/
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public boolean update(Wrapper<T> updateWrapper) {
        // 如果 update_time 没有手动赋值

        if (StringUtils.isEmpty(updateWrapper.getSqlSet())) {
            return false;
        }

        // 主动填充
        try {
            T t = entityClass.newInstance();
            return super.update(t, updateWrapper);
        } catch (Exception e) {
            return super.update(null, updateWrapper);
        }

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

    @Override
    public boolean updateBatchById(Collection<T> collection) {
        if (CollectionUtil.isEmpty(collection)) {
            return false;
        }
        if (baseMapper instanceof HBaseMapper) {
            ArrayList<T> ts = new ArrayList<>(collection);
            CollectionUtil.splitList(ts, 100);

            return ((HBaseMapper<T>) baseMapper).updateBatchSomeColumn(collection) == collection.size();
        }else {
            return super.updateBatchById(collection);
        }
//        return super.updateBatchById(collection);
    }

    public boolean saveBatchWithInput(Collection<T> collection) {
        if (CollectionUtil.isEmpty(collection)) {
            return false;
        }
        if (baseMapper instanceof HBaseMapper) {
            return ((HBaseMapper<T>) baseMapper).insertBatchSomeColumnWithInput(collection) == collection.size();
        }else {
            return false;
        }
    }

    /**
     * 修改非空字段
     */
    public boolean updateNotNull(T entity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        String keyColumn = tableInfo.getKeyColumn();

        Object fieldValue = getKeyValue(entity);

        UpdateWrapper<T> update = Wrappers.update(entity);
        update.eq(keyColumn, fieldValue);
        return update(entity, update);
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


    public long countByTargetFieldEq(SFunction<T, ?> columns, Object value) {
        LambdaQueryWrapper<T> lambdaQuery = getLambdaQuery();
        lambdaQuery.eq(columns, value);
        return count(lambdaQuery);
    }

    public boolean updateByFieldEq(T t, SFunction<T, ?> columns,Object value) {
        LambdaUpdateWrapper<T> lambdaUpdate = getLambdaUpdate();
        lambdaUpdate.eq(columns, value);
        return update(t, lambdaUpdate);
    }

    public boolean updateTargetFieldById(Integer id, SFunction<T, ?> columns,Object value) {
        LambdaUpdateWrapper<T> lambdaUpdate = getLambdaUpdate();
        lambdaUpdate.set(columns, value);
//        lambdaUpdate.eq(T::getId, id);
        return update(lambdaUpdate);
    }

    public boolean updateTargetFieldByIds(List<Integer> ids, SFunction<T, ?> columns,Object value) {
        LambdaUpdateWrapper<T> lambdaUpdate = getLambdaUpdate();
        lambdaUpdate.set(columns, value);
//        lambdaUpdate.in(T::getId, ids);
        return update(lambdaUpdate);
    }

    public boolean saveOrUpdateNotNull(T entity) {
        if (getKeyValue(entity) == null) {
            return save(entity);
        }
        return updateNotNull(entity);
    }

    /**
     * 自动分页查询，通过 {@link PageParam#getEntity()} 不为空的数据库字段查询
     */
    public PageParam<T> autoSelectPage(PageParam<T> page) {

        T entity = page.getEntity();
        QueryWrapper<T> query = Wrappers.query(entity);

        if (entity != null) {

            TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
            if (tableInfo == null) {
                throw new RuntimeException("tableInfo not found");
            }


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
        }


        return selectPage(page, query);
    }

    public boolean delById(Serializable id) {

        return retBool(baseMapper.deleteById(id));
    }

    public boolean delByQuery(LambdaQueryWrapper<T> wrapper) {

        return retBool(baseMapper.delete(wrapper));
    }

    protected boolean retBool(Long result) {
        return SqlHelper.retBool(result);
    }

    /**
     * 获取实体类主键值
     */
    protected Object getKeyValue(T entity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        String keyColumn = tableInfo.getKeyColumn();
        String keyProperty = tableInfo.getKeyProperty();

        Object fieldValue = null;
        try {
            fieldValue = FieldUtils.readField(entity, keyProperty);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return fieldValue;
    }
    
    public OriginSqlBo.OriginSqlBoBuilder<T> getOriginSqlBuilder() {
        return OriginSqlBo.builder();
    }

    public String getUpdateFillSql(){
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        List<String> sqlList = new ArrayList<>();
        Configuration configuration = GlobalConfigUtils.currentSessionFactory(entityClass).getConfiguration();

        T t = null;
        try {
            t = entityClass.newInstance();
        } catch (Exception e) {
            return "";
        }
        MetaObject metaObject = configuration.newMetaObject(t);
        GlobalConfigUtils.getMetaObjectHandler(configuration).ifPresent(metaObjectHandler -> {
            if (metaObjectHandler.openUpdateFill() && tableInfo.isWithUpdateFill()) {
                metaObjectHandler.updateFill(metaObject);
            }
        });

        return "";

    }
}
