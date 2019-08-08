package com.topfox.sql;

import com.topfox.common.DataDTO;
import com.topfox.common.Incremental;
import com.topfox.data.DataHelper;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;

import java.util.Map;

public class EntityUpdate extends IEntitySql{
    private StringBuilder saveSql;
    private StringBuilder updateSetValues;

    private EntityUpdate(){
        saveSql = new StringBuilder();
        updateSetValues = new StringBuilder();
    }

    public static EntityUpdate create(){
        return new EntityUpdate();
    }
    public static EntityUpdate create(Class<?> clazz){
        EntityUpdate current = create();
        current.setEntityClazz(clazz);
        return current;
    }
    public static EntityUpdate create(TableInfo tableInfo){
        EntityUpdate current = create();
        current.setTableInfo(tableInfo);
        return current;
    }

    /**
     * 返回条件对象
     * @return
     */
    @Override
    public Condition<EntityUpdate> where(){
        return  super.where();
    }

    @Override
    public String getSql(){
        saveSql.setLength(0);
        saveSql.append("UPDATE ")
                .append(getTableInfo().getTableName())
                .append("\nSET ")
                .append(updateSetValues);
        saveSql.append(where().getWhereSql());

        String sql=saveSql.toString();
//        init();//对象数据归位,初始化为空
        return sql;
    }

    public EntityUpdate updateBatch(DataDTO bean) {
        return updateBatch(bean,false);
    }
    public EntityUpdate updateBatch(DataDTO bean, boolean isNullValue2Sql) {
        clean();
        Map<String,Object> mapData = BeanUtil.bean2Map(bean,isNullValue2Sql);

        //指定强制更新为 null的字段处理
        if (bean.nullFields() != null) {
            Misc.idsArray2Set(bean.nullFields()).forEach(key -> mapData.put(key, null));
        }

        buildUpdateSetValues(bean.mapSave(mapData));
        return this;
    }

    public String getUpdateByIdSql(DataDTO bean) {
        return getUpdateByIdSqlBuild(bean.mapSave(BeanUtil.bean2Map(bean,false)));
    }

    public String getUpdateByIdSql(DataDTO bean, int updateMode) {
        return getUpdateByIdSqlBuild(bean.mapSave(BeanUtil.getChangeRowData(bean, updateMode)));
    }

    public String getUpdateByIdSql(DataDTO bean, boolean isNullValue2SetSql) {
        return getUpdateByIdSqlBuild(bean.mapSave(BeanUtil.bean2Map(bean,isNullValue2SetSql)));
    }
    private StringBuilder buildUpdateSetValues(Map<String,Object> mapValues){
        updateSetValues.setLength(0);

//        //过滤出修改的字段,顺序按照fields.  mapValues的keyset是无序的,以field为准
//        Object[] fieldsArray =fields.keySet().stream().filter((fieldName) ->
//                (mapValues.containsKey(fieldName) && !fieldName.equals(getTableInfo().getIdFieldName()))//过滤条件
//        ).toArray();

        //处理版本号字段, 只要存在, 则递增 //old if (mapValues.containsKey(getTableInfo().getVersionFieldName())){
        if (getTableInfo().getVersionFieldName() !=null ){
            String versionColumn=getTableInfo().getColumn(getTableInfo().getVersionFieldName());
            updateSetValues.append(versionColumn).append("=").append(versionColumn).append("+1,");
        }

        Map<String, Field> fields = getTableInfo().getFields();
        mapValues.forEach((fieldName, value)->{
            if (getTableInfo().getFieldsByIds().containsKey(fieldName) ||
                    fieldName.equals(getTableInfo().getVersionFieldName())) {
                return;
            }
            Field field = fields.get(fieldName.trim());
            if (field == null ) {
                return;
            }

            if (field.getIncremental()== Incremental.ADDITION || field.getIncremental()== Incremental.SUBTRACT){
                if ( DataHelper.parseLong(value) == 0L ) {return;}
                // 解决更新SQL +-  addition +  / subtract -
                updateSetValues.append(
                        field.getDbName())//getTableInfo().getColumn(fieldName)
                        .append("=")
                        .append(field.getDbName());
                updateSetValues.append(field.getIncremental().getCode()); // + - 符号
                BeanUtil.getSqlValue(fields.get(fieldName), fieldName, value, updateSetValues);
                updateSetValues.append(",");
            }else {
                updateSetValues.append(field.getDbName()).append("=");
                BeanUtil.getSqlValue(fields.get(fieldName), fieldName, value, updateSetValues);
                updateSetValues.append(",");
            }
        });

        if (updateSetValues.length() == 0){
            //一个更新的字段都没有时, 支持 多Id字段
            getTableInfo().getFieldsByIds().forEach((key, field)->{
                updateSetValues.append(key)
                        .append("=")
                        .append(key).append(",");
            });
        }

        //去掉最后的逗号
        updateSetValues.setLength(updateSetValues.length()-1);
        return updateSetValues;
    }

    public String getUpdateByIdSqlBuild(Map<String,Object> mapValues){
        saveSql.setLength(0);
        saveSql.append("UPDATE ")
                .append(getTableInfo().getTableName())
                .append("\n\rSET ")
                .append(buildUpdateSetValues(mapValues));
        where().clean();

        //支持 多Id字段
        getTableInfo().getFieldsByIds().forEach((key, field)->{
            where().eq(key, mapValues.get(key));
        });


        //版本号 字段值 为null 就不生成 version的条件
        String versionFieldName=getTableInfo().getVersionFieldName();
        Object versionValue=mapValues.get(getTableInfo().getVersionFieldName());
        if (versionFieldName!=null && versionValue!=null ){
            where().and(false).eq(getTableInfo().getColumn(versionFieldName), versionValue);
        }
        saveSql.append(where().getWhereSql());
        String sql=saveSql.toString();
        clean();
        return sql;
    }

    @Override
    protected void clean(){
        saveSql.setLength(0);
        updateSetValues.setLength(0);
        where().clean();//清空上次的查询条件
    }
}
