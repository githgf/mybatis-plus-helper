package com.hgf.helper.mybatisplus.salves;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

/**
 * 自定义的sqlSession 执行器
 */
public class MySqlSessionTemplate extends SqlSessionTemplate {

    public MySqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        super(sqlSessionFactory);
    }

    public MySqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
        super(sqlSessionFactory, executorType);
    }

    @Override
    public <T> T selectOne(String statement) {
        try {
            beforeQuery();
            return super.selectOne(statement);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        try {
            beforeQuery();
            return super.selectOne(statement, parameter);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        try {
            beforeQuery();
            return super.selectMap(statement, mapKey);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        try {
            beforeQuery();
            return super.selectMap(statement, parameter, mapKey);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        try {
            beforeQuery();
            return super.selectMap(statement, parameter, mapKey, rowBounds);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <T> Cursor<T> selectCursor(String statement) {
        try {
            beforeQuery();
            return super.selectCursor(statement);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter) {
        try {
            beforeQuery();
            return super.selectCursor(statement, parameter);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
        try {
            beforeQuery();
            return super.selectCursor(statement, parameter, rowBounds);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <E> List<E> selectList(String statement) {
        try {
            beforeQuery();
            return super.selectList(statement);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        try {
            beforeQuery();
            return super.selectList(statement, parameter);
        } finally {
            afterQuery();
        }
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
            beforeQuery();
            return super.selectList(statement, parameter, rowBounds);
        } finally {
            afterQuery();
        }
    }

    @Override
    public void select(String statement, ResultHandler handler) {
        try {
            beforeQuery();
            super.select(statement, handler);
        } finally {
            afterQuery();
        }
    }

    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        try {
            beforeQuery();
            super.select(statement, parameter, handler);
        } finally {
            afterQuery();
        }
    }

    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        try {
            beforeQuery();
            super.select(statement, parameter, rowBounds, handler);
        } finally {
            afterQuery();
        }
    }

    /**
     * 查询之前的操作，设置从库读
     */
    void beforeQuery(){
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        String originKey = DyDataSource.getThreadLocalDbKey();
        if (originKey != null) {
            DyDataSource.putAttr("origin", originKey);
        }
        DyDataSource.setDbKey(DyDataSource.RANDOM_SALVE);
    }

    void afterQuery(){
        Object origin = DyDataSource.getAttr("origin");
        if (origin == null) {
            return;
        }

        DyDataSource.setDbKey(origin);

    }

}
