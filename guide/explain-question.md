# 1. [↖回到主目录](https://gitee.com/topfox/topfox/blob/dev/README.md)

# 2. 高级运用

## 2.1. 查询时如何才能不读取缓存
请读者先阅读 章节 《TopFox配置参数 》中的一级二级缓存开关

通过设置 readCache 为false, 能实现在开启一级/二级缓存的情况下又不读取缓存, 从而保证读取出来的数据和数据库中的一模一样, 下面通过5个例子来说明.

```java

@RestController
@RequestMapping("/demo")
public class DemoController  {
    @Autowired 
    UserService userService;
    
    @TokenOff
    @GetMapping("/test1")
    public Object test1(UserQTO userQTO) {
        //例1: 根据id查询, 通过第2个参数传false 就不读取一二级缓存了
        userService.getObject(1, false);

        //例2: 根据多个id查询, 要查询的id放入Set容器中
        Set setIds = new HashSet();
        setIds.add(1);
        setIds.add(2);
        //通过第2个参数传false 就不读取一二级缓存了
        List<UserDTO> list = userService.listObjects(setIds, false);

        //例3: 通过QTO 设置不读取缓存
        list = userService.listObjects(
            userQTO.readCache(false) //禁用从缓存读取(注意不是读写) readCache 设置为 false, 返回自己(QTO)
        );
        //或者写成:
        userQTO.readCache(false);
        list = userService.listObjects(userQTO);

        //例4: 通过条件匹配器Condition 设置不读取缓存
        list = userService.listObjects(
            Condition.create()     //创建条件匹配器
                .readCache(false)  //禁用从缓存读取
        );

        //例5: 通过查询构造器 EntitySelect 设置不读取缓存
        list = userService.listObjects(
                userService.select()//创建 EntitySelect
                .where()
                .readCache(false)//禁用从缓存读取
                .endWhere() //返回 EntitySelect
        );
        return list;
    }
}
```

## 2.2. 缓存开关 thread-cache redis-cache与readCache区别
请读者先阅读 章节 《TopFox配置参数》

```
一级缓存 top.service.thread-cache 大于 readCache
二级缓存 top.service.redis-cache  大于 readCache
```
也就说, 把一级二级缓存关闭了,  readCache设置为true, 也不会读取缓存. 所有方式的查询也不会读取缓存.

## 2.3. 一级缓存的效果
- 一级缓存默认是关闭的

只打开某个 service的操作的一级缓存

```
@Service
public class UserService extends AdvancedService<UserDao, UserDTO> {
    @Override
    public void init() {
        sysConfig.setThreadCache(true); //打开一级缓存
    }
```

全局开启一级缓存, 项目配置文件 application.properties 增加

```
top.service.thread-cache=true
```

- 开启一级缓存后

1. 一级缓存是只当前线程级别的,  线程结束则缓存消失
2. 下面的例子, 在开启一级缓后 user1,user2和user3是同一个实例的
3. 一级缓存的效果我们借鉴了Hibernate框架的数据实体对象持久化的思想

```java
@RestController
@RequestMapping("/demo")
public class DemoController  {
    @Autowired
    UserService userService;

    @TokenOff
    @GetMapping("/test2")
    public UserDTO test2() {
        UserDTO user1 = userService.getObject(1);//查询后 会放入一级 二级缓存
        UserDTO user2 = userService.getObject(1);//会从一级缓存中获取到
        userService.update(user2.setName("张三"));
        UserDTO user3 = userService.getObject(1);//会从一级缓存中获取到
        return user3;
    }
}
```

## 2.4. 如何将指定字段的值更新为null
下面的 addNullFields 方法, 当字段类型是Number时, 是不能更新为null的, null 将强制转为零. 

### 2.4.1. 单个dto更新 update

```java
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    public void update(){
        UserDTO user1 = new UserDTO();
        user1.setAge(99);
        user1.setId(1);
        user1.setName("Luoping");
        userService.update(user1);//只更新有值的字段
    }
}
```

生成的Sql语句如下:

```sql
UPDATE users
  SET name='Luoping',age=99
WHERE (id = 1)

```
从这个例子我们可以看出, TopFox是将dto中 不为null的字段生成更新sql了, 那么能否实现将指定的字段更新为null呢? 如下修改,增加  user1.addNullFields("sex, lastDate");  即可

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    public void update(){
        UserDTO user1 = new UserDTO();
        user1.setAge(99);
        user1.setId(1);
        user1.setName("Luoping");
        
        //将指定的字段更新为null, 允许有空格
        user1.addNullFields(" sex , lastDate ");
        //这样写也支持
        user1.addNullFields("sex", "lastDate");
        user1.addNullFields("sex,lastDate",  "deptId");
        
        userService.update(user1);
    }
}
```

生成的Sql语句如下:

```sql
UPDATE users
  SET name='Luoping',age=99,sex=null,lastDate=null
