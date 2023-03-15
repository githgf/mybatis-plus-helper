package com.hgf.helper.mybatisplus.generate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.hgf.helper.mybatisplus.BaseEntity;
import com.hgf.helper.mybatisplus.BaseServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hgf
 */
// 演示例子，执行 main 方法控制台输入模块表名回车自动生成对应项目目录中
public class CodeGeneratorUtil {


    /*public static void generate(DataSourceConfig dsc,PackageConfig pc,String dbTables,String tablePrefix) {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir") + "/eco";
        gc.setOutputDir(projectPath);
        gc.setAuthor("hans");
        gc.setIdType(IdType.AUTO);
        gc.setOpen(false);
        // gc.setSwagger2(true); 实体属性 Swagger2 注解
        mpg.setGlobalConfig(gc);

        // 数据源配置
        *//*DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl("jdbc:mysql://inner.com:3307/we_work_admin?useUnicode=true&useSSL=false&characterEncoding=utf8");
        // dsc.setSchemaName("public");
        dsc.setDriverName("com.mysql.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("123456");*//*

        mpg.setDataSource(dsc);

        // 包配置
        *//*PackageConfig pc = new PackageConfig();
        pc.setModuleName("db.mysql");
        pc.setParent("com.eco.robot.wework");*//*
        mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                // to do nothing
            }
        };

        // 如果模板引擎是 freemarker
        String templatePath = "/templates/mapper.xml.ftl";
        // 如果模板引擎是 velocity
        // String templatePath = "/templates/mapper.xml.vm";

        // 自定义输出配置
        List<FileOutConfig> focList = new ArrayList<>();
        // 自定义配置会被优先输出
        focList.add(new FileOutConfig(templatePath) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                // 自定义输出文件名 ， 如果你 Entity 设置了前后缀、此处注意 xml 的名称会跟着发生变化！！
                return projectPath + "/src/main/resources/mapper/" + pc.getModuleName()
                        + "/" + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_XML;
            }
        });
        *//*
        cfg.setFileCreate(new IFileCreate() {
            @Override
            public boolean isCreate(ConfigBuilder configBuilder, FileType fileType, String filePath) {
                // 判断自定义文件夹是否需要创建
                checkDir("调用默认方法创建的目录，自定义目录用");
                if (fileType == FileType.MAPPER) {
                    // 已经生成 mapper 文件判断存在，不想重新生成返回 false
                    return !new File(filePath).exists();
                }
                // 允许生成模板文件
                return true;
            }
        });
        *//*
        cfg.setFileOutConfigList(focList);
        mpg.setCfg(cfg);

        // 配置模板
        TemplateConfig templateConfig = new TemplateConfig();

        // 配置自定义输出模板
        //指定自定义模板路径，注意不要带上.ftl/.vm, 会根据使用的模板引擎自动识别
         templateConfig.setEntity("templates/mp_generate/entity.java");
         templateConfig.setServiceImpl("templates/mp_generate/serviceImpl.java");
         templateConfig.setController("templates/mp_generate/controller.java");

        templateConfig.setXml(null);
        templateConfig.setMapper("templates/mp_generate/mapper.java");
        mpg.setTemplate(templateConfig);

        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setSuperEntityClass(BaseEntity.class);
        strategy.setEntityLombokModel(true);
        strategy.setRestControllerStyle(true);
        strategy.setSuperServiceImplClass(BaseServiceImpl.class);
        // 公共父类
//        strategy.setSuperControllerClass("你自己的父类控制器,没有就不用设置!");
        // 写于父类中的公共字段
        strategy.setSuperEntityColumns("id", "create_time", "update_time");
        strategy.setInclude(dbTables.split(","));

        strategy.setControllerMappingHyphenStyle(true);
        strategy.setEntityTableFieldAnnotationEnable(true);
        strategy.setTablePrefix("");
        mpg.setStrategy(strategy);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }*/

}
