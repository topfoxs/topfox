# 1. [↖回到主目录](https://gitee.com/topfox/topfox/blob/dev/README.md)

# 2. 快速使用
## 2.1. 必备源码
```
必备源码文件(/src目录下):
com.AppSample.java
com.config.DataSourceConfig.java
com.config.MyBatisConfig.java
com.sys.RestSesson.java
com.sys.RestSessionConfig.java

必备资源文件(/resources目录下):
application.properties
logback-spring.xml
```

## 2.2. 新建 RestSession 对象

- 项目中必须有且只能有一个 AbstractRestSession 的实现类, 我们命名为 RestSession.java
- 代替 Web Session对象, 这是一个线程安全的容器
- 用户登录信息, Token信息放入到这个对象中

其源码如下:


```java
package com.sys;

import com.topfox.annotation.Table;
import com.topfox.common.AbstractRestSession;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
 * 代替 web session
 * 是一个简单的POJO对象, 禁止放入连接数据库等无法序列化的对象
 */
@Getter
@Setter
@Accessors(chain = true)
public class RestSession extends AbstractRestSession {
    private String userId;
    private String userName;
    private Boolean isAdmin=false;  //是否管理员
    //根据项目需要自定
}
```

## 2.3. 实现 RestSessionHandler 超类
- 项目中必须有且只能有一个 AbstractRestSessionHandler 的实现类, 我们命名为 RestSessionHandler.java, 其源码如下:
- 详见源码: 

```java
package com.sys;

import com.topfox.util.AbstractRestSessionHandler;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Component
public class RestSessionHandler extends AbstractRestSessionHandler<com.sys.RestSession> {

    @Override
    public void initRestSession(com.sys.RestSession restSession, Method method) {
        //每次请求都会调用这个方法
        //初始化 RestSession 对象的数据
        //调用方每次请求传入 sessionId, 这里可以从Redis读取出来, 把数据复制到 restSession中
    }

    @Override
    public void save(RestSession restSession) {
        //例: 登陆成功调用, 保存RestSession 到Redis
    }
}
```

## 2.4. 数据源配置 DataSourceConfig
- 数据库的账号  密码等配置必须的配置信息
- 详见源码 com.config..DataSourceConfig.java

## 2.5. MyBatisConfig 配置
- 设置使用TopFox的拦截器插件MyBatisInterceptor, 目的是打印SQL,TopFox对输出的SQL做了美化处理; 
- 注意,需要把springboot dao层打印sq的功能关闭, 不然sql打印会重复.   demo项目的dao接口在com.user.dao下,因此资源文件 logback-spring.xml 的dev 增加以下配置:


```xml
<logger name="com.user.dao" level="OFF" />
```

MyBatisConfig.java源码如下: 

- MyBatisConfig.java源码,  请注意 @MapperScan("com.*.dao") 这个是自己项目 dao接口的路径, 

```java
package com.config;

@Configuration
@MapperScan("com.*.dao")
@EnableTransactionManagement
public class MyBatisConfig {
    @Autowired private Environment env;
    @Autowired DataSource dataSource;

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTypeAliasesPackage(env.getProperty("mybatis.type-aliases-package"));
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(env.getProperty("mybatis.mapper-locations")));

        //增加 MyBatis SQL拦截器
        bean.setPlugins(new Interceptor[] {new MyBatisInterceptor()});

        /**
         * 配置驼峰命名转换   true 开启
         */
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);

        return bean.getObject();
    }
    ...略
}
```

# 3. 简单的例子
- 以部门表为例, 实现简单的例子.

## 3.1. 部门建表语句

```sql
DROP TABLE IF EXISTS `depts`;
CREATE TABLE "depts" (
  "deptId" bigint(26) NOT NULL AUTO_INCREMENT COMMENT '主键Id',
  "deptName" varchar(26) NOT NULL,
  "deptManager" varchar(26) DEFAULT NULL,
  PRIMARY KEY ("deptId")
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_german2_ci;

BEGIN;
INSERT INTO `depts` VALUES (1, 'IT部门', '罗平');
INSERT INTO `depts` VALUES (2, '财务部', '张三');
INSERT INTO `depts` VALUES (3, '行政部', '李四');
COMMIT;
```


## 3.2. 新建 DeptDTO.java