WHERE (id = 1)

```

### 2.4.2. 多个dto更新 updateList
多行更新 addNullFields方法一样有效, 例:

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    public void updateList(){
        UserDTO user1 = new UserDTO();
        user1.setAge(99);
        user1.setId(1);
        user1.setName("张三");
        user1.addNullFields("sex, lastDate");//将指定的字段更新为null

        UserDTO user2 = new UserDTO();
        user2.setAge(88);
        user2.setId(2);
        user2.setName("李四");
        user2.addNullFields("mobile, isAdmin");//将指定的字段更新为null

        List list = new ArrayList();
        list.add(user1);
        list.add(user2);
        userService.updateList(list);//只更新有值的字段
    }
}
```

生成的Sql语句如下:

```sql
UPDATE users
  SET name='张三',age=99,sex=null,lastDate=null
WHERE (id = 1)

UPDATE users
  SET name='李四',age=88,mobile=null,isAdmin=null
WHERE (id = 2)
```

### 2.4.3. 自定条件更新 updateBatch
自定义条件更新 addNullFields方法一样有效, 例:

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;

    public void updateBatch(){
        UserDTO dto = new UserDTO();
        dto.setAge(99);
        dto.setDeptId(11);
        dto.addNullFields("mobile, isAdmin");//将指定的字段更新为null

        userService.updateBatch(dto, where().eq("sex","男"));
    }
}
```

生成的Sql语句如下:

```sql
UPDATE users
  SET deptId=11,age=99,mobile=null,isAdmin=null
WHERE (sex = '男')
```

## 2.5. 变化值更新技巧(update-mode=2或者3时)
- update-mode参数 请参考《TopFox配置参数》章节的描述
- 强调一下, 在参数配置 update-mode=2或3 时, 才存在变化值更新技巧的事儿.

变化值更新技巧, 我们先看一个例子

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    public void findAndUpdate(){
        UserDTO user1 = userService.getObject(1);
        user1.setAge(11);
        user1.setName("王五");
        userService.update(user1);
    }
}
```

生成的Sql语句如下:

```sql
UPDATE users
  SET code='*',orgId='*',name='王五',password='000000',age=11,amount=0.00,isAdmin=false,loginCount=loginCount+0
WHERE (id = 1) AND (version = 23)

```

从上面生成SQL的效果可以看出, UserDTO 所有字段的值不是null的,都生成了更新SQL.  当表的字段比较多时性能不是最高, 我们是否能实现只针对有变化的字段生成 update SQL语句呢?  TopFox是支持的, 代码修改如下:

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    public void findAndUpdate(){
        UserDTO user1 = userService.getObject(1);
        
         //增加这行代码就能实现 根据变化值更新, 必须要放在set新值之前.  update-mode=1时无效哦
        user1.addOriginFromCurrent();
        
        user1.setAge(11);
        user1.setName("王五");
        userService.update(user1);
    }
}
```
生成的Sql语句如下:

```sql
UPDATE users
  SET version=version+1,name='王五',password='000000',age=11
WHERE (id = 1) AND (version = 23)
```
在稍微改改, 如下(在配置参数 update-mode=3下测试) :

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService;
    public void findAndUpdate(){
        UserDTO user1 = userService.getObject(1);
        user1.addOriginFromCurrent();
        user1.setAge(null); //多了这行
        user1.setName("王五");
        userService.update(user1);
    }
}
```

执行输出:

```sql
## 注意, 这始终是 变化值, age在数据库的值是11, 现在setAge(null)了, 有变化了.
UPDATE users
  SET name='王五',age=null
WHERE (id = 1)
```

TopFox 实现原理, user1.addOriginFromCurrent() 会为当前值创建一个副本, 在生成SQL时, TopFox会与副本的数据比较, 不同则生成更新SQL.

## 2.6. 递增递减字段更新 例一
以用户表的DTO为例

- loginCountAdd 注解中的 Incremental.ADDITION 表示递增, 而 Incremental.SUBTRACT 表示递减
- loginCountAdd 字段不是数据库存在的一个字段,  更新后 [service.update()] 它值将被处理为 null (判断逻辑 loginCountAdd 与注解的name属性不等时)
- loginCount 为数据库存在的字段, 执行update后它的值将被处理为 修改后的值
- @JsonIgnore 注解说明: 如果loginCountAdd字段连null值都不想序列化到redis, 就可以用这个注解
- transient 修饰符说明: jdk序列化忽略该字段的值

用户表DTO源码:

