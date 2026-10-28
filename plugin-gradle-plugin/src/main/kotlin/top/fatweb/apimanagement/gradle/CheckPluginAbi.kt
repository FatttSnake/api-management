package top.fatweb.apimanagement.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.jar.JarFile

/**
 * Checks plugin bytecode against the gateway API ABI.
 *
 * Every reference to `top.fatweb.apimanagement.api.*` must resolve to a class, method, field
 * or constructor in `META-INF/gateway-api.abi.json`. This catches a plugin compiled against an
 * API surface the gateway does not have before the jar is signed or deployed.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
abstract class CheckPluginAbi : DefaultTask() {
    /**
     * Plugin class output directories.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:InputFiles
    @get:Classpath
    abstract val classesDirs: ConfigurableFileCollection

    /**
     * Gateway API jar carrying the ABI manifest.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @get:InputFiles
    @get:Classpath
    abstract val abiJar: ConfigurableFileCollection

    @TaskAction
    fun run() {
        val jar = abiJar.files.singleOrNull { it.isFile }
            ?: throw GradleException("Cannot locate the gateway API jar among ${abiJar.files}")
        val abiText = JarFile(jar).use { file ->
            val entry = file.getJarEntry(ABI_ENTRY)
                ?: throw GradleException("${jar.name} does not carry $ABI_ENTRY")
            file.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
        }
        val abi = jacksonObjectMapper().readValue<AbiFile>(abiText)
        val classes = abi.classes.associateBy { it.name }
        val errors = mutableListOf<String>()

        classesDirs.files
            .asSequence()
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") } }
            .sortedBy { it.path }
            .forEach { classFile ->
                val relative = classFile.path
                val refs = ClassRefs.read(classFile.readBytes())
                refs.classes
                    .filter { it.startsWith(API_PREFIX) }
                    .forEach { owner ->
                        if (owner !in classes) {
                            errors += "$relative references missing API class $owner"
                        }
                    }
                refs.methods
                    .filter { it.owner.startsWith(API_PREFIX) }
                    .forEach { ref ->
                        val owner = classes[ref.owner]
                        val signature = "${ref.name}|${ref.descriptor}"
                        if (owner != null && signature !in owner.methodSignatures) {
                            errors += "$relative references missing API method ${ref.owner}.${ref.name}${ref.descriptor}"
                        }
                    }
                refs.fields
                    .filter { it.owner.startsWith(API_PREFIX) }
                    .forEach { ref ->
                        val owner = classes[ref.owner]
                        val signature = "${ref.name}|${ref.descriptor}"
                        if (owner != null && signature !in owner.fieldSignatures) {
                            errors += "$relative references missing API field ${ref.owner}.${ref.name}:${ref.descriptor}"
                        }
                    }
            }

        if (errors.isNotEmpty()) {
            throw GradleException(
                "Plugin does not match the gateway API ABI:\n" +
                    errors.take(25).joinToString("\n") { "  $it" } +
                    if (errors.size > 25) "\n  ... and ${errors.size - 25} more" else ""
            )
        }
        logger.lifecycle("Plugin API ABI is compatible with {}", jar.name)
    }

    private data class AbiFile(val classes: List<AbiClass> = emptyList())

    private data class AbiClass(
        val name: String = "",
        val constructors: List<String> = emptyList(),
        val methods: List<AbiMember> = emptyList(),
        val fields: List<AbiMember> = emptyList()
    ) {
        val methodSignatures: Set<String> = buildSet {
            constructors.forEach { add("<init>|$it") }
            this@AbiClass.methods.forEach { add("${it.name}|${it.descriptor}") }
        }
        val fieldSignatures: Set<String> = this@AbiClass.fields
            .map { "${it.name}|${it.descriptor}" }
            .toSet()
    }

    private data class AbiMember(val name: String = "", val descriptor: String = "")

    private data class MemberRef(
        val owner: String,
        val name: String,
        val descriptor: String
    )

    private class ClassRefs(
        val classes: Set<String>,
        val methods: List<MemberRef>,
        val fields: List<MemberRef>
    ) {
        companion object {
            fun read(bytes: ByteArray): ClassRefs {
                val input = DataInputStream(ByteArrayInputStream(bytes))
                val magic = input.readInt()
                require(magic == 0xCAFEBABE.toInt()) { "Not a class file" }
                input.readUnsignedShort()
                input.readUnsignedShort()
                val count = input.readUnsignedShort()
                val utf8 = arrayOfNulls<String>(count)
                val classNames = IntArray(count) { -1 }
                val nameAndTypeNames = IntArray(count) { -1 }
                val nameAndTypeDescriptors = IntArray(count) { -1 }
                val memberKinds = IntArray(count) { -1 }
                val memberClasses = IntArray(count) { -1 }
                val memberNameAndTypes = IntArray(count) { -1 }

                var index = 1
                while (index < count) {
                    when (val tag = input.readUnsignedByte()) {
                        1 -> {
                            val length = input.readUnsignedShort()
                            val value = ByteArray(length)
                            input.readFully(value)
                            utf8[index] = value.toString(Charsets.UTF_8)
                        }

                        3, 4 -> input.skipBytes(4)
                        5, 6 -> {
                            input.skipBytes(8)
                            index++
                        }

                        7 -> classNames[index] = input.readUnsignedShort()
                        8 -> input.skipBytes(2)
                        9, 10, 11 -> {
                            memberKinds[index] = tag
                            memberClasses[index] = input.readUnsignedShort()
                            memberNameAndTypes[index] = input.readUnsignedShort()
                        }

                        12 -> {
                            nameAndTypeNames[index] = input.readUnsignedShort()
                            nameAndTypeDescriptors[index] = input.readUnsignedShort()
                        }

                        15 -> input.skipBytes(3)
                        16 -> input.skipBytes(2)
                        17, 18 -> input.skipBytes(4)
                        19, 20 -> input.skipBytes(2)
                        else -> error("Unknown constant pool tag $tag")
                    }
                    index++
                }

                val classes = classNames
                    .filter { it >= 0 }
                    .mapNotNull { utf8[it]?.replace('/', '.') }
                    .filter { it.startsWith(API_PREFIX) }
                    .toSet()

                val methods = mutableListOf<MemberRef>()
                val fields = mutableListOf<MemberRef>()
                memberKinds.forEachIndexed { memberIndex, kind ->
                    if (kind < 0) {
                        return@forEachIndexed
                    }
                    val owner = utf8[memberClasses[memberIndex]]?.replace('/', '.') ?: return@forEachIndexed
                    if (!owner.startsWith(API_PREFIX)) {
                        return@forEachIndexed
                    }
                    val name = utf8[nameAndTypeNames[memberNameAndTypes[memberIndex]]] ?: return@forEachIndexed
                    val descriptor =
                        utf8[nameAndTypeDescriptors[memberNameAndTypes[memberIndex]]] ?: return@forEachIndexed
                    when (kind) {
                        9 -> fields += MemberRef(owner, name, descriptor)
                        else -> methods += MemberRef(owner, name, descriptor)
                    }
                }

                return ClassRefs(classes, methods, fields)
            }
        }
    }

    private companion object {
        const val ABI_ENTRY = "META-INF/gateway-api.abi.json"
        const val API_PREFIX = "top.fatweb.apimanagement.api."
    }
}
