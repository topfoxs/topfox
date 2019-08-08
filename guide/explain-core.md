 
# 1. [↖回到主目录](https://github.com/luoping2019/topfox)

## 1.1. 核心使用
包括:

- TopFox注解
- DataDTO
- DataQTO
- SimpleService
- AdvancedService
- MultiService

# 2. TopFox注解
以下注解主要用在 DTO QTO VO BO 等POJO对象中

源代码包路径 com.topfox.annotation

- @Table 实体类的注解, 属性如下:
    1. name 实体对应在数据库的表名
    2. cnName 自定 中文表名, 抛出异常用, sevice中通过 tableInfo().getTableCnName() 获取
    3. redisKey 缓存在redis中的别名
- @Id 主键字段注解, 当字段名为id时可不注解,类库将自动识别
- @Version 版本号字段注解,用于实现乐观锁.当字段名为version时可不注解,类库自动识别.
- @TableField 字段注解
    1. exist 是否数据库存在的字段, 默认true
    2. name 数据库的字段名注解,生成插入更新的SQL语句的字段名用这个属性的值;此注解解析到 com.topfox.data.Field 对象的 dbName属性
    3. fillUpdate 是否为更新填充的字段, 默认false
    4. fillInsert 是否为插入填充的字段, 默认false
    5. incremental 生成更新SQL 加减  值:addition +  / subtract 默认为Incremental.NONE(枚举对象)
- @State 状态字段, 主要解决复杂的企业内部系统, 一次提交多条数据且同时存在增删改的情况时, 用于描述DTO的状态,结合service.save()方法使用时,TopFox将根据这个状态值自动生成相对应的SQL(insert/update/delete)语句.提交的数据状态为"无状态"时, 自动忽略. 例子可参考<<常见问题>>章节中的
"增删改一个方法实现 service.save " .

状态字段值的枚举类源码如下(注意,不是枚举对象):

```java
package com.topfox.data;
public class DbState {
    public static final String INSERT="i"; //新增
    public static final String DELETE="d"; //删除
    public static final String UPDATE="u"; //更新
    public static final String NONE  ="n"; //无状态
}
```

# 3. 实体对象的超类 DataDTO 
所有业务的实体对象继承该对象, 源码路径 com.topfox.common.DataDTO

用户DTO实体的例子:

```java
//用户表实体
@Setter
@Getter
@Table(name = "SecUser")//数据库的表名注解
public class UserDTO extends DataDTO {
    @State //状态字段注解
    @TableField(exist = false) //状态字段在数据库是不存在的
    private transient String state = "n";
    
    @Id //主键字段的注解
    private String userId;
    
    @Version //版本号字段注解 乐观锁用
    private Integer userVersion;
    
    private String name;
    private String password;
    
    //递增注解, 更新时始终在该字段更新前的值的基础上加上传入的值
    @TableField(name="loginCount", incremental = Incremental.ADDITION)
    @JsonIgnore
    private  transient Integer loginCountAdd;

    private Integer loginCount;

}
```

# 4. 功能强大的查询
- 查询的sql 是配置在 mapper.xml中的,  则可以qto传递查询参数
- 分布式下服务调用服务查询, 也可以qto传递查询参数
- 利用qto查询, 如果是TopFox自动生成的sql, 参数都是 and like 查询, 不是很灵活. 部分需求通过"QTO后缀增强查询"提高灵活性
- 更灵活的查询可参考《条件匹配器Condition》章节中的后面完整例子.  条件匹配器只支持自动生成的sql的查询 
- 更灵活的查询可参考《实体查询构造器》 章节
- 查询缓存请参考《高级应用》相关的描述

## 4.1. DataQTO
所有业务查询的对象都应该继承DataQTO,  源码路径com.topfox.common.DataQTO, 定义了如下通用查询参数:

- setReadCache 设置是否读取缓存(一二级缓存).  请参考 《高级应用》缓存相关的描述
- setPageIndex 设置 查询页码
- setPageSize 设置 每页条数. 这个参数设置为 0或-1时, 查询返回条数将会获取《TopFox配置参数 》章节中 top.max-page-size的值
- setOrderBy 设置 排序字段, 多个字段用逗号串起来
- setgroupBy 设置 分组字段, 多个字段用逗号串起来
- setHaving 设置 having

## 4.2. QTO后缀增强查询
假定用户表有3个字段:
- 用户编码 id
- 用户姓名 name
- 年龄    age

我们新建一个UserQTO 源码如下:

