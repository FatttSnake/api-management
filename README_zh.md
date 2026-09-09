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
- **插件 SDK** —— 提供生命周期钩子、**隔离的插件专属数据源**、以及获取当前调用者 / 余额 / 接口配置 / 插件设置的受控上下文。
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
│       └── plugin/                               # PluginDescriptor / PluginLifecycle / PluginContext / ApiResponse / PluginSigner
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
    ├── objects/                                  # 本地文件存储根目录
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
| `app.storage` | 文件存储 | `mode`（`local`/`s3`）、`local.root`、`s3.*` |
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

# 安全

- 集成 **Spring Security**，采用 **JWT** 令牌（访问令牌默认 2 小时、刷新令牌默认 128 天，均可在配置中调整），密钥 `app.security.token-secret` 存于配置文件中。
- 支持**两步验证（TOTP）**，并提供 CSRF 令牌管理与相关安全过滤器。
- 首次启动创建管理员：如已在 `app.admin` 中指定账号则使用之；否则默认用户名 `admin` 并**随机生成密码**，仅在控制台打印一次，请及时修改。

# 存储

`app.storage.mode` 支持两种文件存储方式：

- `local`：写入本地磁盘，根目录由 `app.storage.local.root` 指定（默认 `data/objects`）。
- `s3`：写入 S3 兼容对象存储（需配置 `endpoint`、`accessKey`、`secretKey`、`region`、`bucket` 等，支持 `path` / `virtualHosted` 两种路径风格）。

另外，上传的外部插件 Jar 会物化到 `data/plugins` 目录用于类加载。

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
