package com.topfox.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topfox.common.SysConfigRead;
import com.topfox.util.CustomRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {
	@Autowired
	@Qualifier("sysConfigDefault")
	protected SysConfigRead sysConfigRead;//单实例读取值 全局一个实例
	protected Logger logger = LoggerFactory.getLogger(getClass());


	Jackson2JsonRedisSerializer jackson2JsonRedisSerializer2=null;
	private Jackson2JsonRedisSerializer getJacksonSerializer(){
		if (jackson2JsonRedisSerializer2==null) {
			jackson2JsonRedisSerializer2 = new Jackson2JsonRedisSerializer(Object.class);
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer2.setObjectMapper(objectMapper);
		return jackson2JsonRedisSerializer2;
	}

	/**
	 * 开启事务的
=	 * @return
	 */
	@Bean("sysRedisTemplateDTO")//开启事务的
	public CustomRedisTemplate<String, Object> sysRedisTemplateDTO() {
		CustomRedisTemplate<String, Object> sysRedisTemplateDTO = createRedisTemplate();
		if (sysRedisTemplateDTO == null){
			return null;
		}

		sysRedisTemplateDTO.setEnableTransactionSupport(true);//打开事务支持
		sysRedisTemplateDTO.setHashKeySerializer(RedisSerializer.string());//key值始终用纯字符串序列化
		sysRedisTemplateDTO.setHashValueSerializer(sysConfigRead.isRedisSerializerJson()?getJacksonSerializer():new JdkSerializationRedisSerializer());
		sysRedisTemplateDTO.setKeySerializer(RedisSerializer.string());//key值始终用纯字符串序列化
		sysRedisTemplateDTO.setValueSerializer    (sysConfigRead.isRedisSerializerJson()?getJacksonSerializer():new JdkSerializationRedisSerializer());
		sysRedisTemplateDTO.afterPropertiesSet();//初始化操作）加载配置后执行

		return sysRedisTemplateDTO;//sysRedisTemplateDTO
	}

	/**
	 * 注意,这个是没有事务的
	 */
	@Bean("sysStringRedisTemplate")
	public CustomRedisTemplate sysStringRedisTemplate() {
		CustomRedisTemplate<String, Object> sysStringRedisTemplate = createRedisTemplate();
		if (sysStringRedisTemplate == null){
			return null;
		}

		sysStringRedisTemplate.setEnableTransactionSupport(false);//关闭事务支持
		sysStringRedisTemplate.setKeySerializer(RedisSerializer.string());
		sysStringRedisTemplate.setValueSerializer(RedisSerializer.string());
		sysStringRedisTemplate.setHashKeySerializer(RedisSerializer.string());
		sysStringRedisTemplate.setHashValueSerializer(RedisSerializer.string());
		sysStringRedisTemplate.afterPropertiesSet();//初始化操作）加载配置后执行

		return sysStringRedisTemplate; //stringRedisTemplate
	}

	@Autowired(required = false) LettuceConnectionFactory redisConnectionFactory;
	private CustomRedisTemplate createRedisTemplate() {
		if (redisConnectionFactory == null ) {
			return null;
		}

		CustomRedisTemplate<String, Object> redisTemplate = new CustomRedisTemplate();
		redisTemplate.setConnectionFactory(redisConnectionFactory);

		return redisTemplate;
	}
}