```java
@Setter
@Getter
@Accessors(chain = true)
@Table(name = "users")
public class UserDTO extends DataDTO {
    private Long id

    @TableField(name="loginCount", incremental = Incremental.ADDITION)
    @JsonIgnore
    private  transient Integer loginCountAdd;

    private Integer loginCount;
}
```

应用例子:

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService; 
     /**
     * 递增更新 业务场景,如入库增加库存
     */
    public void addUpdate(){
        UserDTO user1 = new UserDTO().setId(1).setLoginCountAdd(10);
        userService.update(user1);
    }
}
```

生成的Sql语句如下:

```sql
UPDATE users
  SET loginCount=loginCount+10
WHERE id = 1
```

## 2.7. 递增递减字段更新 例二
在 《递增递减字段更新 例一》 的基础上, DTO做如下修改, 源码:

```java
@Setter
@Getter
@Accessors(chain = true)
@Table(name = "users")
public class UserDTO extends DataDTO {
    private Long id

    //@TableField(name="loginCount", incremental = Incremental.ADDITION)
    //@JsonIgnore
    //private  transient Integer loginCountAdd;

    @TableField(incremental = Incremental.ADDITION)
    private Integer loginCount;
}
```

- loginCount为数据库中存在的字段
- 效果: 更新后 loginCount字段的值将被修改为 改过之后的值, 即与数据库的值保持一致

## 2.8. 增删改一个方法实现 service.save
以下源码的逻辑重点是, 每个DTO的状态设定方法 dto.dataState(), 目的要告诉TopFox, 这个dto我要做什么操作(insert/update还是delete), 这个标记很重要

UnitTestService.saveTest 源码如下:

```java
@Service
public class UnitTestService extends MultiService<RestSession> {
    @Autowired UserService userService; 
    
    public void saveTest(){
      ic void saveTest(){
        UserDTO user1 = new UserDTO();
        user1.setAge(11);
        user1.setId(1);
        user1.setName("张三");
        user1.addNullFields("sex, lastDate");

        UserDTO user2 = new UserDTO();
        user2.setAge(12);
        user2.setId(2);
        user2.setName("李四");
        user2.addNullFields("mobile, isAdmin");

        UserDTO user3 = new UserDTO();
        user3.setId(3);

        List list = new ArrayList();
        //TopFox将 自动调用 service.insert 方法
        user1.dataState(DbState.INSERT);
        //user1.setState(DbState.INSERT);

        //TopFox将 自动调用 service.update 方法
        user2.dataState(DbState.UPDATE);
        //user2.setState(DbState.UPDATE);

        //TopFox将 自动调用 service.delete 方法
        user3.dataState(DbState.DELETE);
        //user3.setState(DbState.DELETE);

        list.add(user1);
        list.add(user2);
        list.add(user3);
        userService.save(list);
    }
}
```

- 在企业内部ERP或者后台系统中, 可能会常碰到一种需求,  一个表格, 可以同时新增修改和删除多条数据, 然后点击一个保存按钮提交到后台. xxxService.save这个方法就是满足这个需求的. 
- 看到这里, 有过前端开发经验的程序员会问, 这个标记能否开放给 HTML5前端, 或者说调用方指定呢?
<br>答案是肯定的,  我们先修改 UserDTO的代码, 重点是增加  String state字段, 名字自定, 必须要加上@State注解即可, 修改后的源码如下

```java
@Setter
@Getter
@Accessors(chain = true)
@Table(name = "users")
public class UserDTO extends DataDTO {
    /**
     *数据状态字段  数据库不存在的字段, 用于描述   transient修饰符 表示 改字段不会被序列化
     * @see com.topfox.data.DbState ;
     */
    @State @TableField(exist = false)
    private transient String state= DbState.NONE;

