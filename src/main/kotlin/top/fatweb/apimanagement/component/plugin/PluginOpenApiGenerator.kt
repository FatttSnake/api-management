package top.fatweb.apimanagement.component.plugin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.lang.reflect.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.kotlinFunction
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

/**
 * Plugin OpenAPI fragment generator
 *
 * Builds the OpenAPI 3.1 fragment the user-facing documentation is rendered from, out of
 * the very classes the gateway has just mounted. It is the single source of truth for the
 * fragment: the plugin no longer ships `META-INF/plugin-openapi.json`, so a fragment can no
 * longer describe an endpoint the gateway does not serve - the paths, methods, operation
 * IDs and identity codes are the ones [top.fatweb.apimanagement.service.api.impl.ApiPluginServiceImpl.mountEndpoints] computed, and
 * only the parameter, body and response shapes are read back off the handlers with
 * reflection. Plugin-level metadata lives in `META-INF/api-plugin.json`; this only describes
 * interfaces, so the fragment carries no `info` or `servers` for the reader to merge in.
 *
 * Nothing here runs plugin code: types and annotations are read, never a getter, never an
 * instantiation. The DTO shapes are still the author's to declare - descriptions and examples
 * come from the `@Schema` a class, property or parameter already carries, and a handler that
 * returns `Map<String, Object>` yields the generic object that type describes, since the
 * property names it happens to hold are not knowable from the signature.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JsonMapper
 */
