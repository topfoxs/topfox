package com.topfox.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 自己定义的拦截器类
     * @return
     */
    @Bean
    WebMvcInterceptor webMvcInterceptor() {
        return new WebMvcInterceptor();
    }

    /**
     * 跨域支持  CORS
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
//        // 设置了可以被跨域访问的路径和可以被哪些主机跨域访问
//        registry.addMapping("/**")
//                .allowedOrigins("*");
//                //.allowedOrigins("http://localhost:8085");

        //设置允许跨域的路径
        registry.addMapping("/**")
                //设置允许跨域请求的域名
                .allowedOrigins("*")
                //是否允许证书 不再默认开启
                .allowCredentials(true)
                //设置允许的方法
                .allowedMethods("*");

    }

    /**
     * 添加拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webMvcInterceptor());//.addPathPatterns("/api/**");
    }
}
