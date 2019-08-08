# 1. 目录
- [快速使用](https://gitee.com/topfox/topfox/blob/dev/guide/explain-sample.md)
- [高级应用](https://gitee.com/topfox/topfox/blob/dev/guide/explain-question.md)
- [TopFox配置参数](https://gitee.com/topfox/topfox/blob/dev/guide/explain-configuration.md)
- [上下文对象](https://gitee.com/topfox/topfox/blob/dev/guide/explain-tools.md)
- [核心使用](https://gitee.com/topfox/topfox/blob/dev/guide/explain-core.md)
- [条件匹配器](https://gitee.com/topfox/topfox/blob/dev/guide/explain-condition.md)
- [实体查询构造器](https://gitee.com/topfox/topfox/blob/dev/guide/explain-entityselect.md)
- [流水号生成器](https://gitee.com/topfox/topfox/blob/dev/guide/explain-keybuild.md)
- [数据校验组件](https://gitee.com/topfox/topfox/blob/dev/guide/explain-checkdata.md)
- [更新日志组件](https://gitee.com/topfox/topfox/blob/dev/guide/explain-updatelog.md)
- [自动填充组件](https://gitee.com/topfox/topfox/blob/dev/guide/explain-filldata.md)
- [Response 返回结果对象](https://gitee.com/topfox/topfox/blob/dev/guide/explain-response.md)
- [其他](https://gitee.com/topfox/topfox/blob/dev/guide/explain-other.md)

## 1.1. 必备
- 文中涉及的例子源码网址: https://gitee.com/topfox/topfox-sample
- TopFox技术交流群 QQ: 874732179

## 1.2. topfox 介绍
在 srpingboot2.x.x 和MyBatis 的基础上只做增强不做改变，为简化开发、提高效率而生。

编程规范参考《阿里巴巴Java开发手册》

借鉴mybaties plus部分思想

特性:

- **无侵入**：只做增强不做改变，引入它不会对现有工程产生影响
- **损耗小**：启动即会自动注入基本 CURD，性能基本无损耗，直接面向对象操作
- **集成Redis缓存**: 自带Redis缓存功能, 支持多主键模式, 自定义redis-key. 实现对数据库的所有操作, 自动更新到Redis, 而不需要你自己写任何代码; 当然也可以针对某个表关闭. 
- **强大的 CRUD 操作**：内置通用 Mapper、通用 Service，仅仅通过少量配置即可实现单表大部分 CRUD 操作，更有强大的条件构造器，满足各类使用需求
- **支持 Lambda 形式调用**：通过 Lambda 表达式，方便的编写各类查询条件，无需再担心字段写错
- **支持主键自动生成**：可自由配置，充分利用Redis提高性能, 完美解决主键问题.  支持多主键查询、修改等
- **内置分页实现**：基于 MyBatis 物理分页，开发者无需关心具体操作，写分页等同于普通查询
- **支持devtools/jrebel热部署**
- **热加载** 支持在不使用devtools/jrebel的情况下, 热加载 mybatis的mapper文件
- 内置全局、局部拦截插件：提供delete、update 自定义拦截功能
- **拥有预防Sql注入攻击功能**
- **无缝支持spring cloud**: 后续提供分布式调用的例子

## 1.3. topfox 使用
> 推荐上传到自有的maven-nexus服, 然后在业务项目进行以下依赖

```
<!--topfox-->
<dependency>
    <groupId>com.topfox</groupId>
    <artifactId>topfox</artifactId>
    <version>1.2.6</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

# 2. 更新日志
## 2.1. 版本1.2.6  更新日志 2019-08-02

1: 改动如下

```
topfox的 RestSessionHandler 更名为 AbstractRestSessionHandler
建议原二级类库的 RestSessionConfig 更名为 RestSessionHandler

这样与 AbstractRestSession 在二级类库的实现类 RestSession 规范一致

```


2: AppContext优化

- AppContext.getAbstractRestSession 删除, 改为 AppContext.getRestSession
- AppContext.getAbstractRestSessionHandler 删除, 改为 AppContext.getRestSessionHandler

优化前的使用方式:

```java
@RestController
@RequestMapping("/context")
public class AppContextController {
    @GetMapping("/test1")
    public void test1() {
        RestSessionHandlerConfig restSessionHandlerConfig  = (RestSessionHandlerConfig)AppContext.getAbstractRestSessionHandler();

        //与 restSessionConfig.get()的获得的对象一样
        RestSession restSession = (RestSession)AppContext.getAbstractRestSession();
    }
}
```

优化后的使用方式:

```java
@RestController
@RequestMapping("/context")
public class AppContextController {
    @GetMapping("/test1")
    public void test1() {
        RestSessionHandler restSessionHandler  = AppContext.getRestSessionHandler();

        //与 restSessionHandler.get()的获得的对象一样 获得当前线程的 RestSession对象
        RestSession restSession = AppContext.getRestSession();
    }
}
```

完整源码( 请注意源码中 "以下3种获取 useName 的方法" ):

```java
 @GetMapping("/test1")
    public void test1() {
        Environment environment = AppContext.environment();
        RestSessionHandler restSessionHandler1  = AppContext.getRestSessionHandler();

        //根据Class从IOC容器中获取 单实例
        RestSessionHandler restSessionHandler2 = AppContext.getBean(RestSessionHandler.class);

        //与 restSessionConfig.get()的获得的对象一样
        RestSession restSession1 = AppContext.getRestSession();
        RestSession restSession2 = AppContext.getRestSession(RestSession.class);

        /**
         * 以下 3种 获取 useName 的方法
         */
        String useName;
        //1. 这样可以获取 RestSession 中定义的 getUserName 方法
        useName = AppContext.getRestSession(RestSession.class).getUserName();

        //2. 而下面的 getUserName idea 编译器报错
        //useName = AppContext.getRestSession().getUserName();

        //3. 这样也ok
        useName = restSession1.getUserName();
        useName = restSession2.getUserName();

        SysConfigRead configRead = AppContext.getSysConfig();
        System.out.println(configRead);
    }
```

源码请参考 topfox-sample


## 2.2. 版本1.2.5  更新日志 2019-07-30

- 驼峰处理类 CamelHelper

```
BeanUtil.toUnderlineName 删除, 用 CamelHelper.toUnderlineName 替代
BeanUtil.toCamelCase 删除, 用 CamelHelper.toCamel 替代
```

- 解决 form-data 参数值丢失的问题, 即request.getParameterMap()获取不到值

## 2.3. 版本1.2.4  更新日志 2019-07-24
全局缓存参数开关

```
新增  一级缓存开关 top.service.thread-cache
新增  二级缓存开关 top.service.redis-cache
删除  top.service.open-redis
```


以下未完成

- insertGetKey 多主键赋值的问题
- 填充组件优化, 不考虑

## 2.4. 版本1.2.3  更新日志 2019-07-23
已经完成

- 多主键的支持, 包括:  更新,  删除, 查询, 数据校验组件, 修改日志组件; 
- java远程调用返回空对象的处理; 
- 技术文档修改

## 2.5. 版本1.2.2  更新日志 2019-07-18
请参考 << TopFox配置参数 >>中的描述, 增加配置3个参数  :

- top.service.update-not-result-error
- top.service.select-by-before-update
- top.service.redis-log

dto增加 addNullfields 方法, 请参考 <<常见问题>> 章节中的 "如何将指定字段的值更新为null"

SimpleService.setFillDataHandler 方法作废.  不需要写代码, 会自动注入 填充对象FillDataHandler的实现类. 

增加填充组件 FillDataHandler的文档,  DataDTO void方法改为返回自身, DataDTO增加setValue方法, 可以对指定的字段赋值

## 2.6. 版本1.2.0  更新日志

- updateBatch 方法的返回值 由int 改为 List< DTO >
- BaseDao 作废删除, 增加   BaseMapper< DTO extends DataDTO > 代替

- AdvancedService< QTO extends DataQTO, DTO extends DataDTO >  改为
<BR> AdvancedService< M extends BaseMapper< DTO >, DTO extends DataDTO >

- SimpleService 中 方法 setBaseDao 删除.  已经定义 baseMapper , 可访问开发者自定义Dao的方法

## 2.7. 版本1.1.0  更新日志
### 2.7.1. 上下文对象
- 在以后的版本中将删除 com.topfox.common.ApplicationContextProvider 
- 新增加 上下文对象 com.topfox.common.AppContaxt,静态方法如下:

```
	getBean(Class<T> clazz)
	getBean(String name,Class<T> clazz)
	getRestSessionHandler()
	getAbstractRestSession() 
	getSysConfig()
	environment()
```

### 2.7.2. SimpleService 进行了改造和优化
新增方法

```
List<DTO> selectBatch(DataQTO qto)
Response<List<DTO>> selectPage(EntitySelect entitySelect)
Response<List<Map<String, Object>>> selectPageForMap(EntitySelect entitySelect)
List<DTO> selectBatch(DataQTO qto)
Response<List<DTO>> listPage(EntitySelect entitySelect)
List<DTO> listObjects(EntitySelect entitySelect)
```

### 2.7.3. EntitySelect 和DataQTO
EntitySelect 和 DataQTO 对 orderBy groupBy having 分页的支持  

### 2.7.4. ResponseCode 增加了多个 异常编码 保证 Topxfox抛出异常 的编码唯一性
