package com.topfox.common;

import com.topfox.data.TableInfo;

public class Component extends ConfigHandler {
    private TableInfo tableInfo;
    private Class entityClazz;

    public Class getEntityClazz(){
        return entityClazz;
    }

    public Object setEntityClazz(Class clazz){
        this.entityClazz = clazz;
        return this;
    }

    public TableInfo getTableInfo() {
        if (tableInfo == null){
            tableInfo = TableInfo.get(getEntityClazz());
        }

        return tableInfo;
    }

    public Object setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
        this.entityClazz = tableInfo.clazzEntity;
        return this;
    }
}
