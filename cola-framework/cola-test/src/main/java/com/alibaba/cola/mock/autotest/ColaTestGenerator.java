package com.alibaba.cola.mock.autotest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.cola.mock.ColaMockito;
import com.alibaba.cola.mock.persist.DataMapStore;
import com.alibaba.cola.mock.utils.CommonUtils;
import com.alibaba.cola.mock.utils.Constants;
import com.alibaba.cola.mock.utils.FileUtils;
import com.alibaba.cola.mock.utils.TemplateBuilder;

import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shawnzhan.zxy
 * @date 2019/01/12
 */
public class ColaTestGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ColaTestGenerator.class);
    private String testMethod;
    private String superClazz;

    public ColaTestGenerator(String testMethod, String superClazz){
        this.testMethod =testMethod;
        this.superClazz = superClazz;
    }

    public void generate(Object... params){
        Writer writer = null;
        try {
            TestClass testClass = new TestClass(testMethod, superClazz, params);
            // 创建插值的map
            Map<String, Object> map = buildMap(testClass);
            // 执行插值，并输出到指定的输出流中
            File file = createIfNotExists(testClass.getFilePath());
            writer = new FileWriter(file);

            TemplateBuilder builder = new TemplateBuilder(getTemplate());
            builder.setVar(map);
            builder.build(writer);

            saveDataMap(testClass);
            logger.info("generate success. file=" + file.toString());
        }catch(Exception e){
            logger.error("ColaTestGenerate.generate ERROR!", e);
        }finally {
            CommonUtils.closeWriter(writer);
        }
    }

    private Map<String, Object> buildMap(TestClass testClass){
        Map<String, Object> tempalteMap = new HashMap<>();
        tempalteMap.put("namespace", testClass.getNamespace());
        tempalteMap.put("imports", testClass.buildImports());
        tempalteMap.put("date", CommonUtils.formatDate(new Date()));
        tempalteMap.put("unitTestClass", testClass.getUnitTestClassName());
        tempalteMap.put("testClass", testClass.getSimpleClassName());
        tempalteMap.put("testClassName", CommonUtils.toLowerCaseFirstOne(testClass.getSimpleClassName()));
        tempalteMap.put("testMethod", testClass.getMethodName());
        tempalteMap.put("superClass", testClass.getSuperClazzName());
        tempalteMap.put("varDefinitions", testClass.buildVarDefinition());
        tempalteMap.put("params", testClass.buildParams());
        tempalteMap.put("return", testClass.buildReturn());
        return tempalteMap;
    }

    private void saveDataMap(TestClass testClass){
        //Map<String, Class> paramterMap = testClass.getAutoGenerateDataParameter();
        Map<String, Object> paramterValueMap = testClass.getAutoGenerateDataParameter();
        if(paramterValueMap.size() == 0){
            return;
        }
        DataMapStore dataMapStore = new DataMapStore();
        String fileName = testClass.getNamespace() + Constants.DOT + testClass.getUnitTestClassName();
        fileName = FileUtils.getAbbrOfClassName(fileName);
        dataMapStore.save(paramterValueMap, fileName);
    }

    private String getTemplate(){
        StringBuilder sb = new StringBuilder();
        InputStreamReader isReader = null;
        BufferedReader br = null;
        try {
            isReader = new InputStreamReader(FileUtils.getTestClassTemplate());
            br = new BufferedReader(isReader);
            String line;
            //网友推荐更加简洁的写法
            while ((line = br.readLine()) != null) {
                // 一次读入一行数据
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            CommonUtils.closeStream(br);
        }
        return sb.toString();
    }


    private File createIfNotExists(String filePath){
        FileUtils.createDirectory(filePath);
        return new File(filePath);
    }

}