```java 
@Setter
@Getter
@Table(name = "SecUser")
public class UserQTO extends DataQTO {
    private String id;            //用户id, 与数据字段名一样的
    private String age;           //年龄age,与数据字段名一样的

    private String name;          //用户姓名name, 与数据字段名一样的
    private String nameOrEq;      //用户姓名 后缀OrEq
    private String nameAndNe;     //用户姓名 后缀AndNe
    private String nameOrLike;    //用户姓名 后缀OrLike
    private String nameAndNotLike;//用户姓名 后缀AndNotLike
}
```

- 字段名 后缀OrEq
当 nameOrEq 写值为 "张三,李四" 时, 源码如下:

```java
package com.test.service;

/**
 * 核心使用 demo1 源码
 */
@Service
public class CoreService extends AdvancedService<UserDao, UserDTO> {
    public List<UserDTO> demo1(){
        UserQTO userQTO = new UserQTO();
        userQTO.setNameOrEq("张三,李四");//这里赋值
        //依据QTO查询 listObjects会自动生成SQL, 不用配置 xxxMapper.xml
        List<UserDTO> listUsers = listObjects(userQTO);
        return listUsers;
    }
}
```
则生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name = '张三' OR name = '李四')
```

- 字段名 后缀AndNe
当 nameAndNe 写值为 "张三,李四" 时, 则生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name <> '张三' AND name <> '李四')
```
- 字段名 后缀OrLike
当 nameOrLike 写值为 "张三,李四" 时, 则将生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name LIKE CONCAT('%','张三','%') OR name LIKE CONCAT('%','李四','%'))
```

- 字段名 后缀AndNotLike
当 nameAndNotLike 写值为 "张三,李四" 时, 则生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name NOT LIKE CONCAT('%','张三','%') AND name NOT LIKE CONCAT('%','李四','%'))     
```

以上例子是TopFox全自动生成的SQL
如果执行的是MyBaties xxxMapper.xml的SQL, 则条件部分配置如下:

```xml
<!-- 注意是 $ 而不是 # -->
<sql id="whereClause">
    <where>
         <if test = "nameOrEq != null">AND ${nameOrEq}</if>
         <if test = "nameAndNe != null">AND ${nameAndNe}</if>
         <if test = "nameOrLike != null">AND ${nameOrLike}</if>
         <if test = "nameAndNotLike != null">AND ${nameAndNotLike}</if>
    </where>
</sql>
<select id="list" resultMap="list">
    SELECT id, name, ...
    FROM SecUser a
    <include refid="whereClause"/>
    <if test="limit != null">${limit}</if>
</select>
<select id="listCount" resultType="int">
    SELECT count(*)
    FROM SecUser a
    <include refid="whereClause"/>
</select>
```
以上的配置, 类库将实现以下"后缀增强查询"功能

# 5. SimpleService类 新增/修改/删除
SimpleService.java 的路径是 com.topfox.service.SimpleService

部分源码:

```java
public class SimpleService<QTO extends DataQTO, DTO extends DataDTO> {
    //系统可变参数对象, 修改的值只对当前Service有效, 继承的子类可以直接使用这个对象
    protected SysConfig sysConfig; 
    
    //这是一个创建条件构造器的方法, 后面的例子会用到
    public Condition where() {
        return Condition.create();
    }
    
    ...
}
```
以下方法均在 SimpleService 类中已经实现, 直接使用即可.

## 5.1. init()
初始化的方法, 在其他类(Service或者Controller)调用Service的方法前, 首先会执行本方法.
以用户表为例:
```java
@Service("userService")
public class UserService extends SimpleService<UserDao, UserDTO> {    
    @Override
    protected void init(){
        super.init();
        //设置当前Service对应的DTO不启用Redis缓存. 不是必须写的, 这个参数默认为true
        sysConfig.setRedisCache(false);
    }
}
```

## 5.2. insert(DTO xxxDTO)
- 基本方法:  插入一条记录
- @param xxxDTO 实体对象
- @return 插入成功记录数

## 5.3. insertList(List< DTO > list)
- 插入多条记录
- @param list 实体List对象
- @return 插入成功记录数

## 5.4. insertGetKey(DTO xxxDTO)
- 当数据库的主键字段设置为自增时, 才调用这个方法插入.
- @return 返回数据库自增的Id值(注意,不是插入成功的记录数)

## 5.5. update(DTO xxxDTO)
- 更新一条记录,如果版本号字段有值, 将加上乐观锁的SQL
- @param xxxDTO 实体对象
- @return 更新成功记录数

