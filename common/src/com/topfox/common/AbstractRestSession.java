package com.topfox.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.topfox.annotation.Id;
import com.topfox.annotation.Ignore;
import com.topfox.misc.*;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

@Setter
@Getter
@Accessors(chain = true)
public abstract class AbstractRestSession extends DataDTO {
    private static final long serialVersionUID = 1L;

    @Id private String sessionId;
    @JsonIgnore transient private String executeId;       //分布式事务-主事务号
    @JsonIgnore transient private String subExecuteId;    //分布式事务-子事务号
    @JsonIgnore transient private String routeExecuteIds=""; //分布式事务-事务号调用链路/路径
    @JsonIgnore transient private String routeAppName="";

    @JsonIgnore transient private String appName=""; //当前的 spring.application.name

    @JsonIgnore @Ignore transient private String sysUserId="*";
    @JsonIgnore @Ignore transient private String sysUserName="*";
    @JsonIgnore @Ignore transient private String sysUserCode="";
    @JsonIgnore @Ignore transient private String sysUserMobile="";

    /** 第一次创建Session 的时间，即登陆系统的时间 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date sysCreateDate;

    /** 活动时间，登陆后，任何一次请求都会更新此时间*/
    @JsonIgnore @Ignore transient private long sysActiveDate=0;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonIgnore @Ignore transient private Date sysNowTime=null;

    /**
     * 自定义容器 Map, 用于临时放入的数据
     */
    @JsonIgnore @Ignore transient JSONObject attributes=new JSONObject();

    /****************************************
     * @JsonIgnore  //不需要转json的属性用这个 注解
     * @JsonIgnoreProperties(ignoreUnknown = true)
     * ******************************/
    //@JsonIgnore @Ignore transient static public long maxId=0;

    /**
     * 请求地址?后的参数 + 请求头信息 + form-data的数据
     */
    @JsonIgnore @Ignore transient private JSONObject requestData;

    /**
     * 请求头信息
     */
    @JsonIgnore @Ignore transient private JSONObject headers;

    /**
     * 请求 body 数据
     */
    @JsonIgnore @Ignore transient private JSONArray bodyData;

    /**
     * 用户代理头, 用来判断调用方是什么软件, 可能会是
     *  java 微服务调用        Apache-HttpClient
     *  Postman               PostmanRuntime/7.15.0
     *  macos chrmoe浏览器    Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36
     *  华为手机 微信内置浏览器 Mozilla/5.0 (Linux; Android 9; SEA-AL10 Build/HUAWEISEA-AL10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.126 MQQBrowser/6.2 TBS/044705 Mobile Safari/537.36 MMWEBID/4122 MicroMessenger/7.0.5.1440(0x27000537) Process/tools NetType/WIFI Language/zh_CN
     *  window10 chrome浏览器 Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36
     *  window10 ie          Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko
     */
    @JsonIgnore @Ignore transient private String sysUserAgent;

    /**********************************************************************/
    public AbstractRestSession(){
        requestData = new JSONObject();
    }

    @Deprecated
    public void init(){}

    @Override
    public String toString(){
        return JsonUtil.toString(this);
    }

}
