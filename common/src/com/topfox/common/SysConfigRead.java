package com.topfox.common;

import com.topfox.data.SqlUnderscore;

/**
 * 实现类: com.topfox.spring.SysConfigDefault
 */
public interface SysConfigRead {

    String getAppName();

    int getPageSize();

    int getMaxPageSize();

    int getUpdateMode();

    boolean isRedisSerializerJson();

//    @Deprecated
//    boolean isOpenRedis();

    boolean isRedisCache();

    boolean isThreadCache();

    boolean isRedisLog();

    boolean isUpdateNotResultError();

    boolean isCommitDataKeysIsUnderscore();

    boolean isSelectByBeforeUpdate();

    SqlUnderscore getSqlCamelToUnderscore();

    String getLogStart();

    String getLogEnd();

    String getLogPrefix();

    String getString(String key);

}
