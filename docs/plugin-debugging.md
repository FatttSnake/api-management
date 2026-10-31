# Debugging a plugin

A plugin runs in the gateway JVM. `PluginRuntimeLoader` creates a `PluginClassLoader`; the
platform loader exposes only the JDK, Kotlin stdlib and `gateway-api`, and the plugin's own jar
and dependency modules are resolved child-first. Nothing runs in a separate process.

## Attach a debugger

```shell
./gradlew bootRun -Pdebug=true
```

The agent is off by default and binds to `127.0.0.1:5005`; `debug.port`, `debug.address` and
`debug.suspend` change that. Attach from the plugin project and break inside plugin source.

The agent is an unauthenticated way into a JVM that holds every administrator's session. Keep it
off outside a development machine or a network you control.

## Edit loop

```shell
./gradlew deployPlugin -PapiPlugin.serverUrl=http://localhost:8111 -PapiPlugin.accessToken=<token>
```

`deployPlugin` builds, checks the ABI, signs, verifies, uploads dependency modules and installs the
jar. The running plugin is unloaded after in-flight requests drain, then the new runtime is
started and routes are swapped.

## Class loading

| Layer | Sees |
|---|---|
| `GatewayApiClassLoader` | JDK, Kotlin stdlib, `top.fatweb.apimanagement.api.*` |
| `ModuleClassLoader` | one loader unit (one module, or a declared shared-loader group), its dependency units, then the platform |
| `PluginClassLoader` | the plugin jar, its lock unit loaders, then the platform |

A plugin cannot see Spring, Jackson, MyBatis or any gateway internal class. If the plugin needs
one of those, declare it as a normal dependency; it becomes a module.

If a library cannot see another module's classes or `META-INF/services` entries, declare the
modules that must be mutually visible as a shared-loader group in `apiPlugin { sharedLoader(...) }`.
Grouping broadens visibility between those modules only; it does not widen the gateway allowlist.

Resources are resolved from the plugin jar and its modules, plus `gateway-api` resources. This is
a default-behavior boundary, not a security sandbox: signed plugin code can still use reflection.
