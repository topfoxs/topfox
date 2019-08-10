# 1. [↖回到主目录](https://github.com/topfoxs/topfox)

# 2. 自动填充组件 FillDataHandler

- 注解填充字段 `@TableField(fillUpdate = true, fillInsert = true)`

```java
@Setter
@Getter
@Accessors(chain = true)
@Table(name = "users")
public class UserDTO extends DataDTO {
     /**
     * 创建人
     * 注意！这里需要标记为 插入填充
     */
    @TableField(fillInsert = true)
    private String createUser;


    /**
     * 修改人
     * 注意！这里需要标记为 插入和更新填充
     */
    @TableField(fillUpdate = true, fillInsert = true)
    private String updateUser;
    ....
}


```


- 自定义实现类 FillDataHandlerConfig

```java
/**
 * DTO实体对象注解的 填充组件实现, 实现接口  FillDataHandler
 */
 @Component
public class FillDataHandlerConfig implements FillDataHandler {
    @Autowired
    RestSessionConfig restSessionConfig;

    /**
     * 新增和更新的填充
     * @param fillFields 新增或者更新填充的字段
     * @param list 新增或者更新的数据
     */
    @Override
    public void fillData(Map<String, Field> fillFields, List<DataDTO> list) {
        RestSession restSession=restSessionConfig.get();
        list.forEach((dto)->{
            String state = dto.dataState();
            fillFields.forEach((fieldName,field)->{
                //新增时这两个字段都要赋值
                if (DbState.INSERT.equals(state) && (field.isFillUpdate() ||field.isFillInsert())){
                    switch (fieldName){
                        case "createUser":
                        case "updateUser":
                            dto.setValue(fieldName,restSession.getUserName());
                    }
                }

                //更新时只对一个字段赋值
                if (DbState.UPDATE.equals(state) && field.isFillUpdate()){
                    switch (fieldName){
                        case "updateUser":
                            dto.setValue(fieldName,restSession.getUserName());
                            break;
                    }
                }
            });
        });
    }
}
```

::: warning 注意事项

- 字段必须声明`TableField`注解，属性`fillUpdate,fillInsert` 选择对应策略
- 填充组件 `FillDataHandlerConfig` 在 Spring Boot 中需要声明 `@Component`  注入
- 必须使用父类 DataDTO 的 setValue() 赋值
