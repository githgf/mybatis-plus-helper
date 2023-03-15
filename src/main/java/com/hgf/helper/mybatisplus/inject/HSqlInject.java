package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author hgf
 */
@Component
public class HSqlInject extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
        methodList.add(new InsertBatchColumn());
        methodList.add(new BatchUpdate());
        methodList.add(new SelectJoinOne());
        methodList.add(new SelectJoinMaps());
        methodList.add(new SelectJoinPage());
        methodList.add(new SelectJoinList());
        methodList.add(new SelectTargetObjects());
        methodList.add(new InsertBatchSomeColumnWithInput());
        methodList.add(new InsertBatchMethod());
        methodList.add(new UpdateBatchMethod());
        return methodList;
    }
}
