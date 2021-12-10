package com.hgf.helper.mybatisplus.utils;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合处理工具
 */
public class CollectionUtil extends CollectionUtils {

    public static <T> boolean isNotEmpty(Collection<T> collection){
        return !isEmpty(collection);
    }

    public static <K,V> boolean isNotEmpty(Map<K,V> map){
        return !isEmpty(map);
    }

    public static <T> boolean isEmpty(T[] tArray){
        return tArray == null || tArray.length == 0;
    }

    /**
     * 将集合根据指定大小分段
     * @param list      list数据
     * @param size      分段数量
     * @param <T>       T
     */
    public static <T>List<List<T>> subList(List<T> list, int size){
        List<List<T>> lists = new ArrayList<>(size);
        if (list == null || list.size() <= 1){
            lists.add(list);
            return lists;
        }

        int i = list.size() / size;

        for (int j = 0; j < size; j++) {
            if (j == size - 1){
                lists.add(list.subList(j * i, list.size()));
            }else {
                lists.add(list.subList(j * i, j * i + i));
            }

        }

        return lists;
    }

    /**
     * 按照子列表的数量进行分隔，每个群成员的列表大小 <=  memberSize
     * @param list
     * @param memberSize
     * @param <T>
     * @return
     */
    public static <T>List<List<T>> splitList(List<T> list, int memberSize){
        List<List<T>> lists = new ArrayList<>(list.size());
        if (list.size() <= memberSize){
            lists.add(list);
            return lists;
        }

        int i = (int) Math.ceil(list.size() / (memberSize * 1.0));

        for (int j = 0; j < i; j++) {

            int min = Math.min(memberSize, list.size());
            if (min < 0) {
                break;
            }
            lists.add(list.subList(0, min));
            list = list.subList(min, list.size());
        }
        return lists;
    }

    /**
     * 将集合中的成员按照某个字段映射并生成相应的map
     * @param collection 归类对象
     * @param function   字段获取的方法
     * @param <K>        字段类别
     * @param <T>        成员类型
     * @return           map结果集
     */
    public static <K,T> Map<K,T> convertToMapByFunc(Collection<T> collection, Function<T,K> function) {
        return Optional.ofNullable(collection).map(p -> p.stream().filter(t -> null != function.apply(t)).collect(Collectors.toMap(function, Function.identity(), (entity1, entity2) -> entity1))).orElse(null);
    }

    /**
     * 将集合中的成员按照某个字段归类并生成相应的map
     * @param collection 归类对象
     * @param function   字段获取的方法
     * @param <K>        字段类别
     * @param <T>        成员类型
     * @return           map结果集
     */
    public static <K,T> Map<K,List<T>> groupToMapByFunc(Collection<T> collection, Function<T,K> function) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        // 注意：如果function的结果为null，则报错element cannot be mapped to a null key,
        return collection.stream().filter(t -> null != function.apply(t)).collect(Collectors.groupingBy(function,Collectors.toList()));
    }

    /**
     * 用于提取对象数组中的字段数组
     * @param collection 提取对象
     * @param function  字段名称
     * @param <T>        成员类型
     * @param <E>        字段类型
     * @return           set结果集
     */
    public static <T,E> Set<E> mapFieldsToSet(Collection<T> collection, Function<T,E> function) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.stream().filter(t -> null != function.apply(t)).map(function).collect(Collectors.toSet());
    }

    /**
     * 用于提取对象数组中的字段数组
     * @param collection 提取对象
     * @param function  字段名称
     * @param <T>        成员类型
     * @param <E>        字段类型
     * @return           list结果集
     */
    public static <T,E> List<E> mapFieldsToList(Collection<T> collection, Function<T,E> function) {
        if (CollectionUtil.isEmpty(collection)) {
            return null;
        }
        return collection.stream().filter(t -> null != function.apply(t)).map(function).collect(Collectors.toList());
    }

    /**
     * 随机成员
     */
    public static <T> T randomMember(Collection<T> collection) {
        if (CollectionUtil.isEmpty(collection)) {
            return null;
        }
        int index = RandomUtils.nextInt(0, collection.size());
        int i = 0;

        for (T t : collection) {
            if (i == index) {
                return t;
            }
            i++;
        }

        return null;
    }


    /**
     * 获取两个集合相同的部分
     */
    public static <T> List<T> getRepeatList(Collection<T> collection1, Collection<T> collection2) {
        if (collection1 == null || collection2 == null) {
            return null;
        }
        return collection1.stream().filter(collection2::contains).collect(Collectors.toList());
    }
}
