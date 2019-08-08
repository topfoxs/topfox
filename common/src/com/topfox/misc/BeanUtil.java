package com.topfox.misc;

import com.topfox.common.*;
import com.topfox.data.DataHelper;
import com.topfox.data.DataType;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class BeanUtil {
    static Logger logger = LoggerFactory.getLogger("");

    public static Object getValue(Object bean, Field field) {
        return getValue(null,bean, field, null);
    }
    public static Object getValue(Object bean, String fieldName) {
        return getValue(null, bean, null, fieldName);
    }
    public static Object getValue(TableInfo tableInfo, Object bean, Field field){
        return getValue(tableInfo, bean, field,null);
    }
    public static Object getValue(TableInfo tableInfo, Object bean, String fieldName) {
        return getValue(tableInfo, bean, null,fieldName);
    }

    private static Object getValue(TableInfo tableInfo, Object bean, Field field, String fieldName){
        if (bean == null || (field == null && fieldName == null)) {
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.getValue 出现参数为null");
        }
        if (field != null && fieldName == null){
            fieldName=field.getName();
        }
        tableInfo = tableInfo==null?TableInfo.get(bean.getClass()):tableInfo;

        Method getter=tableInfo.getGetter(fieldName);
        Object value;
        try {
            value = getter.invoke(bean);
        } catch (InvocationTargetException e1) {
            if (e1.getMessage() == null && e1.getTargetException() instanceof CommonException){
                throw (CommonException)e1.getTargetException();
            }else {
                throw CommonException.newInstance(ResponseCode.SYSTEM_ERROR)
                        .text("获取 ",bean.getClass().getName(),".",fieldName," 报错:",e1.getMessage());
            }
        } catch (Exception e2) {
            throw CommonException.newInstance(ResponseCode.SYSTEM_ERROR)
                    .text("获取 ",bean.getClass().getName(),".",fieldName," 报错:",e2.getMessage());
        }
        //field.getRightName2();
        return value;
    }

    public static String getValue2String(TableInfo tableInfo, Object bean, Field field){
        Object valueObj = getValue(tableInfo,bean,field, null);
        String valueString;
        if (valueObj instanceof Date){
            valueString = DateUtils.toDateStr((Date)valueObj,
                    Misc.isNull(field.getFormat())? DateFormatter.DATE_FORMAT :field.getFormat());
        }else {
            valueString = DataHelper.parseString(valueObj);
        }
        if (valueString == null) {
            valueString="";
        }
        return valueString;
    }

    /**
     * 这个 方法 赋值, 会把  Integer Long Doulbe 的值 null赋值为0
     * 对实体 指定字段赋值
     * @param bean  被写值得对象
     * @param field 字段对象  要写的字段对象
     * @param value 要写的值
     */
    public static void setValue(Object bean, Field field, Object value) {
        if (bean == null || field == null) {
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.setValue 出现参数为null");
        }
        setValue(null, bean, field, value);
    }
    public static void setValue(TableInfo tableInfo, Object bean, Field field, Object value){
        if (bean == null || field == null) {
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL)
                    .text("BeanUtil.setValue 出现参数为null");
        }
        tableInfo = tableInfo==null?TableInfo.get(bean.getClass()):tableInfo;

        Method setter = tableInfo.getSetter(field.getName());
        DataType dateType = field.getDataType();

        if (setter == null){
            throw CommonException.newInstance(ResponseCode.NULLException)
                    .text(getErrMsg("POJO没有找到setter方法",tableInfo, bean, value, setter));
        }
        Object valueTemp;
        try {
            //根据不同类型, 对数据处理后 set; DataHelper对象模仿上海瑞道

            //根据 pojo 中定义的类型, 转换传入的值
            if (dateType == DataType.STRING) {
                valueTemp = DataHelper.parseString(value);
            }else if (dateType == DataType.LONG) {
                valueTemp = DataHelper.parseLong(value);
            }else if (dateType == DataType.INTEGER) {
                valueTemp = DataHelper.parseInt(value);
            }else if (dateType == DataType.DOUBLE) {
                valueTemp = DataHelper.parseDouble(value);
            }else if (dateType == DataType.DECIMAL) {
                valueTemp = DataHelper.parseBigDecimal(value);
            }else if (dateType == DataType.DATE){
                valueTemp = DataHelper.parseDate(value);
            }else{
                valueTemp = value;
            }
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                    .text("POJO setter错误: 传入值 ",
                            value==null?"null":value.toString(),
                            " 无法转换为指定的类型 ",dateType.getValue(),
                            ". ",
                            getErrMsg("",tableInfo, bean, value, setter)
                    );
        }

        setValue(tableInfo, bean, field.getName(), valueTemp);
    }

    public static void setValue(Object bean, String fieldName, Object value){
        setValue(null, bean, fieldName, value);
    }

    /**
     * 这个 方法 赋值,  不会把  Integer Long Doulbe 的值 null赋值为0
     * @param tableInfo
     * @param bean
     * @param fieldName
     * @param value
     */
    public static void setValue(TableInfo tableInfo, Object bean, String fieldName, Object value){
        if (bean == null || Misc.isNull(fieldName)) {
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.setValue 出现参数为null");
        }
        tableInfo = tableInfo==null?TableInfo.get(bean.getClass()):tableInfo;

        Method setter=tableInfo.getSetter(fieldName);
        if (setter == null){
            throw CommonException.newInstance(ResponseCode.NULLException)
                    .text(getErrMsg("POJO没有找到setter方法",tableInfo, bean, value, setter));
        }

        try {
            setter.invoke(bean, value);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ea){
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                    .text("POJO setter错误: 传入值的类型",
                            value==null?"null":value.getClass().getName(),
                            "与定义的类型不一致, ",
                            getErrMsg("",tableInfo, bean, value, setter)
                    );
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                    .text(getErrMsg("POJO setter错误:",tableInfo, bean, value, setter));
        }
    }

    /**
     * 支持多主键的错误拼接
     */
    public static String getErrMsg(String msg, TableInfo tableInfo, Object bean, Object value, Method setter){
        StringBuilder sb = new StringBuilder(msg);
        sb.append(bean.getClass().getName()).append(".");
        if (setter != null) {
            sb.append(setter.getName()).append("(").append(setter.getParameterTypes()[0].getName());
            sb.append(" ").append(value.toString()).append(") ");
        }

        sb.append(" 主键值 ");
        tableInfo.getFieldsByIds().forEach((key, idField)->
                sb.append(key).append("=")
                        .append(BeanUtil.getValue(bean,idField)).append(" ")
        );
        return sb.toString();
    }

    /**
     * 获得变化值 使用
     * 比较大小,相等返回0，大于返回>0的值   小于返回小于0
     * @return long
     */
    public static long compare(Field field,Object value1,Object value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null && value2 != null || value1 != null && value2 == null){
            return 1;
        }

        DataType datatype = field==null?null:field.getDataType();
        if (datatype == DataType.STRING){
            return value1.equals(value2) == true?0:-1;
        }else if (datatype == DataType.LONG){
            return DataHelper.parseLongByNull2Zero(value1) - DataHelper.parseLongByNull2Zero(value2);
        }else if (datatype == DataType.INTEGER){
            return DataHelper.parseIntByNull2Zero(value1)- DataHelper.parseIntByNull2Zero(value2);
        }else if (datatype == DataType.DOUBLE){
            if (DataHelper.parseDoubleByNull2Zero(value1) - DataHelper.parseDoubleByNull2Zero(value2)>0){
                return 1;
            }else if (DataHelper.parseDoubleByNull2Zero(value1) - DataHelper.parseDoubleByNull2Zero(value2)<0){
                return -1;
            }else{
                return 0;
            }
        }else if (datatype == DataType.DECIMAL){
            return DataHelper.parseBigDecimalByNull2Zero(value1).compareTo(DataHelper.parseBigDecimalByNull2Zero(value2));
        }else if (datatype == DataType.DATE){
            return DataHelper.parseDate(value1).compareTo(DataHelper.parseDate(value2));
        }else if (datatype == DataType.BOOLEAN){
            return DataHelper.parseBoolean(value1) == DataHelper.parseBoolean(value2)?0:-1;
        }else{
            return value1.toString().equals(value2.toString()) == true?0:1;
        }
    }

    //isUpdateSQL true 生成更新SQL用； false保存后将有修改的值返回到前台用


    public static Map<String, Object> getChangeRowData(DataDTO bean) {//,boolean isUpdateSQL
        return getChangeRowData(bean, 2);
    }



    /**
     *
     * @param bean
     *
     * @param updateMode 1/2/3
     * # 重要参数:更新时DTO序列化策略 和 更新SQL生成策略
     * # 1 时, service的DTO=提交的数据.               更新SQL 提交数据不等null 的字段 生成 set field=value
     * # 2 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据) 的字段 生成 set field=value
     * # 3 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据 + 提交数据的所有字段)生成 set field=value
     * #   值为3, 则始终保证了前台(调用方)提交的字段, 不管有没有修改, 都能生成更新SQL, 这是与2最本质的区别
     *
     * @return
     */
    public static Map<String, Object> getChangeRowData(DataDTO bean, int updateMode){
        //booldebug时 map字段有序ean isUpdateSQL
        JSONObject rowNew = new JSONObject(logger.isDebugEnabled());
        if(bean == null){ return rowNew;}

        if (updateMode >1 && bean.origin() !=null && bean.mapModify()!=null) {
            //bean.origin() !=null && bean.mapModify()!=null 说明是html5前台 提交的数据
        }else if ( bean.isChangeUpdateSql==null || bean.isChangeUpdateSql == false
                || updateMode == 1 || bean.origin() == null // || bean.mapModify() == null
        ){
            Map<String,Object>  map = BeanUtil.bean2Map(bean,false);
            //指定强制更新为 null的字段处理
            if (bean.nullFields() != null) {
                Misc.idsArray2Set(bean.nullFields()).forEach(key -> map.put(key, null));
            }
            return map;
        }

        TableInfo tableInfo = TableInfo.get(bean.getClass());

        DataDTO rowOriginal=bean.origin();
        //调用方提交的数据
        Map<String, Object> mapModify = bean.mapModify();
        for (Map.Entry<String, Field> entry : tableInfo.getFields().entrySet()) {
            Field field = entry.getValue();
            String fieldName = entry.getKey();
            Object valueCurrent=getValue(tableInfo, bean, fieldName);

            if (valueCurrent != null && field.getDataType() == DataType.DATE && field.getFormat() != null){
                //#######################日期类型时, 把类型转换为字符 #######################################
                valueCurrent = DateUtils.toDateStr(DataHelper.parseDate(valueCurrent), field.getFormat());
            }

            //Id字段 和版本号 始终放入Map
            if (tableInfo.getFieldsByIds().keySet().contains(fieldName) || fieldName.equals(tableInfo.getVersionFieldName())){
                rowNew.put(fieldName, valueCurrent);
                continue;
            }

            // 标记为 增量 的字段, 只要 有值, 就 始终认为是 修改 字段
            if ((field.getIncremental() == Incremental.ADDITION || field.getIncremental() == Incremental.SUBTRACT)
                    && valueCurrent != null) {
                rowNew.put(fieldName, valueCurrent);
                continue;
            }

            if(rowOriginal == null){continue;}
            //日期 - 原始值, 修改之前的值
            Object valueOriginal=BeanUtil.getValue(tableInfo, rowOriginal, fieldName);
            if (valueOriginal != null && field.getDataType() == DataType.DATE && field.getFormat() != null){
                //#######################日期类型时, 把类型转换为字符 #######################################
                valueOriginal = DateUtils.toDateStr(DataHelper.parseDate(valueOriginal), field.getFormat());
            }

//            //该字段是否是强制传回到前台的字段
//            if (isUpdateSQL == false && afterSaveReturnFields.indexOf(fieldName1)>=0 || fieldName1.indexOf("ModifyDate")>=0) {
//                rowNew.put(fieldName1, row1.get(fieldName1));
//            }
//            //该字段是否是强制更新的字段
//            if (isUpdateSQL == true && updateFields.indexOf(fieldName1)>=0) {
//                rowNew.put(fieldName1, row1.get(fieldName1));
//            }

            if (updateMode == 3 && mapModify != null
                    && ( mapModify.containsKey(fieldName)
                      || mapModify.containsKey(field.getDbName())  ) //带下划线的字段名 也要判断
            ){
                //提交的数据 始终认为 是有变化的数据(实际不一定)
                rowNew.put(fieldName, valueCurrent);
                continue;
            }
//            //与数据库一样的值始终不需要;前台传回的值 后台替换掉呢
//            //if (isUpdateSQL == false && BeanUtil.compare(field, value1, valueOriginal) == 0){
//            if (isUpdateSQL == false && BeanUtil.compare(field, valueCurrent, valueOriginal) == 0){
//                continue;
//            }

//            if (Misc.isNotNull(valueCurrent) && row2.keySet().contains(fieldName1) == false){//rowOld不存在该字段，则视为该字段的值有变化
//                rowNew.put(field.getName(), row1.get(fieldName1));
//                continue;
//            }

            if (valueCurrent == null && valueOriginal == null) continue;
            if ((valueCurrent == null && valueOriginal != null) || (valueCurrent != null && valueOriginal == null)){
                rowNew.put(fieldName, valueCurrent);
                continue;
            }

            long result;
            try {
                result = BeanUtil.compare(field, valueCurrent, valueOriginal);
            }catch(Exception e){
                throw new RuntimeException("转换报错 fieldName="+fieldName+" value1="+valueCurrent.toString()+" value2="+rowOriginal.toString());
            }
            if (result != 0){
                rowNew.put(fieldName, valueCurrent);
            }
            /////////////////////////////////////////////////////////////////////////
        }

        //指定强制更新为 null的字段处理
        if (bean.nullFields() != null) {
            Misc.idsArray2Set(bean.nullFields()).forEach(key -> rowNew.put(key, null));
        }
        return rowNew;
    }
    public static <T> T map2Bean(Map<String, Object> mapData, Class<T> clazz) {
        if (clazz == null || mapData == null) {return null;}
        T newBean = newInstance(clazz);
        map2Bean(mapData, newBean);
        return newBean;
    }

    public static void map2Bean(Map<String, Object> mapData, Object bean) {
        if (bean == null || mapData == null) {return;}
        TableInfo tableInfo = TableInfo.get(bean.getClass());
        Map<String,Field> fields = tableInfo.getFields();
        mapData.forEach((fieldName, value)->{
            Field field = fields.get(fieldName);
            if (field == null) {
                //如果前台提交的数据是xx_org_id 则转为 xxOrgId去获取字段对象Field
                //field = fields.get(BeanUtil.toCamelCase(fieldName));
                field = fields.get(CamelHelper.toCamel(fieldName));

            }
            if (field != null) {
                setValue(tableInfo, bean, field, value);
            }
        });
    }

    public static <T> T newInstance(Class<T> clazz){
        if (clazz == null){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("getEntity clazz不能为空");
        }

        T bean;
        try {
            bean =clazz.newInstance();
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.ERROR).text(clazz.getName()+".newInstance()报错");
        }

        return bean;
    }

    /**
     * 获得Bean的数据
     * @param bean
     * @return Map<String,Object>
     */
    public static Map<String,Object> bean2Map(Object bean) {
        return bean2Map(null, bean, false, false);
    }

    /**
     * 获得Bean的数据
     * @param bean
     * @param isNullValue2map 是否要获得 值为null的 数据
     * @return
     */
    public static Map<String,Object> bean2Map(Object bean, Boolean isNullValue2map) {
        return bean2Map(null, bean,isNullValue2map,false);
    }

    /**
     *
     * @param tableInfo
     * @param bean
     * @param isNullValue2map
     * @param isJsonFormat 是否分局 @JsonFormat 注解格式化日期类型 的值为 字符串
     * @return
     */
    public static Map<String,Object> bean2Map(TableInfo tableInfo,
                                               Object bean, Boolean isNullValue2map, Boolean isJsonFormat)
    {
        if(bean == null){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("bean2Map不能传入值是null的Bean");
        }
        tableInfo = tableInfo==null?TableInfo.get(bean.getClass()):tableInfo;

        Map<String,Object> mapData = logger.isDebugEnabled()?new LinkedHashMap<>():new HashMap();

        Map<String,Field> fields=tableInfo.getFields();

        for (String key : fields.keySet()){
            Field field=fields.get(key);
            try {
                Method getter = bean.getClass().getMethod("get"+field.getRightName2());
                Object value = getter.invoke(bean);
                if (isNullValue2map == true){
                    mapData.put(key,value);
                }else if (value != null){
                    if (isJsonFormat && field.getDataType() == DataType.DATE){
                        //日期格式化为 字符串,  解决QTO + XXXmapper.xml查询时, 老是有00:00:00的问题
                        //格式来自于 @JsonMormat
                        String format = field.getFormat();
                        format = Misc.isNull(format)?DateFormatter.DATE_FORMAT:format;
                        mapData.put(key, DateUtils.toDateStr(DataHelper.parseDate(value),format));
                    }else {
                        mapData.put(key, value);
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
                throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text("获取Bean数据报错");
            }
        }
        return mapData;
    }

    /**
     * POJO对象拷贝,将"源对象与目标对象的交集字段在 源对象的值 拷贝到目标对象
     * @param source 源头对象
     * @param dest   目标对象
     */
    public static void copyBean(Object source, Object dest){
        if(source == null || dest == null) {return;}
        TableInfo tableInfoSource = TableInfo.get(source.getClass());
        TableInfo tableInfoDest   = TableInfo.get(dest.getClass());

        Map<String, Field> fieldsSource = tableInfoSource.getFields();
        Map<String,Field> fieldsDest = tableInfoDest.getFields();

        List<String> list =fieldsSource.keySet().stream()
                /** 筛选源头对象有哪些字段是存在目的对象中的 */
                .filter((key)->fieldsDest.containsKey(key))
                .collect(Collectors.toList());
        list.forEach((fieldName)->
                setValue(tableInfoDest,
                        dest,//要写值得对象
                        fieldName, // fieldsSource.get(fieldName),//要写的字段对象
                        getValue(tableInfoSource,source,fieldsSource.get(fieldName))//值,来自于source
                )
        );
    }

    /**
     * 在不改变传入进来两个对象的情况下, 实现以dest为主的数据合并, 产生一个新的对象,返回
     * @param source 源头对象
     * @param dest   目标对象
     * @return 返回一个新对象
     */
    public static Object copyNewBean(Object source, Object dest){
        Object destNew= cloneBean(dest);//目标对象克隆一个新的
        copyBean(source,destNew);       //将source的数据克隆到 destNew
        return destNew;
    }

    /**
     * 克隆对象
     * @param bean
     * @return
     */
    public static Object cloneBean(Object bean){
        if (bean == null) {return null;}
        Object dest= newInstance(bean.getClass());//创建一个空白对象
        copyBean(bean,dest);//将dest的数据克隆到 destNew
        return dest;
    }

    /**
     *
     * @param field
     * @param value
     * @param stringBuilder
     */
    public static void getSqlValue(Field field, String fieldName, Object value,StringBuilder stringBuilder){
        /**
         * field == null 说明是计算列字段(length(name)),则就当字符串拼接查找条件
         */
        if (field == null) {
            String stringValue=DataHelper.parseString2(value);
            //数字类型 和 boolean不要引号
            if (value instanceof Number || value instanceof Boolean){
                    stringBuilder.append(stringValue);
            }else if (value == null ) {
                stringBuilder.append("null");
            }else{
                stringBuilder.append("'").append(stringValue).append("'");
            }
            return;
        }

        DataType dateType=field.getDataType();

        try{
            if (value == null) {
                stringBuilder.append("null");
            }else if (dateType == DataType.DATE  ) {
                //生成SQL 日期格式化: 根据 DTO注解@JsonFormat( pattern = "yyyy-MM-dd HH:mm")获得, 如果没有, 默认 yyyy-MM-dd
                String format = field.getFormat();
                format = Misc.isNull(format)?DateFormatter.DATE_FORMAT:format;
                stringBuilder.append("'")
                        .append(DateUtils.toDateStr(DataHelper.parseDate(value), format))
                        .append("'");
            }else if (dateType == DataType.DOUBLE || dateType == DataType.DECIMAL) {
                stringBuilder.append(DataHelper.roundToString(value, field.getFormat()));
            }else if (dateType == DataType.LONG) {
                stringBuilder.append(DataHelper.parseLong(value));
            }else if (dateType == DataType.INTEGER) {
                stringBuilder.append(DataHelper.parseInt(value));
            }else if (dateType == DataType.BOOLEAN) {
                stringBuilder.append(value);
            }else {
                String stringValue = DataHelper.parseString2(value);
                stringBuilder.append("'").append(stringValue).append("'");
            }
        }catch (NumberFormatException e){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_INVALID).text(fieldName + "的值 "+value.toString()+" 不能转换为nubmer类型");
        }
    }
//
//
//    /**
//     * 请直接使用 CamelHelper.toUnderlineName(s)
//     * 驼峰命名--> 带下划线
//     * 默认英文全部装成大写
//     */
//    @Deprecated
//    public static String toUnderlineName(String s) {
//        return CamelHelper.toUnderlineName(s);
//    }
//
//    /**
//     * 请直接使用 CamelHelper.toCamel(s)
//     * 下划线的 key --> 驼峰命名
//     */
//    @Deprecated
//    public static String toCamelCase(String s) {
//        return CamelHelper.toCamel(s);
//    }
}
