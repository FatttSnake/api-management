# 代码规范 ([EN](code-style.md))

本规范适用于仓库内所有 Kotlin 源码。`ktlint` 负责格式与 import，`detekt` 负责需要语义分析的
结构规则。

```shell
./gradlew ktlintCheck detekt   # 检查
./gradlew ktlintFormat         # 修复格式与 import
```

两者都已挂到 `check`。`config/detekt/baseline-*.xml` 记录了规范生效前就存在的违规；应修代码，
而不是往 baseline 里加条目。baseline 以函数签名为键，函数被修改后旧条目可能掩盖新问题，所以
baseline 要保持小，并随手删除已修复的条目。

## 文件

- 一个文件一个公开类型，文件名与类型名一致（`PluginRuntimeRegistry.kt`）。
- 紧密相关的小类型（例如一个 schema 和它的 option 类型）可以同文件，放在主类型之后。
- `internal` 辅助类型放在唯一调用方旁边，除非它被整个包共享。
- 测试文件镜像被测类的包和类名（`PluginOpenApiBuilderTests.kt`）。

## 类内声明顺序

1. `companion object`（公开常量与工厂）——放在类体末尾，这是 `detekt` 的 `ClassOrdering`
   强制要求的顺序。
2. 构造注入属性。
3. 其他属性与常量。
4. `init` 块。
5. 次构造函数。
6. override，按生命周期顺序（`onStart`、`onStop`，然后其余）。
7. 公开方法，按职责分组。
8. 私有方法，按调用顺序。

KDoc 与注解始终贴着它们描述的声明。

## Import

- 不使用通配符 import。
- 由 `ktlint` 按字母序排序；不允许未使用的 import。
- 能用 import 表达时，不在代码里写全限定名。

## KDoc 与注释

- 使用英文。每个类型、每个成员都保留 `@author` 与 `@since`，枚举常量也一样。
- 正文说明“做什么、有什么前提、保证什么”，不要复述签名（`@param event event`、
  `@return response` 属于噪音）。
- 解释不显然的决策或约束，不解释语法。
- 注释跟着它解释的代码走；不要留下被注释掉的代码。

## 常量与字面量

- 魔法字符串和数字要命名：插件状态、HTTP header、路由前缀、分页大小、超时、缓冲区大小、
  错误码。
- 错误码与响应码使用现有枚举，不要引入裸数字。
- 每类服务的默认排序和分页默认值只在一处定义。

## 异常与日志

- 不允许静默吞异常。要么带上下文记录，要么转换，要么继续抛出。`runCatching` 不能替代错误处理。
- 不把内部异常文本返回给客户端；日志里保留细节，响应里返回稳定的消息和错误码。
- 使用具体的异常类型（`PluginVersionConflictException`、`NoRecordFoundException` 等），
  不要一律包成通用的安装失败。
- 使用 SLF4J 参数化日志（`logger.warn("... {}", value)`），不要字符串拼接。
- 不记录密钥、令牌、密码或验证码。

## 空值与类型

- 避免 `!!`。用局部 `val`、`requireNotNull` 或显式的 `?: throw`。
- DTO 边界用 `Boolean`/`Int`，写入实体时显式转换（API 暴露 `needKey: Boolean`，列里存 `0/1`）。
- 优先不可变集合和 `val`；共享状态返回快照。
- Long 主键对控制台序列化为字符串；ID 字段保留
  `@JsonSerialize(using = ToStringSerializer::class)`。

## 查询与持久化

- 过滤和分页在数据库层完成，不要在内存里做；列表接口返回 `PageVo`。
- 客户端排序走 `setPageSort`，它会校验字段名。
- 乐观锁列统一叫 `version`。
- 修改多行的服务方法加 `@Transactional`；不要通过同一个 bean 自调用 `@Transactional` 方法
  （自调用绕过代理）。
- 运行时操作可能改变行状态时，操作后重新读取该行，不要相信操作前的快照。

## 插件 API 边界

- `gateway-api` 是 Kotlin 源码，但必须对 Java 作者友好：SAM 接口、工厂方法上的 `@JvmStatic`、
  带默认参数的构造函数上的 `@JvmOverloads`；Java 作者需要 Kotlin 语义的位置改用访问函数而不是属性。
- 不把 Spring、Jackson、MyBatis、Hikari、Servlet 或 `java.io.File` 类型暴露到边界。
- 修改 `gateway-api` 的公开签名就是 ABI 变更：插件是针对精确签名编译的，应升级 API 版本或新增重载。