```java
package com.user.entity;

import com.topfox.annotation.Id;
import com.topfox.annotation.Table;
import com.topfox.common.DataDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
@Table(name = "depts")
public class DeptDTO extends DataDTO {

    /**
     * 部门Id
     */
    @Id private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门经理/经理姓名
     */
    private String deptManager;

}

```

## 3.3. 新建 DeptQTO.java

```java
package com.user.entity;

import com.topfox.annotation.Id;
import com.topfox.annotation.Table;
import com.topfox.common.DataQTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
@Table(name = "depts")
public class DeptQTO extends DataQTO {
    @Id private Integer deptId;
    private String deptName;
    private String deptManager;
}
```


## 3.4. 新建 DeptDao.java层 和 DeptMapper.xml
DeptDao.java:

```java
package com.user.dao;

import com.topfox.mapper.BaseMapper;
import com.user.entity.DeptDTO;
import org.springframework.stereotype.Component;

@Component
public interface DeptDao extends BaseMapper<DeptDTO> {
    
}

```

- 需要说明的是, 单表查询时, DeptMapper.xml 文件是不需要, 没有该文件不会报错.  一般存在多表联合查询时才会有 Mapper.xml 文件
- Topfox针对单表的所有复杂查询, 新增/修改和删除, 都可以自动生成SQL, 所以一般情况下, xxxMapper.xml 可以没有, 有的话也是很干净的. 
- DeptMapper.xml 源码如下:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.user.dao.DeptDao">
	<resultMap id="list" type="com.user.entity.DeptDTO">
	</resultMap>
	<sql id="whereClause" >
		<where>
			<if test = "deptId != null">     AND a.deptId      = #{deptId}</if>
			<if test = "deptName != null">   AND a.deptName    = #{deptName}</if>
			<if test = "deptManager != null">AND a.deptManager = #{deptManager}</if>

		</where>
	</sql>

	<select id="list" resultMap="list">
		SELECT a.deptId, a.deptName, a.deptManager
		FROM depts a
		<include refid="whereClause"/>
		<if test="limit != null">${limit}</if>
	</select>
	<select id="listCount" resultType="int">
		SELECT count(*)
		FROM depts a
		<include refid="whereClause"/>
	</select>
</mapper>
```


## 3.5. 新建服务层 DeptService.java
- 虽然以下代码很简单,  但<<核心使用>>章节里面的所有功能方法都已自动具备. 如 getObject listObjects selectMaps selctCount selectMax等若干. 这也是TopFox的优势所在

```java
package com.user.service;

import com.topfox.common.DataQTO;
import com.topfox.common.Response;
import com.topfox.service.AdvancedService;
import com.user.dao.DeptDao;
import com.user.entity.DeptDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptService extends AdvancedService<DeptDao, DeptDTO> {

    @Override
    public int insert(DeptDTO dto) {
        return super.insert(dto);
    }

    @Override
    public int update(DeptDTO dto) {
        return super.update(dto);
    }

    @Override
    public int deleteByIds(Number... ids) {
        return super.deleteByIds(ids);
    }

    @Override
    public Response<List<DeptDTO>> list(DataQTO qto) {
        return super.list(qto);
    }
}

```


## 3.6. 新建控制层 DeptController.java
```java
package com.user.controller;

import com.topfox.annotation.TokenOff;
import com.topfox.common.Response;
import com.topfox.misc.Misc;
import com.user.entity.DeptDTO;
import com.user.entity.DeptQTO;
import com.user.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public class DeptController {
    @Autowired
    DeptService deptService;

    @PostMapping
    public Response<DeptDTO> insert(@RequestBody DeptDTO dto) {
        deptService.insertGetKey(dto);
        return new Response<>(dto, 1);
    }

    @PutMapping
    public Response<DeptDTO> update(@RequestBody DeptDTO dto) {
        Misc.checkObjNotNull(dto, "id");
        int count = deptService.update(dto);
        return new Response<>(dto, count);
    }

    @DeleteMapping("/{ids}")
    public Response<Integer> delete(@PathVariable("ids") Long ids) {
        Misc.checkObjNotNull(ids, "ids");
        int count = deptService.deleteByIds(ids);
        return new Response<>(count);
    }

    @GetMapping
    public Response<List<DeptDTO>> list(DeptQTO qto) {
        return deptService.list(qto);
    }

    @GetMapping("/{id}")
    public Response<DeptDTO> get(@PathVariable("id") Long id) {
        Misc.checkObjNotNull(id, "id");

        DeptDTO detp = deptService.getObject(id);
        return new Response(detp);
    }

}

```



