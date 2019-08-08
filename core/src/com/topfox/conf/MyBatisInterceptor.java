package com.topfox.conf;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 自定义mybatis拦截器,格式化SQL输出,
 *
 * @author zengsong
 * @version 1.0
 * @description 只对查询和更新语句做了格式化，其它语句需要手动添加
 * @date 2019/5/30 10:17
 **/
@Intercepts(
	{
		@org.apache.ibatis.plugin.Signature(
				type=org.apache.ibatis.executor.Executor.class,
				method="update",
				args={MappedStatement.class, Object.class}
			),
		@org.apache.ibatis.plugin.Signature(
				type=org.apache.ibatis.executor.Executor.class, method="query",
				args={	MappedStatement.class, Object.class,
						org.apache.ibatis.session.RowBounds.class,
						org.apache.ibatis.session.ResultHandler.class}
						)
	}
)
@Component
@Profile({"dev", "test"})
public class MyBatisInterceptor
		implements Interceptor
{
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (logger.isDebugEnabled()) {
			try {
				MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
				Object parameter = null;
				if (invocation.getArgs().length > 1) {
					parameter = invocation.getArgs()[1];
				}
				String sqlId = mappedStatement.getId();
				BoundSql boundSql = mappedStatement.getBoundSql(parameter);
				Configuration configuration = mappedStatement.getConfiguration();
				String sql = getSql(configuration, boundSql, sqlId);
				logger.debug(sql);
			} catch (Exception localException) {

			}
		}
		return invocation.proceed();
	}

	public static String getSql(Configuration configuration, BoundSql boundSql, String sqlId)
	{
		String sql = showSql(configuration, boundSql);
		StringBuilder str = new StringBuilder(100);
		str.append(sqlId);
		str.append("\n");
		str.append(sql);
		return str.toString();
	}

	private static String getParameterValue(Object obj) {
		String value = null;
		if ((obj instanceof String))
		{
			value = "'" + obj.toString() + "'";
		}
		else if ((obj instanceof Date))
		{
			DateFormat formatter = DateFormat.getDateTimeInstance(2, 2, Locale.CHINA);
			value = "'" + formatter.format(new Date()) + "'";
		}
		else if (obj != null)
		{
			value = obj.toString();
		}
		else
		{
			value = "";
		}
		return value;
	}

	public static String showSql(Configuration configuration, BoundSql boundSql){
		Object parameterObject = boundSql.getParameterObject();
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

		String sql = boundSql.getSql().replaceAll("[\n\t\r]+", "\n");
		sql = sql.replaceAll(" +", " ");//去除连续的空格
		sql = sql.replaceAll("\n ","\n");
		sql = sql.replaceAll("\n+","\n  ");
		sql = sql.replaceAll("\n(?i)AND ","\n  AND ");
		sql = sql.replaceAll("\n  (?i)FROM ","\nFROM "); //(?i) 是不区分大小写的
		sql = sql.replaceAll("\n  (?i)WHERE ","\nWHERE ");
		sql = sql.replaceAll("\n  (?i)GROUP BY ","\nGROUP BY ");
		sql = sql.replaceAll("\n  (?i)ORDER BY ","\nORDER BY  ");
		sql = sql.replaceAll("\n  (?i)LIMIT ","\nLIMIT ");


//		// 去掉	多个换行
//		Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
//		Matcher m = p.matcher(sql);
		//sql = m.replaceAll("\n");

//		if (sql.indexOf("\n FROM ")<0)
//			sql = sql.replace(" FROM ","\nFROM ");
//		if (sql.indexOf("\n from ")<0)
//			sql = sql.replace(" from ","\nfrom ");
//		if (sql.indexOf("\n WHERE ")<0)
//			sql = sql.replace(" WHERE ","\nWHERE ");
//		if (sql.indexOf("\n where ")<0)
//			sql = sql.replace(" where ","\nwhere ");
//		if (sql.indexOf("\n ORDER BY ")<0)
//			sql = sql.replace(" ORDER BY ","\nORDER BY ");
//		if (sql.indexOf("\n order by ")<0)
//			sql = sql.replace(" order by ","\norder by ");

		MetaObject metaObject;
		if (!CollectionUtils.isEmpty(parameterMappings) && parameterObject != null ){
			TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
			if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass()))
			{
				sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
			}
			else
			{
				metaObject = configuration.newMetaObject(parameterObject);
				for (ParameterMapping parameterMapping : parameterMappings)
				{
					String propertyName = parameterMapping.getProperty();
					if (metaObject.hasGetter(propertyName))
					{
						Object obj = metaObject.getValue(propertyName);
						sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
					}
					else if (boundSql.hasAdditionalParameter(propertyName))
					{
						Object obj = boundSql.getAdditionalParameter(propertyName);
						sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
					}
					else
					{
						sql = sql.replaceFirst("\\?", "缺失");
					}
				}
			}
		}
		return sql;
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {

	}
}