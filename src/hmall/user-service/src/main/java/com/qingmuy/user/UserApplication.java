package com.qingmuy.user;

import com.qingmuy.api.config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.qingmuy.user.mapper")
@SpringBootApplication
@EnableFeignClients(basePackages = "com.qingmuy.api.client", defaultConfiguration = DefaultFeignConfig.class)
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

}