## 5.6. updateList(List< DTO > list)
类库已经实现,直接使用即可. 没有特殊情况不要Override这个方法

## 5.7. updateBatch(DTO xxxDTO, Condition where)
- @param xxxDTO 要更新的数据, 不为空的字段才会更新. Id字段不能传值
- @param where  条件匹配器
- @return List< DTO >更新的dto集合

用条件匹配器生成SQL条件更新, 影响到多条记录时用此方法 
解决多条记录要更新为同样的值(数据载体xxxDTO), 只生成一条SQL时用此方法

>技巧:
>如何将某些字段更新为null值呢? 代码如下, 更新之前这样写:
xxxDTO.addNullfields("字段名,多个逗号隔开");

完整一点的源码:

```java
    UserDTO userDTO = new UserDTO();
    userDTO.setAge(30);
    userDTO.addNullfields("mobile,deptId");

    //把性别=男的记录 age更新为30, mobile和deptId更新为null
    updateBatch(userDTO, where().eq("sex","男"));
```

重要申明:

- updateBatch 更新所影响的记录, 是不会调用<< AdvancedService类 前置后置方法 >>章节中所描述的前置方法, 但是会调用后置方法. 

当 << TopFox配置参数  >> 章节中的2个参数配置为

```
 top.service.select-by-before-update=false
 top.service.update-mode=1
```
时, 所有前置和后置方法都不会调用---因为更新前不查询, 无法获得更新记录的dto数据, 而前置后置方法的参数需要dto数据. 且返回值list<dto>中的值是null, list.size为正确值

## 5.8. delete(DTO xxxDTO)
- 删除一条记录, 如果版本号字段有值, 将加上乐观锁的SQL
- @param xxxDTO 实体对象
- @return 删除成功记录数

## 5.9. deleteByIds(Object... ids)
- 类库已经实现,直接使用即可. 根据传入主键Id值删除记录
- 主键值只支持字符串和整型
- 返回删除的记录数

## 5.10. deleteList(List< DTO > list)
- 类库已经实现,直接使用即可. 根据list每个DTO的Id值删除记录.
- 返回删除的记录数

## 5.11. deleteBatch(Condition where)
用条件匹配器生成SQL条件去删除, 影响到多条记录时用此方法

## 5.12. save(List<DTO> list)
新增, 修改, 删除的数据都可以调用这个方法

请参考<<常见问题>>章节中的 "增删改一个方法实现 xxxService.save "


## 5.13. list(QTO xxxQTO)
- @param xxxQTO 查询的条件对象
- @return 返回结果 Response< List< DTO>>

优先查找 xxxMapple.xml里面 < select id = "list"> 的SQL, 如果没有, 则类库会调用 listPage 根据DTO自动生成SQL查询. 
<br>如果xxxMapple.xml中定义的查询将不会读取一级和二级缓存
<br>xxxMapple.xml没有定义, 自动调用 listPage查询时, 将会读取一级缓存.

## 5.14. listCount(QTO xxxQTO)
- @param xxxQTO 查询的条件对象
- @return 返回总行数

与list(QTO xxxQTO)配套的分页用总行数查询.  调用 list(QTO xxxQTO) 时,本方法会自动执行.

# 6. SimpleService类 查询getObject/listObjects
```
一二级缓存的概念
* 一级缓存是指当前线程中的缓存数据
* 二级缓存是指redis缓存, 是跨线程, 且跨tomcat实例的缓存
```

::: 重要概念

```
getObject/listObjects 命名的"入参有id"的查询方法, 获取实体对象DTO的优先级顺序是:
1. 先从一级缓存中获取, 有则直接返回
2. 一级缓存没有再从二级缓存Redis中获取, 有则返回;
3. 二级缓存也没有, 则生成SQL语句从数据库中获取, 并放入一级和二级缓存中
```

## 6.1. getObject(String/Number id)
根据Id值获得一个DTO实体对象, 优先从一二级缓存中获取 

## 6.2. getObject(String/Number id, boolean isReadCache)
- @param id 传入的Id, 类型只支持 String Number(Integer Long的父类)
- @param isReadCache 是否从缓存中获取
    1. true 优先从一二级缓存中获取 
    2. false 只执行SQL查询获取
- @return 返回DTO实体对象 

## 6.3. listObjects(String/Number... ids)
- @param ids 传入的多个Id, 类型只支持 String Integer Long
- @return 返回 List< DTO >

根据多个Id值获得对象, 优先从一二级缓存中获取 

