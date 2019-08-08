package com.topfox.util;

import com.topfox.common.SysConfigRead;
import com.topfox.data.SqlUnderscore;

/**
 * 实现类
 * @see  com.topfox.util.SysConfigDefault
 */
public interface SysConfig extends SysConfigRead {

    /**
     * 对应配置文件中的  top.page-size
     */
    void setPageSize(Integer value);

    /**
     * 对应配置文件中的  top.page-max-size
     */
    void setMaxPageSize(Integer value);

    /**
     * 对应配置文件中的  top.service.update-mode
     */
    void setUpdateMode(Integer value);

    /**
     * 对应配置文件中的  top.redis.serializer-json
     */
    void setRedisSerializerJson(Boolean value);

//    /**
//     * 对应配置文件中的  top.service.open-redis
//     */
//    void setOpenRedis(Boolean value);

    /**
     * 一级缓存开关
     * 对应配置文件中的  top.service.thread-cache
     */
    void setThreadCache(Boolean value);

    /**
     * 二级缓存开关
     * 对应配置文件中的  top.service.redis-cache
     */
    void setRedisCache(Boolean value);

    /**
     * 对应配置文件中的  top.redis.log
     */
    void setRedisLog(Boolean value);

    /**
     * update-mode = 1 时本参数生效
     * 对应配置文件中的  top.service.select-by-before-update   更新之前是否先查询, 默认false, 不查询
     */
    void setSelectByBeforeUpdate(Boolean value);

    /**
     * 对应配置文件中的  top.service.update-not-result-error
     */
    void setUpdateNotResultError(Boolean value);

    /**
     * 对应配置文件中的  top.service.sql-camel-to-underscore
     */
    void setSqlCamelToUnderscore(SqlUnderscore value);
}
