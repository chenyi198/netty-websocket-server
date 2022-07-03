##### A netty based WebSocket Server.
**依赖**
* Spring Boot
* Netty

**实现功能**

* [x] 通用启动类：Websocket服务端bind ip、listen port
* [x] 模板方法：资源接口顶层模板类
* [x] 资源池：资源接口可集中注册配置，资源接口的创建及注册到资源池的操作注解配置化.（利用Spring IOC bean注入机制）
* [x] 请求分发器，dispatcher：识别客户端请求并分发至对应后置handler资源处理器
* [x] websocket I/O操作与业务逻辑处理线程池隔离：运用netty中handler可绑定线程的机制，将业务逻辑处理绑定到专门的业务线程池里，即可实现线程池隔离

**websocket通道建立流程**

* 1）client：HTTP[Connection:Upgrade, Upgrade:websocket] ---> server
* 2）server：HTTP[101 Switching Protocols] ---> client，通道建立
* 3）client：WebSocket[Text] <---> server：WebSocket[Text]，通信开始
* 4）client: TCP[Keep-Alive] ---> server:TCP[Keep-Alive ACK]，客户端主动发起TCP层面链接保活

```
websocket通道的建立过程包含由HTTP协议消息切换为WebSocket协议消息过程，
服务端要对客户端第一次发起的HTTP#Upgrade协议升级请求回复HTTP#Switching Protocols协议切换响应，
响应完成后通道即建立。
客户端已实现了TCP层面的链接保活机制，链接保活由客户端维护。
```