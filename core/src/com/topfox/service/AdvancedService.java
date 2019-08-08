package com.topfox.service;

import com.topfox.common.*;
import com.topfox.data.DbState;
import com.topfox.mapper.BaseMapper;

import java.util.*;

public class AdvancedService<M extends BaseMapper<DTO>, DTO extends DataDTO>
        extends SimpleService<M, DTO>
        implements IAdvancedService< DTO>
{
    /**
     * 如果参数存在名为 dbState,  则请参考
     * @see com.topfox.data.DbState
     */


    /**
     * 新增|修改|删除 "之前" 执行
     * updateBatch除外的所有 insert update delete
     * @param list
     */
    @Override
    public void beforeSave(List<DTO> list) {
    }

    /**
     * 新增|修改 "之前" 执行
     * updateBatch除外的所有 insert update
     * @param list
     */
    @Override
    public void beforeInsertOrUpdate(List<DTO> list){
    }

    @Override
    public void beforeInsertOrUpdate(DTO beanDTO, String dbState){
    }

    /**
     * 删除 "之前" 执行
     * 例: delete() deleteByIds() deleteBatch() deleteList()
     * @param list
     */
    @Override
    public void beforeDelete(List<DTO> list){
    }
    @Override
    public void beforeDelete(DTO beanDTO){
    }

    /**
     * 新增|修改|删除 "之后" 执行
     * 例: 所有 insert update delete, 包含 updateBatch方法
     * @param list
     */
    @Override
    public void afterSave(List<DTO> list) {
    }

    /**
     * 新增|修改 "之后" 执行
     * 例: 所有 insert update, 包含 updateBatch方法
     * @param list
     */
    @Override
    public void afterInsertOrUpdate(List<DTO> list){
    }
    @Override
    public void afterInsertOrUpdate(DTO beanDTO, String dbState){
    }

    /**
     * 删除 "之后" 执行
     * 例: delete() deleteByIds() deleteBatch() deleteList()
     */
    @Override
    public void afterDelete(List<DTO> list){
    }
    @Override
    public void afterDelete(DTO beanDTO){
    }




    @Override
    public void beforeSave2(List<DTO> list, String dbState){
//        list.forEach((beanDTO) -> {
//            beanDTO.dataState(dbState);
//            if (DbState.INSERT.equals(dbState) && beanDTO.dataVersion()==null){
//                beanDTO.dataVersion(1);
//            }
//        });
//        if (fillDataHandler != null && (DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState))) {
//            /** 调用"填充接口实现类的方法", 主要是 创建和修改 人/时间 */
//            fillDataHandler.fillData(tableInfo.getFillFields(), (List<DataDTO>) list);
//        }

        super.beforeSave2(list,dbState);
        beforeSave(list);

        if (DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState)) {
            //执行插入或更新之前的的方法
            beforeInsertOrUpdate(list);
            list.forEach((beanDTO) -> beforeInsertOrUpdate(beanDTO, dbState));
        } else if (DbState.DELETE.equals(dbState)) {
            //执行删除之前的逻辑
            beforeDelete(list);
            list.forEach((beanDTO) -> beforeDelete(beanDTO));
        }
    }

    /**
     * 增 删 改 sql执行之后 调用本方法
     * @param list
     */
    @Override
    public final void afterSave2(List<DTO> list, String dbState){
        super.afterSave2(list, dbState);

        /**
         * 必须是合并过的Bean(数据库原始数据 + 需改的数据 的合并)
         */
        afterSave(list);
        if(DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState)){
            afterInsertOrUpdate(list);
            list.forEach((beanDTO) -> afterInsertOrUpdate(beanDTO, dbState));
        }else if(DbState.DELETE.equals(dbState)){
            afterDelete(list);
            list.forEach((beanDTO) -> afterDelete(beanDTO));
        }
    }

}
