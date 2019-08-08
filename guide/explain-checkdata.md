# 1. [↖回到主目录](https://gitee.com/topfox/topfox/blob/dev/README.md)

# 2. 数据校验组件 CheckData

## 2.1. setWhere(Condition where)
- @param where  
- @return CheckData

给数据校验组件设置一个条件匹配器, 数据重复检查时使用

## 2.2. where()
- @return Condition

创建一个新的条件匹配器

## 2.3. setFields(String... fields)
- @param fields    设置要检查的字段名, 多个字段用逗号隔开
- @return Condition

## 2.4. addField(String name,String label)
- @param fields    设置要检查的字段名(英文名, 一般与数据库或dto的名字一样)
- @param label   设置要检查的字段名标题(如中文名, 这个名字将返回给调用者)
- @return Condition

## 2.5. setErrText(String errText)
- @param fields    设置错误信息, 自定义的错误内容, 如 "订单号不可重复", "用户姓名不能为空"
- @param label   设置要检查的字段名标题(如中文名, 这个名字将返回给调用者)
- @return Condition

## 2.6. checkNotNull()
执行检查字段不能为 空的逻辑
要抛出异常

## 2.7. checkNotNull(boolean isThrowNewException)
- @param isThrowNewException  是否抛出异常
- @return String 返回错误信息

执行检查字段不能为 空的逻辑
<br>isThrowNewException ==false 则不抛出异常(吃掉异常), 并返回错误信息

## 2.8. excute()
执行重复检查的逻辑, TopFox会自动生成重复检查的SQL, 一旦SQL的结果🈶记录就会报错
要抛出异常

## 2.9. excute(boolean isThrowNewException)
- @param isThrowNewException  是否抛出异常
- @return String 返回错误信息

执行重复检查的逻辑, TopFox会自动生成重复检查的SQL, 一旦SQL的结果🈶记录就会报错
<br>isThrowNewException ==false 则不抛出异常(吃掉异常), 并返回错误信息

# 3. 数据校验组件之实战- 重复检查
假如用户表中已经有一条用户记录的 手机号是 13588330001, 然后我们再新增一条手机号相同的用户, 或者将其他某条记录的手机号更新为这个手机号,  此时我们希望 程序能检查出这个错误, CheckData对象就是干这个事的.
检查用户手机号不能重复有如下多种写法:  


## 3.1. 示例一 

```java
@Service
public class CheckData1Service extends AdvancedService<UserDao, UserDTO> {
    @Override
    public void beforeInsertOrUpdate(List<UserDTO> list) {
        //多行记录时只执行一句SQL完成检查手机号是否重复, 并抛出异常
        checkData(list)  // 1. list是要检查重复的数据
                // 2.checkData 为TopFox在 SimpleService里面定义的 new 一个 CheckData对象的方法
                .addField("mobile", "手机号")        //自定义 有异常抛出的错误信息的字段的中文标题
                .setWhere(where().ne("mobile","*")) //自定检查的附加条件, 可以不写(手机号为*的值不参与检查)
                .excute();// 生成检查SQL, 并执行, 有结果记录(重复)则抛出异常, 回滚事务
    }
}
```

控制台 抛出异常 的日志记录如下:

```sql92

##这是 inert 重复检查 TopFox自动生成的SQL:
SELECT concat(mobile) result
FROM SecUser a
WHERE (mobile <> '*')
  AND (concat(mobile) = '13588330001')
LIMIT 0,1

14:24|49.920 [4] DEBUG 182-com.topfox.util.CheckData      | mobile {13588330001}
提交数据{手机号}的值{13588330001}不可重复
	at com.topfox.common.CommonException$CommonString.text(CommonException.java:164)
	at com.topfox.util.CheckData.excute(CheckData.java:189)
	at com.topfox.util.CheckData.excute(CheckData.java:75)
	at com.sec.service.UserService.beforeInsertOrUpdate(UserService.java:74)
	at com.topfox.service.AdvancedService.beforeSave2(AdvancedService.java:104)
	at com.topfox.service.SimpleService.updateList(SimpleService.java:280)
	at com.topfox.service.SimpleService.save(SimpleService.java:451)
	at com.sec.service.UserService.save(UserService.java:41)
```
- 异常信息的 "手机号" 是  .addField("mobile", "手机号") 指定的中文名称
- 假如用户表用两条记录,  第一条用户id为001的记录手机号为13588330001, 第一条用户id为002的记录手机号为13588330002.
<br>如果我们把第2条记录用户的手机号13588330002改为13588330001, 则会造成了 数据重复, TopFox执行的检查重复的SQL语句为:

