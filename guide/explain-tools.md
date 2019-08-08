# 1. [↖回到主目录](https://gitee.com/topfox/topfox/blob/dev/README.md)

# 2. 上下文对象 AppContext

## 2.1. getBean(String name)
- 静态方法, 根据指定的名称从Ioc容器中获得Bean

## 2.2. getBean(Class<T> clazz)
- 静态方法, 根据指定的Class 从Ioc容器中获得Bean

## 2.3. getBean(String name,Class<T> clazz)
- 静态方法, 根据指定的名称和Class 从Ioc容器中获得Bean

## 2.4. getRestSessionHandler()
- 静态方法, 返回 单实例对象 RestSessionHandler, 它是 com.topfox.util.AbstractRestSessionHandler 的实现类对象

## 2.5. getRestSession()
- 静态方法, 返回 当前线程的 RestSession对象, 它是 com.topfox.common.AbstractRestSession 的实现类对象

## 2.6. getRestSession(Class<T> clazz)
- 静态方法, 返回 当前线程的 RestSession对象, 它是 com.topfox.common.AbstractRestSession 的实现类对象

## 2.7. getSysConfig()
静态方法,  获得TopFox的参数对象

## 2.8. environment()
静态方法, 获得 org.springframework.core.env.Environment 对象


# 3. 上下文对象 AppContext 如何使用
下面源码中的 RestSession和RestSessionConfig对象可以参考 <<快速使用>>章节中的相关内容

AppContext 提供了几个静态方法, 直接获取相关对象.

```java
package com.user.controller;

import com.topfox.annotation.TokenOff;

import com.sys.RestSession;
import com.sys.RestSessionHandler;
import com.topfox.common.AppContext;
import com.topfox.common.SysConfigRead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/context")
public class AppContextController {
    @GetMapping("/test1")
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
         * 以下3中获取 useName 的方法
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

    /**
     * AppContext.getRestSessionHandler()是同一个实例
     */
    @Autowired
    RestSessionHandler restSessionHandler;
    public void test2() {
        RestSession restSession = restSessionHandler.get();
        SysConfigRead configRead = restSessionHandler.getSysConfig();
    }
}
```