@Component
class PluginOpenApiGenerator(
    private val objectMapper: JsonMapper
) {
    companion object {
        /**
         * OpenAPI version the fragment declares
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val OPENAPI_VERSION = "3.1.2"

        /**
         * Prefix of a schema reference, completed with a name registered in [Ctx.named]
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val SCHEMA_REF_PREFIX = "#/components/schemas/"

        /**
         * Media type of a JSON request body and of every response that is not a byte stream
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val MEDIA_JSON = "application/json"

        /**
         * Media type of a form request body declared with `@RequestPart`
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val MEDIA_MULTIPART = "multipart/form-data"

        /**
         * Media type of a byte-stream response
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val MEDIA_OCTET_STREAM = "application/octet-stream"

        /**
         * The JSON Schema type standing for an absent value, used by a nullable member
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val NULL_TYPE = "null"

        /**
         * Fully qualified name of the SDK response envelope
         *
         * Matched by name rather than by class identity because a plugin loads its own copy
         * of the SDK from the parent loader while the envelope could equally arrive from the
         * gateway's; the shape, not the loader, is what the schema has to describe. It is
         * inlined rather than referenced because one `ApiResponse` name could not stand for
         * two different payloads at once.
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        private const val API_RESPONSE_FQN = "top.fatweb.apimanagement.sdk.plugin.ApiResponse"
    }

    /**
     * Build the OpenAPI fragment of a plugin's mounted endpoints
     *
     * @param endpoints Endpoints computed by [top.fatweb.apimanagement.service.api.impl.ApiPluginServiceImpl.mountEndpoints]
     * @return OpenAPI 3.1 fragment JSON, with `paths` and, when any interface has a shape to
     *         describe, `components.schemas`
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MountedEndpoint
     */
    fun generate(endpoints: List<MountedEndpoint>): String {
        val context = Ctx(
            schemas = objectMapper.createObjectNode(),
            named = LinkedHashMap(),
            usedNames = HashSet()
        )

        val paths = objectMapper.createObjectNode()
        endpoints.forEach { endpoint ->
            paths.child(endpoint.fullPath).set(endpoint.httpMethod.lowercase(), operation(endpoint, context))
        }

        val root = objectMapper.createObjectNode()
        root.put("openapi", OPENAPI_VERSION)
        root.set("paths", paths)
        if (context.named.isNotEmpty()) {
            root.putObject("components").set("schemas", context.schemas)
        }

        return objectMapper.writeValueAsString(root)
    }

    /**
     * Build one path item operation
     *
     * @param endpoint Endpoint the operation belongs to
     * @param context Generation context
     * @return Operation object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see MountedEndpoint
     * @see Ctx
     * @see ObjectNode
     */
    private fun operation(endpoint: MountedEndpoint, context: Ctx): ObjectNode {
        val method = endpoint.method
        val annotation = method.getAnnotation(Operation::class.java)
        val node = objectMapper.createObjectNode()

        // The mount refuses a mapped method with no operation ID, so the fallback only covers
        // a fragment asked for outside a mount: the code already ends in the same ID
        node.put(
            "operationId",
            annotation?.operationId?.takeIf { it.isNotBlank() } ?: endpoint.code.substringAfterLast(':'))
        (annotation?.summary?.takeIf { it.isNotBlank() } ?: endpoint.name)
            .takeIf { it.isNotBlank() }
            ?.let { node.put("summary", it) }
        annotation?.description?.takeIf { it.isNotBlank() }?.let { node.put("description", it) }
        annotation?.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
            val array = objectMapper.createArrayNode()
            tags.forEach { array.add(it) }
            node.set("tags", array)
        }
        if (annotation?.deprecated == true) {
            node.put("deprecated", true)
        }

        parameters(method, context)?.let { node.set("parameters", it) }
        requestBody(method, context)?.let { node.set("requestBody", it) }
        node.set("responses", responses(method, context))

        return node
    }

    /**
     * Build the parameters of a handler
     *
     * Only the four argument-bound parameter annotations are described; a body is not a
     * parameter in OpenAPI and is built by [requestBody] instead.
     *
     * @param method Handler method
     * @param context Generation context
     * @return Parameter array, or null when the handler declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Method
     * @see Ctx
     * @see ArrayNode
     */
    private fun parameters(method: Method, context: Ctx): ArrayNode? {
        val kotlinParameters = valueParameters(method)
        val array = objectMapper.createArrayNode()
        var count = 0

        method.parameters.forEachIndexed { index, parameter ->
            val binding = bindingOf(parameter, kotlinParameters?.getOrNull(index)?.name) ?: return@forEachIndexed
            val annotation = parameter.getAnnotation(Parameter::class.java)
            if (annotation?.hidden == true) return@forEachIndexed
            if (binding.name.isBlank()) return@forEachIndexed

            val node = objectMapper.createObjectNode()
            node.put("name", binding.name)
            node.put("in", binding.location)
            node.put("required", binding.required || annotation?.required == true)
            annotation?.description?.takeIf { it.isNotBlank() }?.let { node.put("description", it) }
            val schema = schemaOf(refOfParameter(parameter, kotlinParameters?.getOrNull(index)), context, emptyMap())
            annotation?.let { exampleOf(it) }?.let { node.set("examples", examples(it, schema)) }
            node.set("schema", schema)
            array.add(node)
            count++
        }

        return if (count > 0) array else null
    }

    /**
     * Build the request body of a handler
     *
     * A `@RequestBody` is a single JSON body; `@RequestPart`s are together one multipart body
     * whose properties are named after the parts, and an unnamed part falls back to its Kotlin
     * parameter name.
     *
     * @param method Handler method
     * @param context Generation context
     * @return Request body object, or null when the handler declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Method
     * @see Ctx
     * @see ObjectNode
     */
    private fun requestBody(method: Method, context: Ctx): ObjectNode? {
        val kotlinParameters = valueParameters(method)

        val bodyIndex = method.parameters.indexOfFirst { it.getAnnotation(RequestBody::class.java) != null }
        if (bodyIndex >= 0) {
            val parameter = method.parameters[bodyIndex]
            val annotation = parameter.getAnnotation(RequestBody::class.java)!!
            val node = objectMapper.createObjectNode()
            node.put("required", annotation.required)
            content(
                node,
                MEDIA_JSON,
                schemaOf(refOfParameter(parameter, kotlinParameters?.getOrNull(bodyIndex)), context, emptyMap())
            )

            return node
        }

        val partIndices =
            method.parameters.indices.filter { method.parameters[it].getAnnotation(RequestPart::class.java) != null }
        if (partIndices.isEmpty()) return null

        val schema = typed("object", false)
        val properties = objectMapper.createObjectNode()
        val required = objectMapper.createArrayNode()
        var requiredParts = 0
        partIndices.forEach { index ->
            val parameter = method.parameters[index]
            val annotation = parameter.getAnnotation(RequestPart::class.java)!!
            val name = annotation.name.ifBlank { annotation.value }
                .ifBlank { kotlinParameters?.getOrNull(index)?.name.orEmpty() }
            if (name.isBlank()) return@forEach

            properties.set(
                name,
                schemaOf(refOfParameter(parameter, kotlinParameters?.getOrNull(index)), context, emptyMap())
            )
            if (annotation.required) {
                required.add(name)
                requiredParts++
            }
        }
        schema.set("properties", properties)
        if (requiredParts > 0) schema.set("required", required)

        val node = objectMapper.createObjectNode()
        node.put("required", requiredParts > 0)
        content(node, MEDIA_MULTIPART, schema)

        return node
    }

    /**
     * Build the responses of a handler
     *
     * The success response is derived from the return type - unwrapping a `ResponseEntity`,
     * `HttpEntity` or `Optional` - and reads content only when that type carries one. Any
     * `@ApiResponse` the author declared for another code supplies that entry's description;
     * the success entry takes its own description from a `@ApiResponse("200")` when there is one.
     *
     * @param method Handler method
     * @param context Generation context
     * @return Responses object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see method
     * @see Ctx
     * @see ObjectNode
     */
    private fun responses(method: Method, context: Ctx): ObjectNode {
        val declared = method.getAnnotationsByType(SwaggerApiResponse::class.java)
        val node = objectMapper.createObjectNode()

        val returned = unwrap(returnRef(method))
        val success = objectMapper.createObjectNode()
        success.put(
            "description",
            declared.firstOrNull { it.responseCode == "200" }?.description?.takeIf { it.isNotBlank() } ?: "OK"
        )
        responseSchema(returned, context)?.let { content(success, mediaTypeOf(returned), it) }
        node.set("200", success)

        declared.filter { it.responseCode.isNotBlank() && it.responseCode != "200" }.forEach { response ->
            val entry = objectMapper.createObjectNode()
            entry.put("description", response.description.takeIf { it.isNotBlank() } ?: "OK")
            node.set(response.responseCode, entry)
        }

        return node
    }

    /**
     * Build the schema of a response, or read that it has no content
     *
     * @param returned Return type, already unwrapped
     * @param context Generation context
     * @return Response schema, or null when the handler returns nothing
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     * @see Ctx
     * @see ObjectNode
     */
    private fun responseSchema(returned: Ref, context: Ctx): ObjectNode? {
        val clazz = returned.clazz ?: return objectMapper.createObjectNode()
        if (clazz == Void.TYPE || clazz == Void::class.java || clazz == Unit::class.java) return null

        return schemaOf(returned, context, emptyMap())
    }

    /**
     * Build the schema of a type
     *
     * @param ref Type to describe
     * @param context Generation context
     * @param variables Type arguments in scope, by type parameter name
     * @return JSON Schema node
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     * @see Ctx
     * @see ObjectNode
     */
    private fun schemaOf(ref: Ref, context: Ctx, variables: Map<String, Ref>): ObjectNode {
        ref.variable?.let { name ->
            val mapped = variables[name] ?: return objectMapper.createObjectNode()

            return schemaOf(if (ref.nullable) mapped.copy(nullable = true) else mapped, context, variables)
        }

        val clazz = ref.clazz ?: return objectMapper.createObjectNode()
        val nullable = ref.nullable

        scalar(clazz, nullable)?.let { return it }
        if (clazz.isEnum) return enumOf(clazz, nullable)
        if (isByteArray(clazz)) return octetStream(nullable)
        // An uploaded part is bytes as well, and 3.1 spells a binary payload the same way: a
        // string carrying the media type, where 3.0's `format: binary` no longer exists
        if (clazz.name == "org.springframework.web.multipart.MultipartFile") {
            return octetStream(nullable)
        }
        if (clazz.isArray || Collection::class.java.isAssignableFrom(clazz)) {
            val item = ref.args.firstOrNull()
                ?: Ref(clazz.componentType ?: Any::class.java, false, emptyList())

            return typed("array", nullable).apply { set("items", schemaOf(item, context, variables)) }
        }
        if (Map::class.java.isAssignableFrom(clazz)) {
            val value = ref.args.getOrNull(1) ?: Ref(Any::class.java, false, emptyList())

            return typed("object", nullable).apply { set("additionalProperties", schemaOf(value, context, variables)) }
        }
        if (clazz.name == API_RESPONSE_FQN) {
            return body(clazz, ref.args, context, variables, nullable)
        }

        return reference(clazz, ref.args, nullable, context, variables)
    }

    /**
     * Build a reference to a named schema, registering its body the first time it is seen
     *
     * Registering before the body is built is what terminates a self-referential DTO: a type
     * reached again on the way down its own properties is already named, so it resolves to a
     * reference rather than recursing forever.
     *
     * @param clazz Type to register
     * @param args Type arguments the use site supplied
     * @param nullable Whether this use site admits null
     * @param context Generation context
     * @param variables Type arguments in scope, by type parameter name
     * @return Reference node, wrapped to admit null when [nullable]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     * @see Ctx
     * @see ObjectNode
     */
    private fun reference(
        clazz: Class<*>,
        args: List<Ref>,
        nullable: Boolean,
        context: Ctx,
        variables: Map<String, Ref>
    ): ObjectNode {
        val known = context.named[clazz]
        val name = known ?: context.register(clazz)
        if (known == null) {
            context.schemas.set(name, body(clazz, args, context, variables, false))
        }

        val node = objectMapper.createObjectNode()
        node.put("\$ref", SCHEMA_REF_PREFIX + name)
        if (!nullable) return node

        val wrapper = objectMapper.createObjectNode()
        val anyOf = wrapper.putArray("anyOf")
        anyOf.add(node)
        anyOf.add(typed(NULL_TYPE, false))

        return wrapper
    }

    /**
     * Build the object schema of a type
     *
     * @param clazz Type to describe
     * @param args Type arguments the use site supplied
     * @param context Generation context
     * @param variables Type arguments in scope, by type parameter name
     * @param nullable Whether this use site admits null
     * @return Object schema node
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ctx
     * @see Ref
     * @see ObjectNode
     */
    private fun body(
        clazz: Class<*>,
        args: List<Ref>,
        context: Ctx,
        variables: Map<String, Ref>,
        nullable: Boolean
    ): ObjectNode {
        val node = typed("object", nullable)
        clazz.getAnnotation(Schema::class.java)?.description?.takeIf { it.isNotBlank() }
            ?.let { node.put("description", it) }

        val properties = objectMapper.createObjectNode()
        val required = objectMapper.createArrayNode()
        var propertyCount = 0
        var requiredCount = 0
        val scoped = scopedVariables(clazz, args, variables)

        propertiesOf(clazz).forEach { (name, ref) ->
            val annotation = schemaAnnotationOf(clazz, name)
            if (annotation?.hidden == true) return@forEach

            val propertyName = annotation?.name?.takeIf { it.isNotBlank() } ?: name
            val propertyRef = if (annotation?.nullable == true) ref.copy(nullable = true) else ref
            val propertyNode = schemaOf(propertyRef, context, scoped)
            if (!propertyNode.has("\$ref")) {
                annotation?.description?.takeIf { it.isNotBlank() }?.let { propertyNode.put("description", it) }
                annotation?.let { exampleOf(it) }?.let { propertyNode.set("examples", examples(it, propertyNode)) }
            }
            properties.set(propertyName, propertyNode)
            propertyCount++
            if (!propertyRef.nullable) {
                required.add(propertyName)
                requiredCount++
            }
        }

        if (propertyCount > 0) node.set("properties", properties)
        if (requiredCount > 0) node.set("required", required)

        return node
    }

    /**
     * Read the properties a type serialises, in declaration order
     *
     * A Kotlin class is read through its primary constructor, which is both the order Jackson
     * writes and the only place nullability survives to runtime - JetBrains' `@Nullable` is a
     * class-retention annotation, so a field carries no trace of it. A type without Kotlin
     * metadata falls back to its declared instance fields, whose members read as non-null.
     *
     * @param clazz Type to read
     * @return Property name and type, in declaration order
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     */
    private fun propertiesOf(clazz: Class<*>): List<Pair<String, Ref>> {
        val fields = clazz.declaredFields.mapTo(HashSet()) { it.name }
        val constructor = runCatching { clazz.kotlin.primaryConstructor }.getOrNull()
        if (constructor != null) {
            // A constructor parameter with no field is not a property and is not serialised
            return constructor.parameters.mapNotNull { parameter ->
                val name = parameter.name ?: return@mapNotNull null
                if (name !in fields) return@mapNotNull null

                name to refOf(parameter.type)
            }
        }

        return clazz.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .map { it.name to refOfJava(it.genericType) }
    }

    /**
     * Read the property type arguments a use site supplies
     *
     * @param clazz Type being described
     * @param args Type arguments the use site supplied
     * @param variables Type arguments already in scope
     * @return Type arguments, by type parameter name
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     */
    private fun scopedVariables(clazz: Class<*>, args: List<Ref>, variables: Map<String, Ref>): Map<String, Ref> {
        if (args.isEmpty()) return variables

        val scoped = LinkedHashMap(variables)
        clazz.typeParameters.forEachIndexed { index, parameter ->
            args.getOrNull(index)?.let { scoped[parameter.name] = it }
        }

        return scoped
    }

    /**
     * Read the `@Schema` a property carries
     *
     * Looked for on the backing field, then the getter, then the Kotlin property, because the
     * use-site target a plugin's Kotlin version picks is not something this can assume.
     *
     * @param clazz Type the property belongs to
     * @param name Property name
     * @return Annotation, or null when the property declares none
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Schema
     */
    private fun schemaAnnotationOf(clazz: Class<*>, name: String): Schema? {
        runCatching { clazz.getDeclaredField(name) }.getOrNull()
            ?.getAnnotation(Schema::class.java)?.let { return it }
        runCatching { clazz.getMethod("get" + name.replaceFirstChar { it.uppercaseChar() }) }.getOrNull()
            ?.getAnnotation(Schema::class.java)?.let { return it }
        runCatching { clazz.kotlin.declaredMemberProperties.firstOrNull { it.name == name } }
            .getOrNull()?.annotations?.filterIsInstance<Schema>()?.firstOrNull()?.let { return it }

        return null
    }

    /**
     * Read the parameters a handler binds from the request
     *
     * @param parameter Handle parameter
     * @param kotlinName Kotlin name of the parameter, or null for a Java handler
     * @return Binding, or null when the parameter is a body rather than a bound parameter
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see java.lang.reflect.Parameter
     * @see Binding
     */
    private fun bindingOf(parameter: java.lang.reflect.Parameter, kotlinName: String?): Binding? {
        parameter.getAnnotation(PathVariable::class.java)?.let {
            return Binding("path", it.name.ifBlank { it.value }.ifBlank { kotlinName.orEmpty() }, true)
        }
        parameter.getAnnotation(RequestParam::class.java)?.let {
            return Binding("query", it.name.ifBlank { it.value }.ifBlank { kotlinName.orEmpty() }, it.required)
        }
        parameter.getAnnotation(RequestHeader::class.java)?.let {
            return Binding("header", it.name.ifBlank { it.value }.ifBlank { kotlinName.orEmpty() }, it.required)
        }
        parameter.getAnnotation(CookieValue::class.java)?.let {
            return Binding("cookie", it.name.ifBlank { it.value }.ifBlank { kotlinName.orEmpty() }, it.required)
        }

        return null
    }

    /**
     * Read a handler's Kotlin value parameters
     *
     * The instance a member function is called on is a parameter too, and is dropped so the
     * list lines up with the Java parameters by index.
     *
     * @param method Handler method
     * @return Value parameters, or null for a Java handler
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Method
     * @see KParameter
     */
    private fun valueParameters(method: Method): List<KParameter>? =
        runCatching { method.kotlinFunction?.parameters?.filter { it.kind == KParameter.Kind.VALUE } }.getOrNull()

    /**
     * Read a method parameter's type
     *
     * @param parameter Method parameter
     * @param kotlinParameter Kotlin parameter, or null for a Java handler
     * @return Parameter type
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see java.lang.reflect.Parameter
     * @see KParameter
     * @see Ref
     */
    private fun refOfParameter(parameter: java.lang.reflect.Parameter, kotlinParameter: KParameter?): Ref =
        kotlinParameter?.let { refOf(it.type) } ?: refOfJava(parameter.parameterizedType)

    /**
     * Read a handler's return type
     *
     * @param method Handler method
     * @return Return type
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Method
     * @see Ref
     */
    private fun returnRef(method: Method): Ref =
        runCatching { method.kotlinFunction }.getOrNull()?.let { refOf(it.returnType) }
            ?: refOfJava(method.genericReturnType)

    /**
     * Unwrap a response envelope down to the type that is actually serialised
     *
     * @param ref Return type
     * @return The wrapped type, or the type itself when it is not an envelope
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     */
    private fun unwrap(ref: Ref): Ref {
        val name = ref.clazz?.name ?: return ref
        if (name != "org.springframework.http.ResponseEntity" &&
            name != "org.springframework.http.HttpEntity" &&
            name != "java.util.Optional"
        ) {
            return ref
        }

        return ref.args.firstOrNull()?.let { unwrap(it) } ?: ref
    }

    /**
     * Read the media type a response is written with
     *
     * @param returned Return type, already unwrapped
     * @return Media type
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Ref
     */
    private fun mediaTypeOf(returned: Ref): String =
        if (returned.clazz?.let { isByteArray(it) } == true) MEDIA_OCTET_STREAM else MEDIA_JSON

    /**
     * Read whether a type is a byte stream
     *
     * Both the primitive and the boxed array are recognised, since a Kotlin `ByteArray` and a
     * Java `Byte[]` are different classes that describe the same stream.
     *
     * @param clazz Type to read
     * @return true=the type is a byte array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun isByteArray(clazz: Class<*>): Boolean =
        clazz == ByteArray::class.java ||
                (clazz.isArray && (clazz.componentType == Byte::class.java || clazz.componentType == Byte::class.javaObjectType))

    /**
     * Build a scalar schema for a known type
     *
     * @param clazz Type to describe
     * @param nullable Whether the use site admits null
     * @return Schema, or null when the type is not one of the known scalars
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     */
    private fun scalar(clazz: Class<*>, nullable: Boolean): ObjectNode? = when (clazz) {
        String::class.java, CharSequence::class.java, Char::class.java, Char::class.javaObjectType -> typed(
            "string",
            nullable
        )

        Int::class.java, Int::class.javaObjectType -> typed("integer", nullable).apply { put("format", "int32") }
        Long::class.java, Long::class.javaObjectType -> typed("integer", nullable).apply { put("format", "int64") }
        Short::class.java, Short::class.javaObjectType,
        Byte::class.java, Byte::class.javaObjectType,
        BigInteger::class.java -> typed("integer", nullable)

        Float::class.java, Float::class.javaObjectType -> typed("number", nullable).apply { put("format", "float") }
        Double::class.java, Double::class.javaObjectType -> typed("number", nullable).apply { put("format", "double") }
        BigDecimal::class.java -> typed("number", nullable)
        Boolean::class.java, Boolean::class.javaObjectType -> typed("boolean", nullable)
        UUID::class.java -> typed("string", nullable).apply { put("format", "uuid") }
        LocalDate::class.java -> typed("string", nullable).apply { put("format", "date") }
        LocalTime::class.java, OffsetTime::class.java -> typed("string", nullable).apply { put("format", "time") }
        LocalDateTime::class.java, Instant::class.java, OffsetDateTime::class.java,
        ZonedDateTime::class.java, Date::class.java -> typed("string", nullable).apply { put("format", "date-time") }

        else -> null
    }

    /**
     * Build the schema of an enum type
     *
     * @param clazz Enum type
     * @param nullable Whether the use site admits null
     * @return Schema
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun enumOf(clazz: Class<*>, nullable: Boolean): ObjectNode {
        val values = objectMapper.createArrayNode()
        clazz.enumConstants.filterIsInstance<Enum<*>>().forEach { values.add(it.name) }

        return typed("string", nullable).apply { set("enum", values) }
    }

    /**
     * Build the schema of a byte stream
     *
     * @param nullable Whether the use site admits null
     * @return Schema
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     */
    private fun octetStream(nullable: Boolean): ObjectNode =
        typed("string", nullable).apply { put("contentMediaType", MEDIA_OCTET_STREAM) }

    /**
     * Build a node carrying a JSON Schema type
     *
     * A nullable member is a type union rather than a flag, which is how OpenAPI 3.1 spells
     * null.
     *
     * @param type JSON Schema type
     * @param nullable Whether the use site admits null
     * @return Schema node
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     */
    private fun typed(type: String, nullable: Boolean): ObjectNode {
        val node = objectMapper.createObjectNode()
        if (nullable) {
            val union = node.putArray("type")
            union.add(type)
            union.add(NULL_TYPE)
        } else {
            node.put("type", type)
        }

        return node
    }

    /**
     * Attach a content map to an operation or response
     *
     * @param node Object the content belongs to
     * @param mediaType Media type of the content
     * @param schema Content schema
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     */
    private fun content(node: ObjectNode, mediaType: String, schema: ObjectNode) {
        node.putObject("content").putObject(mediaType).set("schema", schema)
    }

    /**
     * Read the example a `@Parameter` declares
     *
     * @param annotation Parameter annotation
     * @return Example, or null when none is declared
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Parameter
     */
    private fun exampleOf(annotation: Parameter): String? =
        annotation.example.takeIf { it.isNotBlank() }
            ?: annotation.examples.firstOrNull { it.value.isNotBlank() }?.value

    /**
     * Read the example a `@Schema` declares
     *
     * @param annotation Schema annotation
     * @return Example, or null when none is declared
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Schema
     */
    private fun exampleOf(annotation: Schema): String? =
        annotation.example.takeIf { it.isNotBlank() } ?: annotation.examples.firstOrNull { it.isNotBlank() }

    /**
     * Wrap an example as the array OpenAPI 3.1 declares it in
     *
     * Swagger declares an example as a string whatever the schema's type, so the string is
     * coerced to the type the schema describes: an example a reader shows as `20480` is worth
     * more than one it shows as `"20480"`, and a string value against an integer schema is a
     * mismatch a validator would flag. A value that will not parse is carried as it was
     * declared rather than dropped.
     *
     * @param example Example value, as the annotation declares it
     * @param schema Schema the example belongs to
     * @return Example array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     * @see ArrayNode
     */
    private fun examples(example: String, schema: ObjectNode): ArrayNode {
        val array = objectMapper.createArrayNode()
        val types = schema.get("type")
            ?.let { if (it.isArray) it.values().map { name -> name.asString() } else listOf(it.asString()) }
            .orEmpty()

        when {
            "integer" in types -> example.toLongOrNull()?.let { array.add(it) } ?: array.add(example)
            "number" in types -> example.toBigDecimalOrNull()?.let { array.add(it) } ?: array.add(example)
            "boolean" in types -> example.toBooleanStrictOrNull()?.let { array.add(it) } ?: array.add(example)
            else -> array.add(example)
        }

        return array
    }

    /**
     * Read a type into a describable shape
     *
     * @param type Kotlin type
     * @return Type
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see KType
     * @see Ref
     */
    private fun refOf(type: KType): Ref {
        val args = type.arguments.map { argument ->
            argument.type?.let { refOf(it) } ?: Ref(Any::class.java, false, emptyList())
        }

        return when (val classifier = type.classifier) {
            is KClass<*> -> Ref(classifier.java, type.isMarkedNullable, args)
            is KTypeParameter -> Ref(null, type.isMarkedNullable, emptyList(), classifier.name)
            else -> Ref(null, type.isMarkedNullable, emptyList())
        }
    }

    /**
     * Read a Java type into a describable shape
     *
     * Only reached for a handler or DTO without Kotlin metadata, where no nullability is
     * recoverable and every member reads as non-null.
     *
     * @param type Java type
     * @return Type
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Type
     * @see Ref
     */
    private fun refOfJava(type: Type): Ref = when (type) {
        is Class<*> -> Ref(type, false, emptyList())
        is ParameterizedType -> Ref(type.rawType as? Class<*>, false, type.actualTypeArguments.map { refOfJava(it) })
        is TypeVariable<*> -> Ref(null, false, emptyList(), type.name)
        is GenericArrayType -> Ref(null, false, listOf(refOfJava(type.genericComponentType)))
        is WildcardType -> type.upperBounds.firstOrNull()?.let { refOfJava(it) } ?: Ref(
            Any::class.java,
            false,
            emptyList()
        )

        else -> Ref(Any::class.java, false, emptyList())
    }

    /**
     * Read the object node of a key, creating it when absent
     *
     * @param name Key
     * @return Object node under the key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     */
    private fun ObjectNode.child(name: String): ObjectNode =
        (get(name) as? ObjectNode) ?: putObject(name)

    /**
     * A type reduced to what a schema can be built from
     *
     * @param clazz Type, or null when it is a type variable
     * @param nullable Whether the use site admits null
     * @param args Type arguments the use site supplied
     * @param variable Name of the type parameter, when this is one
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private data class Ref(
        val clazz: Class<*>?,
        val nullable: Boolean,
        val args: List<Ref>,
        val variable: String? = null
    )

    /**
     * A bound request parameter
     *
     * @param location Where OpenAPI reads it from, e.g. `path`
     * @param name Parameter name
     * @param required Whether the request must carry it
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private data class Binding(val location: String, val name: String, val required: Boolean)

    /**
     * State of a single generation run
     *
     * Held per call rather than on the generator, which is shared and reached concurrently by
     * remounts.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ObjectNode
     */
    private class Ctx(
        val schemas: ObjectNode,
        val named: MutableMap<Class<*>, String>,
        val usedNames: MutableSet<String>
    ) {
        /**
         * Name a type and reserve its schema
         *
         * The name is registered before the body is built so a self-referential type resolves
         * to a reference instead of recursing.
         *
         * @param clazz Type to register
         * @return Name the type is referenced by
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         */
        fun register(clazz: Class<*>): String {
            var name = clazz.simpleName.ifBlank { clazz.name.substringAfterLast('.') }
            if (!usedNames.add(name)) {
                var suffix = 2
                while (!usedNames.add("${name}_$suffix")) suffix++
                name = "${name}_$suffix"
            }
            named[clazz] = name
            schemas.putObject(name)

            return name
        }
    }
}
