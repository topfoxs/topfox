# 1. [↖回到主目录](https://github.com/topfoxs/topfox)

# 2. 条件匹配器Condition
[[between] 范围](#between)

[[eq] 等于](#eq)

[[like | likeLeft | likeRight] 匹配](#like)

[[notLike | notLikeLeft | notLikeRight] 非匹配](#notlike)

[[ne] 不等](#ne)

[[le] 小于等于](#le)

[[lt] 小于](#lt)

[[ge] 大于等于](#gt)

[[gt] 大于](#gt)

[[isNull] 值为空](#isnull)

[[isNotNull] 值不为空](#isnotnull)

[完整的例子](#完整的例子)

## 2.1. eq
> eq 等于 =

```java
eq(String fieldName, Object value)
eq(String fieldName, Object... values)
eq(String fieldName, Set set)  //Set<String> Set<Long> Set<Integer>
eq(String fieldName, List list)//List<String> List<Long> List<Integer>
```

- eq("name", "罗平") ---> name = '罗平'
- eq("age", 18)     ---> age = 18
- eq("name", new Object[] {"A", "B"} ) ---> (name = 'A' OR name = 'B')
- eq("name", "C", "D", "E")  ---> (name = 'C'  OR name = 'D' OR name = 'E')

## 2.2. like
> like | likeLeft | likeRight

```java
like(String fieldName, String... values)
likeLeft(String fieldName, String... values) 
likeRight(String fieldName, String... values)
```

例:

- like("name","值")      ---> name LIKE '%值%'
- likeLeft("name","值")  ---> name LIKE '%值'
- likeRight("name","值") ---> name LIKE '值%'
- like("name","值A", "值B")  ---> (name LIKE '%值A%' OR name LIKE '%值B%')

## 2.3. notLike
> notLike | notLikeLeft | notLikeRight

```java
notLike(String fieldName, String... values)
notLikeLeft(String fieldName, String... values) 
notLikeRight(String fieldName, String... values)
```
例 略

## 2.4. ne
> ne 不等 <>

```java
ne(String fieldName, Object... values)
```
 例:
- ne("age",11) ---> age <> 1
- ne("name","张三") ---> name <> '张三'
- ne("name","张三","李四") ---> (name <> '张三' AND name <> '李四')

## 2.5. le
> le 小于等于 <= 

```java
le(String fieldName, Object... values)
```
- 例 le("age",11) ---> age <= 1

## 2.6. lt
> lt 小于 < 

```java
lt(String fieldName, Object... values)
```
- 例 lt("age",11) ---> age < 1

## 2.7. ge
> ge

```java
ge(String fieldName, Object... values)
```
- 例 ge("age",11) ---> age >= 1

## 2.8. gt
> gt 大于 >

```java
gt(String fieldName, Object... values)
```
- 例 gt("age",11) ---> age > 1

## 2.9. isNull
> isNull 字段 IS NULL
- 例: isNull("name") ---> name is null

## 2.10. isNotNull
> isNotNull 字段 IS NOT NULL
- 例: isNull("name") ---> name is not null       

# 3. 条件匹配器其他方法
## 3.1. openPage(boolean value)
- 设置 是否打开分页的开关:  true开启(默认的)   false关闭
- 相当于 setOpenPage(boolean value)
- 默认值为 true,  如果关闭后生成的查询SQL语句始终不生成 limit x,y(即使设置了pageIndex  pageSize); 当然有个例外,如果利用条件匹配器 Condition.add("limit x, y"), 不受本参数影响.  
- Condition.add("limit x, y") 这样去设置分页是 TopFox不推荐的办法, 应该这样:
1. 如果是http get请求, 应该传入参数 pageIndex pageSize 去设置分页
2. 如果是调用xxxService的方法,  应该使用EntitySelect.setPage   setPageSize去设置分页

## 3.2. openPage
- 读取是否关闭分页的开关
- 相当于 getOpenPage
- @return Boolean

## 3.3. readCache(boolean value)
- 设置 是否读取缓存的开关:  true开启(默认的)   false关闭
- 相当于 setReadCache(boolean value)

## 3.4. readCache
- 是否 读取一级/二级缓存里面的数据
- 相当于 getReadCache
- @return Boolean

## 3.5. between
> between 范围

```java
between(String fieldName, Object valueFrom, Object valueTo)
```
BETWEEN 值1 AND 值2
- 例 between("age", 18, 30) ---> age between 18 and 30

# 4. 完整的例子
>条件匹配器Condition 2个完整的例子

## 4.1. 例子一
```java
@RestController
@RequestMapping("/condition")
public class ConditionController {
    @Autowired
    UserService userService;

    /**
     * 条件匹配器的一个例子
     */
    @GetMapping("/query1")
    public List<UserDTO> query1(){
        //**查询 返回对象 */
        List<UserDTO> listUsers = userService.listObjects(
                Condition.create()  //创建条件匹配器对象
                    .between("age",10,20)  //生成 age BETWEEN 10 AND 20
                    .eq("sex","男")        //生成  AND(sex = '男')
                    .eq("name","C","D","E")//生成 AND(name = 'C'  OR name = 'D' OR name = 'E')
                    .like("name","A", "B") //生成 AND(name LIKE '%A%' OR name LIKE '%B%')

                    //不等
                    .ne("name","张三","李四")

                    //自定义括号
                    .and("(")  //生成: and(
                    .eq("amount",10.10)
                    .or()//在此以后的所有字段都用 or
                    .eq("loginCount", 10)
                    .le("loginCount",11)
                    .add(")")// add 条件增加任意字符

                    .and()//因为前面写了 .or(),我们希望括号外面以后的的字段用and

                    .add("substring(name,2)='平' ")//自定义条件,要用到数据库的函数时可以这样写
                    .le("loginCount",1)//小于等于
                    .lt("loginCount",2)//小于
                    .ge("loginCount",4)//大于等于
                    .gt("loginCount",3)//大于

                    .isNull("name")
                    .isNotNull("name")
        );
        return listUsers;
    }

}
```
生成的WHERE条件如下:

```SQL
SELECT id,code,name,password,sex,age,amount,mobile,isAdmin,loginCount,lastDate,deptId,createUser,updateUser
FROM users a
WHERE age BETWEEN 10 AND 20
  AND (sex = '男')
  AND (name = 'C' OR name = 'D' OR name = 'E')
  AND (name LIKE '%A%' OR name LIKE '%B%')
  AND (name <> '张三' AND name <> '李四')
  AND ((amount = 10.1) OR (loginCount = 10) OR (loginCount <= 11))
  AND substring(name,2)='平' 
  AND (loginCount <= 1)
  AND (loginCount < 2)
  AND (loginCount >= 4)
  AND (loginCount > 3)
  AND name is null
  AND name is not null
LIMIT 0,6666
```

## 4.2. 例子二
```java
@RestController
@RequestMapping("/condition")
public class ConditionController {
    @Autowired
    UserService userService;
    @GetMapping("/query2")
    public List<UserDTO> query2(){
        //**查询 返回对象 */
        List<UserDTO> listUsers = userService.listObjects(
            userService.where()
                .eq("concat(name,id)","A1")          //生成 (concat(name,id) = 'A1')
                .eq("concat(name,id)","C1","D2","E3")//生成 AND (concat(name,id) = 'C1' OR concat(name,id) = 'D2' OR concat(name,id) = 'E3' )
        );
        return listUsers;
    }
}
```

生成的WHERE条件如下:

```sql
SELECT id,code,name,password,sex,age,amount,mobile,isAdmin,loginCount,lastDate,deptId,createUser,updateUser
FROM users a
WHERE (concat(name,id) = 'A1')
  AND (concat(name,id) = 'C1'
    OR concat(name,id) = 'D2'
    OR concat(name,id) = 'E3' )
```
