<div align="center">
    <h1>
        <img alt="API Management" src="docs/logo.svg" width="140">
        <br>
        <span>API Management</span>
    </h1>
</div>
<div align="center">
    <b>A self-hosted, plugin-driven API gateway &amp; management platform</b>
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

# Overview ([简体中文](README_zh.md), EN)

API Management is a **self-hosted API gateway and management platform**. It treats "available APIs" as resources delivered by **plugins**: a plugin is hot-installed into the gateway as a jar, the endpoints it declares get registered under a unified namespace, and they are then exposed in a uniform way — with **authentication, access control, rate limiting, metering, billing and auditing** handled by the platform and configured through a complete set of REST APIs.

The backend is written in **Kotlin + Spring Boot**. The stable API surface needed to build plugins is extracted into the standalone **plugin-sdk** submodule, which is published together with the main application. For plugin development, see the [api-management-plugins](https://github.com/FatttSnake/api-management-plugins) project.

# Features

- **Pluginized API hosting** — upload a plugin jar; the gateway verifies its **Ed25519 signature** (against the trust key store maintained by the administrator), then hot-installs / upgrades / uninstalls it inside an **isolated classloader / child container** — no restart required.
- **Namespaced & versioned routing** — every plugin endpoint is uniformly exposed as `/api/{plugin}/v{version}/...`; routes are persisted to the database and automatically restored after a restart.
- **Multi-layer access control** — accounts log in with **JWT**; callers may also use an **AccessKey / SecretKey**; optional **two-factor authentication (TOTP)**; each interface supports a `need-key` switch, an enable toggle and two access modes, `DEFAULT` / `RESTRICTED`, all expressed as RBAC powers.
- **Metering & billing** — per-interface price and billing mode (`FREE` / `SUCCESS_ONLY` / `ALWAYS`), **per-minute rate limits**; per-account **balance** and quota windows.
- **RBAC permission tree** — users, groups, roles and fine-grained powers (module / menu / operation / scope); available APIs are **grouped by plugin** in the permission tree.
- **Self-service for consumers** — "My API accounts", "My API keys", "API docs" and "My usage" endpoints for end users.
- **Observability & audit** — API usage statistics, monitoring, reports and aggregated statistics (periodically computed), plus operation audit and system logs.
- **Plugin SDK** — lifecycle hooks, **isolated per-plugin datasources** (several named ones per plugin, MySQL or SQLite), and a controlled context exposing the current caller, balance, interface configuration and plugin settings.
- **Flexible storage & mail** — file storage on local disk or **S3**; built-in mail delivery (registration, security notices, etc.).
- **Auto-migrated dual database** — MySQL holds key business data while SQLite holds high-volume data such as logs; both are initialized by Flyway.

# Project Structure

```
api-management/
├── build.gradle.kts / settings.gradle.kts        # Gradle build (Java 25 toolchain, plugin-sdk & plugin-gradle-plugin submodules)
├── docs/                                         # Project docs and assets (logo.svg, etc.)
├── plugin-sdk/                                   # Plugin SDK (standalone submodule for plugin authors)
│   └── src/main/kotlin/top/fatweb/apimanagement/sdk/
│       ├── annotation/ApiController.kt           # Declares "one plugin + a set of API routes"
│       └── plugin/                               # PluginDescriptor / PluginLifecycle / PluginContext / PluginStorage / ApiResponse / PluginSigner
├── plugin-gradle-plugin/                         # Gradle plugin `top.fatweb.api-plugin` for plugin authors
│   └── src/main/kotlin/top/fatweb/apimanagement/gradle/
│       ├── ApiPlugin.kt                          # Applies the apiPlugin DSL, adds the SDK dependency & repositories
│       └── ApiPluginTasks.kt                     # generatePluginDescriptor / genPluginKeys / signPlugin / verifyPlugin
├── src/main/kotlin/top/fatweb/apimanagement/
│   ├── ApiManagementApplication.kt               # Entry point (ensures data dirs, generates config template on first run)
│   ├── annotation / aspectj                      # Annotations & interceptors (API access, parameter processing, event/sys logs)
│   ├── component/
│   │   ├── api/                                  # Route registration & version matching (/api/{plugin}/v{version}/...)
│   │   ├── plugin/                               # Isolated plugin classloading & runtime context
│   │   ├── security/                             # JWT, CSRF tokens
│   │   └── storage/                              # Local / S3 file storage
│   ├── config/                                   # Security / Jackson / Redis / Flyway / MyBatis-Plus / Knife4j ...
│   ├── controller/
│   │   ├── permission/                           # Auth (login/register/2FA/token refresh), users, roles, groups, powers
│   │   ├── system/                               # Admin: plugins, trust keys, API accounts, API keys, audit, usage, monitor, report, statistics, settings, sys log
│   │   └── user/                                 # User self-service: my accounts, my keys, API docs, my usage
│   ├── entity / mapper / service / param / vo / converter   # Business layers
│   ├── cron/                                     # Scheduled aggregation of usage metrics & statistics
│   ├── filter / handler / exception / http       # Filters, global exception handling, HTTP client, Turnstile verification
│   ├── migration / properties / settings / util
│   └── resources/
│       ├── application.yaml                      # Base application config (datasource skeleton, SQLite, logging, MyBatis-Plus)
│       ├── application-config-template.yml       # Config template generated on first run
│       └── db/migration/{master,sqlite}/         # Flyway SQL migrations
└── data/                                         # Runtime data (auto-created when absent)
    ├── db/sqlite.db                              # SQLite (high-volume data such as logs)
    ├── log/                                      # Application logs
    ├── plugins/                                  # Directory where external plugin jars are materialized
    ├── objects/                                  # Content-addressed objects (sharded by SHA-256)
    ├── files/                                    # System-wide root of location-addressed files
    │   └── plugin-data/{pluginId}/               # A plugin's own file area
    └── config/settings.yml                       # Dynamic system settings
```

# Requires

- Java 25+
- MySQL 8.0+
- Redis

# Related projects

[Web Console](https://github.com/FatttSnake/api-management-console)

[Plugins](https://github.com/FatttSnake/api-management-plugins)

# Quick Start

1. Build the executable jar (Optional, download from the [Releases](https://github.com/FatttSnake/api-management/releases) page)

```shell
./gradlew bootJar          # on Windows use gradlew.bat bootJar
# The artifact lands in build/libs/, named like api-management.jar
```

2. Run once to generate the configuration-file template

```shell
java -jar api-management.jar
```

> When no `application-config.yml` exists in the running directory or the `data` directory, the program writes a configuration template (containing an auto-generated random `token-secret`) to `data/application-config.example.yml` and then exits.

3. Copy the template to the running directory and rename it to `application-config.yml`

```shell
cp ./data/application-config.example.yml application-config.yml
```

4. Edit the configuration file — fill in MySQL, Redis, etc. (see [Configuration](#configuration))

5. Run again

```shell
java -jar api-management.jar
```

The service listens on port `8080` by default (changeable via `server.port`). The databases are initialized automatically by Flyway on the first start.

# Configuration

The program reads `application-config.yml` from the **running directory or the `data` directory**. Main configuration blocks:

| Block | Purpose | Key options |
| --- | --- | --- |
| `app.admin` | Administrator created when the database is initialized (optional) | `username`, `password`, `nickname`, `email` |
| `app.security` | JWT token & security | `token-secret` (required; random value in the generated template), `token-prefix`, `access-token-ttl`, `refresh-token-ttl` |
| `app.storage` | File storage | `mode` (`local`/`s3`), `local.root`, `public-base-url`, `external-url-*`, `s3.*` |
| `server.port` | Server port | default `8080` |
| `spring.datasource.dynamic.datasource.master` | MySQL (key business data) | `url`, `username`, `password` |
| `spring.data.redis` | Redis | `host`, `port`, `password`, `database` |
| `logging` | Logging | `level.root`, `file.name` |
| `knife4j` | API docs | `production` (`true` disables the docs UI) |

Example:

```yaml
app:
  admin:                      # optional; when set before DB init, the specified account is used
    username: admin
    password: admin
  security:
    token-secret: <uuid>      # JWT signing secret (random value on first template generation)
  storage:
    mode: local               # file storage mode [local, s3]
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

# Plugin Development

Every "available API" on the platform comes from a plugin. For plugin development, see the [api-management-plugins](https://github.com/FatttSnake/api-management-plugins) project.

A plugin is a standalone Gradle project that applies the **`top.fatweb.api-plugin`** Gradle plugin (published together with the SDK from this repository). It adds the SDK dependency automatically, generates the `api-plugin.json` descriptor from an `apiPlugin { }` DSL, creates the Ed25519 developer key pair, embeds the public key into the jar and signs it — `./gradlew build` produces an already-signed plugin jar.

To make the SDK and the Gradle plugin available to plugin projects, publish them from this repository once:

```shell
./gradlew :plugin-sdk:publishToMavenLocal :plugin-gradle-plugin:publishToMavenLocal
```

## Plugin configuration and datasources

A plugin ships a `META-INF/plugin-config.json` inside its own jar declaring which settings it has (their type, default, constraints, whether they are secrets, which named datasources it needs and in which dialect). The gateway reads it **while mounting**, snapshots it onto the plugin row, and the console renders a form from it; the plugin itself only reads `PluginContext.getSetting(key)`, with the gateway resolving defaults. A key declared there belongs to the administrator, and the plugin's `saveSetting` raises on it.

| Endpoint | Description |
|---|---|
| `GET /system/api/plugin/{pluginId}/config` | Declared schema plus current values (a stored secret comes back as the mask `******`, never its value), the settings a datasource is composed from, and each declared datasource's name, dialect, requiredness and state |
| `PUT /system/api/plugin/config` | Save configuration; **an ordinary value takes effect immediately** (the plugin reads per call), sending a secret's mask back keeps the stored one, and a **blank one clears it**. A change to a datasource's settings **remounts the plugin**, because the gateway is what builds the connection from them |
| `POST /system/api/plugin/{pluginId}/config/datasource/test` | Try a connection with values that have not been saved yet (`name` plus the config values to use; a value left out, and a masked secret, mean "read what is stored"). MySQL only - a SQLite datasource is a file the gateway supplies. Answers 40067 with the driver's reason when it fails |
| `POST /system/api/plugin/{pluginId}/reload` | Remount the installed jar in place, for a **jar that has been replaced** - it leaves the plugin's configuration and data untouched. A datasource change does not need it |

- Only a plugin that declares a `datasources` entry gets anything in `context.datasources`, and only the ones that are configured: a name that is absent is a datasource the gateway has nothing to connect with yet. One plugin can declare several and mix the two dialects.
- The dialect is declared by the plugin (`dbType`), never chosen by the administrator: the plugin's own SQL and DDL decide it, and its author is the only one who knows which dialect it was verified against. **SQLite needs no configuration at all** - declaring it is what supplies it, from `app.storage.plugin-datasource-dir/{pluginId}/{name}.db`, with no row stored and no administrator action.
- **A MySQL datasource is described by ordinary config fields**, named one by one in its declaration. They are settings like any other, so they get a rendered form, the declared constraints and secret encryption for free - the password in particular is an ordinary secret, which is what makes clearing it expressible. The gateway composes the JDBC URL and passes `params` to the driver as connection properties, so nobody writes a connection string. **A datasource counts as configured once its host has a value**; blanking that host is how it is un-configured, and the plugin simply stops being given it. `params` is refused for properties that reach past the connection (`autoDeserialize`, `allowLoadLocalInfile`, `propertiesTransform`, …) - a mitigation rather than a sandbox: the boundary that holds is the plugin signature and the trust store behind it.
- **An upgrade keeps the plugin's configuration and data**; `DELETE /system/api/plugin/{pluginId}?purgeData=true` is what purges the plugin's settings, its SQLite database directory and its file area (the default, `false`, keeps them so a reinstall continues where it left off). **An external MySQL database is not in that list**: it belongs to whoever operates it, so the gateway never drops tables it does not own.

# Security

- Integrates **Spring Security** and uses **JWT** tokens (access token 2 hours / refresh token 128 days by default, both configurable); the signing secret `app.security.token-secret` lives in the configuration file.
- Supports **two-factor authentication (TOTP)**, along with CSRF token management and related security filters.
- On first startup an administrator is created: the account configured under `app.admin` is used if present; otherwise the username defaults to `admin` with a **randomly generated password**, printed to the console exactly once — change it promptly after logging in.

# Storage

`app.storage.mode` supports two file-storage backends:

- `local` — writes to local disk, rooted at `app.storage.local.root` (default `data`).
- `s3` — writes to an S3-compatible object store (`endpoint`, `accessKey`, `secretKey`, `region`, `bucket`, etc.; supports `path` / `virtualHosted` path styles).

Two addressing modes sit on top of either backend. The segments below are appended
where they are used, they are not configuration knobs:

| | Content addressing | Location addressing |
|---|---|---|
| Key | SHA-256 of the content, deduplicated globally | A path relative to the location-addressed root |
| Stored as | zstd-compressed | Verbatim bytes |
| local | `data/objects/{first2}/{rest}` | `data/files/{key}` |
| s3 | `{prefix}/objects/{first2}/{rest}` | `{prefix}/files/{key}` |

Location addressing is a **system-wide** capability of which plugins are only the
first user: a plugin's key is `plugin-data/{pluginId}/{path}`, and another subsystem
can claim its own prefix later (e.g. `reports/2026/xxx.csv`) without interfering.

In addition, uploaded external plugin jars are materialized to `data/plugins` for class loading.

## Plugin storage and external links

A plugin reads and writes its own file area through `PluginContext.storage`
(`PluginStorage`). Every path is confined to that plugin's own namespace.
Location-addressed files are stored verbatim, which is what allows a **login-free
external link** to be issued for them:

- `local` — the gateway mints an HMAC-signed URL (`/public/storage/{pluginId}/{path}?e=<expiry>&s=<signature>`), keyed on `app.security.tokenSecret`. Stateless, so it works unchanged across instances. The URL carries a public reference rather than the internal key, so changing the layout does not invalidate links already handed out.
- `s3` — a presigned URL against the object store, which the client fetches directly without touching the gateway.

```yaml
app:
  storage:
    public-base-url: https://api.example.com  # Public base of external links; falls back to the request URL (set it behind a reverse proxy)
    external-url-default-ttl: 1               # Default validity of an external link
    external-url-ttl-unit: hours              # Unit of the TTLs
    external-url-max-ttl: 168                 # Upper bound; longer requests are clamped
```

Content-addressed objects are compressed at rest and cannot be handed to a browser,
so they have **no** external link and are reachable only through the read/write
methods of `PluginStorage`.

> An external link is an unrevocable bearer credential: whoever holds it can read
> the file until it expires. Rotating `app.security.tokenSecret` invalidates every
> outstanding link and logs every user out. In `local` mode the link is same-origin
> with the console, so every type outside a strict image allowlist is served with
> `Content-Disposition: attachment` and `nosniff`.

# Database

A dynamic-datasource dual-database architecture is used:

- **MySQL** (`master`) — key business data such as users, permissions, plugins, interfaces, accounts and keys;
- **SQLite** (`data/db/sqlite.db`) — high-volume data such as logs.

Both databases are initialized and upgraded automatically by **Flyway**, with migration scripts under `src/main/resources/db/migration/{master,sqlite}`. **Back up the databases before upgrading.**

# Q&A

> **Q: What is the default administrator account and password?**
>
> A: If `app.admin` is configured before the database is initialized, the specified account and password are used. Otherwise the username defaults to `admin` and the password is randomly generated on the first start — see the console output (shown only once).

> **Q: Do I need to initialize the database manually?**
>
> A: No. This project uses `Flyway` to automatically initialize both the MySQL and SQLite databases — no manual schema setup is needed. To ensure data safety, please back up the databases before upgrading.

> **Q: How can I view the API documentation in a browser?**
>
> A: The `dev` build bundles Knife4j / Swagger UI; setting `knife4j.production` to `true` in the configuration disables access to the documentation.

# License

[GPL-3.0](LICENSE)
