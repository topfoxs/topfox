 
# 1. [↖回到主目录](https://github.com/topfoxs/topfox)

# 2. com.topfox.common.Response< T > 返回结果对象
服务调用服务和前端调用后端的放回结果, 统一用此对象.

如若要符合标准http restful结构, 可在网关层转换, 单推荐微服务内部接口使用此结构, 方便对接调试等.

## 2.1. 主要属性
- boolean success 是否成功
- String code 异常编码
- String msg 详细信息
- String executeId 前端(请求方)每次请求传入的 唯一 执行Id, 如果没有, 后端会自动生成一个唯一的
- Integer totalCount 
    1. 查询结果的所有页的行数合计
    2. update insert delete SQL执行结果影响的记录数
- Integer pageCount 查询结果(当前页)的行数合计
- T data 泛型.  数据, List<xxxDTO> 或者是一个DTO
- String exception  失败时, 出现异常类的名字
- String method 失败时, 出现异常的方法及行数
- JSONObject attributes 自定义的数据返回, 这部分数据不方便定义到"T data"中时使用


## 2.2. 构造函数(无业务数据)
出错抛出异常时使用
- Response(Throwable e)
- Response(Throwable e, ResponseCode responseCode, String msg)
- Response(Throwable e, ResponseCode responseCode)
- Response(ResponseCode responseCode, String msg)
- Response(ResponseCode responseCode)
- Response(String code, String msg)

## 2.3. 构造函数(需要返回业务数据)
查询或者更新插入删除返回数据时使用.
- Response(T data)
- Response(T data, Integer totalCount)
    data为List<xxxDTO>, list.size()会设置为当前页行数pageCount, 总行数通过第2个参数传入进来. 本构造函数分页查询时常用.
    
    
## 2.4. 数据格式示例
- 失败出错返回的数据

```json

{
    "success": false,
    "code": "30020",
    "msg": "密码不正确,请重试",
    "executeId": "190409103131U951KMD495",
    "exception": "com.topfox.common.CommonException",
    "method": "com.user.service.UserService.login:190"
}
```

- 登陆成功返回的数据(attributes为权限信息):

```json
{
    "success": true,
    "code": "200",
    "msg": "请求成功",
    "executeId": "190409102553Y303TYC041",
    "totalCount": 1,
    "data": {
        "userId": "00022",
        "userName": "张三",
        "userCode": "00384",
        "userMobile": "*",
        "isAdmin": true
    },
    "attributes": {
        "secString": {
            "programList": {
                "btnSave": "0",
                "btnNew": "0",
                "btnDelete": "1",
                "btnExport": "1",
                "btnImport": "1"
            },
            "userProgramList": {
                "btnSave": "1",
                "btnNew": "0",
                "btnDelete": "0"
            }
        },
        "sessionId": "190409022553381R786S343"
    }
}
```
