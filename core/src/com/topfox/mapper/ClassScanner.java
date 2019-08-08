//package com.topfox.mapper;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.core.io.support.ResourcePatternResolver;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class ClassScanner {
//    protected Logger logger = LoggerFactory.getLogger(getClass());
//
//    /**
//     * 以文件路径为key, 以文件的修改时间为 value
//     */
//    private static final ConcurrentHashMap<String, String> mapClass = new ConcurrentHashMap();
//    private static final ConcurrentHashMap<String, Class<?>> mapHotClasses = new ConcurrentHashMap();
//
//
//    private static final String CLASS_RESOURCE_PATTERN = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "com/user/**/*.class";
//    protected final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
//    private Resource[] resourcesClass;
//
//    public ClassScanner() throws Exception {
//        resourcesClass = resourcePatternResolver.getResources(CLASS_RESOURCE_PATTERN);
//        initFiles(resourcesClass, mapClass);
//    }
//
//    public static Class<?> getHotClasses(String name){
//        return mapHotClasses.get(name);
//    }
//
//    public void initFiles(Resource[] resources, ConcurrentHashMap map) throws Exception{
//        if (resources == null) {return ;}
//
//        for (Resource resource : resources) {
//            String key = resource.getURI().toString();
//            String value = getFileLastModified(resource);
//            map.put(key, value);
//        }
//    }
//
//    /**
//     * 重新加载FILE
//     * 在这里，将这个CLASS文件重新加载到内存中，从而替换掉之前的CLASS文件
//     * 即将之前那个类重新new一下
//     */
//    private void reloadClass(File newFile) {
//        HotClassLoader hotClassLoader = new HotClassLoader(newFile);
//        try {
//            String namespace = newFile.getPath()
//                    .substring(newFile.getPath().indexOf("/classes/")+9)
//                    .replaceAll("/",".");
//            namespace = namespace.substring(0, namespace.length() - 6);
//            Class<?> clazzHot = hotClassLoader.findClass(namespace);
//            Thread.currentThread().setContextClassLoader(hotClassLoader);
//
//            mapHotClasses.put(clazzHot.getName(), clazzHot);
//
//            logger.debug("reload success  {}", clazzHot.getName());
//
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//        }
//    }
//
//    public void monitorChanged() throws Exception {
//        for (Resource resource : resourcesClass) {
//            String filePath = resource.getURI().toString();
//            String lastModified = getFileLastModified(resource);
//            if (!lastModified.equals(mapClass.get(filePath))) {
//                mapClass.put(filePath, lastModified);
//                reloadClass(new File(filePath.replace("file:","")));
//            }
//        }
//    }
//
//    public String getFileLastModified(Resource resource) {
//        try {
//            return new StringBuilder()
//                    .append(resource.contentLength())
//                    .append("-")
//                    .append(resource.lastModified())
//                    .toString();
//        } catch (IOException e) {
//        }
//        return "";
//    }
//}