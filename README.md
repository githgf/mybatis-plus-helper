# mybatis-plus-helper

# 描述

是在`mybatis-plus`的基础上进行二次开发，实现一些自定义的功能

# 实现功能

- 多表联查
- 读写分离
- 自定义的`LambdaQueryWrapper`,`LambdaUpdateWrapper`
- 格式化打印执行的sql语句



# 应用实例DEMO

[mybatis-plus-helper-demo地址](https://gitee.com/hanscoding/mybatis-plus-helper-demo.git)


# 安装方式

## pom文件引入依赖

最新版本：0.0.2

```xml
<dependency>
   <groupId>io.github.githgf</groupId>
   <artifactId>mybatis-plus-helper</artifactId>
   <version>0.0.2</version>
</dependency>

<dependency>
   <groupId>com.baomidou</groupId>
   <artifactId>mybatis-plus-boot-starter</artifactId>
   <version>3.4.1</version>
</dependency>


<dependency>
   <groupId>org.mybatis.spring.boot</groupId>
   <artifactId>mybatis-spring-boot-starter</artifactId>
   <version>2.0.1</version>
</dependency>

<dependency>
   <groupId>com.baomidou</groupId>
   <artifactId>mybatis-plus-generator</artifactId>
   <version>3.4.1</version>
</dependency>
```
## 源码构建安装

下载源码之后可以修改pom文件提交到自己私服，或者本地install


# 使用文档


## 格式化打印执行的sql语句

### 使用方式

```java
@Configuration
@MapperScan("com.hgf.helper.mybatisplusdemo.db.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public SqlCostInterceptor sqlCostInterceptor() {
        return new SqlCostInterceptor();
    }

}
```

### 参数说明

| 参数名         | 类型 | 作用                                                         |
| -------------- | ---- | ------------------------------------------------------------ |
| mp.printQuery  | bol  | 是否打印查询sql                                              |
| mp.bigListSize | int  | 大list的最小size，超过这个size的list参数不打印，防止引起业务严重超时 |
| mp.execTimeOut | int  | 超时预警时间，超过这个时间则会打印超时预警sql                |

# 多表联查

## 适配的数据库类型

 - mysql


## 使用方式

spring引入`JoinHelperStartHook`,`HSqlInject`

```java
@Configuration
@MapperScan("com.hgf.helper.mybatisplusdemo.db.mapper")
@Import({JoinHelperStartHook.class})
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public SqlCostInterceptor sqlCostInterceptor() {
        return new SqlCostInterceptor();
    }

    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }

   // 自定义方法注入
    @Bean
    public HSqlInject hSqlInject() {
        return new HSqlInject();
    }
}
```

### 查询结果类

- 查询结果实体类是数据库表关联

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
    @TableField(select = false,
            typeHandler = JacksonTypeHandler.class,
            insertStrategy = FieldStrategy.NEVER,
            whereStrategy = FieldStrategy.NEVER
    )
    @Association()
    ScoreInfo score;

    public void setName(String name) {
        this.name = name;
    }
}
```

- 如果查询结果实体类不是数据库表关联

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

service 代码，在`com.hgf.helper.mybatisplusdemo.db.service.StudentServiceImpl`有多种demo
```java
/**
     * 联查多条记录测试
     */
