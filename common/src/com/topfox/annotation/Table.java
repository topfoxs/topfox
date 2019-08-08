package com.topfox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    /**
     * 数据库的表名
     * @return
     */
    String name() default "";

    /**
     * 自定 中文表名, 抛出异常用, sevice中通过 tableInfo().getTableCnName() 获取
     * @return
     */
    String cnName() default "";


    /**
     * 自定 缓存到redis中的key
     * @return
     */
    String redisKey() default "";

}