## 6.4. listObjects(String/Number... ids, boolean isReadCache)
- @param ids 传入的多个Id值, 类型只支持 String Integer Long
- @param isReadCache 是否从一二级缓存中获取
- @return 返回 List< DTO >

## 6.5. listObjects(Set<String> setIds) 
- @param setIds 传入的多个Id值, 类型只支持 String Integer Long
- @return 返回 List< DTO >

优先从一二级缓存中获取 

## 6.6. listObjects(Set<String> ids, boolean isReadCache) {
- @param setIds 传入的多个Id值, 类型只支持 String Integer Long
- @param isReadCache 是否从一二级缓存中获取
- @return 返回 List< DTO >

## 6.7. listObjects(QTO qto)
- @param qto 依据QTO生成WHERE条件的SQL, QTO字段间用 and连接
- @return 返回 List< DTO >

优先从一级缓存中获取, 不读取二级缓存

```java
//例3: 通过QTO 设置不读取缓存
//具体可参考<<高级应用>>章节中 "查询时如何才能不读取缓存"的描述
list = userService.listObjects(
    userQTO.readCache(false) //禁用从缓存读取(注意不是读写) readCache 设置为 false, 返回自己(QTO)
);
```
## 6.8. listObjects(Condition where)
- @param where 条件匹配器, 可参考后面的<条件匹配器Condition的用法>
- @return 返回 List< DTO >

> 优先从一级缓存中获取, 不读取二级缓存
> 返回DTO实体的所有字段的值
> 自定义条件的查询, 使用条件匹配器Condition生成SQL条件, 功能最强大的查询, > 可以生成任何复杂的条件. 不会从缓存中获取.

```java
//例4: 通过条件匹配器Condition 设置不读取缓存
//具体可参考<<高级应用>>章节中 "查询时如何才能不读取缓存"的描述
list = userService.listObjects(
    Condition.create()     //创建条件匹配器
        .readCache(false)  //禁用从缓存读取
);
```

## 6.9. 【new 必读】listObjects(EntitySelect entitySelect)
- @param EntitySelect 实体查询构造器
- @return 返回 List < DTO > 

优先从一级缓存中获取, 不读取二级缓存

::: EntitySelect 实体查询构造器: 

 - 它集成了 Condition 
 - 设置SQL setPage(Integer pageIndex, Integer pageSize),  orderBy groupBy having的功能
 - 并产生sql语句, 方法 getSql  getSelectCountSql

在Topfox类库 SimpleService.java中, 提供了3个 select方法, 源码如下:

```java
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
     * 请注意下面两个参数的解释
     * @param fields 指定查询返回的字段,多个用逗号串联, 为空时则返回所有DTO的字段. 支持数据库函数对字段处理,如: substring(name,2)
     * @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
     * @return
     */
    @Override
    public EntitySelect select(String fields,Boolean isAppendAllFields){
        return EntitySelect.create(tableInfo).select(fields,isAppendAllFields);
    }
```

::: 实际使用例子:

```java
/**
 * 核心使用 demo2 demo3源码
 */
@Service
public class CoreService extends SimpleService<UserDao, UserDTO> {
    public List<UserDTO> demo2(){
        List<UserDTO> listUsers=listObjects(
                select("sex, count('*')") //通过调用SimpleService.select() 获得或创建一个新的 EntitySelect 对象,并返回它
                        .where()         //创建一个新 Condition对象,并返回它
                        .eq("sex","男")  //条件匹配器自定义条件 返回对象 Condition
                        .endWhere()      //条件结束           返回对象 EntitySelect
                        .orderBy("name") //设置排序的字段      返回对象 EntitySelect
                        .groupBy("name") //设置分组的字段      返回对象 EntitySelect
                        .setPage(1,5)    //设置分页           返回对象 EntitySelect

        );
        return listUsers;
    }

   public List<UserDTO> demo3(){
        List<UserDTO> list1 = listObjects(select()   //select 无参数
                .where().eq("sex","男")
        );

        List<UserDTO> list2 = listObjects(select("id,name,sex") //select 一个参数
                .where().eq("sex","男")
        );

        List<UserDTO> list3 = listObjects(select("count(1)", true)//select 两个参数
                .where().eq("sex","男")
        );
//        return list1;
//        return list2;
        return list3;
    }

}
```

::: 备注

- 不会从Redis缓存中获取
- 自定义条件的查询, 设置分页pageInde pageSize,  排序orderBy 分组groupBy having,  可参考 listObjectsByPage方法中的例子
- 特别说明, 以上所有的 getObject/listObject本质都是调用的当前方法. 下面
- 把类库的源码贴出来:

