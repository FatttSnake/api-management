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

后端使用 **Kotlin + Spring Boot** 编写。平台将插件开发所需的稳定 API 抽取为独立子模块 **plugin-sdk**，与主程序一同发布。插件开发相关见项目 [api-management-plugins](https://github.com/FatttSnake/api-management-plugins)。

# 特性

- **插件化 API 托管** —— 上传插件 Jar，网关会先校验其 **Ed25519 签名**（对照管理员维护的信任公钥库），再装入**隔离的类加载器 / 子容器**中完成**热安装/升级/卸载**，无需重启。
- **命名空间与版本化路由** —— 每个插件接口被统一暴露为 `/api/{plugin}/v{version}/...`，接口路由注册到数据库，重启后自动恢复。
- **多级访问控制** —— 账户通过 JWT 登录，调用方亦可使用 **AccessKey / SecretKey**；可选 **两步验证（TOTP）**；每个接口支持 `need-key`、启停开关以及 `DEFAULT` / `RESTRICTED` 两种访问模式，并以 RBAC 权限点的形式统一表达。
- **计量与计费** —— 每个接口可独立设置单价与计费模式（`FREE` / `SUCCESS_ONLY` / `ALWAYS`）、**每分钟频控**；账户维度支持**余额**与配额周期。
- **RBAC 权限体系** —— 用户、分组、角色与细粒度权限点（模块 / 菜单 / 操作 / 范围）；可用 API 在权限树中**按插件分组**展示。
- **用户自助服务** —— 面向终端用户的「我的 API 账户」「我的密钥」「API 文档」「我的用量」等接口。
- **可观测与审计** —— API 用量统计、监控、报表与汇总统计（定时聚合），以及操作审计日志、系统日志。
- **插件 SDK** —— 提供生命周期钩子、**隔离的插件专属数据源**（每个插件可有多个具名数据源，MySQL 或 SQLite）、以及获取当前调用者 / 余额 / 接口配置 / 插件设置的受控上下文。
- **灵活的存储与邮件** —— 文件存储支持本地磁盘或 **S3**；内置邮件发送（注册、安全通知等）。
- **双数据库自动迁移** —— MySQL 存放关键业务数据，SQLite 存放日志等大量读写数据，均由 Flyway 自动初始化。

# 项目结构

```
api-management/
├── build.gradle.kts / settings.gradle.kts        # Gradle 构建（Java 25 toolchain，含 plugin-sdk 与 plugin-gradle-plugin 子模块）
├── docs/                                         # 项目文档与素材（logo.svg 等）
├── plugin-sdk/                                   # 插件 SDK（独立子模块，供插件开发者依赖）
│   └── src/main/kotlin/top/fatweb/apimanagement/sdk/
│       ├── annotation/ApiController.kt           # 声明「一个插件 + 一组 API 路由」
│       └── plugin/                               # PluginDescriptor / PluginLifecycle / PluginContext / PluginStorage / ApiResponse / PluginSigner
├── plugin-gradle-plugin/                         # 插件开发者使用的 Gradle 插件 `top.fatweb.api-plugin`
│   └── src/main/kotlin/top/fatweb/apimanagement/gradle/
│       ├── ApiPlugin.kt                          # 应用 apiPlugin DSL、自动加入 SDK 依赖与仓库
│       └── ApiPluginTasks.kt                     # generatePluginDescriptor / genPluginKeys / signPlugin / verifyPlugin
├── src/main/kotlin/top/fatweb/apimanagement/
│   ├── ApiManagementApplication.kt               # 启动入口（校验/创建 data 目录、首次运行生成配置模板）
│   ├── annotation / aspectj                      # 注解与拦截切面（API 访问拦截、参数处理、操作/事件日志）
│   ├── component/
│   │   ├── api/                                  # API 路由注册与版本匹配（/api/{plugin}/v{version}/...）
│   │   ├── plugin/                               # 插件类加载隔离与运行上下文
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
│   └── resources/
│       ├── application.yaml                      # 应用基础配置（数据源骨架、SQLite、日志、MyBatis-Plus）
│       ├── application-config-template.yml       # 首次运行生成的配置模板
│       └── db/migration/{master,sqlite}/         # Flyway SQL 迁移
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
# 产物位于 build/libs/ 目录下，文件名形如 api-management-1.0.0.jar
```

2. 首次运行，生成配置文件模板

```shell
java -jar api-management.jar
```

> 当运行目录或 `data` 目录下不存在 `application-config.yml` 时，程序会在 `data/application-config.example.yml` 生成一份配置模板（内含自动生成的随机 token-secret），随后自动退出。

3. 将模板复制到运行目录并重命名为 `application-config.yml`

```shell
cp ./data/application-config.example.yml application-config.yml
```

4. 编辑配置文件，填写 MySQL、Redis 等（详见[配置](#配置)）

5. 再次运行

```shell
java -jar api-management.jar
```

服务默认监听 `8080` 端口（可用 `server.port` 修改）。数据库由 Flyway 在首次启动时自动初始化。

# 配置

程序从**运行目录或 `data` 目录**读取 `application-config.yml`。主要配置项如下：

| 配置块 | 说明 | 关键项 |
| --- | --- | --- |
| `app.admin` | 初始化数据库时创建的管理员（可选） | `username`、`password`、`nickname`、`email` |
| `app.security` | JWT 令牌与安全相关 | `token-secret`（必填，模板已生成随机值）、`token-prefix`、`access-token-ttl`、`refresh-token-ttl` |
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

平台的全部「可用 API」均来自插件。插件开发相关见项目 [api-management-plugins](https://github.com/FatttSnake/api-management-plugins)。

插件是一个独立的 Gradle 工程，应用本仓库随 SDK 一同发布的 Gradle 插件 **`top.fatweb.api-plugin`**：它会自动加入 SDK 依赖、从 `apiPlugin { }` DSL 生成 `api-plugin.json` 描述符、生成 Ed25519 开发者密钥对、把公钥嵌入 jar 并完成签名——`./gradlew build` 即可产出**已签名**的插件 jar。

把 SDK 与 Gradle 插件发布给插件工程使用（本仓库内执行一次）：

```shell
./gradlew :plugin-sdk:publishToMavenLocal :plugin-gradle-plugin:publishToMavenLocal
```

## 插件配置与数据源

插件在自己的 jar 里放一份 `META-INF/plugin-config.json` 声明它有哪些配置项（类型、默认值、约束、是否密钥项、需要哪些具名数据源、各是什么方言），网关在**挂载时**读取并快照到插件行上，控制台据此渲染配置界面；插件侧只读 `PluginContext.getSetting(key)`，默认值由网关回落。schema 声明过的 key 归管理员，插件 `saveSetting` 写它会抛异常。

| 接口 | 说明 |
|---|---|
| `GET /system/api/plugin/{pluginId}/config` | 配置结构 + 当前值（**secret 不回值**，`hasValue` 表示是否已设置、`unreadable` 表示已存的密文解不开）+ 构成数据源的配置项 + 每个数据源的名称、方言、是否必需与当前状态 + 每个分组所描述的那个数据源（未描述任何数据源时为 `null`；控制台就是在这个分组上给出「测试连接」的） |
| `PUT /system/api/plugin/config` | **按分组保存**：`{pluginId, groups:[{key, values:[{key, value}]}]}`，一次提交一个（或几个）分组。`value` **缺省即不动**、**留空即清除回默认**（文本项例外，见下）、非空即写入。**普通配置项立即生效**（插件每次读取都查库），**改到数据源相关配置项会重挂载插件**——连接是网关按这些值构建的 |
| `POST /system/api/plugin/{pluginId}/config/datasource/test` | 用**尚未保存**的值试连一次（`name` + 要用的配置项；某项不传或传 `null` 都表示用已存值）。仅 MySQL：SQLite 是网关自备的文件，没有管理员的连接可试。失败回 40067 与驱动原文 |
| `POST /system/api/plugin/{pluginId}/reload` | 用已存储的 jar 原地重挂载，用于**换了 jar**；配置与数据不受影响。改数据源**不需要**它 |

- 配置项只有三种提交状态：**缺省 = 不动、空串 = 清除、有值 = 写入**。数字是唯一有"空表单"的类型——空输入框就是"没设"，所以它未设置时 `value` 是 `null`，声明的默认值在 `default` 里（插件读到的是它）；其余类型未设置时直接回默认值：文本项（`string` / `text`）的**空串是一种值**，布尔项（`boolean`）与枚举项（`enum`）则是开关、选择器这类**画不出"未设置"的控件**——空着必然显示成某个状态，所以显示它实际会读到的那个。都由 `hasValue` 区分"显示的是默认值"还是"管理员设的值"——要回到默认值，前端把默认值写进去即可。**必填项按分组校验**：只要求本次提交的分组里那些必填项有值，别的分组没填不拦你保存这一组。
- **secret 永不回值**：留下不提交即保持原值，提交空串即清除；有没有值看 `hasValue`。
- 只有声明了 `datasources` 的插件才会拿到 `context.datasources`，且只包含**已配置**的那些：名字不在 map 里，就是网关还没有东西可连。一个插件可以声明多个，两种方言也可以混用。
- 方言由插件声明（`dbType`），不由管理员选——写 SQL/DDL 的是插件作者，他才知道自己在哪个方言上验证过。**SQLite 零配置**：声明即由网关按 `app.storage.plugin-datasource-dir/{pluginId}/{name}.db` 供给，不落配置行、无需管理员动作。
- **MySQL 数据源就是几条普通配置项**，在声明里逐个点名。它们和其它配置项走同一套机制，因此表单、声明的约束、密钥加密全部复用——密码尤其就是一个普通 secret，这也正是「能清空密码」得以成立的原因。JDBC URL 由网关拼装，`params` 以**连接属性**下发给驱动，没人写连接串。**host 有值即视为已配置**；清空 host 就是取消配置，插件随即不再拿到该数据源——注意 host 要是声明了 `minLength` 或 `pattern`，空串过不了它自己的校验，也就无法再被清空。`params` 会拒绝越出连接本身的属性（`autoDeserialize`、`allowLoadLocalInfile`、`propertiesTransform` 等）——这是缓解而非沙箱，真正的边界仍是插件签名与信任库。**这些配置项必须落在同一个分组里，一个分组也只能描述一条连接**：分组就是一条连接被填写、保存和测试的单位，槽位跨分组的声明、以及一个已经描述过某数据源的分组再描述第二条连接，都会在安装期被拒；它们共处的那一个分组，也正是控制台给出「测试连接」的地方。
- **升级不清除插件配置与数据**；`DELETE /system/api/plugin/{pluginId}?purgeData=true` 才会清除插件设置、SQLite 数据库目录与文件区（默认 `false` 即保留，便于重装续用）。**外部 MySQL 库里的数据不在此列**：那是 DBA 的库，网关不会去删对方库里的表。

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

位置寻址是**系统级**能力，插件只是它的第一个使用方：插件的 key 为 `plugin-data/{插件ID}/{路径}`，其他子系统以后可以用别的前缀（如 `reports/2026/xxx.csv`），互不干扰。

另外，上传的外部插件 Jar 会物化到 `data/plugins` 目录用于类加载。

## 插件存储与外链

插件通过 `PluginContext.storage` 读写自己的文件区（`PluginStorage`），路径自动限定在该插件自己的命名空间内。位置寻址的文件会原样存储，因此可以签发**免登外链**：

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

内容寻址对象是压缩存储的，无法直接交给浏览器，因此**不提供外链**，只能通过 `PluginStorage` 的读写接口访问。

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
> A: 不需要。本项目使用 `Flyway` 自动初始化 MySQL 与 SQLite 两套库，无需手动建表。为保证数据安全，升级时请先备份数据库。

> **Q: 如何在浏览器中查看接口文档？**
>
> A: 使用 `dev` 构建时默认集成 Knife4j / Swagger UI；若在配置中把 `knife4j.production` 设为 `true`，将关闭文档访问。

# 许可证

[GPL-3.0](LICENSE)
