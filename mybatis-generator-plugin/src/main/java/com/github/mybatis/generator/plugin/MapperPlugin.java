package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaFormatter;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

/**
 * 生成 Mapper 类
 * 
 * @author Johnny
 *
 */
public class MapperPlugin extends PluginAdapter {

    private static final String DEFAULT_DAO_SUPER_CLASS = "com.github.mybatis.mapper.GenericMapper";

    private ShellCallback shellCallback = null;

    private String daoTargetDir;

    private String daoTargetPackage;

    /**
     * Model基类文件包名
     */
    private String baseModelPackage;

    /**
     * Model类的前缀名称
     */
    private String baseModelNamePrefix;

    private String daoSuperClass;

    public MapperPlugin() {
        shellCallback = new DefaultShellCallback(false);
    }

    @Override
    public boolean validate(List<String> warnings) {
        daoTargetDir = properties.getProperty("daoTargetDir");
        boolean valid = stringHasValue(daoTargetDir);

        daoTargetPackage = properties.getProperty("daoTargetPackage");
        boolean valid2 = stringHasValue(daoTargetPackage);

        daoSuperClass = properties.getProperty("daoSuperClass");
        if (!stringHasValue(daoSuperClass)) {
            daoSuperClass = DEFAULT_DAO_SUPER_CLASS;
        }

        baseModelPackage = properties.getProperty("baseModelPackage");
        if (!stringHasValue(baseModelPackage)) {
            baseModelPackage = ModelAndExampleBaseClassPlugin.DEFAULT_BASE_MODEL_PACKAGE;
        }

        baseModelNamePrefix = properties.getProperty("baseModelNamePrefix");
        if (!stringHasValue(baseModelNamePrefix)) {
            baseModelNamePrefix = ModelAndExampleBaseClassPlugin.DEFAULT_BASE_MODEL_NAME_PREFIX;
        }

        return valid && valid2;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        System.out.println("===============开始：生成Mapper文件================");

        JavaFormatter javaFormatter = context.getJavaFormatter();

        List<GeneratedJavaFile> mapperJavaFiles = new ArrayList<GeneratedJavaFile>();
        for (GeneratedJavaFile javaFile : introspectedTable.getGeneratedJavaFiles()) {

            CompilationUnit unit = javaFile.getCompilationUnit();
            FullyQualifiedJavaType baseModelJavaType = unit.getType();

            String shortName = baseModelJavaType.getShortName();

            if (shortName.endsWith("Example")) {// 针对Example类不要生成Mapper
                continue;
            }

            String subModelType = getSubModelType(baseModelJavaType);
            String subModelExampleType = subModelType + "Example";

            System.out.println("shortName:" + shortName);

            String subModelName = shortName.replace(baseModelNamePrefix, "");

            Interface mapperInterface = new Interface(daoTargetPackage + "." + subModelName + "Mapper");

            mapperInterface.setVisibility(JavaVisibility.PUBLIC);
            mapperInterface.addJavaDocLine(" /**");
            mapperInterface.addJavaDocLine(" * 由MBG工具自动生成，添加与扩展XML文件中对应的同名方法");
            mapperInterface.addJavaDocLine(" **/");

            FullyQualifiedJavaType subModelJavaType = new FullyQualifiedJavaType(subModelType);
            mapperInterface.addImportedType(subModelJavaType);
            FullyQualifiedJavaType subModelExampleJavaType = new FullyQualifiedJavaType(subModelExampleType);
            mapperInterface.addImportedType(subModelExampleJavaType);

            FullyQualifiedJavaType daoSuperType = new FullyQualifiedJavaType(daoSuperClass);
            // 添加泛型支持
            daoSuperType.addTypeArgument(subModelJavaType);
            daoSuperType.addTypeArgument(subModelExampleJavaType);
            daoSuperType.addTypeArgument(new FullyQualifiedJavaType("java.lang.String"));
            mapperInterface.addImportedType(daoSuperType);
            mapperInterface.addSuperInterface(daoSuperType);

            try {
                GeneratedJavaFile mapperJavafile = new GeneratedJavaFile(mapperInterface, daoTargetDir, javaFormatter);

                File mapperDir = shellCallback.getDirectory(daoTargetDir, daoTargetPackage);

                File mapperFile = new File(mapperDir, mapperJavafile.getFileName());

                // 文件不存在
                if (!mapperFile.exists()) {

                    mapperJavaFiles.add(mapperJavafile);
                }
            } catch (ShellException e) {
                e.printStackTrace();
            }

        }

        System.out.println("===============结束：生成Mapper文件================");

        return mapperJavaFiles;
    }

    private String getSubModelType(FullyQualifiedJavaType fullyQualifiedJavaType) {
        String type = fullyQualifiedJavaType.getFullyQualifiedName();
        String defaultPrefix = baseModelPackage + "." + baseModelNamePrefix;
        String newType = type.replace(defaultPrefix, "");
        return newType;
    }
}
