package com.topfox.common;

import com.topfox.annotation.Ignore;
import com.topfox.misc.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 基础QTO
 */
@Accessors(chain = true)
public class DataQTO implements IBean, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 允许返回null
     */
    @Getter @Setter
    private Boolean allowNull;// = true;

    /**
     * 是否读取 一级 二级缓存的数据,  null 和true都会读取,  false不读取
     */
    @Ignore transient
    private boolean readCache=true;
    public DataQTO readCache(boolean value){
        readCache = value;
        return this;
    }
    public boolean readCache(){
        return readCache;
    }

    /**
     * 查询页码
     */
    @Getter @Setter
    private Integer pageIndex=0;

    /**
     * 每页条数
     */
    @Getter @Setter
    private Integer pageSize;


    /**
     * 类库专用字段. 类库根据 pageIndex 和 pageSize写值
     */
    @Getter @Setter
    private String limit;

    /**
     * 排序字段, 多个字段用逗号串起来
     */
    @Getter @Setter
    private String orderBy;

    /**
     * 分组字段
     */
    @Getter @Setter
    private String groupBy;

    @Getter @Setter
    private String having;

    @Override
    public String toString(){
        return JsonUtil.toString(this);
    }

}
