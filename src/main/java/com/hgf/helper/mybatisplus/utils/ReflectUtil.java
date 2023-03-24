package com.hgf.helper.mybatisplus.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 反射工具
 */
@Slf4j
public class ReflectUtil {

    /**
     * 获取指定对象所有属性值
     */
    public static JSONObject getParamValue(Object object, Class<?> tClass){
        Field[] declaredFields = tClass.getDeclaredFields();

        JSONObject valueJson = new JSONObject();
        for (Field declaredField : declaredFields) {

            String name = declaredField.getName();
            declaredField.setAccessible(true);
            Object o = null;
            try {
                o = declaredField.get(object);
                /*if (declaredField.isAnnotationPresent(NotValue.class)){
                    continue;
                }*/
                if (declaredField.isAnnotationPresent(JsonIgnore.class)){
                    o = JSON.toJSONString(o);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            valueJson.put(name, o);
        }

        return valueJson;
    }

    /**
     * 获取父类泛型 类型集合
     */
    public static Type[] getGenericParamClassArray(Class<?> clazz){
        ParameterizedType genericSuperclass = getType(clazz);
        if (genericSuperclass == null) {
            throw new RuntimeException("Generic super class is null");
        } else {
            return genericSuperclass.getActualTypeArguments();
        }
    }

    private static ParameterizedType getType(Class<?> clazz) {
        for(int index = 0; index < 5; ++index) {
            Type superType = clazz.getGenericSuperclass();
            if (superType instanceof ParameterizedType) {
                return (ParameterizedType)superType;
            }

            clazz = clazz.getSuperclass();
        }

        return null;
    }

    /**
     * 验证当前class 是否是 list、set、collection
     */
    public static boolean isList(Class<?> aClass) {
        return aClass.isAssignableFrom(List.class) || aClass.isAssignableFrom(Set.class) || aClass.isAssignableFrom(Collection.class);
    }

    public static Class<?> getRealListClass(Type genericType) {
        // 获取list中泛型类的class
        ParameterizedType pt = (ParameterizedType) genericType;
        String typeName = pt.getActualTypeArguments()[0].getTypeName();

        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            log.error("typeName not found");
            return null;
        }
    }


    /**
     * 获取属性真实的class
     */
    public static Class<?> getFieldRealType(Field field) {
        Class<?> type = field.getType();
        if (isList(type)) {
            Type genericType = field.getGenericType();
            return getRealListClass(genericType);
        }else {
            return type;
        }
    }

    /**
     * 获取方法返回真实返回class
     */
    public static Class<?> getMethodRealReturnType(Method method) {

        Class<?> type = method.getReturnType();
        if (isList(type)) {
            Type genericType = method.getGenericReturnType();
            return getRealListClass(genericType);
        } else {
            return type;
        }
    }

}
