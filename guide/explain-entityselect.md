# 1. [↖回到主目录](https://github.com/topfoxs/topfox)

# 2. 实体查询构造器 EntitySelect 应用例子
参考  章节<核心使用>  listObjects(EntitySelect entitySelect) 中的例子

# 3. 实体查询构造器 EntitySelect
 实体查询构造器, 它集成了 Condition和设置查询的排序和分组字段, 分页等功能的综合查询对象

## 3.1. create()
- 静态方法
- return new EntitySelect()

## 3.2. create(Class<?> clazz)
- 静态方法
- return new EntitySelect()
- 并设置DTO的 clazz, 生成SQL语句要用

## 3.3. create(TableInfo tableInfo)
- 静态方法
- return new EntitySelect()
- 并设置 TableInfo对象,  生成SQL语句要用 生成SQL语句要用

## 3.4. where() 
- return Condition.create()
- 新建一个条件匹配器

## 3.5. setWhere(Condition where)
- 设置一个已经存在的条件匹配器对象

## 3.6. select()
```java
public EntitySelect select() {
    return select(null,true);
}
```
因此请参考 select(String fields,Boolean isAppendAllFields)

## 3.7. select(String fields) 
```java
public EntitySelect select(String fields) {
    return select(fields,true);
}
```
因此请参考 select(String fields,Boolean isAppendAllFields)

## 3.8. select(String fields,Boolean isAppendAllFields)
请查看源码中参数的解释:

```java
 /**
 * 获得或者创建 EntitySelect对象
 * 请注意下面两个参数的解释
 * @param fields 指定查询返回的字段,多个用逗号串联, 为空时则返回所有DTO的字段. 支持数据库函数对字段处理,如: substring(name,2)
 * @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
 * @return
 */
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

```
 
## 3.9. getSelect() 
返回  select(String fields)  中参数 fields 的值

## 3.10. isAppendAllFields() 
返回  select(String fields,Boolean isAppendAllFields) 中参数 isAppendAllFields 的值

## 3.11. orderBy(String fields)
- 指定查询排序的字段,  多个用逗号隔开
- 如:   entitySelect.orderBy("name asc, sex desc")

## 3.12. groupBy(String fields)
- 指定查询分组的字段,  多个用逗号隔开
- 如:   entitySelect.orderBy("isAdmin, sex")

## 3.13. having(String fields) 
与groupBy 配合使用, 略

## 3.14. setPageSize(Integer pageSize)
设置返回的行数

## 3.15. setPage(Integer pageIndex, Integer pageSize)
设置查询第几页, 和返回的行数

## 3.16. openPage(boolean value)
- 相当于 setOpenPage(boolean value)
- 开启分页, 默认值 开启 true, 如果关闭后sql语句后 始终不生成 limit x,y
- 例如根据Id 字段的查询 getObject() , 就应该关闭 这个.

## 3.17. openPage()
相当于 getOpenPage

## 3.18. getSelectCountSql()
构建 分页要获得总记录数的 countsql

## 3.19. getSql()
构建 查询SQL

## 3.20. initQTO(DataQTO qto)
处理QTO增强的功能等, 如  OrEq AndNe OrLike AndNotLike增强的处理等


