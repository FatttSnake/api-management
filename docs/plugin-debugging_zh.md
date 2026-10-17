# 调试插件 ([EN](plugin-debugging.md), 简体中文)

插件的 jar 是在**网关自己的 JVM 内**、由一个 child-first 类加载器
（`PluginClassLoaderManager.create`）加载的。插件没有任何部分跑在独立进程里，所以附着到网关
的调试器可以直接在插件源码里下断点、单步执行——插件打出的日志也落在网关的日志里，
`data/log/api-management.log`。

日志走 SLF4J：SDK 已把 `slf4j-api` 放进编译类路径，插件直接声明
`private val logger = LoggerFactory.getLogger(MyController::class.java)` 即可。运行期网关的类
加载器从网关解析 `org.slf4j`，所以这些日志拿到的是网关的 binding 与 appender，落在网关日志
里、logger 名是插件类名。不要在插件 jar 里打包 `slf4j-api` 或任何日志后端：`org.slf4j.` 是
共享前缀且不会回落到 jar，打进去的副本永远不会被加载。`println` 不需要任何声明，调试时想快点
看到点什么它更快，不过它走标准输出而不是日志系统。

## 1. 带调试 Agent 启动网关

### 从本仓库启动

```shell
./gradlew bootRun -Pdebug=true
```

该开关读取 Gradle property、环境变量或 `ORG_GRADLE_PROJECT_` 变量，所以 `DEBUG=true`
效果一样。它有三个可调项，各有默认值：

| Gradle property | 环境变量 | 默认值 | 含义 |
|---|---|---|---|
| `debug.port` | `DEBUG_PORT` | `5005` | Agent 监听的端口 |
| `debug.address` | `DEBUG_ADDRESS` | `127.0.0.1` | 绑定的网络接口 |
| `debug.suspend` | `DEBUG_SUSPEND` | `n` | 设为 `y` 时网关启动即挂起，等调试器附着后才继续 |

Agent 就绪后网关会打印 `Listening for transport dt_socket at address: 5005`，端口绑在回环
接口上。想断在插件的 `onStart`（它在插件挂载过程中执行），需要 `-Pdebug.suspend=true`。

### 从普通 jar 启动

```shell
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005 \
     -jar api-management.jar
```

`JAVA_TOOL_OPTIONS` 同样可行，也是改不了命令行时的办法，但它会被下面启动的每个 JVM 继承
——包括 Gradle daemon——所以优先用显式的 `-agentlib:`。

### 容器里

镜像是在别处构建的，所以 Agent 只能从环境变量传进去：

```shell
docker run -p 8080:8080 -p 5005:5005 \
  -e JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  api-management
```

容器内 Agent **必须**绑 `*`：容器自己的回环接口不是发布端口能够到达的那个。调试端口只应
发布在可信网络上，或者在宿主机上把它绑到 `127.0.0.1`（`-p 127.0.0.1:5005:5005`）。

### 远程主机

在远端把 Agent 绑到回环接口，让隧道成为唯一的入口：

```shell
# 在远端：address=127.0.0.1:5005
ssh -L 5005:127.0.0.1:5005 user@host
# 然后把调试器附到 localhost:5005
```

> **JDWP 就是远程代码执行。** 该 Agent 没有任何形式的鉴权：能连到那个端口的人就能在网关里
> 执行代码，而网关装着所有管理员的会话和所有插件的数据。绝不要把它暴露在公网接口上，用完
> 就关掉——这也正是为什么这里默认不启用它、默认只绑回环接口。

## 2. 从插件工程附着

在插件工程里（不是本仓库）：

1. **Run → Edit Configurations → + → Remote JVM Debug**。
2. Host 与 port：`localhost:5005`，或隧道/发布端口最终对应的那个。
3. **Use module classpath**：选插件工程自己的 `main` 模块。
4. 开始调试，然后请求插件的路由。

不需要指向物化后的 jar。网关会把上传的 jar 复制到 `data/plugins/<sha256>.jar` 并从那里
加载，但远程调试器是按**全限定类名**匹配断点、并按已加载字节码里的行号表定位——由哪个类
加载器加载、文件叫什么名字，都不影响。唯一的要求是：网关里跑的字节码和磁盘上的源码来自
同一次构建；只改源码不部署，断点就会错行。

## 3. 编辑循环

`deployPlugin` 负责构建、签名、验签，并把 jar 直接推到运行中的网关、替换已安装的版本：

```shell
./gradlew deployPlugin \
  -PapiPlugin.serverUrl=http://localhost:8080 \
  -PapiPlugin.accessToken=<access token>
```

两项配置也都可以走环境变量（`API_PLUGIN_SERVER_URL`、`API_PLUGIN_ACCESS_TOKEN`），而且都不
该写进 `build.gradle.kts`——那个文件是要入库的。如果网关会拒绝这个 jar，任务在上传之前就
失败，并写明原因（签名公钥不受信、签名损坏、版本低于已安装的版本）。

access token 就是控制台登录响应里的 `accessToken`，默认有效期 2 小时，所以一个编辑循环
通常从登录一次开始。**新版本的 jar 走 `install`，绝不走 `reload`**——`reload` 重新读的是
网关已经存着的那个 jar，拿不到重新构建的产物。

### 不部署也能热替换

附着调试器之后，IntelliJ 的 **Build → Reload Changed Classes** 会原地重定义已加载的类，
对插件类和别的类一样有效。但它只限于**已加载类的方法体**：

| 改动 | Reload Changed Classes |
|---|---|
| 已加载方法的方法体 | 有效，不用部署 |
| 增删方法、字段或类 | 需要部署 |
| 改签名、修饰符或继承关系 | 需要部署 |
| 新增或改动 `@ApiController` / `@RequestMapping` / `@Operation` | 需要部署——Spring 在挂载时扫描并注册了路由，不会重新扫描 |
| 类以外的任何东西：`META-INF/api-plugin.json`、`META-INF/plugin-config.json`、密钥对 | 需要部署 |

所以热替换只是调方法体时省一次部署；其它一切仍然走 `deployPlugin`。

## 4. 出问题的时候

挂载失败的插件仍然是「已安装」状态：它不提供任何服务、路由不注册，原因写在它的
`load_error` 上，插件列表能读到。另一半情况是网关自己的校验失败——那类会直接拒绝上传，
由 `deployPlugin` 报出来。

- 插件自己的 `onStart` 抛异常：已安装但没起来，`load_error` 有原因。对它做一次保存、或
  执行 `reload`，都会重新挂载——所以最快的修法通常是改掉它启动时用不了的那个配置。
- 网关侧拒绝：`40064` 签名公钥不受信、`40065` 签名无效、`40066` 版本低于已安装的版本、
  `40063` 挂载本身失败。
- 插件打包了网关也拥有的类：`PluginClassLoaderManager` 里那些共享前缀只会从网关解析、
  不回落到 jar，所以打包了其中某个类的插件必须停止打包它。
