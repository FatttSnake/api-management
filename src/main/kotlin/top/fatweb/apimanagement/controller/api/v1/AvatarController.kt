package top.fatweb.apimanagement.controller.api.v1

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import top.fatweb.apimanagement.annotation.ApiController
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.api.v1.avatar.AvatarBaseParam
import top.fatweb.apimanagement.param.api.v1.avatar.AvatarGitHubParam
import top.fatweb.apimanagement.service.api.v1.IAvatarService
import top.fatweb.apimanagement.vo.api.v1.avatar.AvatarBase64Vo

/**
 * Avatar controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IAvatarService
 */
@ApiController(
    plugin = "avatar",
    pluginName = "随机头像",
    version = 1,
    value = "avatarControllerV1",
    description = "随机头像相关接口"
)
class AvatarController(
    private val avatarService: IAvatarService
) {
    /**
     * Get random avatar
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Avatar byte array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ByteArray
     */
    @Operation(summary = "获取随机头像", operationId = "getRandom")
    @GetMapping(produces = [MediaType.IMAGE_PNG_VALUE])
    fun getRandom(@Valid avatarBaseParam: AvatarBaseParam?): ByteArray =
        avatarService.random(avatarBaseParam)

    /**
     * Get random avatar as base64
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Response object includes avatar base64 string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ResponseResult
     * @see AvatarBase64Vo
     */
    @Operation(summary = "获取随机头像 Base64", operationId = "getRandomBase64")
    @GetMapping("/base64")
    fun getRandomBase64(
        @Valid avatarBaseParam: AvatarBaseParam?
    ): ResponseResult<AvatarBase64Vo> =
        ResponseResult.success(
            ResponseCode.API_AVATAR_SUCCESS, data = avatarService.randomBase64(avatarBaseParam)
        )

    /**
     * Get triangle avatar
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Avatar byte array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ByteArray
     */
    @Operation(summary = "三角形头像", operationId = "triangle")
    @GetMapping("/triangle", produces = [MediaType.IMAGE_PNG_VALUE])
    fun triangle(@Valid avatarBaseParam: AvatarBaseParam?): ByteArray =
        avatarService.triangle(avatarBaseParam)

    /**
     * Get triangle avatar as base64
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Response object includes avatar base64 string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ResponseResult
     * @see AvatarBase64Vo
     */
    @Operation(summary = "三角形头像 Base64", operationId = "triangleBase64")
    @GetMapping("/triangle/base64")
    fun triangleBase64(
        @Valid avatarBaseParam: AvatarBaseParam?
    ): ResponseResult<AvatarBase64Vo> =
        ResponseResult.success(
            ResponseCode.API_AVATAR_SUCCESS,
            data = avatarService.triangleBase64(avatarBaseParam)
        )

    /**
     * Get square avatar
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Avatar byte array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ByteArray
     */
    @Operation(summary = "正方形头像", operationId = "square")
    @GetMapping("/square", produces = [MediaType.IMAGE_PNG_VALUE])
    fun square(@Valid avatarBaseParam: AvatarBaseParam?): ByteArray =
        avatarService.square(avatarBaseParam)

    /**
     * Get square avatar as base64
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Response object includes avatar base64 string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ResponseResult
     * @see AvatarBase64Vo
     */
    @Operation(summary = "正方形头像 Base64", operationId = "squareBase64")
    @GetMapping("/square/base64")
    fun squareBase64(
        @Valid avatarBaseParam: AvatarBaseParam?
    ): ResponseResult<AvatarBase64Vo> =
        ResponseResult.success(
            ResponseCode.API_AVATAR_SUCCESS,
            data = avatarService.squareBase64(avatarBaseParam)
        )

    /**
     * Get identicon avatar
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Avatar byte array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ByteArray
     */
    @Operation(summary = "Identicon 头像", operationId = "identicon")
    @GetMapping("/identicon", produces = [MediaType.IMAGE_PNG_VALUE])
    fun identicon(@Valid avatarBaseParam: AvatarBaseParam?): ByteArray =
        avatarService.identicon(avatarBaseParam)

    /**
     * Get identicon avatar as base64
     *
     * @param avatarBaseParam Avatar base parameters
     * @return Response object includes avatar base64 string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarBaseParam
     * @see ResponseResult
     * @see AvatarBase64Vo
     */
    @Operation(summary = "Identicon 头像 Base64", operationId = "identiconBase64")
    @GetMapping("/identicon/base64")
    fun identiconBase64(
        @Valid avatarBaseParam: AvatarBaseParam?
    ): ResponseResult<AvatarBase64Vo> =
        ResponseResult.success(
            ResponseCode.API_AVATAR_SUCCESS,
            data = avatarService.identiconBase64(avatarBaseParam)
        )

    /**
     * Get GitHub avatar
     *
     * @param avatarGitHubParam Avatar base parameters
     * @return Avatar byte array
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarGitHubParam
     * @see ByteArray
     */
    @Operation(summary = "GitHub 头像", operationId = "github")
    @GetMapping("/github", produces = [MediaType.IMAGE_PNG_VALUE])
    fun github(@Valid avatarGitHubParam: AvatarGitHubParam?): ByteArray =
        avatarService.github(avatarGitHubParam)

    /**
     * Get GitHub avatar as base64
     *
     * @param avatarGitHubParam Avatar base parameters
     * @return Response object includes avatar base64 string
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see AvatarGitHubParam
     * @see ResponseResult
     * @see AvatarBase64Vo
     */
    @Operation(summary = "GitHub 头像 Base64", operationId = "githubBase64")
    @GetMapping("/github/base64")
    fun githubBase64(
        @Valid avatarGitHubParam: AvatarGitHubParam?
    ): ResponseResult<AvatarBase64Vo> =
        ResponseResult.success(
            ResponseCode.API_AVATAR_SUCCESS,
            data = avatarService.githubBase64(avatarGitHubParam)
        )
}
