package com.topfox.mapper;

import com.topfox.misc.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 开发用 热加载 *Mapper.xml
 */
@Component
public class MapperLoader implements InitializingBean, ApplicationContextAware {

    private volatile ConfigurableApplicationContext context = null;
    private volatile MapperScanner mapperScanner = null;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String className = "org.springframework.boot.devtools.RemoteSpringApplication";
        Class devToolsApp = null;
        try {
            devToolsApp = Class.forName(className);
        } catch (ClassNotFoundException e) {
        }

        /**
         * 没有启用devtools和jrebel插件时, 解决mapper.xml文件修改 热加载立即生效;
         * 特点是 速度快
         */
        if (logger.isDebugEnabled() && devToolsApp == null && Misc.isNull(System.getProperty("rebel.base"))){
            try {
                mapperScanner = new MapperScanner();
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                    try {
                        //监控修改
                        mapperScanner.monitorChanged();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    }
                }, 10 * 1000, 3 * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
