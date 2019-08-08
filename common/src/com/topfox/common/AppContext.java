package com.topfox.common;

import com.topfox.misc.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AppContext implements ApplicationContextAware {

	/**
	 * 上下文对象实例
	 */
	public static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext value) throws BeansException {
		applicationContext = value;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public static Object getBean(String name){
		return getApplicationContext().getBean(name);
	}

	public static <T> T getBean(Class<T> clazz){
		return getApplicationContext().getBean(clazz);
	}

	public static <T> T getBean(String name,Class<T> clazz){
		return getApplicationContext().getBean(name, clazz);
	}

	private static IRestSessionHandler restSessionHandler;
	public static <T extends IRestSessionHandler> T getRestSessionHandler() {
		// 双重检查锁
		if (restSessionHandler == null ) {
			synchronized(AppContext.class){
				if (restSessionHandler == null && getApplicationContext() != null) {
					restSessionHandler = getApplicationContext().getBean(IRestSessionHandler.class);
				}
			}
		}
		return (T)restSessionHandler;
	}

	public static <T extends AbstractRestSession> T getRestSession() {
		if (getRestSessionHandler()==null){
			return null;
		}
		return (T) getRestSessionHandler().get();
	}

	public static <T extends AbstractRestSession> T getRestSession(Class<T> clazz) {
		if (getRestSessionHandler()==null){
			return null;
		}
		return (T) getRestSessionHandler().get();
	}

	public static SysConfigRead getSysConfig() {
		return getRestSessionHandler()==null?null:getRestSessionHandler().getSysConfig();
	}

	public static org.springframework.core.env.Environment environment(){
		return getRestSessionHandler()==null?null:getRestSessionHandler().environment();
	}


	static Logger logger = LoggerFactory.getLogger("");

	public static void initStart(long start, ApplicationContext value) {
		applicationContext = value;

		long end = System.currentTimeMillis();

		org.springframework.core.env.Environment environment = environment();

		logger.info("------------------------------------------------------------------------");
		logger.info("启动完毕 耗时:" +(end - start)/1000+"秒");
		logger.info("server.port               : {}{}", environment.getProperty("server.port")," (服务端口)");
		logger.info("spring.profiles.active    : {}", environment.getProperty("spring.profiles.active"));
		logger.info("spring.datasource.database: {}{}", environment.getProperty("spring.datasource.database")," (连接的数据库名)");
		logger.info("spring.datasource.driver  : {}", environment.getProperty("spring.datasource.driver-class-name"));
		String url = environment.getProperty("spring.datasource.url");
		int pos = url.indexOf("?");
		logger.info("spring.datasource.url     : {}",url.substring(0,pos)+" ...");
		String elkRedisKey = environment.getProperty("elk.redis.key");
		if (Misc.isNotNull(elkRedisKey)) {
			logger.info("elk.redis.key             : {}", elkRedisKey);
		}

		logger.info("------------------------------------------------------------------------");
	}
}