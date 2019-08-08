package com.topfox.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.topfox.common.*;
import com.topfox.data.*;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;
import com.topfox.mapper.BaseMapper;
import com.topfox.util.CheckData;
import com.topfox.util.DataCache;
import com.topfox.util.KeyBuild;
import com.topfox.sql.*;
import org.apache.ibatis.binding.BindingException;
import org.mybatis.spring.MyBatisSystemException;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class SimpleService<M extends BaseMapper<DTO>, DTO extends DataDTO>
        extends SuperService<DTO>
        implements IService<DTO>
{
    @Autowired(required = false)
    SqlSessionTemplate sqlSessionTemplate;

    @Autowired(required = false)
    protected M baseMapper;


    /**
     * 初始化之前, 类库要做的事情
     * @param listUpdate 更新时,  对DTO的处理用
     */
    @Override
    public void beforeInit(List<DataDTO> listUpdate){
        super.beforeInit(listUpdate);

        restSessionHandler.getEntitySql(clazzDTO()); //SQL构造类
        afterInit();
        if (listUpdate != null){
            initUpdateDTO(listUpdate);
        }
    }

    @Override
    public DataCache dataCache() {
        return restSessionHandler.getDataCache();
    }

    /**
     * 创建一个新的条件对象Condition
     * com.topfox.sql.Condition
     * @return
     */
    @Override
    public Condition where(){
        return Condition.create();
    }

    @Override
    public void beforeSave2(List<DTO> list, String dbState){
        list.forEach((beanDTO) -> {
            beanDTO.dataState(dbState);
            if (DbState.INSERT.equals(dbState) && beanDTO.dataVersion()==null){
                beanDTO.dataVersion(1);
            }
        });
        if (fillDataHandler != null && (DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState))) {
            /** 调用"填充接口实现类的方法", 主要是 创建和修改 人/时间 */
            fillDataHandler.fillData(tableInfo().getFillFields(), (List<DataDTO>) list);
        }

//        beforeSave(list);
//
//        if (DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState)) {
//            //执行插入或更新之前的的方法
//            beforeInsertOrUpdate(list);
//            list.forEach((beanDTO) -> beforeInsertOrUpdate(beanDTO, dbState));
//        } else if (DbState.DELETE.equals(dbState)) {
//            //执行删除之前的逻辑
//            beforeDelete(list);
//            list.forEach((beanDTO) -> beforeDelete(beanDTO));
//        }
    }
    /**
     * 增 删 改 sql执行之后 调用本方法
     * @param list
     */
    @Override
    public void afterSave2(List<DTO> list, String dbState){
        if (sysConfig.isRedisCache()){
            Map<String, Field> fieldsForIncremental = tableInfo().getFieldsForIncremental();//注解为 自增减的字段 的 值 特殊处理
            Map<String, Field> fieldsAll = tableInfo().getFields();//注解为 自增减的字段 的 值 特殊处理
            //缓存处理
            list.forEach(dto->{
                if (DbState.UPDATE.equals(dto.dataState())){
                    if (dto.origin()==null){
                        DTO redisDTO1 = dataCache().get2CacheById(clazzDTO(), dto.dataId(), sysConfig, "afterSave2");
                        if (redisDTO1 == null || fieldsForIncremental.size() > 0){
                            dataCache().deleteRedis(tableInfo(), dto.dataId(), sysConfig);//没有原始数据,则必须要 删除redis缓存
                            return;  //continue; 后台 直接 new 的dto, 缓存也没有, 则更新后不放入缓存. 因为要去查一次才能获得原始数据
                        }else {
                            dto.addOrigin(redisDTO1);
                        }
                    }

                    DTO redisDTO2 = sysConfig.getUpdateMode()==1?dto.merge():dto;
                    //对递增减的所有字段循环
                    fieldsForIncremental.forEach((fieldName, fieldIncremental) -> {
                        Field field = fieldsAll.get(fieldIncremental.getDbName());
                        if (field == null){
                            throw CommonException.newInstance(ResponseCode.SYS_FIELD_ISNOT_EXIST).text("递增减的原始字段不存在");
                        }
                        Number changeValue = DataHelper.parseDoubleByNull2Zero(BeanUtil.getValue(tableInfo(), dto, fieldIncremental));
                        Number originValue;
                        if (fieldsForIncremental.containsKey(field.getName()) == false
                                && fieldName.length() > field.getName().length() && field.getName().length() > 0 )
                        {
                            originValue = DataHelper.parseDoubleByNull2Zero(BeanUtil.getValue(tableInfo(), dto.origin(), field));
                            Number result = ChangeData.incrtl(fieldIncremental, changeValue, originValue);
                            if (result !=null ){
                                BeanUtil.setValue(tableInfo(), redisDTO2, field, result);
                                BeanUtil.setValue(tableInfo(), redisDTO2, fieldIncremental.getName(), null);
                                dto.mapSave().put(field.getName(), result); //将递增/减 后的值返回到 调用方(如前端)
                            }
                        }else{
                            originValue = DataHelper.parseDoubleByNull2Zero(BeanUtil.getValue(tableInfo(), dto.origin(), fieldIncremental));
                            Number result = ChangeData.incrtl(fieldIncremental, changeValue, originValue);
                            if (result !=null ){
                                BeanUtil.setValue(tableInfo(), redisDTO2, fieldIncremental, result);
                                dto.mapSave().put(fieldIncremental.getName(), result); //将递增/减 后的值返回到 调用方(如前端)
                            }
                        }

//                        Number result = ChangeData.incrtl(field, changeValue, originValue);
//                        if (result !=null ){
//                            BeanUtil.setValue(tableInfo(), redisDTO2, field, result);
//                            dto.mapSave().put(fieldName, result); //将递增/减 后的值返回到 调用方(如前端)
//                        }
                    });

                    //DTO 和 mapSave 的版本字段 +  1, 便于 缓存reids 和输出到 前台
                    String versionFieldName = tableInfo().getVersionFieldName();
                    if (Misc.isNotNull(versionFieldName) && redisDTO2.dataVersion() != null ){
                        redisDTO2.dataVersion(redisDTO2.dataVersion() + 1);//DTO +1
                        dto.mapSave().put(versionFieldName, dto.dataVersion());  //将递增后的值返回到 调用方(如前端)
                    }

                    //@RowId字段始终返回前端
                    String rowIdDFieldName = tableInfo().getRowNoFieldName();
                    if (Misc.isNotNull(rowIdDFieldName)){
                        dto.mapSave().put(rowIdDFieldName, dto.dataRowId());
                    }

                    dataCache().addCacheBySave(redisDTO2);//记录哪些DTO需要缓存到到redis, 此时还没有保存到redis
                    //dto.addOrigin(redisDTO2); //重要一个DTO 在一个线程中保存多次使用
                    return;
                }

                /**
                 * 新增时, 不考虑 放入缓存.  因为 DTO没有 默认值,  默认值 是在 数据库 中
                 */

                if (DbState.DELETE.equals(dto.dataState())){//DbState.INSERT.equals(dto.dataState()) ||
                    //删除 时放入 缓存
                    dataCache().addCacheBySave(dto);//记录哪些DTO需要缓存到到redis, 此时还没有保存到redi
                }
            });
        }
//
//        /**
//         * 必须是合并过的Bean(数据库原始数据 + 需改的数据 的合并)
//         */
//        afterSave(list);
//        if(DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState)){
//            afterInsertOrUpdate(list);
//            list.forEach((beanDTO) -> afterInsertOrUpdate(beanDTO, dbState));
//        }else if(DbState.DELETE.equals(dbState)){
//            afterDelete(list);
//            list.forEach((beanDTO) -> afterDelete(beanDTO));
//        }
    }

    @Override
    public int insert(DTO dto){
        List<DTO> list = new ArrayList<>(1);
        list.add(dto);
        return insertList(list);
    }

    /**
     * 约束: 一个表只有一个 主键字段, 整型自增
     * @param dto
     *
     * @return 返回 插入DTO的 主键Id
     */
    @Override
    public long insertGetKey(DTO dto) {
        if (tableInfo().getFieldsByIds().size()>1 ) {
            throw CommonException.newInstance(ResponseCode.SYSTEM_ERROR)
                    .text("插入并获得自增Id, 不支持多主键字段的表");
        }

        insert(dto);
        //按照connect的对象隔离的
        Long lastId = baseMapper.selectForLong("select LAST_INSERT_ID()");
        //多主键递增怎么赋值呢?

        dto.setValue(tableInfo().getIdField(), lastId);
        return lastId;
    }

    /**
     *
     * 实现多个beanDTO 生成一句SQL插入
     * @param list
     *
     * @return
     */
    @Override
    public int insertList(List<DTO> list){
        if (list == null|| list.isEmpty()) {return 0;}

        beforeSave2(list, DbState.INSERT);
        int result = baseMapper.insert(restSessionHandler.getEntitySql(clazzDTO()).getInsertSql((List<DataDTO>)list));
        //执行插入之后的的方法
        afterSave2(list, DbState.INSERT);

        return result;
    }

    @Override
    public void initUpdateDTO(List<DataDTO> listUpdate){
        //获得更新之前的数据
        Set<String> setIds = new HashSet<>();//listUpdate.stream().map(DataDTO::dataId).collect(Collectors.toSet());
        if (sysConfig.getUpdateMode() > 1 || (sysConfig.getUpdateMode()==1 && sysConfig.isSelectByBeforeUpdate())) {
            listUpdate.forEach(dto -> {
                String id = dto.dataId();
                if (Misc.isNotNull(id)) {
                    setIds.add(dto.dataId());
                }
            });
        }
//        setIds.forEach(key->{
//            if (Misc.isNull(key)) {throw CommonException.newInstance2(ResponseCode.DB_UPDATE_KEY_ISNULL);}
//        });
        List<DTO> listQuery = setIds.size()==0?new ArrayList<>():listObjects(setIds);

        //提交的修改过的数据, list结构转成 map结构,  便于 获取
        Map<String, JSONObject> mapModifyData = new HashMap<>();
        JSONArray bodyData = restSessionHandler.get().getBodyData();
        if (bodyData != null) {
            bodyData.forEach(map -> {
                JSONObject mapTemp = (JSONObject) map;
                if (tableInfo().getFieldsByIds().size()>1) {
                    //多主键字段的处理
                    StringBuilder sbIds = new StringBuilder();
                    tableInfo().getFieldsByIds().forEach((key, field)->{
                        sbIds.append(mapTemp.getString(key)).append("-");
                    });
                    //去掉最后的-
                    sbIds.setLength(sbIds.length()-1);
                    mapModifyData.put(sbIds.toString(), mapTemp);
                }else {
                    mapModifyData.put(mapTemp.getString(tableInfo().getIdField()), mapTemp);
                }
            });
        }
        Map<String, DTO> mapOriginDTO = listQuery.stream().collect(Collectors.toMap(DTO::dataId, Function.identity()));//转成map

        listUpdate.forEach(dto -> {
            if (dto.origin()==null) {
                //value是DTO. 添加 原始DTO-修改之前的数据, 来自于数据库查询或者Redis
                DTO origin = mapOriginDTO.get(dto.dataId());
                if (origin!=null && origin.hashCode() == dto.hashCode()){
                    dto.addOrigin(BeanUtil.cloneBean(origin));//重要
                }else {
                    dto.addOrigin(origin);
                }
            }
            dto.addModifyMap(mapModifyData.get(dto.dataId()));//value是JSONObject. 添加 提交的修改过的数据. 当依据变化值生成更新SQL时 需要用到

            ///////////////////////////////////////////////////////////////////////////////////
            //处理当前DTO= 原始数据 + 提交修改的数据
            if (sysConfig.getUpdateMode()>1) {
                if (dto.mapModify()==null){
                    //后台 自己查询出来的数据做保存, 这里无逻辑
                    //后台自己new DTO 做的报错, 这里无逻辑
                }else {
                    //前台传回的数据, 执行以下逻辑
                    BeanUtil.copyBean( mapOriginDTO.get(dto.dataId()), dto);   //拷贝原始数据      到当前DTO
                    BeanUtil.map2Bean(mapModifyData.get(dto.dataId()), dto);   //拷贝提交修改的数据 到当前DTO
                    dataCache().addCacheBySelected(dto);//20181217添加到一级缓存
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////
            //String tem=null;
        });
    }

    /**
     * 根据Id更新, version不是null, 则增加乐观锁
     * @param dto
     * @return
     */
    @Override
    public int update(DTO dto){
        List<DTO> list = new ArrayList<>(1);
        list.add(dto);
        return updateList(list, sysConfig.getUpdateMode());
    }

    @Override
    public int update(DTO dto, int updateMode){
        List<DTO> list = new ArrayList<>(1);
        list.add(dto);
        return updateList(list, updateMode);
    }

    @Override
    public int updateList(List<DTO> list) {
        return updateList(list, sysConfig.getUpdateMode());
    }

    /**
     * 当行更新 返回 0 调用本方法抛出异常
     * @param dto
     */
    public void updateNotResultError(DTO dto){
        //获得中文表名
        String tableCnName = tableInfo().getTableCnName();
        String idvalue = dto.dataId();
        if (sysConfig.isUpdateNotResultError()) {
            if (dto.dataVersion() != null) {
                throw CommonException.newInstance(ResponseCode.DB_UPDATE_VERSION_ISNOT_NEW)
                        .text(Misc.isNull(tableCnName)?clazzDTO().getName():tableCnName, " id=", idvalue, " 更新失败, 该数据可能已经被他人修改, 请刷新");
            }
            throw CommonException.newInstance(ResponseCode.DB_UPDATE_FIND_NO_DATA)
                    .text(Misc.isNull(tableCnName)?clazzDTO().getName():tableCnName, " id=", idvalue, " 更新失败");
        }else{
            logger.warn("{} id={} 更新影响的行数为0", Misc.isNull(tableCnName)?clazzDTO().getName():tableCnName, idvalue);
        }
    }

    /**
     * @param listUpdate
     * @param updateMode
     * @return
     */
    @Override
    public int updateList(List<DTO> listUpdate, int updateMode){
        if (listUpdate == null || listUpdate.isEmpty()) {return 0;}
//        //beforeInit();

        beforeSave2(listUpdate, DbState.UPDATE);

        AtomicInteger result =  new AtomicInteger();//线程安全的自增类

        listUpdate.forEach((beanDTO)-> {
            String idvalue = beanDTO.dataId();
            if (Misc.isNull(idvalue)) {throw CommonException.newInstance2(ResponseCode.DB_UPDATE_KEY_ISNULL);}

            DTO origin = beanDTO.origin();
            if (origin==null && beanDTO.dataVersion() != null) {
                origin = getObject(idvalue);
            }
            if (beanDTO.dataVersion() != null && origin.dataVersion()!=null
                    && beanDTO.dataVersion()<origin.dataVersion()){
                throw CommonException.newInstance(ResponseCode.DB_UPDATE_VERSION_ISNOT_NEW)
                        .text(clazzDTO().getName(), ".", idvalue,
                                " 可能已经被他人修改, 请刷新, 传入的版本 ",
                                beanDTO.dataVersion(),
                                " 数据库的版本 ",
                                origin.dataVersion());
            }

            //beanDTO.mapModify() ==null, 说明是后台 new DTO()或者getObject()出来的对象, 这时只更新不为null的字段
            int updateModeTemp = (beanDTO.mapModify()==null && beanDTO.isChangeUpdateSql !=null && beanDTO.isChangeUpdateSql != true )? 1 :updateMode;
            updateModeTemp = (beanDTO.isChangeUpdateSql !=null && beanDTO.isChangeUpdateSql == true)?3:updateModeTemp;

            // 执行更新SQl
            String updateSQL = restSessionHandler.getEntitySql(clazzDTO()).getUpdateByIdSql(beanDTO, updateModeTemp);
            int one = baseMapper.update(updateSQL);
            if (one == 0 ) {
                updateNotResultError(beanDTO);
            }
            //DTO.mapSave中处理beanDTO.dataVersion(beanDTO.dataVersion() == null?null:(beanDTO.dataVersion()+1));
            result.set(result.get() + one);
        });

        //before 可能会修改list里面的DTO, 这里重新合并一次
        afterSave2(listUpdate, DbState.UPDATE);//执行更新之后的的方法

        return result.get();
    }

    /**
     * 自定更新条件, 不是根据Id更新时用此方法
     */
    @Override
    public List<DTO>  updateBatch(DTO dto, Condition where){
        //Id字段设置为null, 不支持修改的
        Map<String,Field> mapKeyFields = tableInfo().getFieldsByIds();
        if (mapKeyFields.size() > 1) {
            mapKeyFields.forEach((key, field) -> dto.setValue(key, null));
        }

        //sysConfig.isSelectByBeforeUpdate() 更新之前是否先查询

        List<DTO> listQuery =null;
        if (sysConfig.isSelectByBeforeUpdate()) {
            //查询出原始数据, 带缓存
            listQuery = listObjects(where);
            if (listQuery == null || listQuery.isEmpty()) {
                throw CommonException.newInstance(ResponseCode.DB_UPDATE_FIND_NO_DATA).text("查无需要更新的数据");
            }
        }
//        Map<String, DTO> mapOriginDTO = listQuery.stream().collect(
//                Collectors.toMap(DTO::dataId, Function.identity())); //原始数据转成 Map
        //这里逻辑 移动到 SQL语句执行后

        //更新之前
        //这个方法不能调用, 否则冲突, 如果用户在 before 事件更改了DTO的值, 那更新如何处理,总不能一条条更新,这样就失去了批量更新的意义了
        //beforeSave2(listUpdate, DbState.UPDATE);

        //执行批量更新SQL, 一句SQL
        int result = baseMapper.updateBatch(
                restSessionHandler.getEntitySql(clazzDTO())
                        .updateBatch(dto)
                        .setWhere(where)
                        .getSql()
        );//super.update(beanDTO, where);//本行不会考虑redis

        /** 要更新的数据 注意: listUpdate 里面始终只有一条记录 */
        List<DTO> listUpdate = new ArrayList<>(1);
        /** 合并数据 */
        //Map<String, DTO> mapMergeDTO = new HashMap<>(); //合并数据

        if (sysConfig.isSelectByBeforeUpdate()) {
            listQuery.forEach((queryDTO) -> {
                DTO newDTO = newInstanceDTO();
                newDTO.addOrigin(queryDTO);//set原始DTO
                BeanUtil.copyBean(queryDTO, newDTO); //合并1
                BeanUtil.map2Bean(dto.mapSave(), newDTO);
                newDTO.mapSave(dto.mapSave());
                newDTO.dataState(DbState.UPDATE);
                listUpdate.add(newDTO);
            });

            afterSave2(listUpdate, DbState.UPDATE);/** 执行更新之后的方法, bean是合并后数据 */
        }else{
            for (int i=0;i<result;i++){
                listUpdate.add(null);
            }
        }
        return listUpdate;
    }

    /**
     * deleteByIds() deleteBatch() deleteList()不经过此方法
     */
    @Override
    public int delete(DTO dto){
        List<DTO> list = new ArrayList<>(1);
        list.add(dto);
        return deleteList(list);
    }

    /**
     * 根据多个Id, 生成一句SQL删除, 无乐观锁
     * 先查询出来, 目的是得到每条记录的id, 然后根据Id删除redis缓存
     * @param ids
     * @return
     */
    @Override
    public int deleteByIds(String... ids){
        Set setIds = Misc.idsArray2Set(ids);
        List<DTO> list  = listObjects(setIds);//删除之前先查询出来, 带缓存
        if (ids != null && setIds.size() != list.size()){
            throw CommonException.newInstance2(ResponseCode.DB_DELETE_FIND_NO_DATA);
        }
        return deleteList(list, false);
    }

    @Override
    public int deleteByIds(Number... ids){
        if (tableInfo().getFieldsByIds().size()>1 ) {
            throw CommonException.newInstance(ResponseCode.SYSTEM_ERROR)
                    .text("多主键字段的表删除请使用 deleteBatch(condition)");
        }

        Set setIds = Misc.idsArray2Set(ids);
        List<DTO> list  = listObjects(setIds);//删除之前先查询出来, 带缓存
        if (ids != null && setIds.size() != list.size()){
            throw CommonException.newInstance2(ResponseCode.DB_DELETE_FIND_NO_DATA);
        }
        return deleteList(list, false);
    }

    /**
     * 主方法
     * 实现多个DTO批量一次删除, 不增加乐观锁
     * @param list
     * @return
     */
    @Override
    public int deleteList(List<DTO> list) {
        return deleteList(list, false);
    }

    /**
     * 自定义删除条件, 即不是根据主键的删除才用这个
     * 先查询出来, 目的是得到每条记录的id, 然后根据Id删除redis缓存
     * @param where
     * @return
     */
    @Override
    public List<DTO> deleteBatch(Condition where){
        List<DTO> list = listObjects(where); //删除之前先查询出来, 不带缓存
        deleteList(list,false);//执行删除逻辑, 执行一句删除SQL, Id 拼 Or
        return list;
    }

    /**
     * 内部方法, 禁止使用
     * @param list
     * @param isSaveMethod
     * @return
     */
    @Override
    public int deleteList(List<DTO> list, boolean isSaveMethod){
        if (list == null || list.isEmpty()) {return 0;}
        //beforeInit();

        if (isSaveMethod == false) {
            beforeSave2(list, DbState.DELETE);//执行删除之前的逻辑
        }

        Set setIds = list.stream().map(DataDTO::dataId).collect(Collectors.toSet());
        //            Object[] ids  = setIds.toArray(new Object[list.size()]);
        int result = baseMapper.delete(
            //生成删除SQL语句
            restSessionHandler.getEntitySql(clazzDTO()).getEntityDelete()
                    .deleteBatch()
                    .where().eq(tableInfo().getIdFieldsBySql(), setIds)
                    .getSql()
        );

        if (isSaveMethod == false) {
            afterSave2(list, DbState.DELETE);//执行删除之后的逻辑
        }
        return result;
    }

    @Override
    public boolean checkIdType(Object id){
        Misc.checkObjNotNull(id,"id");
        if (id instanceof String || id instanceof Integer || id instanceof Long){
            return true;
        }
        throw CommonException.newInstance(ResponseCode.SYS_KEY_FIELD_DATATYPE_ISINVALID)
                .text("id 字段的类型(",id.getClass().getName(),")不对, 目前仅仅支持 String Integer Long");
    }

    /**
     * 支持一次提交存在增删改的多行, 依据DTO中dataState()状态来识别增删改操作.
     * @see DbState
     * @param list
     */
    @Override
    public void save(List<DTO> list) {
        if (list == null) {return;}
        //所有新增的记录
        List<DTO> listInsert = list.stream().filter(beanDTO -> DbState.INSERT.equals(beanDTO.dataState())).collect(toList());
        //所有更新的记录
        List<DTO> listUpdate = list.stream().filter(beanDTO -> DbState.UPDATE.equals(beanDTO.dataState())).collect(toList());
        //所有删除的记录
        List<DTO> listDelete = list.stream().filter(beanDTO -> DbState.DELETE.equals(beanDTO.dataState())).collect(toList());

        insertList(listInsert);
        updateList(listUpdate);
        deleteList(listDelete);
    }

    @Override
    public Response<List<DTO>> query(DataQTO qto, String sqlId, String index){
        String basePath = getClass().getName().replace(".service.", ".dao.").replace("Service", "")+"Dao";
        String methodQuery = basePath + "."+sqlId+(index == null?"":index);
        String methodQueryCount = basePath + "."+sqlId+"Count"+(sqlId+index == null?"":index);

//        Integer maxPageSize = DataHelper.parseInt(environment.getProperty("top.maxPageSize"));
//        if (pageSize != null && pageSize  <=  0) qto.setPageSize(maxPageSize <= 0?20000:maxPageSize); //pageSize <= 0, 表示不分页查询时, 最多只查询20000条
        restSessionHandler.getEntitySql(clazzDTO()).getEntitySelect().initQTO(qto);
        //initQTO(qto);

        Integer pageSize = qto.getPageSize();
        Map<String,Object> maoQTO = BeanUtil.bean2Map(TableInfo.get(qto.getClass()), qto, false, true);
        List<DTO> list = sqlSessionTemplate.selectList(methodQuery, maoQTO);
        if (pageSize  <=  0){
            return new Response(list, list.size());
        }
        return new Response(list, sqlSessionTemplate.selectOne(methodQueryCount, maoQTO));
    }

    @Override
    public Response<List<DTO>> list(DataQTO qto){
        //优先执行 mapper.xml 对应的SQL sqlId = listObjects, 没有则类库自动生成当前表所有字段的查询SQL
        Response<List<DTO>> response;
        try {
            response = query(qto, "list", "");
        }catch(BindingException e){
            //dao文件没有对应的方法, 则 报错
            response = listPage(select().where().setQTO(qto).endWhere());
        }catch(MyBatisSystemException e){
            if(e.getMessage().indexOf("Mapped Statements collection does not contain value for")>0){
                //mapper.xml没有对应的sqlId, 则 报错
                response = listPage(select().where().setQTO(qto).endWhere());
            }else {
                throw e;
            }
        }

        return response;
    }

    @Override
    public DTO getObject(Number id) {
        return getObject(id, true);
    }
    @Override
    public DTO getObject(String id) {
        return getObject(id, true);
    }


    /**
     * @param id
     * @param isReadCache 是否要从一二级缓存中获取
     * @return
     */
    @Override
    public DTO getObject(Object id, boolean isReadCache) {
        checkIdType(id);//目前仅仅支持 String Integer Long

//        if (tableInfo().getFieldsByIds().size()>1 ) {
//            throw CommonException.newInstance(ResponseCode.SYSTEM_ERROR)
//                    .text("多主键字段查询请使用getObject(qto/condition)");
//        }
        //beforeInit();
        DTO beanDTO;
        if(isReadCache ) {//&& dataCache().readAndOpenCache()
            //该方法先去一级缓存(从当前线程)，再取二级缓存(Redis)
            beanDTO = dataCache().get2CacheById(clazzDTO(), id, sysConfig, "getObject");
            if (beanDTO  !=  null) {
                return beanDTO;
            }
        }

        //注释, 老方法 List<DTO> list = listObjects(where().eq(tableInfo().getIdFieldName(), id).openPage(false));

        //调用 List<DTO> listObjects(EntitySelect entitySelect)
        List<DTO> list = listObjects(
                select()
                        .where()
                        .eq(tableInfo().getIdFieldsBySql(), id)
                        .openPage(false)
                        .endWhere()
        );

        beanDTO = list.isEmpty() ? null : list.get(0);

        return beanDTO;
    }
    /**
     * 本方法会考虑缓存,  优先获取一级缓存中的数据, 但不会读取二级缓存
     * 获取到的数据会放入 一级二级缓存
     */
    @Override
    public List<DTO> listObjects(EntitySelect entitySelect){
        //跨 service容易出问题,这里纠正
        entitySelect.setTableInfo(tableInfo());

        //直接从数据库中查询出结果  entitySelect.getSql() 获得构建的SQL
        List<DTO> listQuery = baseMapper.listObjects(entitySelect.getSql());
        Condition condition = entitySelect.where();
        if (condition.readCache()==false || (condition.getQTO()!=null && condition.getQTO().readCache()==false)){
            return listQuery;//不读取缓存, 直接返回
        }

        List<DTO> listDTO =  new ArrayList<>();//定义返回的List对象
        listQuery.forEach((dto) -> {
            DTO cacheDTO=null;
            if (Misc.isNotNull(dto.dataId())){
                //根据Id从一级缓存中获取
                cacheDTO = dataCache().getCacheBySelect(clazzDTO(), dto.dataId(), sysConfig);
            }

            if (cacheDTO != null) {//一级缓存找到对象, 则以一级缓存的为准,作为返回
                listDTO.add(cacheDTO);
            }else {
                //fields为空, 默认返回所有字段, 所以可以更新缓存
                if(Misc.isNull(entitySelect.getSelect()) || entitySelect.isAppendAllFields()) {
                    //添加一级缓存, 二级缓存(考虑版本号)
                    dataCache().addCacheBySelected(dto);
                }
                listDTO.add(dto);
            }
        });

        return listDTO;
    }

    @Override
    public DTO getObject(DataQTO qto) {
        return getObject(where().setQTO(qto));
    }
    @Override
    public DTO getObject(Condition where) {
        List<DTO> list = listObjects(getEntitySelect(where));

        if (list.isEmpty()) {return null;}
        if (list.size()>1){
            logger.warn("{}getObject()获得了多条记录,默认返回第一条记录", sysConfigRead.getLogPrefix());
        }
        return list.get(0);
    }

    @Override
    public List<DTO> listObjects(String... ids) {
        return listObjects(Misc.idsArray2Set(ids), true);
    }
    @Override
    public List<DTO> listObjects(Number... ids) {
        return listObjects(Misc.idsArray2Set(ids), true);
    }

    @Override
    public List<DTO> listObjects(Set setIds) {
        return listObjects(setIds, true);
    }
    /**
     * 优先从一二级缓存中获取
     * @param ids 要查找的多个id值, 用英文逗号串起来
     * @param isReadCache 是否要从缓存中获取
     * @return
     */
    @Override
    public List<DTO> listObjects(Set ids, boolean isReadCache) {
        boolean isAllCache = true;//是否全部可以从缓存中得到

        //查询的多个Ids都从缓存中获取一次, 如果全部获取到, 就不用查数据库了
        List<DTO> listBeans = new ArrayList();
        if (isReadCache) {//从缓存中获取
            for (Object id : ids) {
                if (Misc.isNull(id)) {continue;}
                DTO beanDTO = dataCache().get2CacheById(clazzDTO(), id, sysConfig, "listObjects");
                if (beanDTO  !=  null) {
                    listBeans.add(beanDTO);
                } else {
                    isAllCache = false;
                    break;
                }
            }
        }
        if(isReadCache == true && isAllCache == true){//全部命中缓存
            if(ids.size()>1) {
                logger.debug("{}listObjects({})全部命中缓存", sysConfigRead.getLogPrefix(), ids.toString()); //多条 才 打印 全部命中缓存
            }
            return listBeans;
        }else{
            //缓存找不到记录, 则执行查询
            return listObjects(select()
                    .where()
                    .eq(tableInfo().getIdFieldsBySql(), ids)
                    .openPage(false)  //设置为不分页
                    .readCache(false) //设置为 不读取缓存, 因为找过一次缓存了
                    .endWhere()
            );
        }
    }

    @Override
    public List<DTO> listObjects(DataQTO qto) {
        //调用 List<DTO> listObjects(EntitySelect entitySelect)
        return listObjects(select().where().setQTO(qto).endWhere());//
    }

    /**
     * 自定义条件的查询. 不会从二级缓存Redis中获取
     * @param where
     * @return
     */
    @Override
    public List<DTO> listObjects(Condition where) {
        //调用 List<DTO> listObjects(EntitySelect entitySelect)
        //return listObjects(select().setWhere(where).endWhere());
        return listObjects(getEntitySelect(where));
    }

//    /**
//     * 自定义条件的查询. 不会从二级缓存Redis中获取
//     * @param fields
//     * @param where
//     * @return
//     */
//    @Deprecated @Override
//    public List<DTO> listObjects(String fields, Condition where){
//        //调用 List<DTO> listObjects(EntitySelect entitySelect)
//        return listObjects(select(fields, false).setWhere(where).endWhere());
//    }
//
//    /**
//     * 自定义条件的查询. 不会从二级缓存Redis中获取
//     *
//     * @param fields 指定查询返回的字段,多个用逗号串联, 即select 后的字段名
//     * @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
//     * @param where 条件匹配器Condition对象
//     * @return 返回 List< DTO >
//     *
//     */
//    @Deprecated @Override
//    public List<DTO> listObjects(String fields, Boolean isAppendAllFields, Condition where){
//        //调用 List<DTO> listObjects(EntitySelect entitySelect)
//        return listObjects(select(fields, isAppendAllFields).setWhere(where).endWhere());
//    }



    /**
     * 本方法会考虑缓存,  优先获取一级缓存中的数据
     * 单表分页自定义SQL查询, 将执行2条SQL语句
     * 与select搭配使用   select() 返回 EntitySelect 对象
     */
    @Override
    public Response<List<DTO>> listPage(EntitySelect entitySelect){
        //执行主sql, 获取指定分页的数据
        List<DTO> list = listObjects(entitySelect);
        //查询结果放入缓存
        list.forEach(dto-> dataCache().addCacheBySelected(dto));

        //执行符合条件的总记录数 SQL
        Integer count = selectCount(entitySelect.where());

        Response<List<DTO>> response= new Response(list, count);
        return response;
    }

    @Override
    public EntitySelect select(){
        return select(null, true);
    }
    @Override
    public EntitySelect select(String fields){
        return select(fields,false);
    }
    /**
     * 获得或者创建 EntitySelect对象
     * @param fields 指定查询返回的字段,多个用逗号串联, 为空时则返回所有DTO的字段. 支持数据库函数对字段处理,如: substring(name,2)
     * @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
     * @return
     */
    @Override
    public EntitySelect select(String fields,Boolean isAppendAllFields){
        return EntitySelect.create(tableInfo()).select(fields,isAppendAllFields);
    }
//
//    @Override
//    public DTO selectById(Object id) {
//        List<DTO> list = selectByIds(id);
//        return list==null|list.isEmpty()?null:list.get(0);
//    }
//
//    @Override
//    public List<DTO> selectByIds(Object... ids) {
//        return selectObjects(select().where().eq(tableInfo().getIdFieldName(),ids).openPage(false).endWhere());
//    }
//
//    @Override
//    public DTO selectOne(Condition where) {
//        List<DTO> list = selectObjects(where);
//        return list==null|list.isEmpty()?null:list.get(0);
//    }
//
//    @Override
//    public DTO selectOne(DataQTO qto) {
//        List<DTO> list = selectObjects(qto);
//        return list==null|list.isEmpty()?null:list.get(0);
//    }
//    @Override
//    public DTO selectOne(EntitySelect entitySelect){
//        List<DTO> list = selectObjects(entitySelect);
//        return list==null|list.isEmpty()?null:list.get(0);
//    }
//
//    @Override
//    public List<DTO> selectObjects(DataQTO qto) {
//        return baseMapper.selectForDTO(select().where().setQTO(qto).getSql());
//    }
//    @Override
//    public List<DTO> selectObjects(Condition where) {
//        return baseMapper.selectForDTO(getEntitySelect(where).getSql());
//    }
//    @Override
//    public List<DTO> selectObjects(EntitySelect entitySelect){
//        entitySelect.setTableInfo(tableInfo());// 跨 service容易出问题,这里纠正
//        return baseMapper.selectForDTO(entitySelect.getSql());
//    }

    @Override
    public List<Map<String, Object>> selectMaps(DataQTO qto) {
        return baseMapper.selectMaps(select().where().setQTO(qto).getSql());
    }
    @Override
    public List<Map<String, Object>> selectMaps(Condition where) {
        return baseMapper.selectMaps(getEntitySelect(where).getSql());
    }
    @Override
    public List<Map<String, Object>> selectMaps(EntitySelect entitySelect){
        entitySelect.setTableInfo(tableInfo());// 跨 service容易出问题,这里纠正
        return baseMapper.selectMaps(entitySelect.getSql());
    }

//    /**
//     * 不会考虑 读写缓存
//     * 单表分页自定义SQL查询, 将执行2条SQL
//     */
//    @Override
//    public Response<List<DTO>> selectPage(EntitySelect entitySelect){
//        //执行主sql, 获取指定分页的数据
//        List<DTO> list = baseMapper.selectObjects(entitySelect.getSql());
//        //执行符合条件的总记录数 SQL
//        Integer count= selectCount(entitySelect.where());
//
//        Response<List<DTO>> response= new Response(list, count);
//        return response;
//    }

    @Override
    public Response<List<Map<String, Object>>> selectPageMaps(EntitySelect entitySelect){
        //执行主sql, 获取指定分页的数据
        List<Map<String, Object>> list = selectMaps(entitySelect);
        //执行符合条件的总记录数 SQL
        Integer count= selectCount(entitySelect.where());

        Response<List<Map<String, Object>>> response= new Response(list, count);
        return response;
    }

    private EntitySelect getEntitySelect(Condition where){
        Object obj = null;
        try {
            obj = where.endWhere();
        }catch(CommonException e){
        }
        EntitySelect entitySelect;
        if (obj != null && obj instanceof EntitySelect){
            entitySelect = (EntitySelect)obj;
            entitySelect.setTableInfo(tableInfo());
            return entitySelect;
        }
        return select().setWhere(where).endWhere();
    }

    /**
     * 自定义条件的计数查询, 不会从缓存中获取
     * @param where
     * @return
     */
    @Override
    public int selectCount(Condition where){
        EntitySelect entitySelect = select();//.setWhere(where).endWhere();
        entitySelect.setWhere(where).openPage(false);

        return baseMapper.selectCount(entitySelect.getSelectCountSql());

//        List<Map<String, Object>> list = baseMapper.selectMaps(entitySelect.getSelectCountSql());
//        if (!list.isEmpty()){
//            return DataHelper.parseInt(list.get(0).get("count"));
//        }
//        return 0;
    }

    /**
     *
     * @param fieldName 指定的字段名
     * @param where 条件匹配器
     * @param <T> 返回类型的泛型
     * @return
     */
    @Override
    public <T> T  selectMax(String fieldName, Condition where){
        //beforeInit();
        List<Map<String, Object>> list = baseMapper.selectMaps(
                restSessionHandler.getEntitySql(clazzDTO())
                        .select("max("+fieldName+") maxData")
                        .setWhere(where)
                        .endWhere()
                        .openPage(false)
                        .getSql()
        );
        if (!list.isEmpty()){
            return list.get(0)==null?(T)new Integer(0):(T)list.get(0).get("maxData");
        }
        return null;
    }

//    @Deprecated //selectObjects 代替
//    @Override
//    public List<DTO> selectBatch(DataQTO qto) {
//        return baseMapper.selectForDTO(select().where().setQTO(qto).getSql());
//
//    }
//    @Deprecated //selectObjects 代替
//    @Override
//    public List<DTO> selectBatch(Condition where) {
//        return baseMapper.selectForDTO(getEntitySelect(where).getSql());
//    }

    /**
     * 创建数据校验类
     * @param bean
     * @return
     */
    public final CheckData checkData(IBean bean){
        List<IBean> list  = new ArrayList(1);
        list.add(bean);
        return checkData(list);
    }
    public final CheckData checkData(List list){
        CheckData<DataDTO> checkData = new CheckData(list, tableInfo(), this);
        return checkData;
    }

    public final KeyBuild keyBuild(){
        //用没有事务的 stringRedisTemplate,  支持一个线程中多次获得的序列号唯一
        return new KeyBuild(restSessionHandler.getStringRedisTemplate(),this);
    }
    public final KeyBuild keyBuild(String prefix, String dateFormat){
        return keyBuild().setPrefix(prefix).setDateFormat(dateFormat);
    }

    FormatConfig formatConfig;//格式化对象
    public FormatConfig formatConfig(){
        if (formatConfig == null){
            formatConfig = new FormatConfig();
        }
        return formatConfig;
    }
    public ChangeManager changeManager(DTO dto){
        ChangeManager changeManager = new ChangeManager(dto, formatConfig(), restSessionHandler.get()){};
        return changeManager;
    }
    public ChangeManager changeManager(DTO dto, FormatConfig formatConfig){
        ChangeManager changeManager = new ChangeManager(dto, formatConfig, restSessionHandler.get()){};
        return changeManager;
    }
}