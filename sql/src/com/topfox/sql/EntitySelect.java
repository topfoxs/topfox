package com.topfox.sql;

import com.topfox.common.AppContext;
import com.topfox.common.DataQTO;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntitySelect extends IEntitySql {
    private Boolean isAppendAllFields=true;
    private Integer pageIndex=null, pageSize=null;
    private String fieldsSelect, orderBy=null, groupBy=null, having=null;
    private StringBuilder selectSql;
    private Boolean openPage=true; // 是否开启 mysql Limit  ,  false 时SQL语句就没有 limit 了

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private EntitySelect(){
        selectSql = new StringBuilder();
    }

    public static EntitySelect create(){
        return new EntitySelect();
    }
    public static EntitySelect create(Class<?> clazz){
        EntitySelect current = create();
        current.setEntityClazz(clazz);
        return current;
    }
    public static EntitySelect create(TableInfo tableInfo){
        EntitySelect current = create();
        current.setTableInfo(tableInfo);
        return current;
    }

    /**
     * 返回条件对象
     *
     * @return
     */
    @Override
    public Condition<EntitySelect> where() {
        return super.where();
    }

    @Override
    public Condition<EntitySelect> setWhere(Condition where){
        return super.setWhere(where);

    }

    @Override
    protected void clean(){
        openPage=true;
        pageIndex =null;
        pageSize = null;

        fieldsSelect =null;
        orderBy=null;
        groupBy=null;
        having =null;
        isAppendAllFields=true;
        selectSql.setLength(0);
        where().setQTO(null);
        where().clean();//清空上次的查询条件
    }

    @Override
    public EntitySelect setTableInfo(TableInfo tableInfo) {
        super.setTableInfo(tableInfo);
        if (where() != null){
            where().setTableInfo(tableInfo);
        }
        return this;
    }

    public EntitySelect select() {
        return select(null,true);
    }
    public EntitySelect select(String fields) {
        return select(fields,false);
    }
    public EntitySelect select(String fields,Boolean isAppendAllFields) {
        clean();//初始化

        if(Misc.isNull(fields)){
            //假如没有指定字段查询, 则默认查询所有字段
            isAppendAllFields=true;
        }
        this.fieldsSelect=fields;
        this.isAppendAllFields=isAppendAllFields;
        return this;
    }
    public String getSelect() {
        return this.fieldsSelect;
    }
    public Boolean isAppendAllFields() {
        return this.isAppendAllFields;
    }

    public EntitySelect orderBy(String fields){
        this.orderBy=fields;
        return this;
    }

    public EntitySelect groupBy(String fields){
        this.groupBy=fields;
        return this;
    }

    public EntitySelect having(String fields){
        this.having=fields;
        return this;
    }

    public EntitySelect setPageSize(Integer pageSize) {
        return setPage(0, pageSize);
    }
    public EntitySelect setPage(Integer pageIndex, Integer pageSize) {
        this.pageIndex=pageIndex;
        this.pageSize=pageSize;
        return this;
    }
    /**
     * set方法
     * this.openPage
     *
     * 开启分页, 默认值 开启 true, 如果关闭后sql语句后 始终不生成 limit x,y .
     * 例如根据Id 字段的查询 getObject() , 就应该关闭 这个.
     */
    public EntitySelect openPage(boolean value){
        this.openPage = value;
        return this;
    }
    public boolean openPage(){
        return this.openPage;
    }

    public String getSelectCountSql() {
        selectSql.setLength(0);

        selectSql.append("SELECT count(1) count");
        if (groupBy == null || groupBy.length() == 0) {
            selectSql.append("\nFROM ").append(getTableInfo().getTableName()).append(" a")
            .append( where().getWhereSql());
            return selectSql.toString();
        }else {
            selectSql.append("\nFROM \n(select 1 FROM ").append(getTableInfo().getTableName()).append( where().getWhereSql());
            selectSql.append("\nGROUP BY ").append(groupBy);
            if (having != null && having.length() > 0) {
                selectSql.append("\nHAVING ").append(having);
            }

            selectSql.append(") TEMP");
            return selectSql.toString();
        }
    }

    @Override
    public String getSql(){
        selectSql.setLength(0);
        selectSql.append("SELECT ");
        if(fieldsSelect != null){
            selectSql.append(fieldsSelect);
        }

        //生成查询的字段
        if (isAppendAllFields) {
            StringBuilder sbSelectFields = new StringBuilder();
            getTableInfo().getFields().forEach((fieldName, field)->{
                if (field.getAnnotationName() && !field.getDbName().equals(field.getName())) {return;}
                sbSelectFields.append(field.getDbName()).append(",");
            });
//            for (String fieldName : tableInfo.getFields().keySet()) {
//                sbSelectFields.append(tableInfo.getColumn(fieldName)).append(",");
////                if (logger.isDebugEnabled()){
////                    sbSelectFields.append("\n\t");
////                }
//            }

            if (fieldsSelect !=null && !fieldsSelect.trim().endsWith(",")){
                selectSql.append(",");
            }
            selectSql.append(sbSelectFields.substring(0, sbSelectFields.length() - 1));
        }
        selectSql.append("\nFROM ").append(getTableInfo().getTableName()).append(" a");

        //得到条件
        selectSql.append( where().getWhereSql());

        DataQTO dataQTO =  where().qto;
        if (groupBy != null && groupBy.length()>0) {
            selectSql.append("\nGROUP BY ").append(groupBy);
        }else if ( where().qto !=null &&  where().qto.getGroupBy() != null &&  where().qto.getGroupBy().length()>0){
            selectSql.append("\nGROUP BY ").append( where().qto.getGroupBy());
        }

        if (having != null && having.length()>0) {
            selectSql.append("\nHAVING ").append(having);
        }else if (dataQTO !=null && dataQTO.getHaving() != null && dataQTO.getHaving().length()>0){
            selectSql.append("\nHAVING ").append(dataQTO.getHaving());
        }

        if (orderBy != null && orderBy.length()>0) {
            selectSql.append("\nORDER BY ").append(orderBy);
        }else if (dataQTO !=null && dataQTO.getOrderBy() != null && dataQTO.getOrderBy().length()>0){
            selectSql.append("\nORDER BY ").append(dataQTO.getOrderBy());
        }

        // count( 开头 认为是在求 总行数, 故不拼写limit
        boolean temp = fieldsSelect != null && fieldsSelect.toLowerCase().startsWith("count(") == false;
        //分页  优先级别
        if (where().openPage() && this.openPage() && ( Misc.isNull(fieldsSelect) || temp )) {
            Integer pageIndex2=null,pageSize2=null;
            if (pageSize!=null){
                pageIndex2 = pageIndex;
                pageSize2  = pageSize;
            }else if (dataQTO != null && dataQTO.getPageSize() != null){
                pageIndex2 = dataQTO.getPageIndex();
                pageSize2  = dataQTO.getPageSize();
            }
            //没有设置默认第1页
            pageIndex2 = pageIndex2==null?1:pageIndex2;
            //没有设置 取最大行数
            pageSize2 = pageSize2!=null?pageSize2: AppContext.getSysConfig().getMaxPageSize();

            StringBuilder limit = new StringBuilder("\nLIMIT ");
            limit.append(pageIndex2 >= 2 ? (pageIndex2 - 1) * pageSize2 : 0).append(",").append(pageSize2);
            selectSql.append(limit.toString());
        }
        String sqlString=selectSql.toString();
        //init();//查询 初始化,便于下次使用
        return sqlString;
    }

    /**
     * 1.设置分页参数的值. pageSize 的值小于等于0, 表示不分页, 设置最大值
     * 2.pageSize 没有值, 则设置默认值
     * 3.默认值都可以从SpringBoot配置文件中更改
     *
     * @param qto
     */
    public String initQTO(DataQTO qto){
        //获取第几页
        int pageIndex    = qto.getPageIndex();
        //每页返回条数
        int pageSize    = AppContext.getSysConfig().getPageSize();
        //每页返回最大值条数
        int maxPageSize = AppContext.getSysConfig().getMaxPageSize();
        //以上为配置文件的信息

        if (qto.getPageSize() != null && qto.getPageSize() <= 0){
            qto.setPageSize(maxPageSize);
            //pageSize <= 0时, 表示不分页,  则默认从第一页查
            qto.setPageIndex(0);
        }
        if (qto.getPageSize() == null) {
            qto.setPageSize(pageSize);
        }

        /**
         * Eq:等   Ne:不等
         * 任何字段名后缀说明(加入该字段的值是 value1,value2)
         * fieldOrEq      则生成SQL: (field = valus1 OR field = value2)
         * fieldAndNe     则生成SQL: (field <> valus1 AND field = value2)
         * fieldOrLike    则生成SQL: (field LIKE CONCAT("%",valus1,"%") OR field LIKE CONCAT("%",valus2,"%"))
         * fieldAndNotLike 则生成SQL: (field NOT LIKE CONCAT("%",valus1,"%") AND field NOT LIKE CONCAT("%",valus2,"%"))
         */

        /**
         *  与之对应的mybatis 的 mapper.xml文件配置如下
         <where>
         <if test = "idOrEq != null">AND ${idOrEq}</if>
         <if test = "idAndNe != null">AND ${idAndNe}</if>
         <if test = "idOrLike != null">AND ${idOrLike}</if>
         <if test = "idAndNotLike != null">AND ${idAndNotLike}</if>
         </where>
         */

        TableInfo tableInfoQTO=TableInfo.get(qto.getClass());
        Map<String,Object> mapQTO = BeanUtil.bean2Map(qto);
        mapQTO.forEach((key, value)->{
            String valueString=value==null?"":value.toString().trim();//去掉参数值 左右的空格
            if (valueString !=null && valueString.indexOf("(")>=0) {return;}
            List<String> listValues= Misc.string2Array(valueString,",");
            if (key.endsWith("OrEq")){
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereOrSql(valueString, key.substring(0,key.length()-4)));
            }else if (key.endsWith("AndNe")) {
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereNotInSql(listValues, key.substring(0,key.length()-5)));
            }else if (key.endsWith("OrLike")) {
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereLikeOrSql(listValues, key.substring(0,key.length()-6)));
            }else if (key.endsWith("AndNotLike")) {
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereNotLikeSql(listValues, key.substring(0,key.length()-10)));
            }
        });

        pageSize = qto.getPageSize();
        StringBuilder limit = new StringBuilder("LIMIT ");
        limit.append(pageIndex>=2?(pageIndex-1)*pageSize:0).append(",").append(pageSize);

        qto.setLimit(limit.toString());
        return qto.getLimit();
    }

}