```sql92

##这是 update时重复检查 TopFox自动生成的SQL:
SELECT concat(mobile) result
FROM SecUser a
WHERE (mobile <> '*')
  AND (concat(mobile) = '13588330001')
  AND (id <> '002')   ## 修改用户手机号那条记录的用户Id
LIMIT 0,1
```

通过这个例子, 希望读者能理解 新增和更新 TopFox 生成SQL不同的原因.

## 3.2. 示例二
如果希望不指定 mobile 的中文名, 则这样改写 示例一 的代码

```java
@Service
public class CheckData1Service extends AdvancedService<UserDao, UserDTO> {
    @Override
    public void beforeInsertOrUpdate(List<UserDTO> list) {
        //多行执行依据SQL, 检查手机号是否重复 并抛出异常
        checkData(list) 
                .setFields("mobile")  //这里直接传 字段英文名.  可以是多个字段哦
                .setWhere(where().ne("mobile","*")) 
                .excute();
        // 报错信息将会是:  提交数据{mobile}的值{13588330001}不可重复,  这个英文可以由 调用者自己处理(如专职html前端处理/替换为自己想要的中文)
    }
}
```

## 3.3. 示例三
不抛出异常, 但希望返回错误信息, 开发者自己处理, 修改处为"  .excute(false) "

```java
@Service
public class CheckData1Service extends AdvancedService<UserDao, UserDTO> {
    @Override
    public void beforeInsertOrUpdate(List<UserDTO> list) {
        String errorText = checkData(list)
                .setFields("mobile")        
                .setWhere(where().ne("mobile","*")) 
                .excute(false);  //参数传入false 就不会抛出异常, 但会返回错误信息, 开发者自己处理
        //errorText 为 返回的错误信息
    }
}
```

## 3.4. 示例四

- 传入检查的数据是DTO,  不是  示例一  的 list
- 本例子是继承的 SimpleService, 不是AdvancedService,  AdvancedService主要封装了 insert update delete的前置,后置事件

```java
@Service
public class CheckData2Service extends SimpleService<UserDao, UserDTO> {

    //插入时
    @Override
    public int insert(UserDTO userDTO) {
        //从检查传入的手机号 是否有重复
        checkData(userDTO)
                .setFields("mobile")
                .excute();
        // 通过检查后, 则执行父类的插入方法
        return super.insert(userDTO);
    }

    ////更新时
    @Override
    public int update(UserDTO userDTO) {
        //从检查传入的手机号 是否有重复
        checkData(userDTO)
                .setFields("mobile")
                .excute();
        // 通过检查后, 则执行父类的插入方法
        return super.update(userDTO);
    }
}
```

## 3.5. 示例五

- 传入检查的数据是DTO, 不是 示例一 的 list
- 继承AdvancedService

### 3.5.1. 示例源码

```java
@Service
public class CheckData1Service extends AdvancedService<UserDao, UserDTO> {
    //插入 或者更新 之前的事件
    @Override
    public void beforeInsertOrUpdate(UserDTO userDTO) {
        //从检查传入的手机号 是否有重复
        checkData(userDTO)
                .setFields("mobile")        
                .excute();
    }
}
```

### 3.5.2. 总结-重要

- 与示例四对比, 可见继承了 AdvancedService 的代码 写法更简洁
- 这就是  AdvancedService存在的理由