```java
/**
 * 本方法会考虑缓存,  优先获取一级缓存中的数据
 */
public List<DTO> listObjects(EntitySelect entitySelect){
    //直接从数据库中查询出结果  entitySelect.getSql() 获得构建的SQL
    List<DTO> listQuery =baseDao.selectForDTO(entitySelect.getSql());

    List<DTO> listDTO =  new ArrayList<>();//定义返回的List对象
    if (listQuery.isEmpty()){
        return listDTO;
    }

    listQuery.forEach((dto) -> {
        DTO cacheDTO=null;
        if (Misc.isNotNull(dto.dataId())){
            //根据Id从一级缓存中获取
            cacheDTO = restSessionHandler.getDataCache().getCacheBySelect(clazz, dto.dataId());
        }

        if (cacheDTO != null) {//一级缓存找到对象, 则以一级缓存的为准,作为返回
            listDTO.add(cacheDTO);
        }else {
            //fields为空, 默认返回所有字段, 所以可以更新缓存
            if(Misc.isNull(entitySelect.getSelect()) || entitySelect.isAppendAllFields()) {
                //添加一级缓存, 二级缓存(考虑版本号)
                restSessionHandler.getDataCache().addCacheBySelected(dto, sysConfig.isOpenRedis());
            }
            listDTO.add(dto);
        }
    });

    return listDTO;
}
```

> 从源码我们可以看出来, 当从数据库查询出的结果当中, 如果某个DTO在一级缓存中已经存在, 则会舍去从数据库查询的DTO, 以一级缓存的DTO为准. 

## 6.10. 【new】listPage(EntitySelect entitySelect)
- @param EntitySelect 实体查询构造器
- @return 返回 Response< List < DTO > >

优先从一级缓存中获取, 不读取二级缓存

::: 当前方法 topfox 实现的源码:

```java
public Response<List<DTO>> listObjectsByPage(EntitySelect entitySelect){
        //执行主sql, 获取指定分页的数据
        List<DTO> list = listObjects(entitySelect);
        //执行符合条件的总记录数 SQL
        Integer count = selectCount(entitySelect.where());

        Response<List<DTO>> response= new Response(list, count);
        return response;
    }
```

::: 从源码看, 有两个查询

- 1. 执行主sql , 获取指定分页的数据
- 2. 执行和主sql条件一样的 countSql 获得 总记录数

::: 实际例子:

```java
/**
 * 核心使用 demo4 源码
 */
@Service
public class CoreService extends SimpleService<UserDao, UserDTO> {
    public Response<List<UserDTO>>  demo4(){
        Response<List<UserDTO>> response=listPage(
                select("sex,max(age)") //通过调用getEntitySelect()获得或创建一个新的 EntitySelect对象,并返回它
                        .where()       //创建一个新 Condition对象,并返回它
                        .eq("sex","男")//返回对象 Condition
                        .endWhere()    //返回对象 EntitySelect
                        .setPage(3,100)//返回对象 EntitySelect
                        .groupBy("sex")//返回对象 EntitySelect
                        .orderBy("sex")//返回对象 EntitySelect
        );
        //返回对象response说明:
        response.getData();      // 主数据
        response.getTotalCount();//总记录数
        response.getPageCount(); //当前页记录数:
        return response;
    }
}

```

:::  listObjectsByPage方法执行2句SQL,  控制台输出如下:
 
```sql
## 执行主sql , 获取指定分页的数据
14:39|09.642 [52-1] DEBUG  34-c.topfox.conf.MyBatisInterceptor  #com.user.com.sys.dao.UserDao.selectForDTO
SELECT sex,max(age)
FROM SecUser a
WHERE (sex = '男')
GROUP BY sex
ORDER BY sex
LIMIT 200,100

## 执行和主sql查询条件一样的总记录数的 countsql
14:39|09.650 [52-1] DEBUG  34-c.topfox.conf.MyBatisInterceptor  #com.user.com.sys.dao.UserDao.selectBySql
SELECT count(1) 
FROM (
    select 1 FROM SecUser
    WHERE (sex = '男')
    GROUP BY sex
) TEMP
```

# 7. SimpleService类 select开头的方法
与listObjects/getObject的区别是:
- 不会读写一二级缓存.
- 不会对查询结果遍历, 性能比listObjects getObject高.
- listObjects 本质也是调用selectBatch或者直接从数据库获得结果的

