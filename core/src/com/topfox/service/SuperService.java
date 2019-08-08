package com.topfox.service;

import com.alibaba.fastjson.JSONObject;
import com.topfox.common.*;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.ReflectUtils;
import com.topfox.util.FillDataHandler;
import com.topfox.util.AbstractRestSessionHandler;
import com.topfox.util.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import java.util.List;

public class SuperService<DTO extends DataDTO> implements ISuperService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private Class<DTO> clazzDTO;
    private TableInfo tableInfo = null; //得到表结构
    private static ThreadLocal<JSONObject> threadLocalAttributes = new ThreadLocal();

    @Autowired @Qualifier("sysConfigDefault")
    protected SysConfigRead sysConfigRead;       //单实例读取值 全局一个实例
    protected SysConfig sysConfig;               //每个service独用的实例

    @Autowired protected Environment environment;
    @Autowired protected AbstractRestSessionHandler restSessionHandler;

    @Autowired(required = false)
    protected FillDataHandler fillDataHandler; //填充对象

    @Override
    public void init(){}

    /**
     * new一个DTO实体
     * @return
     */
    public final DTO newInstanceDTO(){
        //beforeInit();
        return BeanUtil.newInstance(clazzDTO());
    }

    /**
     * 热加载需要
     * @return
     */
    protected final Class<DTO> clazzDTO(){
        //保证初始化 只执行一次
        if (clazzDTO == null) {
            clazzDTO = ReflectUtils.getClassGenricType(getClass(), 1); //获得当前DTO的Class
        }
        return clazzDTO;
    }

    public final TableInfo tableInfo(){
        if (tableInfo == null){
            tableInfo = TableInfo.get(clazzDTO());
        }
        return tableInfo;
    }

    /**
     * 初始化之前, 类库要做的事情
     * @param listUpdate 更新时,  对DTO的处理用
     */
    @Override
    public void beforeInit(List<DataDTO> listUpdate){
        threadLocalAttributes.set(new JSONObject());
        if (sysConfig == null) {
            sysConfig = (SysConfig)BeanUtil.cloneBean(sysConfigRead); //为每个Service
        }

        init();//开发者自定义的初始化逻辑
        if (tableInfo() != null) { //不指定DTO tableInfo就是 null
            tableInfo().setSysConfig(sysConfig);
        }
        if (!sysConfig.isRedisCache()){
            logger.debug("{}重要参数openReids已经被 设置为 false", sysConfigRead.getLogPrefix());
        }

        //###################################################################################
        AbstractRestSession abstractRestSession = restSessionHandler.get();//获得当前线程的 restSession
        if (abstractRestSession == null) {
            abstractRestSession = restSessionHandler.create();//创建 restSession
            restSessionHandler.initRestSession(abstractRestSession, null);
        }
        //###################################################################################

    }

    /**
     * 线程安全级别的自定属性
     * @return
     */
    public final JSONObject attributes() {
        return threadLocalAttributes.get();
    }

    /**
     * 初始化之后, 类库要做的事情
     */
    public void afterInit(){
    }
}