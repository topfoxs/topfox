# 1. [↖回到主目录](https://gitee.com/topfox/topfox/blob/dev/README.md)

# 2. 流水号生成器 KeyBuild
## 2.1. getKeyId()
静态方法, 获得一个随机的全局唯一流水号, 不使用数据库和Redis, 纯算法生成的唯一流水号, 长度为26位

## 2.2. setPrefix(String prefix)
- @param prefix 设置流水号的前缀
- @return KeyBuild

## 2.3. setSuffix(String suffix)
- @param suffix 设置流水号的后缀
- @return KeyBuild

## 2.4. setDateFormat(String dateFormat)
- @param dateFormat 日期格式, 如yyyMMdd yyMMdd yyMM yMMdd
- @return KeyBuild

## 2.5. getKey(String fieldName, int fillLength)
- @param fieldName  字段名, 必须是DTO中存在的
- @param fillLength   序列号的长度, 不足用数字零填充
- @return  一个流水号 (String)

## 2.6. getKeys(String fieldName, int fillLength, int count)
- @param fieldName  字段名, 必须是DTO中存在的
- @param fillLength   序列号的长度, 不足用数字零填充
- @param count        一次获取的流水号个数
- @return 返回类型是  ConcurrentLinkedQueue 流水号容器

# 3. 流水号生成器之实战和思路解析
下面以用户表(UserDTO对应的表)为例,说明流水号生成器的使用方法

## 3.1. 简单流水号
- 简单的流水号, 我们定义为 是递增的序列号
- keyBuild()方法是 类库封装的创建 KeyBuild对象的方法.

::: 示例一

- 假如表中只有2条数据, id 字段的值分别为 001, 002, 则执行下面程序获得的值是003

```JAVA
package com.test.service;
@Service
public class KeyBuildService extends AdvancedService<UserDao, UserDTO> {
    public void test1() {
        //logger为TopFox声明的日志对象
        //例: 根据UserDTO中字段名id 来获取一个纯 3位数 递增的流水号
        logger.debug(
            keyBuild()          //创建一个 KeyBuild对象, 会自动获取当前Service的 UserDTO 对象
                .getKey("id",3) //参数id 必须是 UserDTO中存在的字段
        ); //打印出来的值是 003
    }
}
```

::: 示例二

- 假如表中只有6条数据, id 字段的值分别为 06,07,  112,113,   2222,2223  这里有长度为2,3,4位的Id值, 执行下面的程序, debug的信息分别是08, 114, 2224.

```java
package com.test.service;
@Service
public class KeyBuildService extends AdvancedService<UserDao, UserDTO> {
    public void test2() {
        logger.debug(keyBuild().getKey("id",2));  //打印出来的值是 08
        logger.debug(keyBuild().getKey("id",3));  //打印出来的值是 114
        logger.debug(keyBuild().getKey("id",4));  //打印出来的值是 2224
        //这个例子说明是按照 id字段 值的长度隔离的.
    }
}
```

总结: 

1. 流水号是通过分析当前service的UserDTO对应表的已有数据而生成的, 并将分析结果缓存到Redis中, 减少对表的读取.
2. 流水号的生成是按照表名,字段名和已有数据的长度 隔离的
3. 位数满后会自动增加1位, 例如获得2位数的流水号, 当99后, 再次获取会增加一位变为100
4. 获取到流水号后, 是不会因为抛出异常而回滚,  每次调用始终 加一的. <br>例如 获取到 2224后抛一个异常, 事务是回滚了, 但下次获取这个流水号, 取到的是 2225(2224不会回滚).这样设计主要是考虑到"避免分布式下高并发 流水号可能会重复的问题".
5. 这是按照调用次数 变化的数字,  我们称之为是 "递增的次序号". 位数不足用 0 填补

## 3.2. 复杂流水号(含前缀|日期|后缀)
 - 流水号 = 前缀 + 日期字符 + 递增的序列号 + 后缀
 - 如何设置 前缀和日期字符,以及后缀呢? 请看如下例子:
 
```java
package com.test.service;
@Service
public class KeyBuildService extends AdvancedService<UserDao, UserDTO> {
    /**
     * 每行数据执行本方法一次,新增和修改的 之前的逻辑写到这里,  如通用的检查, 计算值得处理
     */
    public void test3() {
        //获取一个 带前缀TL 带日期字符(yyMMdd) + 6位数递增的序列号  的流水号
        logger.debug(
            keyBuild()
                .setPrefix("TL")           //设置前缀
                .setSuffix("END")          //设置后缀
                .setDateFormat("yyyyMMdd") //设置日期格式
                .getKey("id",3)            //参数依次是  1.字段名  2.序列号长度
        );
    }
}
```
- 假如生成的流水号 是 TL20190601001END ,  其中 TL 是前缀,  20190601是年月日,  001是递增的序列号, END 是后缀
-  日期格式可以自定,  例如: yyyyMMdd yyMM   MMdd   yyMMdd   yMMDD

## 3.3. 批量流水号
一次要获得多个流水号, 如企业内部系统 的 订单导入等,  建议用如下办法获得一批流水号

```java
package com.test.service;
@Service
public class KeyBuildService extends AdvancedService<UserDao, UserDTO> {
    public void test4() {
        logger.debug("获得多个流水号");
        //获得多个序列号
        ConcurrentLinkedQueue<String> queue =
                keyBuild("TL", "yyMMdd")        //前缀, 设置日期格式
                        .getKeys("id",  6,  4); //参数依次是  1.字段名  2.序列号长度  3.要获得流水号个数

        // poll 执行一次, 容器 queue里面少一个
        logger.debug(queue.poll());//获得第1个序列号
        logger.debug(queue.poll());//获得第2个序列号
        logger.debug(queue.poll());//获得第3个序列号
        logger.debug(queue.poll());//获得第4个序列号
    }
}
```

也可以写成

```java
package com.test.service;
@Service
public class KeyBuildService extends AdvancedService<UserDao, UserDTO> {
    public void test5() {
        logger.debug("获得多个流水号");
        //获得多个序列号
        ConcurrentLinkedQueue<String> queue =
            keyBuild()
                .setPrefix("TL")            //设置前缀
                .setDateFormat("yyyyMMdd")  //设置日期格式
                .getKeys("id",  6,  4);     //参数依次是  1.字段名  2.序列号长度  3.要获得流水号个数
        ... 略
    }
}
```

# 4. 多Service时务必正取获取流水号
下面的例子 当前Service 是 Order 订单表,  注入了 用户的UserService;

```java
package com.user.service;
@Service
public class DeptService extends AdvancedService<DeptDao, DeptDTO> {
    @Autowired
    UserService userService;
    public void test5() {
        UserDTO user = new UserDTO();
        user.setId(
                //获得一个用户的流水号 (错误的代码)---其实获取到的是 Dept表的流水号
                keyBuild().getKey("id",3)
        );
        user.setName("张三");
        //...
        userService.insert(user);
    }
}
```

假如用户表中只有2条数据,用户id 字段的值分别为 001和002 ;  订单表中只有3条数据, 订单id 字段的值分别为 110,111和112.  上面的程序执行后插入到数据库记录的用户Id是113, 而不是正确的003.

- 获得用户流水号正确的程序代码是:

```java
    user.setId(
        userService.keyBuild().getKey("id",3) //纠正后的代码
    ); 
```