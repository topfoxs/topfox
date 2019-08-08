package com.topfox.service;

import com.alibaba.fastjson.JSONObject;
import com.topfox.common.AbstractRestSession;
import com.topfox.common.DataDTO;
import com.topfox.common.SysConfigRead;
import com.topfox.misc.BeanUtil;
import com.topfox.sql.Condition;
import com.topfox.sql.EntitySelect;
import com.topfox.util.AbstractRestSessionHandler;
import com.topfox.util.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import java.util.List;


public class MultiService<SESSION extends AbstractRestSession> implements ISuperService {

    private static ThreadLocal<JSONObject> threadLocalAttributes = new ThreadLocal();
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired protected
    Environment environment;

    @Autowired
    @Qualifier("sysConfigDefault")
    protected SysConfigRead sysConfigRead;//单实例读取值 全局一个实例

    protected SysConfig sysConfig;        //每个service独用的实例

    @Autowired
    protected AbstractRestSessionHandler<SESSION> abstractRestSessionHandler;


    @Override
    public void init() {
    }


    public final SESSION restSession() {
        return abstractRestSessionHandler.get();
    }

    @Override
    public void beforeInit(List<DataDTO> listUpdate) {
        threadLocalAttributes.set(new JSONObject());
        if (sysConfig == null) {
            sysConfig = (SysConfig) BeanUtil.cloneBean(sysConfigRead); //为每个Service
        }

        init();//开发者自定义的初始化逻辑

        if (!sysConfig.isRedisCache()) {
            logger.debug("{}重要参数 redisCache 已经被设置为 false", sysConfigRead.getLogPrefix());
        }

        SESSION session = abstractRestSessionHandler.get();//获得当前线程的 restSession
        if (session == null) {
            session = abstractRestSessionHandler.create();//创建 restSession
            abstractRestSessionHandler.initRestSession(session, null);
        }
    }

    /**
     * 线程安全级别的自定属性
     *
     * @return
     */
    public final JSONObject attributes() {
        return threadLocalAttributes.get();
    }

    public EntitySelect select(){
        return select(null, true);
    }
    public EntitySelect select(String fields){
        return select(fields,false);
    }
    public EntitySelect select(String fields,Boolean isAppendAllFields){
        return EntitySelect.create().select(fields,isAppendAllFields);
    }

    public Condition where(){
        return Condition.create();
    }
}