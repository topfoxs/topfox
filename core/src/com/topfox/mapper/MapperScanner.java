package com.topfox.mapper;

import com.topfox.common.AppContext;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapperScanner {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 以文件路径为key, 以文件的修改时间为 value
     */
    public static final ConcurrentHashMap<String, String> mapXml = new ConcurrentHashMap();
    private static final String XML_RESOURCE_PATTERN = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "**/*Mapper.xml";


    protected final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private Resource[] resourcesMappers;

    public MapperScanner() throws Exception {
        super();
        resourcesMappers= resourcePatternResolver.getResources(XML_RESOURCE_PATTERN);
        initFiles(resourcesMappers, mapXml);
    }

    public void initFiles(Resource[] resources, ConcurrentHashMap map) throws Exception{
        if (resources == null) {return ;}

        for (Resource resource : resources) {
            String key = resource.getURI().toString();
            String value = getFileLastModified(resource);
            map.put(key, value);
        }
    }

    public void reloadXML(Resource resource) throws Exception {
        SqlSessionFactory factory = AppContext.getBean(SqlSessionFactory.class);
        Configuration configuration = factory.getConfiguration();
        InputStream inputStream = resource.getInputStream();
        String resourceString = resource.toString();
        try {
            // 清理原有资源，更新为自己的StrictMap方便，增量重新加载
            String[] mapFieldNames = new String[]{
                    "mappedStatements", "caches",
                    "resultMaps", "parameterMaps",
                    "keyGenerators", "sqlFragments"
            };
            for (String fieldName : mapFieldNames){
                Field field = configuration.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Map map = ((Map)field.get(configuration));
                if (!(map instanceof StrictMap)){
                    Map newMap = new StrictMap(StringUtils.capitalize(fieldName) + "collection");
                    for (Object key : map.keySet()){
                        try {
                            newMap.put(key, map.get(key));
                        }catch(IllegalArgumentException ex){
                            newMap.put(key, ex.getMessage());
                        }
                    }
                    field.set(configuration, newMap);
                }
            }

            //清理已加载的资源标识，方便让它重新加载。
            Field loadedResourcesField = configuration.getClass().getDeclaredField("loadedResources");
            loadedResourcesField.setAccessible(true);
            Set loadedResourcesSet = ((Set)loadedResourcesField.get(configuration));
            loadedResourcesSet.remove(resourceString);

            //重新编译加载资源文件。
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resourceString,
                    configuration.getSqlFragments()
            );
            xmlMapperBuilder.parse();
            logger.debug("reload success  {}", resource.getURI().toString());
        } catch (Exception e) {
            logger.warn("Failed to parse mapping resource: '" + resourceString + "'", e);
            //throw new NestedIOException("Failed to parse mapping resource: '" + resourceString + "'", e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    public void monitorChanged() throws Exception {
        //super.monitorChanged();
        //long start = System.currentTimeMillis();
        //logger.debug("扫描开始 {} 耗时 {}", resources.length, System.currentTimeMillis() - start);
        for (Resource resource : resourcesMappers) {
            String key = resource.getURI().toString();
            String value = getFileLastModified(resource);
            if (!value.equals(mapXml.get(key))) {
                mapXml.put(key, value);
                reloadXML(resource);
            }
        }
        //logger.debug("扫描结束 {} 耗时 {}", resourcesMappers.length, System.currentTimeMillis() - start);
    }

    public String getFileLastModified(Resource resource) {
        try {
            return new StringBuilder()
                    .append(resource.contentLength())
                    .append("-")
                    .append(resource.lastModified())
                    .toString();
        } catch (IOException e) {
        }
        return "";
    }

}