# 调试插件

插件运行在网关 JVM 内。`PluginRuntimeLoader` 创建 `PluginClassLoader`；平台类加载器只暴露
JDK、Kotlin stdlib 与 `gateway-api`，插件 jar 与依赖模块由 child-first 解析。插件没有独立进程。

## 附着调试器

```shell
./gradlew bootRun -Pdebug=true
```

Agent 默认关闭、默认绑 `127.0.0.1:5005`，可用 `debug.port`、`debug.address`、
`debug.suspend` 调整。从插件工程附着后即可在插件源码里下断点。

该 Agent 是进入 JVM 的**无鉴权**通道，而 JVM 内装着所有管理员会话；只在本机或可控网络开启。

## 编辑循环

```shell
./gradlew deployPlugin -PapiPlugin.serverUrl=http://localhost:8111 -PapiPlugin.accessToken=<token>
```

`deployPlugin` 会构建、校验 ABI、签名、验签、上传依赖模块并安装 jar。当前运行时在在途请求排空后
卸载，新运行时启动并原子替换路由。

## 类加载

| 层 | 可见内容 |
|---|---|
| `GatewayApiClassLoader` | JDK、Kotlin stdlib、`top.fatweb.apimanagement.api.*` |
| `ModuleClassLoader` | 一个依赖模块、它的直接依赖，再到平台 |
| `PluginClassLoader` | 插件 jar、lock 中的模块，再到平台 |

插件看不到 Spring、Jackson、MyBatis 或任何网关内部类；需要时按普通依赖声明，它会成为模块。

资源从插件 jar、模块与 `gateway-api` 资源中解析。这是默认行为边界，不是安全沙箱：签名可信的
插件代码仍可使用反射。
