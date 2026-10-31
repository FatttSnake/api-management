<div align="center">
    <h1>
        <img alt="API Management" src="docs/logo.svg" width="140">
        <br>
        <span>API Management</span>
    </h1>
</div>
<div align="center">
    <b>一个可自托管的、插件驱动的 API 网关与管理平台</b>
</div>
<div align="center">
    <a href="https://ci.fatweb.top/job/API%20Management/">
        <img alt="Build" src="https://ci.fatweb.top/job/API%20Management/badge/icon">
    </a>
    <a href="https://github.com/FatttSnake/api-management/releases/latest">
        <img alt="Release" src="https://img.shields.io/github/v/release/FatttSnake/api-management">
    </a>
    <a href="LICENSE">
        <img alt="LICENSE" src="https://img.shields.io/github/license/FatttSnake/api-management">
    </a>
</div>

# 概述 ([EN](README.md), 简体中文)

API Management 是一个**可自托管的 API 网关与管理平台**。它将「可用 API」视为由**插件**提供的资源：插件以 Jar 包的形式**热插拔**安装到网关后，其中声明的接口会被注册到统一的命名空间下，再以统一的方式对外开放 —— 由平台统一负责**身份认证、访问控制、频控限流、计量计费与审计**，并通过一套完整的 REST 接口进行管理。