    @Id private Integer id;
    private String code;
    private String name;
    ...略
}
    
```

然后, 上面的  UnitTestService.saveTest 源码中,  

```java
user1.dataState(DbState.INSERT); 可以改为 user1.setState(DbState.INSERT);
user2.dataState(DbState.UPDATE); 可以改为 user2.setState(DbState.UPDATE);
user3.dataState(DbState.DELETE); 可以改为 user3.setState(DbState.DELETE);
```
两种写法效果是一样的

然后增加控制层

```java
@RestController
@RequestMapping("/user")
public class UserController {
    /**
     * 一次提交,存在 增加 修改 删除的情况
     *
     * @return
     */
    @PostMapping("/save")
    public Response<List<UserDTO>> save(@RequestBody List<UserDTO> list) {
        userService.save(list);
        return new Response<List<UserDTO>>(list);
    }
}
```

此时, HTML5前端, 或者说调用方(如PostMan工具)就能指定dto的 state 状态了, 提交的数据格式如下:

```json
// 与UnitTestService.saveTest 源码效果一样
[
    {   
        "state":"i", //user1 "state":"i" 表示是insert, 将生成插入SQL
        "id":1, 
        "name":"张三",
        "age":11,
        "sex":,
        "lastDate":
    },
    {   
        "state":"u", //user2 "state":"u" 表示是 update,将生成更新SQL
        "id":2, 
        "name":"李四",
        "age":12,
        "mobile":,
        "isAdmin":
    }, 
    {
        "state":"d", //user3 "state":"d" 表示是 delete,将生成删除SQL
        "id":3
    } 
]

```

总结两个重点:

- json格式中的 state 名字 要与 UserDTO中 @State注解的字段名 一样
- state的值 必须是 com.topfox.data.DbState 中的定义值(i/u/d/n)
- com.topfox.data.DbState 源码如下

```java
package com.topfox.data;

/**
 * DTO的状态值定义
 * i 新增
 * u 更新
 * d 删除
 * n 无状态
 */
public class DbState {
    /*** 新增*/
    public static final String INSERT="i";
    /*** 删除 */
    public static final String DELETE="d";
    /** * 修改 */
    public static final String UPDATE="u";
    /*** 无状态*/
    public static final String NONE  ="n";
}
```

## 2.9. 多主键 查询/删除
下面这个表有两个字段作为主键, userId 和 deptId :

```java
/**
 * 薪水津贴模板表
 * 假定一个主管 管理了多个部门, 每管理一个部门, 就有管理津贴作为薪水
 */
@Setter
@Getter
@Accessors(chain = true)
@Table(name = "salary")
public class SalaryDTO extends DataDTO {
    /**
     * 两个主键字段, 用户Id  和部门Id
     */
    @Id
    private Integer userId;

    @Id
    private Integer deptId;

    /**
     * 管理津贴
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "###0.00")
    private BigDecimal amount;

    ...
}
```

 表 salary 的数据如下:
 
| userId | deptId | amount | createUser | updateUser |
| ------ | ------ | ------ | ---------- | ---------- |
| 1      | 1      | 11     | *          | *          |
| 1      | 2      | 22     | *          | *          |
| 1      | 3      | 33     | *          | *          |

::: 重要备注:

1-1, 1-2, 1-2 我们称之为3组主键Id值, 任何一组主键值 可以定位到 唯一的行.

### 2.9.1. 技巧一: 单组主键值查询
多主键时, sql语句主键字段的拼接顺序是 按照 SalaryDTO 中定义的字段顺序来的.

具体来说, 如 concat(userId,'-', deptId) 这个先是 userId, 然后是deptId, 与 SalaryDTO 中定义的字段顺序一致. 因此在拼接Id值时注意顺序要一致.

单组主键值查询, 获得单个DTO对象:

```java
@RestController
@RequestMapping("/salary")
public class SalaryController {
    @Autowired
    SalaryService salaryService;
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("/test1")
    public SalaryDTO test1() {
        return salaryService.getObject("1-2");
    }
}
```

输出SQL:

```sql
    SELECT userId,deptId,amount,createUser,updateUser
    FROM salary a
    WHERE (concat(userId,'-', deptId) = '1-2')
```

### 2.9.2. 技巧二 : 多组主键值查询
多组主键值查询, 获得多个DTO对象:

```java
@RestController
@RequestMapping("/salary")
public class SalaryController {
    @Autowired SalaryService salaryService;
    
    @GetMapping("/test2")
    public List<SalaryDTO> test2() {
        return salaryService.listObjects("1-1,1-2,1-3");
    }
}
```

输出SQL:

```sql
SELECT userId,deptId,amount,createUser,updateUser
FROM salary a
WHERE (concat(userId,'-', deptId) = '1-1'
    OR concat(userId,'-', deptId) = '1-2'
    OR concat(userId,'-', deptId) = '1-3')
```

### 2.9.3. 技巧三: 获取主键字段拼接的SQL
下面的程序代码 打印出来的是字符串:  (concat(userId,'-', deptId)

```java
@RestController
@RequestMapping("/salary")
public class SalaryController {
    @Autowired SalaryService salaryService;

    @GetMapping("/test3")
    public String test3() {
        String idFieldsBySql = salaryService.tableInfo().getIdFieldsBySql();
        logger.debug(idFieldsBySql);
        return idFieldsBySql;
    }
}
```

### 2.9.4. 技巧四: 按多组主键值删除

```java
@RestController
@RequestMapping("/salary")
public class SalaryController {
    @Autowired SalaryService salaryService;
    
    @GetMapping("/test4")
    public void test4() {
        salaryService.deleteByIds("1-1,1-2");
    }
}
```

输出SQL:

```sql
DELETE FROM salary
WHERE (concat(userId,'-', deptId) = '1-1' 
    OR concat(userId,'-', deptId) = '1-2')
```
