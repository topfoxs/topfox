# 1. [↖回到主目录](https://github.com/luoping2019/topfox)

# 2. 更新日志组件 ChangeManager
读取修改日志的代码很简单, 共写了2个例子,  如下:

```java
@Service
public class UserService extends AdvancedService<UserDao, UserDTO> {
     @Override
    public void afterInsertOrUpdate(UserDTO userDTO, String state) {
        if (DbState.UPDATE.equals(state)) {
            // 例一:
            ChangeManager changeManager = changeManager(userDTO)
                                .addFieldLabel("name", "用户姓名")  //设置该字段的日志输出的中文名
                                .addFieldLabel("mobile", "手机号"); //设置该字段的日志输出的中文名
                    
            //输出 方式一 参数格式
            logger.debug("修改日志:{}", changeManager.output().toString() );
            // 输出样例: 
            /**
                修改日志:
                 id:000000,      //用户的id
                 用户姓名:开发者->开发者2,
                 手机号:13588330001->1805816881122
            */
            
            // 输出 方式二 JSON格式
            logger.debug("修改日志:{}", changeManager.outJSONString() );
            // 输出样例:  c是 current的简写, 是当前值, 新值; o是 old的简写, 修改之前的值
            /**
                修改日志:
                 {
                     "appName":"sec",
                     "executeId":"1561367017351_14",
                     "id":"000000",
                     "data":{
                             "version":{"c":"207","o":206},
                             "用户姓名":{"c":"开发者2","o":"开发者"},
                             "手机号":{"c":"1805816881122","o":"13588330001"}
                     }
                }
            */

            //************************************************************************************
            // 例二  没有用 addFieldLabel 设置字段输出的中文名, 则data中的keys输出全部为英文
            logger.debug("修改日志:{}", changeManager(userDTO).outJSONString() );
            // 输出 JSON格式
            /**
                修改日志:
                 {
                     "appName":"sec",
                     "executeId":"1561367017351_14",
                     "id":"000000",
                     "data":{
                             "version":{"c":"207","o":206},
                             "name":{"c":"开发者2","o":"开发者"},
                             "mobile":{"c":"1805816881122","o":"13588330001"}
                     }
                }
            */
            //************************************************************************************
        }
    }
}
```