## 3.6. 示例六 多字段重复检查
现在我们回过头想想 示例一 的SQL中怎么会有  concat(mobile) 呢?   因为TopFox考虑了多个字段 并且检查重复的情况, 如:

```java
@Service
public class CheckData2Service extends SimpleService<UserDao, UserDTO> {
    @Override
    public int insert(UserDTO userDTO) {
        //country和mobile的值一般是客户端写入值, 通过 userDTO 传递到这里
        userDTO.setCountry("中国")     //指定国家
            .setMobile("13588330001");//指定手机号
            
        //检查重复
        checkData(userDTO)
                .addFields("country, mobile") //2个字段重复检查 country mobile
                .setWhere(where().ne("mobile","*"))   //自定义条件
                .excute();
        //通过检查, 则执行父类的插入方法
        return super.insert(userDTO);
    }
    
}
```

新增生成的SQL是:

```sql92 

## 这是 update时重复检查 TopFox自动生成的SQL:
SELECT concat(country, mobile) result
FROM SecUser a
WHERE (mobile <> '*')
  AND (concat(country,"-",mobile) = '中国-13588330001') // 新增 用户.country = 中国
LIMIT 0,1
```
这个业务需求场景是: 同一个国家中, 手机号不能有重复, 如中国的用户允许有且只能有一个用户的手机号为13588330001, 美国的用户中也可以有一个用户的手机号为13588330001.

## 3.7. 示例七 多字段检查指定字段中文标题
示例六的例子可以改为如下, 功能一样, 区别是抛出的错误信息中, 字段名将是 程序中指定的中文.

```java
@Service
public class CheckData2Service extends SimpleService<UserDao, UserDTO> {
    @Override
    public int insert(UserDTO userDTO) {
        //country和mobile的值一般是客户端写入值, 通过 userDTO 传递到这里
        userDTO.setCountry("中国")     //指定国家
            .setMobile("13588330001");//指定手机号
            
        //检查重复
        checkData(userDTO)
                .addField("country", "国家")           //自定义 国家字段  有异常抛出的错误信息的字段的中文标题
                .addField("mobile", "手机号")          //自定义 手机号字段 有异常抛出的错误信息的字段的中文标题
                .setWhere(where().ne("mobile","*"))   //自定义条件
                .excute();
        //通过检查, 则执行父类的插入方法
        return super.insert(userDTO);
    }
    
}
```

# 4. 数据校验组件之实战- 不可空白检查
与重复检查基本类似,  不同的是使用时 .excute() 方法改为  .checkNotNull(), 且这个检查不会访问数据库, 源码如下:

```java
public class UserServiceCheckData extends AdvancedService<UserDao, UserDTO> {
    @Override
    public void beforeInsertOrUpdate(UserDTO userDTO, String state) {
        /**
         * 不能空白检查
         */
        /** 检查指定的字段不可空白,存在则抛出异常 */
        checkData(userDTO) //新建检查类,并传入检查的对象
                .setFields("id","sex","name","remark")//设置检查的字段
                .checkNotNull();

        /**
         * 不能空白检查
         */
        /** 检查指定的字段不可空白,存在则抛出异常 */
        checkData(userDTO) //新建检查类,并传入检查的对象
                .setFields("id,sex,name,remark")//设置检查的字段
                .checkNotNull();


        /** 检查指定的字段不可空白,存在不抛出异常,返回错误的文本信息,自己处理 */
        String errText=checkData(userDTO) //新建检查类,并传入检查的对象
                .setFields("id","sex","name","remark")//设置检查的字段
                .checkNotNull();// 参数传入false, 表示不抛出异常
        logger.error(errText);//打印出错误的文本信息

        /** 检查指定的字段不可空白,自定义字段中文标题*/
        checkData(userDTO)
                .addField("name","姓名")//有异常抛出的错误信息的字段中文标题自定义
                .addField("sex","性别")
                .addField("remark","备注")
                .checkNotNull();//执行检查逻辑
     }
}
```