package com.topfox.data;

/**
 *
 * 主要解决复杂的企业内部系统, 一次提交多条数据且同时存在增删改的情况时, 用于描述DTO的状态,
 * 结合service.save()方法使用时, TopFox将根据这个状态值自动生成相对应的SQL(insert/update/delete)语句.
 * 提交的数据状态为"无状态"时, 自动忽略.
 *
 * DTO的状态值定义
 * i 新增
 * u 更新
 * d 删除
 * n 无状态
 */
public class DbState {
    /**
     * 新增
     */
    public static final String INSERT="i";
    /**
     * 删除
     */
    public static final String DELETE="d";
    /**
     * 修改
     */
    public static final String UPDATE="u";

    /**
     * 无状态
     */
    public static final String NONE  ="n";
}