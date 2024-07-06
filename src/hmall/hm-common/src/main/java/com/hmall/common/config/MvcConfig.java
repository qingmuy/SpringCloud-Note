package com.hmall.common.config;

import com.hmall.common.interceptors.UserInfoInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/*
*   如果不加入@ConditionalOnClass(DispatcherServlet.class)注解，会导致启动失败
*   这是因为MvcConfig继承了WebMvcConfigurer，而WebMvcConfigurer是Spring MVC包内的内容
*   然而Gateway网关并不是Spring MVC而是基于WebFlags的响应式编程
*   这就需要开启@ConditionalOnClass注解，通过识别当前微服务中是否有MVC的核心API（DispatcherServlet.class）来判断是否有Spring MVC
*   此时由于网关中并没有该MVC的核心API，所以拦截器不会生效
* */
@ConditionalOnClass(DispatcherServlet.class)
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 默认拦截所有路径
        registry.addInterceptor(new UserInfoInterceptor());
    }
}
