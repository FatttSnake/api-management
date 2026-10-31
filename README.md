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

The backend is written in **Kotlin + Spring Boot**. The stable API surface needed to build plugins is extracted into the standalone **`gateway-api`** submodule; the plugin runtime, module store and Gradle tooling live in **`plugin-runtime`**, **`plugin-signing`** and **`plugin-gradle-plugin`**. For plugin examples, see the [api-management-plugins](https://github.com/FatttSnake/api-management-plugins) project.

# Features

- **Pluginized API hosting** — upload a plugin jar; the gateway verifies its **Ed25519 signature** (against the trust key store maintained by the administrator), then hot-installs / upgrades / uninstalls it with an **isolated plugin class loader and per-plugin module graph** — no restart required.
- **Namespaced & versioned routing** — every plugin endpoint is uniformly exposed as `/api/{plugin}/v{version}/...`; routes are persisted to the database and automatically restored after a restart.
- **Multi-layer access control** — accounts log in with **JWT**; callers may also use an **AccessKey / SecretKey**; optional **two-factor authentication (TOTP)**; each interface supports a `need-key` switch, an enable toggle and two access modes, `DEFAULT` / `RESTRICTED`, all expressed as RBAC powers.
- **Metering & billing** — per-interface price and billing mode (`FREE` / `SUCCESS_ONLY` / `ALWAYS`), **per-minute rate limits**; per-account **balance** and quota windows.
- **RBAC permission tree** — users, groups, roles and fine-grained powers (module / menu / operation / scope); available APIs are **grouped by plugin** in the permission tree.
- **Self-service for consumers** — "My API accounts", "My API keys", "API docs" and "My usage" endpoints for end users.
- **Observability & audit** — API usage statistics, monitoring, reports and aggregated statistics (periodically computed), plus operation audit and system logs.
- **Plugin platform** — lifecycle hooks, **isolated per-plugin datasources** (several named ones per plugin, MySQL or SQLite), independent dependency modules with version locks, and a controlled `GatewayContext` exposing settings, storage, caller identity, balance and interface configuration.
- **Flexible storage & mail** — file storage on local disk or **S3**; built-in mail delivery (registration, security notices, etc.).
- **Auto-migrated dual database** — MySQL holds key business data while SQLite holds high-volume data such as logs; both are initialized by Flyway.

# Project Structure

```
api-management/
├── build.gradle.kts / settings.gradle.kts        # Gradle build (Java 25 toolchain, plugin modules included)
├── docs/                                         # Project docs and assets (logo.svg, plugin-debugging.md / _zh.md, etc.)
├── gateway-api/                                  # Stable plugin contract (Kotlin, Java-friendly, ABI manifest)
│   └── src/main/kotlin/top/fatweb/apimanagement/api/v1/
│       └── ApiPlugin / GatewayContext / ApiRoutes / ApiRequest / ApiResponse / capabilities
├── plugin-runtime/                               # Class loading, module store, route registry, install lifecycle
│   └── src/main/kotlin/top/fatweb/apimanagement/plugin/runtime/
│       ├── classloader/                          # GatewayApiClassLoader / ModuleClassLoader / PluginClassLoader
│       ├── module/                               # lock parser, content-addressed module store
│       ├── route/                                # route registry, MVC handler mapping & adapter
│       └── runtime/                              # descriptor parser, GatewayContext implementation, plugin loader
├── plugin-signing/                               # Ed25519 signing/verification shared by gateway & Gradle tooling
├── plugin-gradle-plugin/                         # Gradle plugin `top.fatweb.api-plugin` for plugin authors
│   └── src/main/kotlin/top/fatweb/apimanagement/gradle/
│       ├── ApiPlugin.kt                          # Applies the apiPlugin DSL, adds gateway-api as compileOnly
│       ├── GeneratePluginLock.kt                 # Resolves runtime dependencies into api-plugin-deps.json
│       ├── CheckPluginAbi.kt                     # Verifies plugin bytecode against gateway-api.abi.json
│       └── GeneratePluginDescriptor / GenPluginKeys / SignPlugin / VerifyPlugin / DeployPlugin
├── src/main/kotlin/top/fatweb/apimanagement/
│   ├── ApiManagementApplication.kt               # Entry point (ensures data dirs, generates config template on first run)
│   ├── annotation / aspectj                      # Annotations & interceptors (API access, parameter processing, event/sys logs)
│   ├── component/
│   │   ├── api/                                  # Route registration & version matching (/api/{plugin}/v{version}/...)
│   │   ├── plugin/                               # Plugin artifacts, config schema, datasources, runtime registry
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
├── src/main/resources/
│   ├── application.yaml                          # Base application config (datasource skeleton, SQLite, logging, MyBatis-Plus)
│   ├── application-config-template.yml           # Config template generated on first run
│   └── db/migration/{master,sqlite}/             # Flyway SQL migrations
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
# The artifact lands in build/libs/, named like api-management-<version>.jar
```

2. Run once to generate the configuration-file template

```shell
java -jar api-management-<version>.jar
```

> When no `application-config.yml` exists in the running directory or the `data` directory, the program writes a configuration template (containing an auto-generated random `token-secret`) to `data/application-config.example.yml` and then exits.

3. Copy the template to the running directory and rename it to `application-config.yml`

```shell
cp ./data/application-config.example.yml application-config.yml
```

4. Edit the configuration file — fill in MySQL, Redis, etc. (see [Configuration](#configuration))

5. Run again

```shell
java -jar api-management-<version>.jar
```

The service listens on port `8080` by default (changeable via `server.port`). The databases are initialized automatically by Flyway on the first start.

# Configuration

The program reads `application-config.yml` from the **running directory or the `data` directory**. Main configuration blocks:

| Block | Purpose | Key options |
| --- | --- | --- |
| `app.admin` | Administrator created when the database is initialized (optional) | `username`, `password`, `nickname`, `email` |
| `app.security` | JWT token & security | `token-secret` (required; random value in the generated template), `token-prefix`, `access-token-ttl`, `refresh-token-ttl`, `trust-proxy-headers`, `allowed-origin-patterns` |
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

For the full architecture walkthrough — modules, class loading, install/startup flow, request path and data model — see [docs/plugin-platform.md](docs/plugin-platform.md). Conventions for code layout, declaration order, imports, KDoc and errors are in [docs/code-style.md](docs/code-style.md); `./gradlew ktlintCheck detekt` enforces them.

Every API is contributed by a plugin. A plugin is a standalone Gradle project applying the
**`top.fatweb.api-plugin`** Gradle plugin; its only compile dependency is
`top.fatweb:api-management-gateway-api`.

Publish the contract and tooling once:

```shell
./gradlew :gateway-api:publishToMavenLocal :plugin-signing:publishToMavenLocal :plugin-gradle-plugin:publishToMavenLocal
```

`./gradlew build` produces a signed jar containing:

| Entry | Purpose |
|---|---|
| `META-INF/api-plugin.json` | Descriptor format 1: pluginId, versions, mainClass, `requiresGateway`, `requiresGatewayApi` |
| `META-INF/api-plugin-deps.json` | Exact dependency lock: GAV, SHA-256, size, direct dependency edges and optional shared-loader groups |
| `META-INF/plugin.pub.pem` / `META-INF/plugin.sig` | Ed25519 public key and signature |

`checkPluginAbi` (part of `build`) reads `META-INF/gateway-api.abi.json` from the gateway API jar
and fails when the plugin references a class, method, field or constructor the gateway does not
have. Signing runs after ABI validation.

## Programming model

Plugins do not use Spring MVC annotations and do not get a Spring child context. Implement
`ApiPlugin` and register routes on the `GatewayContext` passed to `onStart`:

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

Java plugin authors use the same types. `ApiHandler`, `EventSubscriber` and `Subscription` are
SAM interfaces, `ApiResponse`/`ParameterSpec`/`ApiPrincipal` expose static factories, and
`GatewayContext` uses accessors (`context.api()`, `context.settings()`) rather than Kotlin
properties.

`GatewayContext` is the only gateway surface: `api()`, `settings()`, `dataSources()`,
`storage()`, `users()`, `audit()`, `events()`, `http()`, `scheduler()`, `json()`, `logger()` and
`metadata()`. It never exposes the Spring container, beans, mappers or the gateway object mapper.

The request body is raw bytes (`request.body` / `request.bodyText()`). Use `context.json()` for
structured JSON, or a library the plugin brings itself. A response body can be bytes, a string, a
map or a plain bean; `ApiResponse.raw(...)` bypasses the business envelope.

## Dependencies

The plugin jar is thin. Runtime dependencies are resolved at build time into the lock, and each
module jar is uploaded separately (by `deployPlugin` before the plugin). The gateway stores modules
content-addressed under `app.storage.plugin-module-dir`, verifies SHA-256, and shares one class
loader for the same loading unit. Different versions coexist.

By default one module is one loading unit, and a module sees only its declared dependencies.
Some libraries do not fit that model: Flyway resolves database types from a static
`ServiceLoader` registry bound to the loader that defined `flyway-core`, and MyBatis-Plus
references MyBatis and SLF4J from POM dependencies marked optional. Declare such modules as a
**shared loader group** and they are loaded by one composite child-first loader, so they see each
other without a dependency edge:

```kotlin
apiPlugin {
    sharedLoader("org.flywaydb:flyway-core", "org.flywaydb:flyway-mysql")
}
```

The DSL takes `group:artifact` values without versions; the resolved version comes from the
classpath. A group is the unit of sharing, reference counting and unloading, and grouping must not
create a cycle between loading units. Group only modules that must be mutually visible, and never
modules that ship the same classes: the first jar by digest wins.

A module the lock names but the gateway does not have is downloaded from
`app.plugin.module.repositories` (Maven Central by default) while `app.plugin.module.online` is
true, and accepted only when its SHA-256 and size match the lock. `deployPlugin` still uploads
modules first, so offline deployments work without network access. The console lists every
dependency and its download progress before installing the plugin.

The gateway provides **only** the JDK, Kotlin stdlib and `gateway-api`. Spring, Jackson, MyBatis,
Hikari, Flyway, JDBC drivers and everything else are ordinary plugin dependencies and are loaded
from the plugin's own module graph. Kotlin stdlib and `org.jetbrains:annotations` are excluded from
the lock because the platform supplies them.

The lock is generated from `runtimeClasspath`; the runtime never resolves a version. Modules can
also be managed independently through `/system/api/plugin/module` (upload, list, fetch from Maven,
delete an unreferenced module, GC).

Resource lookups see the plugin jar and its modules, plus the platform's `gateway-api` resources.
This is a default-behavior boundary, not a security sandbox: plugins are signed, and code running
in the same JVM can still use reflection.

## Plugin configuration and datasources

A plugin ships a `META-INF/plugin-config.json` inside its own jar declaring which settings it has (their type, default, constraints, whether they are secrets, which named datasources it needs and in which dialect). The gateway reads it **while mounting**, snapshots it onto the plugin row, and the console renders a form from it; the plugin reads `GatewayContext.settings().get(key)`, with the gateway resolving defaults. A key declared there belongs to the administrator, and `settings().save(...)` refuses it.

| Endpoint | Description |
|---|---|
| `GET /system/api/plugin/{pluginId}/config` | Declared schema + current values + datasource declarations. A secret is never returned (`hasValue` says whether one is stored, `unreadable` whether the stored ciphertext can still be read); each datasource carries its name, dialect, requiredness and state; each group names the datasource it describes (`null` when it describes none) — that group being the one a console offers a connection test on |
| `PUT /system/api/plugin/config` | **Saved a group at a time**: `{pluginId, groups:[{key, values:[{key, value}]}]}`. An ordinary value takes effect immediately (the plugin reads per call); a change to a datasource's settings **remounts the plugin**, because the gateway is what builds the connection from them, and **a plugin left down by a failed mount is remounted on any save** |
| `POST /system/api/plugin/{pluginId}/config/datasource/test` | Try a connection with values that have not been saved yet (`name` plus the config values to use; a value left out or sent as `null` means "read what is stored"). MySQL only — a SQLite datasource is a file the gateway supplies. Answers 40067 with the driver's reason when it fails |
| `POST /system/api/plugin/{pluginId}/reload` | Remount the installed jar in place, for a **jar that has been replaced** — it leaves the plugin's configuration and data untouched. A datasource change does not need it |

A value has three states: **left out means leave it alone, blank means clear it, anything else is written**. A number is the only type with a blank form, so an unset one reports `value: null` with the declared default in `default`, which is what the plugin reads; every other type reports the declared default as its `value`. `hasValue` says which of the two is shown, and writing the default back restores it. **A required field is checked within the group it was submitted with**. **A secret is never handed back** — leaving the key out keeps the stored one, a blank one clears it.

Datasources: only a plugin that declares a `datasources` entry gets anything in `context.datasources`, and only the configured ones — an absent name is a datasource the gateway cannot connect yet. One plugin may declare several and mix dialects. The dialect is declared by the plugin (`dbType`), never chosen by the administrator. **SQLite needs no configuration** — declaring it supplies `app.storage.plugin-datasource-dir/{pluginId}/{name}.db`. **MySQL datasources are described by ordinary config fields**, so they get the form, constraints and secret encryption for free; the gateway composes the JDBC URL and passes `params` as connection properties (properties reaching past the connection such as `autoDeserialize`, `allowLoadLocalInfile` and `propertiesTransform` are refused). **A datasource is configured once both its host and database have values**, and blanking either un-configures it — so a host or database field declaring `minLength` or `pattern` refuses that blank. Those fields belong to one group, and one group describes one datasource, because a group is the form a connection is filled in, saved and tested as.

**An upgrade replaces the installed version in place and keeps everything the administrator set up**: the plugin row and its interface rows are updated rather than rebuilt, so plugin-level pricing, rate limit and access mode, each interface's pricing, billing mode, key requirement, rate limit, access mode and enable flag, and every role grant survive. Only interfaces the new version no longer serves are removed with their grants. **Re-installing the installed version is allowed** and behaves the same, so an edit loop need not bump `versionCode`; only a version **older** than the installed one is refused (40066).

A plugin can be switched off without being removed: `PATCH /system/api/plugin` with `enable=false` takes its runtime down. **A disabled plugin does not run at all** — it is not loaded at startup, its routes are not registered, and requests to its APIs are refused. Its settings, file area, datasource configuration and grants are untouched; switching it back on mounts the same jar over the same rows.

`DELETE /system/api/plugin/{pluginId}?purgeData=true` purges the plugin's settings, SQLite database directory and file area (the default `false` keeps them for a reinstall). The plugin's own data is deleted by its `onPurge` hook, which runs while its datasources, storage and settings are still open and nothing is calling into it — where it drops its own tables (including those in an external MySQL the gateway never touches) and releases the content-addressed objects it saved. Only a real uninstall runs it: an upgrade, a reload, a switch-off and a shutdown never do. A plugin that is not mounted is mounted first so its exit hooks can run, and **a failure there refuses the uninstall** (40068) instead of deleting the administrator's record of a plugin whose data it failed to remove; nothing has been deleted, so the uninstall can be retried. Released content-addressed objects are swept on a schedule.

Lifecycle hooks: `onStart` (every mount, before routes are activated), `onStop` (every unmount), `onUninstall` (as the version is taken out — an upgrade runs it too, and it is not the place to delete anything) and `onPurge` (an uninstall that asked for its data to be deleted). See `gateway-api`'s `ApiPlugin`.

## Debugging a plugin

A plugin's jar is loaded into the gateway's own JVM, so a debugger attached to the gateway can break inside plugin source. Start the gateway with the agent and attach from the plugin project:

```shell
./gradlew bootRun -Pdebug=true
```

The agent is off by default and binds to `127.0.0.1:5005`; `debug.port`, `debug.address` and `debug.suspend` change that. [docs/plugin-debugging.md](docs/plugin-debugging.md) covers attaching from the plugin project and the class-loading boundary — worth knowing, because the agent is an unauthenticated way into a JVM that holds every administrator's session.

The edit loop itself is one task, which builds, signs, verifies, uploads any dependency modules and pushes the jar to a running gateway:

```shell
./gradlew deployPlugin -PapiPlugin.serverUrl=http://localhost:8080 -PapiPlugin.accessToken=<token>
```

Both settings also read an environment variable (`API_PLUGIN_SERVER_URL`, `API_PLUGIN_ACCESS_TOKEN`), and neither belongs in `build.gradle.kts`. The token is the `accessToken` a console login returns, and lasts 2 hours by default.

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

Location addressing is **system-wide**: a plugin's key is `plugin-data/{pluginId}/{path}`,
leaving other prefixes free for other subsystems.

In addition, uploaded external plugin jars are materialized to `data/plugins` for class loading.

## Plugin storage and external links

A plugin reads and writes its own file area through `GatewayContext.storage()`.
Every path is confined to that plugin's own namespace.
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
methods of the `Storage` capability.

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
> A: No. Flyway initializes both databases automatically. Back them up before upgrading.

> **Q: How can I view the API documentation in a browser?**
>
> A: The `dev` build bundles Knife4j / Swagger UI; `knife4j.production: true` disables it.

# License

[GPL-3.0](LICENSE)
