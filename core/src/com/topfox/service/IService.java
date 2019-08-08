package com.topfox.service;


import com.topfox.common.*;
import com.topfox.util.DataCache;
import com.topfox.sql.Condition;
import com.topfox.sql.EntitySelect;
import java.util.*;

public interface IService<DTO extends DataDTO> extends ISuperService {
    Condition where();
    DataCache dataCache();

    void beforeSave2(List<DTO> list, String dbState);
    void afterSave2(List<DTO> list, String dbState);

    int insert(DTO dto);
    long insertGetKey(DTO dto);
    int insertList(List<DTO> list);

    void initUpdateDTO(List<DataDTO> listUpdate);
    int update(DTO dto);
    int update(DTO dto, int updateMode);
    int updateList(List<DTO> list);
    int updateList(List<DTO> listUpdate, int updateMode);
    List<DTO> updateBatch(DTO dto, Condition where);

    int delete(DTO dto);
    int deleteByIds(String... ids);
    int deleteByIds(Number... ids);
    int deleteList(List<DTO> list);
    int deleteList(List<DTO> list, boolean isSaveMethod);
    List<DTO> deleteBatch(Condition where);

    void save(List<DTO> list) ;

    boolean checkIdType(Object id);
    Response<List<DTO>> query(DataQTO qto, String sqlId, String index);
    Response<List<DTO>> list(DataQTO qto);

    DTO getObject(Number id) ;
    DTO getObject(String id) ;
    DTO getObject(Object id, boolean isCache) ;
    DTO getObject(DataQTO qto) ;
    DTO getObject(Condition where) ;

    List<DTO> listObjects(String... ids) ;
    List<DTO> listObjects(Number... ids) ;
    List<DTO> listObjects(Set setIds) ;
    List<DTO> listObjects(Set ids, boolean isCache) ;


    List<DTO> listObjects(DataQTO qto) ;
    List<DTO> listObjects(Condition where) ;
//    @Deprecated
//    List<DTO> listObjects(String fields, Condition where);
//    @Deprecated
//    List<DTO> listObjects(String fields, Boolean isAppendAllFields, Condition where);
    List<DTO> listObjects(EntitySelect entitySelect);


    Response<List<DTO>> listPage(EntitySelect entitySelect);

    EntitySelect select();
    EntitySelect select(String fields);
    EntitySelect select(String fields,Boolean isAppendAllFields);

//    DTO selectById(Object id);
//    List<DTO> selectByIds(Object ... ids);
//
//    DTO selectOne(DataQTO qto);
//    DTO selectOne(Condition where);
//    DTO selectOne(EntitySelect entitySelect);
//    List<DTO> selectObjects(DataQTO qto);
//    List<DTO> selectObjects(Condition where);
//    List<DTO> selectObjects(EntitySelect entitySelect);

    List<Map<String, Object>> selectMaps(DataQTO qto) ;
    List<Map<String, Object>> selectMaps(Condition where) ;
    List<Map<String, Object>> selectMaps(EntitySelect entitySelect);

    //Response<List<DTO>> selectPage(EntitySelect entitySelect);

    Response<List<Map<String, Object>>> selectPageMaps(EntitySelect entitySelect);

    int selectCount(Condition where);
    <T> T selectMax(String fieldName, Condition where);

//    @Deprecated
//    List<DTO> selectBatch(DataQTO qto) ;
//    @Deprecated
//    List<DTO> selectBatch(Condition where);
}