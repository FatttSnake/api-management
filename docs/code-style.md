# Code style ([简体中文](code-style_zh.md))

These rules apply to every Kotlin source file in this repository. `ktlint` enforces formatting and
imports; `detekt` enforces the structural rules that need semantics.

```shell
./gradlew ktlintCheck detekt   # check
./gradlew ktlintFormat         # fix formatting and imports
```

Both run as part of `check`. `config/detekt/baseline-*.xml` records the violations that predate the
rules; fix the code rather than adding to a baseline. A baseline entry for a function that is later
edited may hide a new violation, so keep the baseline files small and remove fixed entries.

## Files

- One public type per file, named after the type (`PluginRuntimeRegistry.kt`).
- Small, tightly coupled types (a schema and its nested option type) may share a file; put them
  after the main type.
- `internal` helpers live next to their only caller unless they are shared by a package.
- Test files mirror the package and class they cover (`PluginOpenApiBuilderTests.kt`).

## Declaration order inside a class

1. `companion object` (public constants and factories) when the class has one — at the end of the
   class body, which is the order `detekt`'s `ClassOrdering` enforces.
2. Constructor-injected properties.
3. Other properties and constants.
4. `init` blocks.
5. Secondary constructors.
6. Overrides, in lifecycle order (`onStart`, `onStop`, then the rest).
7. Public methods, grouped by responsibility.
8. Private methods, in call order.

KDoc and annotations stay attached to the declaration they describe.

## Imports

- No wildcard imports.
- Sorted alphabetically by `ktlint`; no unused imports.
- Do not use fully qualified names in code when an import would do.

## KDoc and comments

- English. Every type and every member keeps `@author` and `@since`; enum constants included.
- The body says what the declaration does, what it requires, and what it makes true. Do not
  restate the signature (`@param event event`, `@return response` are noise).
- Explain a non-obvious decision or a constraint, not the syntax.
- Keep comments on the code they explain; do not leave commented-out code.

## Constants and literals

- Name magic strings and numbers: plugin states, HTTP header names, route prefixes, page sizes,
  timeouts, buffer sizes, error codes.
- Error codes and response codes come from the existing enums; do not introduce raw numbers.
- Default sort order and pagination defaults live in one place per service family.

## Errors and logging

- Never swallow an exception silently. Either log it with context, translate it, or let it
  propagate. `runCatching` is not a substitute for error handling.
- Do not return internal exception text to a client; log it and return a stable message/code.
- Use the specific exception types (`PluginVersionConflictException`, `NoRecordFoundException`,
  ...) rather than a generic install failure.
- Use SLF4J parameterised logging (`logger.warn("... {}", value)`); never string concatenation.
- Do not log secrets, tokens, passwords or verification codes.

## Nullability and types

- Avoid `!!`. Use a local `val`, `requireNotNull`, or an explicit `?: throw`.
- Use `Boolean`/`Int` at the DTO boundary and convert explicitly to the entity representation
  (the API exposes `needKey: Boolean`, the column stores `0/1`).
- Prefer immutable collections and `val`; return snapshots from shared state.
- Long IDs are serialised as strings for the console; keep `@JsonSerialize(using = ToStringSerializer::class)` on ID fields.

## Queries and persistence

- Filter and paginate in the database, not in memory; list endpoints return `PageVo`.
- Use `setPageSort` for client sorting; it validates the field name.
- The optimistic-lock column is always named `version`.
- Services that mutate several rows are `@Transactional`; do not call a `@Transactional` method on
  the same bean (self-invocation bypasses the proxy).
- Re-read a row after a runtime operation if the operation can change its state; do not trust a
  pre-operation snapshot.

## Plugin API boundary

- `gateway-api` is Kotlin source but must stay Java-friendly: SAM interfaces, `@JvmStatic` on
  factories, `@JvmOverloads` on constructors with defaults, accessor functions instead of
  properties where a Java author would otherwise need Kotlin semantics.
- Never expose Spring, Jackson, MyBatis, Hikari, Servlet or `java.io.File` types across the
  boundary.
- Changing a public signature in `gateway-api` is an ABI change: plugins compile against the exact
  surface, so bump the API version or add an overload.
