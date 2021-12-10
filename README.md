# mybatis-plus 组件相关说明

# join包

作用： 多表联查的相关组件

## 使用方式

### 查询结果

- 实例一

如果查询结果实体类是数据库表

```java
@Setter
@Getter
@TableName(value = "student", autoResultMap = true)
public class Student extends BaseEntity {

    @TableField
    String name;

    @TableField("no")
    String no;

    // TableField 只能是这个格式
    @TableField(select = false,typeHandler = JacksonTypeHandler.class)
    @Association()
    ScoreInfo score;

    public void setName(String name) {
        this.name = name;
    }


}
```

- 示例2

如果查询结果实体类不是数据库表

```java
@Setter
@Getter
public class ResultVo {
    @TableField
    private String name;

    @Association
    private Student student;

    @Association(aliasPrefix = "score_")
    private ScoreInfo scoreInfo;


    public void setName(String name) {
        this.name = name;
    }
}
```

其中 `@Association` 注解的属性所属类型必须是数据库表映射实体类

### 查询

service 代码
```java
        // 查询wrapper
        JoinLambdaQueryWrapper<Student> studentWrapper = new JoinLambdaQueryWrapper<>(Student.class);
        // 设置查询返回结果，如果是数据库实体类不需要
        studentWrapper.setQueryType(ResultVo.class);
        JoinLambdaQueryWrapper<ScoreInfo> scoreWrapper = new JoinLambdaQueryWrapper<>(ScoreInfo.class);
        // 查询类型
        studentWrapper.innerJoin(scoreWrapper, Student::getNo, ScoreInfo::getStuNo);

        // 查询指定字段
        studentWrapper.select(Student::getNo);
        scoreWrapper.select(ScoreInfo::getScore, ScoreInfo::getStuScore);
        // 查询条件
        studentWrapper.eq(Student::getId, 2);

        // 排序
        studentWrapper.orderByAsc(Student::getSex);

        return baseMapper.getResultVo(studentWrapper);
```

mapper 代码

普通查询
```java
@SelectTargetResult
ResultVo getResultVo(@Param(Constants.WRAPPER) JoinLambdaQueryWrapper<Student> lambdaQueryWrapper);
```

如果是分页查询
```java
/**
  * 查询 {@link TrainerInfoBo} 并且分页
  *
  * @param queryWrapper 查询条件
  */
 @SelectTargetResult(TrainerInfoBo.class)
 <E extends IPage<?>> Page<TrainerInfoBo> selectEmployerInfoBoPage(E page, @Param(Constants.WRAPPER) JoinLambdaQueryWrapper<TrainerInfo> queryWrapper);

```



# generate

作用：自动生成mvc代码

# inject
作用：自定义mapper方法

# salves

作用：读取分离

