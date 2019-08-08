package com.topfox.common;

public class ConfigHandler {
    private static SysConfigRead sysConfigRead;  //单实例读取值 全局一个实例
    private SysConfigRead sysConfig;             //每个表独立一个配置文件
    public SysConfigRead getSysConfig(){
        if (sysConfig != null){
            return sysConfig;
        }
        if (sysConfigRead == null) {
            sysConfigRead = AppContext.getSysConfig();
        }

        return sysConfigRead;
    }

    public Object setSysConfig(SysConfigRead sysConfig){
        this.sysConfig = sysConfig;
        return this;
    }
}
