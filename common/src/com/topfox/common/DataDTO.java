package com.topfox.common;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.topfox.annotation.TableField;
import com.topfox.annotation.Ignore;
import com.topfox.data.DbState;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.JsonUtil;
import com.topfox.misc.Misc;
import com.topfox.data.DataHelper;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataDTO implements IBean, Serializable {
    private static final long serialVersionUID = 1L;

    @TableField(exist = false) @JsonIgnore @Ignore
    transient private Object originDTO;

    @TableField(exist = false) @JsonIgnore @Ignore
    transient private Object megerDTO;

    @TableField(exist = false) @JsonIgnore @Ignore
    transient private JSONObject mapModify;

    @TableField(exist = false) @JsonIgnore @Ignore
    transient private Map<String, Object> mapSave;

    @TableField(exist = false) @JsonIgnore @Ignore
    transient private String[] nullFields;
    public DataDTO addNullFields(String... nullfields) {
        this.nullFields = nullfields;
        return this;
    }
    public  String[] nullFields() {
        return this.nullFields;
    }

    /**
     * set 一个原始没有修改的DTO
     * @param originDTO
     */
    public DataDTO addOrigin(Object originDTO) {
        this.originDTO = originDTO;
        //isChangeUpdateSql=true;
        return this;
    }

    /**
     * 将DTO的当前值Copy到原始值. top.updateMode >1时, 后台直接查询出来的DTO做更新,需要根据变化值生成更新SQL时调用本方法
     */

    @TableField(exist = false) @JsonIgnore @Ignore
    transient public Boolean isChangeUpdateSql;
    public DataDTO addOriginFromCurrent() {
        this.originDTO = BeanUtil.cloneBean(this);
        isChangeUpdateSql=true;
        return this;
    }

    /**
     * 获得更新前的原始数据
     */
    public <T> T origin(){
        return (T)this.originDTO;
    }


//    public <T> T origin(Class<T> clazz){
//        return (T)this.originDTO;
//    }

    public DataDTO addModifyMap(JSONObject mapModify) {
        this.mapModify = mapModify;
        return this;
    }

    public JSONObject mapModify() {
        return this.mapModify;
    }

//    public <T> T dtoModify(){
//        return (T)dtoModify(getClass());
//    }

//    /**
//     * 调用本方法将 创建一个新的DTO
//     * 当 SysConfig.getUpdateMode==1, 应该是 自己, 本逻辑需要开发者自己实现
//     *
//     * @param clazz
//     * @param <T>
//     * @return
//     */
//    public <T> T dtoModify(Class<T> clazz){
//        T modifyDTO = BeanUtil.map2Bean(mapModify, clazz);
//        return modifyDTO;
//    }

    /**
     * 更新时,  获得 原始数据+修改数据的合并数据
     */
    public <T> T merge(){
        if (DbState.INSERT.equals(dataState()) || DbState.DELETE.equals(dataState())){
            throw CommonException.newInstance(ResponseCode.NULLException).text("新增或删除时,不存在合并的DTO");
        }
        if (origin() == null){
            throw CommonException.newInstance(ResponseCode.NULLException).text("DTO的原始数据还没有设值, 不能获得合并的DTO");
        }

        //参数配置为 合并DTO, 就是 当前自己
        if (1==2){
            return (T)this;
        }

        try {
            if (megerDTO == null) {
                megerDTO = getClass().newInstance();
            }
            //拷贝原始数据
            BeanUtil.copyBean(origin(), megerDTO);
            //拷贝当前数据
            BeanUtil.copyBean(this, megerDTO);
            return (T) megerDTO;
        } catch (CommonException ce){
            throw ce;
        } catch (Exception e) {
            //e.printStackTrace();
            throw CommonException.newInstance(ResponseCode.ERROR).text("合并DTO时 new DTO出错");
        }
    }

    /**
     * 获得实体版本号
     * @return
     */
    public Integer dataVersion(){
        Field field = tableInfo().getField(tableInfo().getVersionFieldName());
        if (field == null) {
            return null;
        }
        Object value = BeanUtil.getValue(this, field);
        if (value == null) {
            return null;
        }
        return DataHelper.parseInt(value);
    }
    public DataDTO dataVersion(Integer value){
        Field field = tableInfo().getField(tableInfo().getVersionFieldName());
        if (field != null) {
            BeanUtil.setValue(tableInfo(), this, field.getName(), value);
        }
        return this;
    }

    /**
     * 获得实体行号(没有Id时增行管用)
     * @return
     */
    public String dataRowId(){
        String fieldName = tableInfo().getRowNoFieldName();
        if (Misc.isNull(fieldName)) {
            return null;
        }
        return DataHelper.parseString(BeanUtil.getValue(tableInfo(),this, fieldName));
    }
    public DataDTO dataRowId(String value){
        String fieldName = tableInfo().getRowNoFieldName();
        if (Misc.isNotNull(fieldName)) {
            BeanUtil.setValue(tableInfo(), this, fieldName, value);
        }
        return this;
    }

    /**
     *数据状态字段  数据库不存在的字段, 用于描述   transient修饰符 表示 改字段不会被序列化
     * @see DbState ;
     */
    @TableField(exist = false) @JsonIgnore @Ignore transient String dataState;
    public String dataState(){
        String fieldName = tableInfo().getStateFieldName();
        if (Misc.isNull(fieldName)) {
            return dataState;
        }
        return DataHelper.parseString(BeanUtil.getValue(tableInfo(),this, fieldName));
    }
    public DataDTO dataState(String value){
        String fieldName = tableInfo().getStateFieldName();
        if (Misc.isNull(fieldName)) {
            this.dataState = value;
        }else{
            BeanUtil.setValue(tableInfo(), this, fieldName, value);
        }
        return this;
    }

    /**
     * 获得实体Id 解决多主键字段值用 - 号串起来
     * @return
     */
    public String dataId(){
        Map<String,Field> mapKeyFields = tableInfo().getFieldsByIds();
        if (mapKeyFields.size() > 1) {
            //多主键字段值用 - 号串起来
            StringBuilder sb = new StringBuilder();
            mapKeyFields.forEach((key, field) -> sb.append(BeanUtil.getValue(tableInfo(), this, field)).append("-"));
            //去掉最后一个字符 -
            sb.setLength(sb.length()-1);
            return sb.toString();
        }else{
            return DataHelper.parseString(BeanUtil.getValue(this, tableInfo().getIdField()));
        }
    }

    /**
     * 多主键赋值的问题, 故注释
     */
//    public DataDTO dataId(Object value){
//        Map<String,Field> map = tableInfo().getFieldsByIds();
//        if (map.size() > 1) {
//            map.forEach((key, field) -> setValue(key, value) );
//        }
//
//        setValue(tableInfo().getIdFieldName(), value);
//        return this;
//    }

    @TableField(exist = false) @JsonIgnore @Ignore transient private TableInfo tableInfo = null;
    public TableInfo tableInfo(){
        if (tableInfo == null) {
            tableInfo = TableInfo.get(this.getClass());
        }
        return tableInfo;
    }

    public DataDTO setValue(String fieldName, Object value){
        Field field = tableInfo().getField(fieldName);
        if (field == null ) {
            throw CommonException.newInstance(ResponseCode.SYS_FIELD_ISNOT_EXIST).text("找不到DTO的主键字段");
        }
        BeanUtil.setValue(tableInfo(), this, field, value);
        return this;
    }

    /**
     * 判断某个字段 是否有修改
     * @param fieldName
     * @return
     */
    public boolean checkChange(String fieldName) {
        return checkChange(fieldName, null, null);
    }
    private boolean checkChange(String fieldName, DataObject current, DataObject origin){
        Field field = tableInfo().getField(fieldName);
        if (field == null) {
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text(fieldName+"不存在");
        }

        Object currentValue = BeanUtil.getValue(tableInfo(), this, field);
        Object originValue = null;
        if (DbState.UPDATE.equals(dataState())) {
            if (origin() ==null) {
                //logger.error("dto更新checkChange()时, {}.origin()==null, 只会更新dto不为null字段的值, 且不能获得修改日志", getClass().getName());
            }else{
                originValue = BeanUtil.getValue(tableInfo(), origin(), field);
            }
        }

        boolean result=true;
        if (DbState.INSERT.equals(dataState()) || DbState.DELETE.equals(dataState())){

        }else if (currentValue == null && originValue != null
                || currentValue != null && originValue == null){

        }else if (currentValue == null && originValue == null){
            result = false;
        }else if (currentValue == null && originValue == null ||
                currentValue.toString().equals(originValue.toString())){
            result = false;
        }

        if (current!=null) {current.setValue(currentValue);}
        if (origin!=null) {origin.setValue(originValue);}

        return result;
    }
    /**
     * 获取某一个字段的修改日志用
     * 可以获得 新旧值
     * @param fieldName
     * @return
     */
    public ChangeData newChangeData(String fieldName, FormatConfig formatConfig){
        DataObject current = DataObject.newInstance();
        DataObject origin  = DataObject.newInstance();
        boolean result = checkChange(fieldName, current,  origin);
        ChangeData changeData = new ChangeData(result, current,  origin, TableInfo.get(getClass()).getField(fieldName));
        changeData.setFormatConfig(formatConfig);
        return changeData;
    }

    public ChangeData newChangeData(String fieldName){
        return newChangeData(fieldName, null);
    }

    /**
     * 获得所有修改的字段, 按照DTO的字段顺序
     * @return
     */
    public Map<String, ChangeData> changeDataForMap(){
        Map<String, Object> mapChange = BeanUtil.getChangeRowData(this);
        Map<String, ChangeData> mapChangeData = new LinkedHashMap<>(mapChange.size());
        mapChange.forEach((key, current)->{
            mapChangeData.put(key, newChangeData(key));
        });
        return mapChangeData;
    }

    /**
     * 返回调用方(如前端)的数据
     * @return
     */
    public Map<String, Object> mapSave(){
        return mapSave;
    }

    /**
     * 本方法 生成更新SQL时自动调用
     * @param mapSave
     * @return 返回调用方(如前端)的数据
     */
    public Map<String, Object> mapSave(Map<String, Object> mapSave){

        this.mapSave = mapSave;
        return mapSave;
    }

    /**
     * 所有字段设置为 null
     */
    public DataDTO clearAllData(){
        for (java.lang.reflect.Field field : tableInfo().getJavaFields() ){
            try {
                field.setAccessible(true);
                if (tableInfo().getFields().containsKey(field.getName()) && field.get(this) != null) {
                    field.set(this, null);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    @Override
    public String toString(){
        return JsonUtil.toString(this);
    }
}
