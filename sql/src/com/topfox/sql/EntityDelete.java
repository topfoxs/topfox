package com.topfox.sql;

import com.topfox.common.DataDTO;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;

public class EntityDelete extends IEntitySql {
    private StringBuilder saveSql;

    private EntityDelete(){
        saveSql = new StringBuilder();
    }

    public static EntityDelete create(){
        return new EntityDelete();
    }
    public static EntityDelete create(Class<?> clazz){
        EntityDelete current = create();
        current.setEntityClazz(clazz);
        return current;
    }
    public static EntityDelete create(TableInfo tableInfo){
        EntityDelete current = create();
        current.setTableInfo(tableInfo);
        return current;
    }

    @Override
    public String getSql() {
        saveSql.setLength(0);
        saveSql.append("DELETE FROM ").append(getTableInfo().getTableName());
        saveSql.append(where().getWhereSql());
        String sql = saveSql.toString();
        //init();
        return sql;
    }

    public EntityDelete deleteBatch() {
        clean();
        return this;
    }


    public String getDeleteByIdSql(DataDTO dto){
        clean();
        saveSql.append("DELETE FROM ").append(getTableInfo().getTableName());

        //单Id,多Id字段 的条件处理
        getTableInfo().getFieldsByIds().forEach((key, field)->{
            where().eq(key, BeanUtil.getValue(getTableInfo(), dto, key));
        });

        //版本号 字段值 为null 就不生成 version的条件
        String versionFieldName = getTableInfo().getVersionFieldName();
        if (versionFieldName != null && dto.dataVersion() != null ){
            where().and(false).eq(versionFieldName, dto.dataVersion());
        }
        saveSql.append(where().getWhereSql());
        String sql=saveSql.toString();
        clean();

        return sql;
    }


    public String getDeleteByIdSql(Integer versionValue, Object... idValues){
        clean();
        //仅仅支持一个主键字段的表
        saveSql.append("DELETE FROM ").append(getTableInfo().getTableName());
        where().eq(getTableInfo().getIdFieldsBySql(), idValues);

        //版本号 字段值 为null 就不生成 version的条件
        String versionFieldName=getTableInfo().getVersionFieldName();
        if (versionFieldName!=null && versionValue!=null ){
            where().and(false).eq(versionFieldName, versionValue);
        }
        saveSql.append(where().getWhereSql());
        String sql=saveSql.toString();
        clean();
        return sql;
    }

    @Override
    protected void clean(){
        //清空
        saveSql.setLength(0);

        //清空上次的查询条件
        where().clean();
    }

}
