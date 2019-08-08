package com.topfox.conf;

import com.topfox.common.DataDTO;
import com.topfox.common.IBean;
import com.topfox.common.Response;
import com.topfox.data.DbState;
import com.topfox.misc.JsonUtil;
import com.topfox.service.ISuperService;
import com.topfox.service.SuperService;
import com.topfox.sql.Condition;
import com.topfox.sql.EntitySelect;
import com.topfox.util.SysConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Aspect
@Component
public class ServiceAopConfig {
    protected Logger logger= LoggerFactory.getLogger(getClass());

    @Autowired
    private SysConfig sysConfig;//单实例读取值

    /**
     * 针对controller层入参/返回的数据格式化输出到 log里
     */
    @Pointcut(value = " execution(public * com.*.service..*.*(..)) "
            +"|| execution(public * com.*.service.*..*.*(..)) "
            +"|| execution(public * com.topfox.service.SuperService.*(..))"
            +"|| execution(public * com.topfox.service.SimpleService.*(..))"
            +"|| execution(public * com.topfox.service.AdvancedService.*(..))"
    )
    public void remoteLogPrint(){
    }

    @Around("remoteLogPrint()")
    public Object around(ProceedingJoinPoint joinPoint)throws Throwable{
        long start = System.currentTimeMillis();
        Object object;
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        ISuperService iSuperService;
        Object target=joinPoint.getTarget();
        if (target instanceof SuperService){
            if ("beforeInit".equals(joinPoint.getSignature().getName())){
                return null;
            }
            iSuperService=(ISuperService)target;
            List<DataDTO> listUpdate=null;
            for(Object arg : args) {
                if (arg instanceof ArrayList) {
                    //更新时, dto 数据处理
                    ArrayList lisTemp = (ArrayList) arg;
                    if (lisTemp.isEmpty()) break;
                    Object dto = lisTemp.get(0);
                    if (dto instanceof DataDTO == false) break;

                    if (methodName.equals("updateList")){
                        listUpdate = lisTemp;
                    }else if (methodName.startsWith("save")){
                        ArrayList<DataDTO> listSave = (ArrayList<DataDTO>)lisTemp;

                        listUpdate = listSave.stream().filter(beanDTO ->
                                DbState.UPDATE.equals(beanDTO.dataState())).collect(toList()
                        );//所有更新的记录
                    }
                }else if(arg instanceof DataDTO
                        &&  methodName.equals("update")
                        && !methodName.equals("updateBatch"))//这是个例外
                {
                    //当个DTO的更新
                    listUpdate = new ArrayList();
                    listUpdate.add((DataDTO)arg);
                }
            }
            iSuperService.beforeInit(listUpdate);// listUpdate 更新时 对 DTO的处理用, 重要.
        }

        try {
            if (logger.isDebugEnabled()) {
                StringBuilder remote_begin = new StringBuilder();
                remote_begin.append("###### remote-begin : ")
                        .append(joinPoint.getTarget().getClass().getName())
                        .append(".")
                        .append(joinPoint.getSignature().getName())
                        .append("()");
                logger.debug(remote_begin.toString());

                int i = 1;
                for (Object arg : args) {
                    arg = arg == null ? "null" : arg;
                    StringBuilder remote_param = new StringBuilder();
                    remote_param.append(sysConfig.getLogPrefix()).append("remote-param(")
                            .append(i++)
                            .append(") : ");
                    if (arg instanceof Condition) {
                        remote_param.append("Condition");
                    }else if (arg instanceof EntitySelect){
                        remote_param.append("EntitySelect");
                    }else{
                        remote_param.append(arg.toString());
                    }
                    logger.debug(remote_param.toString());
                }
            }
        }finally {

        }

        object = joinPoint.proceed();

        try {
            // debug 时, 打印信息处理
            if (logger.isDebugEnabled()) {
                long end = System.currentTimeMillis();
                StringBuilder time_use = new StringBuilder();
                time_use.append(sysConfig.getLogPrefix()).append("remote-time-use : ")
                        .append(end - start)
                        .append("ms");
                logger.debug(time_use.toString());

                if (object != null) {
                    StringBuilder time_response = new StringBuilder();
                    time_response.append(sysConfig.getLogPrefix()).append("remote-response : ");
                    if (object instanceof Number || object instanceof String) {
                        time_response.append(object);
                    }else if (object instanceof DataDTO){
                        time_response.append(object.toString());
                    }else if (object instanceof IBean || object instanceof List || object instanceof Response){
                        time_response.append(JsonUtil.toJson(object));
                    }else{
                        time_response.append(object.getClass().getName());
                    }
                    logger.debug(time_response.toString());
                }

                StringBuilder time_end = new StringBuilder();
                time_end.append("###### remote-end : ")
                        .append(joinPoint.getTarget().getClass().getName())
                        .append(".")
                        .append(joinPoint.getSignature().getName())
                        .append("()");
                logger.debug(time_end.toString());
            }
        }finally {
        }

        return object;
    }

}