public List<Student> joinListTest() {
  // 主表wrapper
  JoinLambdaQueryWrapper<Student> studentWrapper = joinQueryWrapperForTable();
  // 副表wrapper
  JoinLambdaQueryWrapper<ScoreInfo> scoreWrapper = studentWrapper.innerJoin(ScoreInfo.class, Student::getNo, ScoreInfo::getStuNo);

  // 查询指定字段
  //        studentWrapper.select(Student::getNo);
  //        scoreWrapper.select(ScoreInfo::getScore, ScoreInfo::getStuScore);
  // 查询条件
  studentWrapper.in(Student::getId, Arrays.asList(2, 13));

  // 排序
  scoreWrapper.orderByDesc(ScoreInfo::getScore);

  return baseMapper.selectJoinList(studentWrapper);
}
```

mapper 代码，在`com.hgf.helper.mybatisplusdemo.db.mapper.StudentMapper`有多个demo

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

### 原理简要说明

`com.hgf.helper.mybatisplus.inject.HSqlInject`中注入了自定义的方法，在项目启动之后会启动`com.hgf.helper.mybatisplus.join.JoinHelperStartHook`钩子方法

此时有两个步骤的操作

1. 会将所有数据库实体类中某个属性带有`Association`注解的实体类对应的resultMap通过反射修改。

举例：

假设原来`Student`对应的resultMap大致如下xml

```xml
<resultMap id="com.hgf.boot.db.mapper.StudentMapper.mybatis-plus_Student"  type="com.hgf.boot.db.entity.Student">
    <id property="id" column="id" />
    <result property="no" column="no"/>
    <result property="name" column="name"/>
    <result property="sex" column="sex"/>
    <result property="score" column="score"/>
</resultMap>
```

经过修改后为

```xml
<resultMap id="com.hgf.boot.db.mapper.StudentMapper.mybatis-plus_Student"  type="com.hgf.boot.db.entity.Student">
    <id property="id" column="id" />
    <result property="no" column="no"/>
    <result property="name" column="name"/>
    <result property="sex" column="sex"/>
 		<association property="score" 
                 columnPrefix="score_" 	resultMap="com.hgf.boot.db.mapper.ScoreInfoMapper.mybatis-plus_ScoreInfo">
  	</association>
</resultMap>
```

其中`columnPrefix` 属性取自`Association#aliasPrefix`

2. 将在`mapper` 中声明但是非数据库映射实体类的类自动封装`resultMap`

   此时`com.hgf.helper.mybatisplus.inject.SelectTargetObjects#autoAddMappedStatements()`会自动构建



#  读写分离

## 使用方式

```java
@Configuration
@MapperScan("com.hgf.helper.mybatisplusdemo.db.mapper")
@Import({SalvesReadAutoConfig.class})
public class MybatisPlusConfig {
    ......

}
```

## 数据库配置

```yml
spring:
  datasource:
  #主库
    url: jdbc:mysql://172.16.4.208:3306/xx
    username: root
    password: pwd
    driver-class-name: com.mysql.jdbc.Driver
    salves:
    # 读库 s1
      s1:
        url: jdbc:mysql://172.16.1.139:3306/xx
        username: root
        password: pwd
      s2:
        url: jdbc:mysql://172.16.1.139:3306/xx
        username: root
        password: pwd   
db:
  # 开关
  more: true        
```

## 原理简要说明

将mp容器中注入自定义的`SessionTemplate`===>`com.hgf.helper.mybatisplus.salves.MySqlSessionTemplate`

向spring容器中注入自定义`DataSource`=====>`com.hgf.helper.mybatisplus.salves.DyDataSource`

在查询语句前在当前线程上下文设置标记，任何sql语句执行前一定会调用`DyDataSource#getConnection()`,此时判断当前线程上下文中是否有查询sql标记，如果是则从读库中随机挑一个地址查询

## 注意事项

支持的数据源

- HikariDataSource

# 丰富的LambdaWarpper

## MyLambdaQueryWrapper

### 使用demo

```java
public Student getById(){
    return getOne(
            getMyLambdaQuery()
                    .gt(Student::getId, 2)
                    .sum(Student::getSex).as("sex")

    );
}
```



## MyLambdaUpdateWrapper

### 使用demo

```java
/**
 * case update
 */
public boolean updateCase() {

    Map<Object, Object> updateMap = new HashMap<>();
    updateMap.put(2, 2);
    updateMap.put(13, 1);

    /**
     * UPDATE student
     * SET sex = (case id WHEN 2 THEN 2 WHEN 13 THEN 1 end)
     * WHERE (id IN (2, 13))
     */
    return update(
            getMyLambdaUpdate()
                    .caseSet(Student::getSex, updateMap, Student::getId)

    );
}
```

