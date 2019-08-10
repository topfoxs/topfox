 
# 1. [↖回到主目录](https://github.com/topfoxs/topfox)

# 2. TopFox配置参数
以下参数在项目  application.properties 文件中配置, 不配置会用默认值. 下面的等号后面的值就是默认值. 

## 2.1. top.log.start="▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼..."
debug 当前线程开始 日志输出分割

## 2.2. top.log.prefix="# "
debug 中间日志输出前缀

## 2.3. top.log.end=▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲..."
debug 当前线程结束 日志输出分割符

## 2.4. top.page-size=100
分页时,默认的每页条数

## 2.5. top.max-page-size=20000
不分页时(pageSize<=0),查询时最多返回的条数

## 2.6. [新增] top.service.thread-cache=false
是否开启一级缓存(线程缓存), 默认false 关闭, 查询不会读取一级缓存

## 2.7. [新增] top.service.redis-cache=false
是否开启二级缓存(redis缓存), 默认false 关闭, 替代老的 open-redis

## 2.9. top.service.redis-log=flase

日志级别是DEBUG时, 是否打印 操作redis的操作日志

```
 默认 false  不打印操作redis的日志
     true   打印操作redis的日志
```

参数配置为true时, 控制台打印的日志大概如下:

```
# DEBUG 112-com.topfox.util.DataCache 更新后写入Redis成功 com.user.entity.UserDTO hashCode=2125196143 id=0 
##DEBUG 112-com.topfox.util.DataCache更新后写入Redis成功 com.user.entity.UserDTO hashCode=1528294732 id=1 
##DEBUG 112-com.topfox.util.DataCache查询后写入Redis成功 com.user.entity.UserDTO hashCode=620192016 id=2
```

## 2.10. top.redis.serializer-json=true

```
# redis序列化支持两种, true:jackson2JsonRedisSerializer false:JdkSerializationRedisSerializer
# 注意, 推荐生产环境下更改为 false, 类库将采用JdkSerializationRedisSerializer 序列化对象,
# 这时必须禁用devtools(pom.xml 注释掉devtools), 否则报错.
```

## 2.11. top.service.update-mode=3

更新时DTO序列化策略 和 更新SQL生成策略

```
重要参数: 
参数值为 1 时, service的DTO=提交的数据.              
   更新SQL 提交数据不等null 的字段 生成 set field=value
   
参数值为 2 时, service的DTO=修改前的原始数据+提交的数据. 
   更新SQL (当前值 != 原始数据) 的字段 生成 set field=value
   
参数值为 3 时, service的DTO=修改前的原始数据+提交的数据. 
   更新SQL (当前值 != 原始数据 + 提交数据的所有字段)生成 set field=value
   始终保证了前台(调用方)提交的字段, 不管有没有修改, 都能生成更新SQL, 这是与2最本质的区别
```

## 2.12. top.service.select-by-before-update=false

top.service.update-mode=1 时本参数才生效

默认值为false

更新之前是否先查询(获得原始数据).  如果需要获得修改日志, 又开启了redis, 建议在 update-mode=1时, 将本参数配置为true

## 2.13. top.service.update-not-result-error=true

根据Id更新记录时, sql执行结果(影响的行数)为0时是否抛出异常

```
 默认 true  抛出异常
 false 不抛异常
```

## 2.14. top.service.sql-camel-to-underscore=OFF

生成SQL 是否驼峰转下划线 默认 OFF

一共有3个值:

1. OFF 关闭, 生成SQL 用驼峰命名
2. ON-UPPER 打开, 下划线并全大写
3. ON-LOWER 打开, 下划线并全小写

# 3. Topfox 在运行时更改参数值---对象 SysConfig
- SysConfig 接口的实现类是 com.topfox.util.SysConfigDefault

```java
package com.topfox.util;

public interface SysConfig extends SysConfigRead {

    /**
     * 对应配置文件中的  top.service.update-mode
     */
    void setUpdateMode(Integer value);

    /**
     * 对应配置文件中的  top.service.redis-cache
     */
    void setRedisCache(Boolean value);

    /**
     * 对应配置文件中的  top.service.thread-cache
     */
    void setThreadCache(Boolean value);
        
    /**
     * 对应配置文件中的  top.service.update-not-result-error
     */
    void setUpdateNotResultError(Boolean value);

    ...等等, 没有全部列出
}
```

- 以上接口定义的方法是set方法, 允许在运行时 修改,  每个service 都有一个SysConfig的副本, 通过set更改的值只对当前service有效.
- 使用场景举例:

以参数 open-redis为例:
<br>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp; 我们假定项目配置文件 application.properties中开启了 读写Redis 的功能, 即  top.service.open-redis=true , 此时的含义表示, 当前项目的所有service操作数据库的增删改查的数据都会同步到Redis中.   

那问题来了,  假如刚好某个service 如UserService 需要关闭 redis-cache, 怎么处理呢, 代码如下:

```java
@Service
public class UserService extends AdvancedService<UserDao, UserDTO> {
    @Override
    public void init() {
        /**
            1. sysConfig 为 AdvancedService的父类 SuperService 中定义的 变量, 直接使用即可
            2. sysConfig的默认值 来自于 application.properties 中的设置的值,  
            如果 application.properties  中没有定义, 则TopFox会自动默认一个
            3.sysConfig中定义的参数在这里都可以更改
        */
        
        //关闭UserService 读写redis的功能, 其他service不受影响
        sysConfig.setRedisCache(false);
        
        //关闭UserService 读取一级缓存, 其他service不受影响
        //sysConfig.setThreadCache(false);
    }
}
```
这样调用了 UserService 的 getObject  listObjects update  insert  delete 等方法操作的数据是不会同步到redis的 . 
<br>其他参数同理可以在运行时修改