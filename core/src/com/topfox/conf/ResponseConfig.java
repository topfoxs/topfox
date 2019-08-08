package com.topfox.conf;


import com.topfox.common.*;
import com.topfox.data.DbState;
import com.topfox.misc.BeanUtil;
import com.topfox.util.AbstractRestSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ControllerAdvice
public class ResponseConfig implements ResponseBodyAdvice {
    protected Logger logger= LoggerFactory.getLogger(getClass());

    @Autowired
    AbstractRestSessionHandler abstractRestSessionHandler;

    @Autowired @Qualifier("sysConfigDefault")
    protected SysConfigRead sysConfigRead;//单实例读取值 全局一个实例

    /**
     * Closing non transactional SqlSession 事务结束之后才会执行
     * @return
     */
    @Override
    public Object beforeBodyWrite(Object result,
                                  MethodParameter methodParameter,
                                  MediaType mediaType,
                                  Class clazz,
                                  ServerHttpRequest httpRequest,
                                  ServerHttpResponse httpResponse)
    {
        //拦截控Cotroller层 返回的对象

        //不拦截swagger controller path地址
        if(httpRequest.getURI().getPath().startsWith("/swagger")){
            return result;
        }

        Method method=methodParameter.getMethod();
        Class<?> returnClass = method.getReturnType();
        // Response 处理
        if (Response.class.getName().equals(returnClass.getName())) { //返回类型是  Response 时
            Response response = result==null?new Response(ResponseCode.SUCCESS):(Response)result;
            initResponse(httpRequest, response, method);//重要方法  逻辑处理
            return response;
        }

        if(result instanceof LinkedHashMap){
            //原生的错误
            /**  如打印一下错误
             {
             timestamp=Mon Oct 22 10:28:04 CST 2018,
             status=404,
             error=Not Found,
             message=No message available,
             path=/static/bootstrap/bootstrap.min.css.map
             */
            Map mapReturn = (LinkedHashMap )result;
            if (mapReturn.containsKey("status") && mapReturn.containsKey("message")
                    && method.getDeclaringClass().getName().equals("org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController")){
                ((LinkedHashMap)result).forEach((key,value)->{
                    logger.debug("{}",value);
                });
            }
        }

        return result;
    }

    //处理返回的对象是否要处理为 空对象
    private void initResponse(ServerHttpRequest httpRequest, Response response, Method method){

        Type type = method.getGenericReturnType();
        Type[] types;

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //获得控制层 方法返回的 Response<T> T 的实际类型  //自己摸索的精华代码
        while (type instanceof ParameterizedType) {
            ParameterizedType type2 = (ParameterizedType) type;
            Class rawType = (Class) type2.getRawType();
            if (Collection.class.isAssignableFrom(rawType)
                    || Map.class.isAssignableFrom(rawType)
                    || DataDTO.class.isAssignableFrom(rawType)) {
                type = rawType;
                break;
            }
            if (Response.class.isAssignableFrom(rawType)){
                type = rawType;
            }
            types = type2.getActualTypeArguments();
            if (types != null && types.length >= 0) {
                type = types[0];
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        checkSave(response);

        if (response.getDataType() ==null ){
            response.setDataType(type);
        }

        String userAgent;
        AbstractRestSession restSession= abstractRestSessionHandler.get();
        if (restSession !=null && restSession.getSysUserAgent() != null) {
            userAgent = restSession.getSysUserAgent();
        }else{
            List<String> list = httpRequest.getHeaders().get("User-Agent");
            userAgent = (list != null && list.size() > 0) ? list.get(0) : "";
            userAgent = userAgent == null ? "" : userAgent.toLowerCase();
        }

        //增加参数, 默认false,
        if (response.getData()==null
                && type instanceof Class
                && !userAgent.contains("httpclient")
                && !userAgent.contains("java")
                //&& restSession.getSysUserAgent().contains("mozilla")   //说明是浏览器访问的
        ){
            //浏览器为调用方时的 特殊处理
            if (Collection.class.isAssignableFrom((Class)type)) {
                //解决 JSON 输出 {data:[]}
                response.setData(new ArrayList<>());
            }else if (Map.class.isAssignableFrom((Class)type)) {
                //解决 JSON 输出 {data:{}}
                response.setData(new HashMap<>());
            }else if (DataDTO.class.isAssignableFrom((Class)type)) {
                DataDTO dataDTO = (DataDTO)BeanUtil.newInstance((Class)type);
                dataDTO.clearAllData();
                //解决 JSON 输出 {data:{}}
                response.setData(dataDTO);
            }else if (String.class.isAssignableFrom((Class)type)) {
                //解决 JSON 输出 {data:""}
                response.setData("");
            }

            response.setTotalCount(0);
            response.setPageCount(0);
        }

        if (restSession == null) {
            logger.warn("restSession is null");
        } else {
            response.setExecuteId(restSession.getExecuteId());
        }

        if (response.getAttributes() != null && response.getAttributes().size() == 0){
            response.setAttributes(null);
        }
    }

    //实现修改保存返回更新过的字段功能
    private Response checkSave(Response response){
        Object dataCheckSave = response.getData();
        //判断DTO被更新过时
        AtomicBoolean isSaveDTO= new AtomicBoolean(false);
        List list = null;
        if (dataCheckSave instanceof List) {
            //当前页的行数
            list = ((List) dataCheckSave);
            if (list.isEmpty()) {
                return response;
            }
            list.forEach(row->{
                if (row instanceof DataDTO) {
                    DataDTO dto = (DataDTO)row;
                    if ( isSaveDTO.get() == false && dto.mapSave()!=null){ // 有 提交的数据的时
                        isSaveDTO.set(true);
                        return;
                    }

                    if (dto.mapSave() != null && DbState.UPDATE.equals(dto.dataState())) {
                        isSaveDTO.set(true);
                        return;
                    }
                }
            });

        } else if (dataCheckSave instanceof DataDTO && ((DataDTO) dataCheckSave).mapSave()!=null) {
            isSaveDTO.set(true);
            list = new ArrayList(1);
            list.add(dataCheckSave);
        }

        if (list ==null || isSaveDTO.get() == false) {
            return response;
        }

        //把保存的数据 转成 Map, 以便只返回有 更新的字段给 前端(或调用者)
        List listOutSave = new ArrayList<>();
        list.forEach(row ->{
            if (row instanceof DataDTO) {
                DataDTO dto = (DataDTO)row;
                if (DbState.UPDATE.equals(dto.dataState())) {
                    listOutSave.add(dto.mapSave());
                }else{
                    listOutSave.add(dto);
                }
            }
        });

        response.setData(listOutSave);

        return response;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }
}
