# Spring Cloud



## Docker

Docker的常用命令参考：

[docker常规操作——启动、停止、重启容器实例_docker容器重启-CSDN博客](https://blog.csdn.net/Michel4Liu/article/details/80889977)

[docker查看容器/删除(所有)容器/删除镜像_查看docker删除运行的容实例-CSDN博客](https://blog.csdn.net/HYZX_9987/article/details/118999188)

[设置docker开机自启动，并设置容器自动重启_小雅全家桶容器重启-CSDN博客](https://blog.csdn.net/chj_1224365967/article/details/109029856)



## Spring Cloud



### 服务拆分

> - **高内聚**：每个微服务的职责要尽量单一，包含的业务相互关联度高、完整度高。
> - **低耦合**：每个微服务的功能要相对独立，尽量减少对其它微服务的依赖，或者依赖接口的稳定性要强。

> **高内聚**首先是**单一职责，**但不能说一个微服务就一个接口，而是要保证微服务内部业务的完整性为前提。目标是当我们要修改某个业务时，最好就只修改当前微服务，这样变更的成本更低。
>
> 一旦微服务做到了高内聚，那么服务之间的**耦合度**自然就降低了。
>
> 当然，微服务之间不可避免的会有或多或少的业务交互，比如下单时需要查询商品数据。这个时候我们不能在订单服务直接查询商品数据库，否则就导致了数据耦合。而应该由商品服务对应暴露接口，并且一定要保证微服务对外**接口的稳定性**（即：尽量保证接口外观不变）。虽然出现了服务间调用，但此时无论你如何在商品服务做内部修改，都不会影响到订单微服务，服务间的耦合度就降低了。

拆分方式有如下两种：

- **纵向**拆分
- **横向**拆分

> 所谓**纵向拆分**，就是按照项目的功能模块来拆分。例如黑马商城中，就有用户管理功能、订单管理功能、购物车功能、商品管理功能、支付功能等。那么按照功能模块将他们拆分为一个个服务，就属于纵向拆分。这种拆分模式可以尽可能提高服务的内聚性。
>
> 而**横向拆分**，是看各个功能模块之间有没有公共的业务部分，如果有将其抽取出来作为通用服务。例如用户登录是需要发送消息通知，记录风控数据，下单时也要发送短信，记录风控数据。因此消息发送、风控数据记录就是通用的业务功能，因此可以将他们分别抽取为公共服务：消息中心服务、风控管理服务。这样可以提高业务的复用性，避免重复开发。同时通用业务一般接口稳定性较强，也不会使服务之间过分耦合。



### 服务治理

拆分服务后，不同的模块的业务需求多数会与其他模块产生联系，此时需要远程调用其他模块的接口获取数据，较为原始的方法有RestTemplate，但其缺点在于代码死板，不能灵活变通，而且代码量大，所以引入注册中心的概念：通过注册中心将不同的模块引入，便于统一管理和调用。



### 注册中心

> 在微服务远程调用的过程中，包括两个角色：
>
> - 服务提供者：提供接口供其它微服务访问，比如`item-service`
> - 服务消费者：调用其它微服务提供的接口，比如`cart-service`



#### Nacos注册中心

阿里巴巴开发，目前被集成在SpringCloudAlibaba中，一般用于Java应用，同时还存在Eureka（Netflix公司出品），Consul（HashiCorp公司出品），均被集成再Spring Cloud中。



Nacos的使用十分简单，步骤如下：

- 引入依赖
- 配置Nacos地址
- 重启

引入Nacos依赖：

```XML
<!--nacos 服务注册发现-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

配置Nacos：

```YAML
spring:
  application:
    name: item-service # 服务名称
  cloud:
    nacos:
      server-addr: 192.168.150.101:8848 # nacos地址
```



### OpenFeign

> OpenFeign利用SpringMVC的相关注解来声明请求方式、请求路径、请求参数、返回值类型，然后基于动态代理帮我们生成远程调用的代码，而无需我们手动再编写，非常方便。



#### 使用

##### 导入依赖

在`cart-service`服务的pom.xml中引入`OpenFeign`的依赖和`loadBalancer`依赖：

```XML
  <!--openFeign-->
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-openfeign</artifactId>
  </dependency>
  <!--负载均衡器-->
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
```



##### 启用OpenFeign

在启动类上添加注解`@EnableFeignClients`，启动OpenFeign功能：



##### 编写OpenFeign客户端

创建接口类，实现方式如Controller接口一致，无需实现。需要注意的是要使用`@FeignClient("服务名称")`为接口类声明服务名称。



#### 连接池

Feign底层发起http请求，依赖于其它的框架。其底层支持的http客户端实现包括：

- HttpURLConnection：默认实现，不支持连接池
- Apache HttpClient ：支持连接池
- OKHttp：支持连接池

因此我们通常会使用带有连接池的客户端来代替默认的HttpURLConnection。比如，我们使用OK Http.

连接池可以优化OpenFeign的性能。



##### 引入依赖

```XML
<!--OK http 的依赖 -->
<dependency>
  <groupId>io.github.openfeign</groupId>
  <artifactId>feign-okhttp</artifactId>
</dependency>
```



##### 开启连接池

在`application.yml`配置文件中开启Feign的连接池功能：

```YAML
feign:
  okhttp:
    enabled: true # 开启OKHttp功能
```



#### 最佳实践

对于某些客户端而言，其会被多个模块使用，这就造成了重复编码，应对的思路就是将重复的代码抽取出来，一般有两种方式：

- 思路1：抽取到微服务之外的公共module
- 思路2：每个微服务自己抽取一个module

![img](./assets/1720094147212-1.jpeg)

两个方案之间的差别在于：

方案1抽取更加简单，工程结构也比较清晰，但缺点是整个项目耦合度偏高。

方案2抽取相对麻烦，工程结构相对更复杂，但服务之间耦合度降低。



个人看法：方案一便于维护



##### 抽取Feign客户端

1. 新建一个模块
2. 导入依赖
3. 建立domain类和client类
4. 在client类中编写接口

需要注意的是：当其他模块需要调用接口时，请求参数和返回参数的接值都需要使用该模块中的domain类中的类



##### 扫描包配置

调用API的模块扫描不到客户端，此时需要进行配置

- 声明扫描包：

​	在启动类上启用注解`@EnableFeignClients(basePackages = "Feign客户端所在包")`

- 声明要用的FeignClient

​	单独声明，`@EnablefeignClients(clients = {client1, client2,...})`



#### 日志配置

> OpenFeign只会在FeignClient所在包的日志级别为**DEBUG**时，才会输出日志。而且其日志级别有4级：
>
> - **NONE**：不记录任何日志信息，这是默认值。
> - **BASIC**：仅记录请求的方法，URL以及响应状态码和执行时间
> - **HEADERS**：在BASIC的基础上，额外记录了请求和响应的头信息
> - **FULL**：记录所有请求和响应的明细，包括头信息、请求体、元数据。
>
> Feign默认的日志级别就是NONE，所以默认我们看不到请求日志。



##### 定义日志级别

定义一个config类，新建配置类，定义Feign的日志级别

```java
package com.qingmuy.api.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
```



##### 配置

要让日志级别生效，还需要配置这个类。有两种方法：

- **局部**生效：在某个`FeignClient`中配置，只对当前`FeignClient`生效

```Java
@FeignClient(value = "item-service", configuration = DefaultFeignConfig.class)
```

- **全局**生效：在`@EnableFeignClients`中配置，针对所有`FeignClient`生效。

```Java
@EnableFeignClients(defaultConfiguration = DefaultFeignConfig.class)
```



### 网关路由

前端在于后端交互时，前端中的后端URI地址往往是写死的，这与微服务的多实例特性冲突，所以需要一个统一的地址来转发不同微服务之间的数据。



#### 使用

大概步骤如下：

- 创建网关微服务
- 引入SpringCloudGateway、NacosDiscovery依赖
- 编写启动类
- 配置网关路由



##### 创建项目

创建一个新的模块，作为网关微服务



##### 引入依赖

在该模块下引入依赖：

```XML
<dependencies>
    <!--common-->
    <dependency>
        <groupId>com.heima</groupId>
        <artifactId>hm-common</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!--网关-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <!--nacos discovery-->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <!--负载均衡-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
</dependencies>
```



##### 启动类

新建启动类



##### 配置路由

在`resources`目录下新建`application.yaml`文件，声明路由：

```YAML
server:
  port: 8080
spring:
  application:
    name: gateway
  cloud:
    nacos:
      server-addr: 192.168.150.101:8848
    gateway:
      routes:
        - id: item # 路由规则id，自定义，唯一
          uri: lb://item-service # 路由的目标服务，lb代表负载均衡，会从注册中心拉取服务列表
          predicates: # 路由断言，判断当前请求是否符合当前规则，符合则路由到目标服务
            - Path=/items/**,/search/** # 这里是以请求路径作为判断规则
        - id: cart
          uri: lb://cart-service
          predicates:
            - Path=/carts/**
        - id: user
          uri: lb://user-service
          predicates:
            - Path=/users/**,/addresses/**
        - id: trade
          uri: lb://trade-service
          predicates:
            - Path=/orders/**
        - id: pay
          uri: lb://pay-service
          predicates:
            - Path=/pay-orders/**
```



#### 路由过滤

路由规则的定义语法如下：

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: item
          uri: lb://item-service
          predicates:
            - Path=/items/**,/search/**
```

`routes`是一个集合，即可以定义很多的路由规则，常见的属性如下：

- `id`：路由的唯一标示
- `predicates`：路由断言，其实就是匹配条件
- `filters`：路由过滤条件，后面讲
- `uri`：路由目标地址，`lb://`代表负载均衡，从注册中心获取目标微服务的实例列表，并且负载均衡选择一个访问。

对于`predicates`路由断言，SpringCloudGateway中支持的断言类型有很多：

| **名称**   | **说明**                       | **示例**                                                     |
| :--------- | :----------------------------- | :----------------------------------------------------------- |
| After      | 是某个时间点后的请求           | - After=2037-01-20T17:42:47.789-07:00[America/Denver]        |
| Before     | 是某个时间点之前的请求         | - Before=2031-04-13T15:14:47.433+08:00[Asia/Shanghai]        |
| Between    | 是某两个时间点之前的请求       | - Between=2037-01-20T17:42:47.789-07:00[America/Denver], 2037-01-21T17:42:47.789-07:00[America/Denver] |
| Cookie     | 请求必须包含某些cookie         | - Cookie=chocolate, ch.p                                     |
| Header     | 请求必须包含某些header         | - Header=X-Request-Id, \d+                                   |
| Host       | 请求必须是访问某个host（域名） | - Host=**.somehost.org,**.anotherhost.org                    |
| Method     | 请求方式必须是指定方式         | - Method=GET,POST                                            |
| Path       | 请求路径必须符合指定规则       | - Path=/red/{segment},/blue/**                               |
| Query      | 请求参数必须包含指定参数       | - Query=name, Jack或者- Query=name                           |
| RemoteAddr | 请求者的ip必须是指定范围       | - RemoteAddr=192.168.1.1/24                                  |
| weight     | 权重处理                       |                                                              |

























