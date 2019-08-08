package com.topfox.service;

import com.topfox.common.DataDTO;

import java.util.List;

public interface IAdvancedService<DTO extends DataDTO> extends IService<DTO>{//

    void beforeSave(List<DTO> list) ;
    void beforeInsertOrUpdate(List<DTO> list);
    void beforeInsertOrUpdate(DTO beanDTO, String dbState);
    void beforeDelete(List<DTO> list);
    void beforeDelete(DTO beanDTO);

    void afterSave(List<DTO> list) ;
    void afterInsertOrUpdate(DTO beanDTO, String dbState);
    void afterInsertOrUpdate(List<DTO> list);
    void afterDelete(List<DTO> list);
    void afterDelete(DTO beanDTO);

}