后端使用 **Kotlin + Spring Boot** 编写。平台将插件开发所需的稳定 API 抽取为独立子模块 **`gateway-api`**；插件运行时、模块仓库与 Gradle 工具分别位于 **`plugin-runtime`**、**`plugin-signing`**、**`plugin-gradle-plugin`**。插件示例见项目 [api-management-plugins](https://github.com/FatttSnake/api-management-plugins)。

# 特性

- **插件化 API 托管** —— 上传插件 Jar，网关会先校验其 **Ed25519 签名**（对照管理员维护的信任公钥库），再装入**隔离的插件类加载器与模块图**中完成**热安装/升级/卸载**，无需重启。
- **命名空间与版本化路由** —— 每个插件接口被统一暴露为 `/api/{plugin}/v{version}/...`，接口路由注册到数据库，重启后自动恢复。
- **多级访问控制** —— 账户通过 JWT 登录，调用方亦可使用 **AccessKey / SecretKey**；可选 **两步验证（TOTP）**；每个接口支持 `need-key`、启停开关以及 `DEFAULT` / `RESTRICTED` 两种访问模式，并以 RBAC 权限点的形式统一表达。
- **计量与计费** —— 每个接口可独立设置单价与计费模式（`FREE` / `SUCCESS_ONLY` / `ALWAYS`）、**每分钟频控**；账户维度支持**余额**与配额周期。
- **RBAC 权限体系** —— 用户、分组、角色与细粒度权限点（模块 / 菜单 / 操作 / 范围）；可用 API 在权限树中**按插件分组**展示。
- **用户自助服务** —— 面向终端用户的「我的 API 账户」「我的密钥」「API 文档」「我的用量」等接口。
- **可观测与审计** —— API 用量统计、监控、报表与汇总统计（定时聚合），以及操作审计日志、系统日志。
- **插件平台** —— 提供生命周期钩子、**隔离的插件专属数据源**（每个插件可有多个具名数据源，MySQL 或 SQLite）、可独立安装并按版本锁定的依赖模块，以及只暴露设置 / 存储 / 调用者 / 余额 / 接口配置的受控 `GatewayContext`。
- **灵活的存储与邮件** —— 文件存储支持本地磁盘或 **S3**；内置邮件发送（注册、安全通知等）。
- **双数据库自动迁移** —— MySQL 存放关键业务数据，SQLite 存放日志等大量读写数据，均由 Flyway 自动初始化。

# 项目结构

```
api-management/
├── build.gradle.kts / settings.gradle.kts        # Gradle 构建（Java 25 toolchain，含插件相关子模块）
├── docs/                                         # 项目文档与素材（logo.svg、plugin-debugging.md / _zh.md 等）
├── gateway-api/                                  # 稳定插件契约（Kotlin，Java 友好，含 ABI 清单）
│   └── src/main/kotlin/top/fatweb/apimanagement/api/v1/
│       └── ApiPlugin / GatewayContext / ApiRoutes / ApiRequest / ApiResponse / 能力接口
├── plugin-runtime/                               # 类加载、模块仓库、路由注册、安装生命周期
│   └── src/main/kotlin/top/fatweb/apimanagement/plugin/runtime/
│       ├── classloader/                          # GatewayApiClassLoader / ModuleClassLoader / PluginClassLoader
│       ├── module/                               # lock 解析、内容寻址模块存储
│       ├── route/                                # 路由表、MVC HandlerMapping / HandlerAdapter
│       └── runtime/                              # 描述符解析、GatewayContext 实现、插件加载器
├── plugin-signing/                               # 网关与 Gradle 工具共用的 Ed25519 签名/验签
├── plugin-gradle-plugin/                         # 插件开发者使用的 Gradle 插件 `top.fatweb.api-plugin`
│   └── src/main/kotlin/top/fatweb/apimanagement/gradle/
│       ├── ApiPlugin.kt                          # 应用 apiPlugin DSL，将 gateway-api 作为 compileOnly
│       ├── GeneratePluginLock.kt                 # 解析运行时依赖并生成 api-plugin-deps.json
│       ├── CheckPluginAbi.kt                     # 对照 gateway-api.abi.json 校验插件字节码
│       └── GeneratePluginDescriptor / GenPluginKeys / SignPlugin / VerifyPlugin / DeployPlugin
├── src/main/kotlin/top/fatweb/apimanagement/
│   ├── ApiManagementApplication.kt               # 启动入口（校验/创建 data 目录、首次运行生成配置模板）
│   ├── annotation / aspectj                      # 注解与拦截切面（API 访问拦截、参数处理、操作/事件日志）
│   ├── component/
│   │   ├── api/                                  # API 路由注册与版本匹配（/api/{plugin}/v{version}/...）
│   │   ├── plugin/                               # 插件产物、配置 schema、数据源、运行时注册表
│   │   ├── security/                             # JWT、CSRF 令牌
│   │   └── storage/                              # 本地 / S3 文件存储
│   ├── config/                                   # Security / Jackson / Redis / Flyway / MyBatis-Plus / Knife4j 等
│   ├── controller/
│   │   ├── permission/                           # 认证（登录/注册/两步验证/刷新令牌）、用户、角色、分组、权限点
│   │   ├── system/                               # 管理端：插件、信任密钥、API 账户、API 密钥、审计、用量、监控、报表、统计、设置、系统日志
│   │   └── user/                                 # 用户自助：我的 API 账户、我的密钥、API 文档、我的用量
│   ├── entity / mapper / service / param / vo / converter   # 业务各层
│   ├── cron/                                     # 用量指标与统计的定时聚合
│   ├── filter / handler / exception / http       # 过滤器、全局异常处理、HTTP 客户端、Turnstile 校验
│   ├── migration / properties / settings / util
├── src/main/resources/
│   ├── application.yaml                          # 应用基础配置（数据源骨架、SQLite、日志、MyBatis-Plus）
│   ├── application-config-template.yml           # 首次运行生成的配置模板
│   └── db/migration/{master,sqlite}/             # Flyway SQL 迁移
└── data/                                         # 运行期数据（目录不存在时自动创建）
    ├── db/sqlite.db                              # SQLite（日志等大量读写的数据）
    ├── log/                                      # 应用日志
    ├── plugins/                                  # 外部插件 Jar 物化目录
    ├── objects/                                  # 内容寻址对象（按 SHA-256 分片）
    ├── files/                                    # 位置寻址文件的系统级根目录
    │   └── plugin-data/{插件ID}/                 # 插件自己的文件区
    └── config/settings.yml                       # 动态系统设置
```

# 环境要求

- Java 25+
- MySQL 8.0+
- Redis

# 关联项目

[Web Console](https://github.com/FatttSnake/api-management-console)

[Plugins](https://github.com/FatttSnake/api-management-plugins)

# 快速开始

1. 打包可执行 Jar (可选从 [Releases](https://github.com/FatttSnake/api-management/releases) 页面下载)

```shell
./gradlew bootJar          # Windows 使用 gradlew.bat bootJar
# 产物位于 build/libs/ 目录下，文件名形如 api-management-<version>.jar
```

2. 首次运行，生成配置文件模板

```shell
java -jar api-management-<version>.jar
```

> 当运行目录或 `data` 目录下不存在 `application-config.yml` 时，程序会在 `data/application-config.example.yml` 生成一份配置模板（内含自动生成的随机 token-secret），随后自动退出。

3. 将模板复制到运行目录并重命名为 `application-config.yml`

```shell
cp ./data/application-config.example.yml application-config.yml
```

4. 编辑配置文件，填写 MySQL、Redis 等（详见[配置](#配置)）

5. 再次运行

```shell
java -jar api-management-<version>.jar
```

服务默认监听 `8080` 端口（可用 `server.port` 修改）。数据库由 Flyway 在首次启动时自动初始化。

# 配置

程序从**运行目录或 `data` 目录**读取 `application-config.yml`。主要配置项如下：

| 配置块 | 说明 | 关键项 |
| --- | --- | --- |
| `app.admin` | 初始化数据库时创建的管理员（可选） | `username`、`password`、`nickname`、`email` |
| `app.security` | JWT 令牌与安全相关 | `token-secret`（必填，模板已生成随机值）、`token-prefix`、`access-token-ttl`、`refresh-token-ttl`、`trust-proxy-headers`、`allowed-origin-patterns` |
| `app.storage` | 文件存储 | `mode`（`local`/`s3`）、`local.root`、`public-base-url`、`external-url-*`、`s3.*` |
| `server.port` | 服务端口 | 默认 `8080` |
| `spring.datasource.dynamic.datasource.master` | MySQL（关键业务数据） | `url`、`username`、`password` |
| `spring.data.redis` | Redis | `host`、`port`、`password`、`database` |
| `logging` | 日志 | `level.root`、`file.name` |
| `knife4j` | 接口文档 | `production`（`true` 时关闭文档访问） |

配置示例：

```yaml
app:
  admin:                      # 可选；数据库初始化前配置则使用指定账号
    username: admin
    password: admin
  security:
    token-secret: <uuid>      # JWT 签名密钥（模板首次生成时为随机值）
  storage:
    mode: local               # 文件存储模式 [local, s3]
server:
  port: 8080
spring:
  datasource:
    dynamic:
      datasource:
        master:               # MySQL
          url: jdbc:mysql://localhost
          username: root
          password: root
  data:
    redis:
      host: localhost
logging:
  level:
    root: info
knife4j:
  production: true
```

# 插件开发

完整架构说明——模块、类加载、安装/启动流程、请求链路与数据模型——见 [docs/plugin-platform_zh.md](docs/plugin-platform_zh.md)。代码布局、成员顺序、import、KDoc 与异常规范见 [docs/code-style_zh.md](docs/code-style_zh.md)，由 `./gradlew ktlintCheck detekt` 强制执行。

平台的全部 API 都由插件提供。插件是独立 Gradle 工程，应用 **`top.fatweb.api-plugin`**，唯一编译依赖是 `top.fatweb:api-management-gateway-api`。

发布契约与工具（本仓库内执行一次）：

```shell
./gradlew :gateway-api:publishToMavenLocal :plugin-signing:publishToMavenLocal :plugin-gradle-plugin:publishToMavenLocal
```

`./gradlew build` 产出已签名 jar，包含：

| 条目 | 用途 |
|---|---|
| `META-INF/api-plugin.json` | 描述符 format 1：pluginId、版本、mainClass、`requiresGateway`、`requiresGatewayApi` |
| `META-INF/api-plugin-deps.json` | 精确依赖 lock：GAV、SHA-256、大小、直接依赖边与可选的共享 loader 组 |
| `META-INF/plugin.pub.pem` / `META-INF/plugin.sig` | Ed25519 公钥与签名 |

`checkPluginAbi`（`build` 的一部分）读取网关 API jar 内的 `META-INF/gateway-api.abi.json`，插件引用了网关不存在的类、方法、字段或构造器时直接失败；签名在 ABI 校验之后执行。

## 编程模型

插件不使用 Spring MVC 注解，也不会拿到 Spring 子上下文。实现 `ApiPlugin`，在 `onStart` 里通过 `GatewayContext` 注册路由：

```kotlin
class EchoPlugin : ApiPlugin {
    override fun onStart(context: GatewayContext) {
        context.api().register(
            RouteDefinition("POST", "/echo/{name}", 1, OperationMetadata("echo", "Echo a name"))
        ) { request ->
            ApiResponse.ok(mapOf("message" to "hello, ${request.pathParam("name")}"))
        }
    }
}
```

Java 插件作者使用同一套类型：`ApiHandler`、`EventSubscriber`、`Subscription` 是 SAM 接口，`ApiResponse`/`ParameterSpec`/`ApiPrincipal` 提供静态工厂，`GatewayContext` 用访问函数（`context.api()`、`context.settings()`）而不是 Kotlin 属性。

`GatewayContext` 是插件能看到的全部网关能力：`api()`、`settings()`、`dataSources()`、`storage()`、`users()`、`audit()`、`events()`、`http()`、`scheduler()`、`json()`、`logger()`、`metadata()`。它不暴露 Spring 容器、Bean、Mapper 或网关的 ObjectMapper。

请求体是原始字节（`request.body` / `request.bodyText()`）；需要结构化 JSON 时用 `context.json()`，或使用插件自带的库。响应体可以是字节、字符串、Map 或普通 Bean；`ApiResponse.raw(...)` 绕过业务信封。

## 依赖

插件 jar 是瘦包。运行时依赖在构建期解析进 lock，每个模块 jar 单独上传（`deployPlugin` 会先传模块再传插件）。网关按 SHA-256 内容寻址存储在 `app.storage.plugin-module-dir`，同一个装载单元跨插件共享一个类加载器，不同版本并存。

默认一个模块就是一个装载单元，只能看到自己声明的依赖。有些库不符合这个模型：Flyway 通过绑定在“定义 `flyway-core` 的 loader”上的静态 `ServiceLoader` 注册表发现数据库类型；MyBatis-Plus 会引用 POM 里标成 optional 的 MyBatis 与 SLF4J。把它们声明成**共享 loader 组**后，整组由一个 composite child-first loader 装载，成员之间不需要依赖边也能互相看见：

```kotlin
apiPlugin {
    sharedLoader("org.flywaydb:flyway-core", "org.flywaydb:flyway-mysql")
}
```

DSL 只接受 `group:artifact`（不带版本），版本来自实际解析结果。组是共享、引用计数与卸载的单位，组之间不能形成装载单元环。只把确实需要互相可见的模块放进一组，也不要把包含同一批类的模块放进一组：同名类由 digest 排序靠前的 jar 决定。

lock 中有、网关没有的模块会在安装时从 `app.plugin.module.repositories`（默认 Maven Central）下载，前提是 `app.plugin.module.online` 为真，且 SHA-256 与大小都必须与 lock 一致。`deployPlugin` 仍会先上传模块，因此离线部署不需要外网。控制台会在安装插件前逐个列出依赖并展示下载进度。

网关只提供 **JDK、Kotlin stdlib 与 `gateway-api`**。Spring、Jackson、MyBatis、Hikari、Flyway、JDBC 驱动等全部是插件自己的普通依赖，由插件自己的模块图加载。Kotlin stdlib 与 `org.jetbrains:annotations` 由平台提供，因此从 lock 中排除。

lock 由 `runtimeClasspath` 生成；运行期不做任何版本解析。模块也可以通过 `/system/api/plugin/module` 独立管理（上传、列表、从 Maven 获取、删除无引用模块、GC）。

资源查找只看到插件 jar、它的模块与平台的 `gateway-api` 资源。这是默认行为边界，不是安全沙箱：插件需签名可信，同 JVM 内仍可通过反射绕过。

## 插件配置与数据源

插件在自己的 jar 里放一份 `META-INF/plugin-config.json` 声明它有哪些配置项（类型、默认值、约束、是否密钥项、需要哪些具名数据源、各是什么方言），网关在**挂载时**读取并快照到插件行上，控制台据此渲染配置界面；插件侧通过 `GatewayContext.settings().get(key)` 读取，默认值由网关回落。schema 声明过的 key 归管理员，`settings().save(...)` 写它会抛异常。

| 接口 | 说明 |
|---|---|
| `GET /system/api/plugin/{pluginId}/config` | 配置结构 + 当前值 + 数据源声明。secret 不回值（`hasValue` 表示是否已设置，`unreadable` 表示已存密文解不开）；每个数据源带名称、方言、是否必需与当前状态；每个分组标出它描述的那个数据源（未描述时为 `null`，控制台就在这个分组上给出「测试连接」） |
| `PUT /system/api/plugin/config` | **按分组保存**：`{pluginId, groups:[{key, values:[{key, value}]}]}`。普通配置项立即生效（插件每次读取都查库）；**改到数据源相关项会重挂载插件**，因为连接是网关按这些值构建的；插件因挂载失败而处于未挂载状态时，**任意一次保存都会重挂载它** |
| `POST /system/api/plugin/{pluginId}/config/datasource/test` | 用**尚未保存**的值试连一次（`name` + 要用的配置项；某项不传或传 `null` 表示用已存值）。仅 MySQL——SQLite 是网关自备的文件，没有管理员的连接可试。失败回 40067 与驱动原文 |
| `POST /system/api/plugin/{pluginId}/reload` | 用已存储的 jar 原地重挂载，用于**换了 jar**；配置与数据不受影响。改数据源**不需要**它 |

配置项的三种提交状态：**缺省 = 不动、空串 = 清除、有值 = 写入**。数字是唯一有「空表单」的类型，未设置时 `value` 为 `null`、声明的默认值在 `default` 里（插件读到的是它）；其余类型未设置时直接回默认值。`hasValue` 区分「显示的是默认值」还是「管理员设的值」——要回到默认值，把默认值写进去即可。**必填项按分组校验**。**secret 永不回值**，不提交即保持原值、提交空串即清除。

数据源：只有声明了 `datasources` 的插件才会拿到 `context.datasources`，且只含**已配置**的那些；名字不在 map 里，就是网关还没有东西可连。一个插件可以声明多个，两种方言也可以混用。方言由插件声明（`dbType`），不由管理员选。**SQLite 零配置**，声明即由网关按 `app.storage.plugin-datasource-dir/{pluginId}/{name}.db` 供给。**MySQL 数据源就是普通配置项**，因此表单、约束与密钥加密全部复用；JDBC URL 由网关拼装，`params` 以**连接属性**下发给驱动（`autoDeserialize`、`allowLoadLocalInfile`、`propertiesTransform` 等越出连接本身的属性会被拒绝）。**host 与 database 都有值才算已配置**，清空任意一个即取消配置——因此声明了 `minLength` 或 `pattern` 的 host / database 字段会拒绝那个空串。这些配置项必须落在同一个分组里，一个分组也只能描述一条连接：分组就是一条连接被填写、保存和测试的单位。

**升级是原地替换，管理员设过的东西全部保留**：插件行与接口行是**更新**而非重建，因此插件级的默认单价、限流与门禁，每个接口自己的单价、计费模式、是否需要 key、限流、访问模式与启用状态，以及这些接口上的角色授权，全部原样保留；只有新版本不再提供的接口会连同其授权一起消失。**重装当前已安装的版本号是允许的**，行为相同，所以调试循环不必每改一次就递增 `versionCode`；只有**比已装版本更旧**才会被拒绝（40066）。

插件可以只被关掉而不被移除：`PATCH /system/api/plugin` 传 `enable=false` 会把它拆下来。**被禁用的插件完全不运行**——网关启动时不加载它、路由不注册、请求它的接口会被拒；它的设置、文件区、数据源配置与授权一律不动，重新启用就是把同一个 jar 挂回同一批行上。

`DELETE /system/api/plugin/{pluginId}?purgeData=true` 才会清除插件设置、SQLite 数据库目录与文件区（默认 `false` 即保留，便于重装续用）。插件自己的数据由 `onPurge` 删除：它在数据源、storage、配置都还开着、且没有请求在跑时被调用，正是插件删掉自己建的表（包括网关从不碰的外部 MySQL 库）、释放内容寻址对象的地方。只有「真的卸载」才触发它：升级、reload、被禁用、关闭网关都不触发。没挂载的插件会**先被挂起来**让退出钩子有东西可跑，而**该钩子失败会拒绝这次卸载**（40068），此时什么都还没删，修好原因重跑即可。插件释放掉的内容寻址对象由网关定期回收。

生命周期钩子见 `gateway-api` 的 `ApiPlugin`：`onStart`（每次挂载、路由激活之前）、`onStop`（每次卸载）、`onUninstall`（版本被摘除之时，升级也会走——它**不是删数据的地方**）、`onPurge`（明确要求清除数据的卸载）。

## 调试插件

插件的 jar 是在**网关自己的 JVM 内**加载的，所以附着到网关的调试器可以直接在插件源码里下断点——用 Agent 启动网关、再从插件工程附着即可：

```shell
./gradlew bootRun -Pdebug=true
```

该开关默认关闭、默认绑 `127.0.0.1:5005`，可用 `debug.port`、`debug.address`、`debug.suspend` 调整。从插件工程附着与类加载边界见 [docs/plugin-debugging_zh.md](docs/plugin-debugging_zh.md)——该 Agent 是进入这个 JVM 的**无鉴权**通道，而里面装着所有管理员的会话。

编辑循环本身是一条命令，它负责构建、签名、验签、上传依赖模块并把 jar 推到运行中的网关：

```shell
./gradlew deployPlugin -PapiPlugin.serverUrl=http://localhost:8080 -PapiPlugin.accessToken=<token>
```

两项配置也都能走环境变量（`API_PLUGIN_SERVER_URL`、`API_PLUGIN_ACCESS_TOKEN`），且都不该写进 `build.gradle.kts`。token 即控制台登录响应里的 `accessToken`，默认有效期 2 小时。

# 安全

- 集成 **Spring Security**，采用 **JWT** 令牌（访问令牌默认 2 小时、刷新令牌默认 128 天，均可在配置中调整），密钥 `app.security.token-secret` 存于配置文件中。
- 支持**两步验证（TOTP）**，并提供 CSRF 令牌管理与相关安全过滤器。
- 首次启动创建管理员：如已在 `app.admin` 中指定账号则使用之；否则默认用户名 `admin` 并**随机生成密码**，仅在控制台打印一次，请及时修改。

# 存储

`app.storage.mode` 支持两种文件存储方式：

- `local`：写入本地磁盘，根目录由 `app.storage.local.root` 指定（默认 `data`）。
- `s3`：写入 S3 兼容对象存储（需配置 `endpoint`、`accessKey`、`secretKey`、`region`、`bucket` 等，支持 `path` / `virtualHosted` 两种路径风格）。

后端之上有两种寻址方式，它们是**按需拼接**的子目录，不是配置项：

| | 内容寻址 | 位置寻址 |
|---|---|---|
| key | 内容 SHA-256，相同内容全局去重 | 相对位置寻址根目录的路径 |
| 存储形态 | zstd 压缩 | 原始字节 |
| local | `data/objects/{前2位}/{其余62位}` | `data/files/{key}` |
| s3 | `{prefix}/objects/{前2位}/{其余62位}` | `{prefix}/files/{key}` |

位置寻址是**系统级**能力：插件的 key 为 `plugin-data/{插件ID}/{路径}`，其他前缀留给其他子系统。

另外，上传的外部插件 Jar 会物化到 `data/plugins` 目录用于类加载。

## 插件存储与外链

插件通过 `GatewayContext.storage()` 读写自己的文件区，路径自动限定在该插件自己的命名空间内。位置寻址的文件会原样存储，因此可以签发**免登外链**：

- `local`：网关签发 HMAC 签名 URL（`/public/storage/{插件ID}/{路径}?e=<过期时间戳>&s=<签名>`），密钥复用 `app.security.tokenSecret`，无状态、多实例可用。URL 用的是对外引用而非内部 key，所以内部布局调整不会让已发出的链接失效。
- `s3`：生成对象存储的预签名 URL，客户端直连 S3，不经网关。

```yaml
app:
  storage:
    public-base-url: https://api.example.com  # 外链的公开基址；未配置时取当前请求地址（反向代理后必须显式配置）
    external-url-default-ttl: 1               # 外链默认有效期
    external-url-ttl-unit: hours              # 有效期单位
    external-url-max-ttl: 168                 # 有效期上限，超出的申请会被钳制
```

内容寻址对象是压缩存储的，无法直接交给浏览器，因此**不提供外链**，只能通过 `Storage` 能力的读写接口访问。

> 外链是不可吊销的 bearer 凭证：持有者到过期前一直可读。轮换 `app.security.tokenSecret` 会同时使所有外链失效并让所有用户下线。`local` 模式的外链与后台同源，故除图片白名单外的类型一律以 `Content-Disposition: attachment` 下发并附 `nosniff`。

# 数据库

采用动态数据源双库架构：

- **MySQL**（`master`）—— 存放用户、权限、插件、接口、账户、密钥等关键业务数据；
- **SQLite**（`data/db/sqlite.db`）—— 存放日志等读写量大的数据。

两库均由 **Flyway** 自动初始化与升级，迁移脚本位于 `src/main/resources/db/migration/{master,sqlite}`。**升级前请先备份数据库。**

# Q&A

> **Q: 默认管理员账号和密码是什么？**
>
> A: 初始化数据库前在 `app.admin` 中配置则使用所指定账号密码；未配置则默认用户名为 `admin`，密码为首次启动时随机生成，详见控制台输出（仅显示一次）。

> **Q: 是否需要手动初始化数据库？**
>
> A: 不需要。Flyway 会自动初始化两库。升级前请先备份数据库。

> **Q: 如何在浏览器中查看接口文档？**
>
> A: 使用 `dev` 构建时默认集成 Knife4j / Swagger UI；若在配置中把 `knife4j.production` 设为 `true`，将关闭文档访问。

# 许可证

[GPL-3.0](LICENSE)