## 7.1. 【new】selectMaps(DataQTO qto)
- @param qto
- 返回 List< Map< String, Object >> 

## 7.2. 【new】selectMaps(Condition where)
- @param where 条件匹配器Condition对象
- 返回 List< Map< String, Object >> 

## 7.3. 【new】selectMaps(EntitySelect entitySelect)
- @param EntitySelect 实体查询构造器
- 返回 List< Map< String, Object >> 

## 7.4. 【new】selectPage(EntitySelect entitySelect)
- @param EntitySelect 实体查询构造器
- @return 返回 Response< List < DTO > >

执行2句SQL, 与 listObjectsByPage唯一区别是不会"读写"一二级缓存

selectPageMaps 与 selectPage区别:Response中的对象不一样, 一个是  list< DTO > , 一个是 List< Map >

## 7.5. 【new】selectPageMaps(EntitySelect entitySelect)
- @param EntitySelect 实体查询构造器
- @return 返回 Response< List< Map< String, Object > > >

执行2句SQL


## 7.6. selectCount(Condition where)
- @param where
- @return 查询结果的行数

自定义条件的计数查询, 不会从缓存中获取

## 7.7. selectMax(String fieldName, Condition where)
自定义条件, 获得指定字段名的最大值, 字段类型支持任何类型. 
源码:

```java
/**
 *
 * 注意,返回类型时泛型哦
 * @param fieldName 指定的字段名
 * @param where 条件匹配器
 * @param <T> 返回类型的泛型
 * @return
 */
public <T> T  selectMax(String fieldName, Condition where){
    ...源码略...
}
```

奇妙的用法:

```java
//这样写则将返回的结果转为 String型
String maxString = selectMax("字段名", where()...);

//这样写则将返回的结果转为 Long型
Long maxLong = selectMax("字段名", where()...);
...
```


# 8. AdvancedService类 前置后置方法

AdvancedService.java 的路径是 com.topfox.service.AdvancedService

## 8.1. 所有前置后置方法
- AdvancedService.java 是继承 SimpleService的, 提供了新增/修改/删除的前置和后置方法
- before 开头的为前置方法, after 开头的为后置方法
- 如果存在名为 dbState的参数,  则请参考 com.topfox.data.DbState
- 下面所有方法的排序 即为 这些方法执行的优先级顺序

AdvancedService类 的部分源码如下:

```java
    
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
    
```



## 8.2. 一个实际例子
```java
@Service("userService")
public class UserService extends AdvancedService<UserDao, UserDTO> {
     
     /**
     * @param userDTO
     * @param state  DTO实体的状态, 分为新增i/修改u/删除d/无n 4种状态, 
     *     可参考 com.topfox.data.DbState
     */
    @Override
    protected void beforeInsertOrUpdate(UserDTO userDTO, String state) {
        if(DbState.INSERT.equals(state)){
            /** 新增之前的业务逻辑 */
        }else if (DbState.UPDATE.equals(state)){
            /** 更新之前业务逻辑 */
        }
        /** 新增和更新之前的业务逻辑 */
    }
}
```

# 9. MultiService 类

## 9.1. MultiService 源码
select 方法中, 创建了一个查询构造器, 然后调用了它的select方法而已.

MultiService的源码如下:

```java
public class MultiService<SESSION extends AbstractRestSession> implements ISuperService {
    ...略去部分代码
    public final SESSION restSession() {
        return restSessionHandler.get();
    }

    /**
     * 线程安全级别的自定属性
     * @return
     */
    public final JSONObject attributes() {
        return threadLocalAttributes.get();
    }

    public EntitySelect select(){
        return select(null, true);
    }
    public EntitySelect select(String fields){
        return select(fields,false);
    }
    public EntitySelect select(String fields,Boolean isAppendAllFields){
        //创建了一个查询构造器,然后调用了它的select方法而已
        return EntitySelect.create().select(fields,isAppendAllFields);
    }
    public Condition where(){
        //创建了一个条件匹配器而已
        return Condition.create();
    }
}
```

## 9.2. MultiService的 select where 方法运用例子

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    @Autowired DeptService deptService;

    // select() 方法运用例子
    public Object query1(){
        List<UserDTO> list = userService.listObjects(
                select("id, name, sex")
                        .where()
                        .like("name","张三")
                        .eq("id","999")
                        .endWhere()
        );
        return list;
    }
    
    // where() 方法运用例子
    public Object query2(){
        List<DeptDTO> list =deptService.listObjects(
                where().eq("deptName","IT部门")
        );
        return list;
    }
}
```