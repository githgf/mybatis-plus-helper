package com.hgf.helper.mybatisplus.inject;


import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.Insert;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;
import com.hgf.helper.mybatisplus.annotation.Association;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * 自定义sql注入器
 * @author hgf
 */
public class HSqlInject extends DefaultSqlInjector {
    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass);

//        methodList = methodList.stream().filter(t -> !(t instanceof Insert)).collect(Collectors.toList());

        InsertBatchSomeColumn insertBatchSomeColumn = new InsertBatchSomeColumn();
        insertBatchSomeColumn.setPredicate(t -> !t.getField().isAnnotationPresent(Association.class));
        methodList.add(insertBatchSomeColumn);



        methodList.add(new SelectJoinOne());
        methodList.add(new SelectJoinMaps());
        methodList.add(new SelectJoinPage());
        methodList.add(new SelectJoinList());
        methodList.add(new SelectTargetObjects());
        return methodList;
    }

}
