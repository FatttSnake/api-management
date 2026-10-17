# Debugging a plugin ([简体中文](plugin-debugging_zh.md), EN)

A plugin's jar is loaded into the **gateway's own JVM**, by a child-first class loader
(`PluginClassLoaderManager.create`). Nothing about a plugin runs in a separate process, so a
debugger attached to the gateway can put a breakpoint in plugin source and step through it — and
anything the plugin logs lands in the gateway's log, `data/log/api-management.log`.

Logging goes through SLF4J: the SDK puts `slf4j-api` on the compile classpath, so a plugin just
declares `private val logger = LoggerFactory.getLogger(MyController::class.java)`. At runtime the
gateway's class loader resolves `org.slf4j` from the gateway itself, so those calls get the
gateway's binder and appenders and land in the gateway's log under the plugin class's logger name.
Do not bundle `slf4j-api` or a logging backend: `org.slf4j.` is a shared prefix with no fallback
to the jar, so a bundled copy is never loaded. `println` needs no declaration and is the quicker
way to see something while debugging, though it goes to standard output rather than through the
logging system.

## 1. Start the gateway with a debug agent

### From this repository

```shell
./gradlew bootRun -Pdebug=true
```

The switch reads a Gradle property, an environment variable, or an `ORG_GRADLE_PROJECT_`
variable, so `DEBUG=true` works the same way. It has three knobs, each with a default:

| Gradle property | Environment variable | Default | Meaning |
|---|---|---|---|
| `debug.port` | `DEBUG_PORT` | `5005` | Port the agent listens on |
| `debug.address` | `DEBUG_ADDRESS` | `127.0.0.1` | Interface to bind |
| `debug.suspend` | `DEBUG_SUSPEND` | `n` | `y` holds the gateway at startup until a debugger attaches |

The gateway prints `Listening for transport dt_socket at address: 5005` once the agent is up, and
the port is bound to the loopback interface. `-Pdebug.suspend=true` is what it takes to break
inside a plugin's `onStart`, which runs while the plugin is being mounted.

### From a plain jar

```shell
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005 \
     -jar api-management.jar
```

`JAVA_TOOL_OPTIONS` also works and is the way in when the command line cannot be changed, but it
is inherited by every JVM started underneath — including the Gradle daemon — so prefer the
explicit `-agentlib:`.

### In a container

The image is built elsewhere, so the agent has to come from the environment:

```shell
docker run -p 8080:8080 -p 5005:5005 \
  -e JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  api-management
```

Inside a container the agent **must** bind `*`: a container's loopback interface is not what the
published port reaches. Publish the debug port only on a trusted network, or bind it to
`127.0.0.1` on the host (`-p 127.0.0.1:5005:5005`).

### On a remote host

Bind the agent to the loopback interface there and let the tunnel be the way in:

```shell
# on the host: address=127.0.0.1:5005
ssh -L 5005:127.0.0.1:5005 user@host
# then attach the debugger to localhost:5005
```

> **JDWP is remote code execution.** The agent has no authentication of any kind: whoever can
> reach the port can run code in the gateway, which holds every administrator's session and every
> plugin's data. Never expose it on a public interface, and turn it off when you are done — which
> is why nothing here enables it by default and why the default bind is the loopback interface.

## 2. Attach from the plugin project

In the plugin project — not this repository:

1. **Run → Edit Configurations → + → Remote JVM Debug**.
2. Host and port: `localhost:5005`, or whatever the tunnel or published port ended up being.
3. **Use module classpath**: the plugin project's own `main` module.
4. Debug it, then hit the plugin's route.

No path to the materialized jar is needed. The gateway copies the uploaded jar to
`data/plugins/<sha256>.jar` and loads it from there, but a remote debugger matches a breakpoint by
**fully-qualified class name** and takes the line table from the loaded bytecode — which class
loader loaded it, and what the file is called, make no difference. The only requirement is that
the bytecode in the gateway came from the same build as the source on disk; edit the source
without deploying and the breakpoints will drift to the wrong lines.

## 3. The edit loop

`deployPlugin` builds, signs and verifies the jar and pushes it straight to a running gateway,
replacing the installed version:

```shell
./gradlew deployPlugin \
  -PapiPlugin.serverUrl=http://localhost:8080 \
  -PapiPlugin.accessToken=<access token>
```

Both settings also read an environment variable (`API_PLUGIN_SERVER_URL`,
`API_PLUGIN_ACCESS_TOKEN`), and neither ever belongs in `build.gradle.kts` — that file is
committed. The task fails before uploading if the gateway would refuse the jar, naming the reason
(an untrusted signing key, a broken signature, a version older than the one installed).

The access token is the one the console login returns (`accessToken`); it lasts 2 hours by
default, so an edit loop tends to start with one login. A **new version's jar goes up through
`install`, never `reload`** — `reload` re-reads the jar the gateway already has, so it cannot pick
up a rebuild.

### Hot reload, without deploying

With a debugger attached, IntelliJ's **Build → Reload Changed Classes** redefines loaded classes
in place, which works on plugin classes like any others. It is limited to **method bodies of
classes already loaded**:

| Change | Reload Changed Classes |
|---|---|
| Body of a method that is already loaded | works, no deploy needed |
| Adding or removing a method, a field or a class | needs a deploy |
| Changing a signature, a modifier or a hierarchy | needs a deploy |
| A new or changed `@ApiController`, `@RequestMapping`, `@Operation` | needs a deploy — Spring scanned and registered the routes at mount time and will not rescan |
| Anything outside the classes: `META-INF/api-plugin.json`, `META-INF/plugin-config.json`, the key pair | needs a deploy |

So hot reload is a way to skip a deploy while tuning a method; `deployPlugin` is the loop for
everything else.

## 4. When something goes wrong

A plugin that fails to mount is still installed: it serves nothing, its routes are not
registered, and the reason is on its row as `load_error`, which the plugin list shows. The
gateway's own validation failures are the other half of the picture — those refuse the upload
outright, and `deployPlugin` reports them.

- A plugin's own `onStart` throwing: installed but down, `load_error` set. A save against it, or
  a `reload`, mounts it again — so the fastest fix is usually to correct the setting it could not
  start against.
- A gateway-side rejection: `40064` the signing key is not trusted, `40065` the signature is
  invalid, `40066` the version is older than the one installed, `40063` the mount itself failed.
- A class the plugin bundles that the gateway also owns: the shared prefixes in
  `PluginClassLoaderManager` resolve from the gateway with no fallback to the jar, so a plugin
  that ships its own copy of one has to stop bundling it.
