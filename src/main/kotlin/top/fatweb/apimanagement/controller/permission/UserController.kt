package top.fatweb.apimanagement.controller.permission

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.permission.user.*
import top.fatweb.apimanagement.service.permission.IUserService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.UserWithInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithPowerInfoVo
import top.fatweb.apimanagement.vo.permission.UserWithRoleInfoVo

/**
 * User management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IUserService
 */
@BaseController(path = ["/system/user"], name = "用户管理", description = "用户管理相关接口")
class UserController(
    private val userService: IUserService
) {
    @Operation(summary = "生成头像")
    @GetMapping("/avatar/generate")
    fun generateAvatar(@Valid avatarGenerateParam: AvatarGenerateParam?): ResponseResult<String> =
        ResponseResult.success(data = userService.generateAvatar(avatarGenerateParam))

    /**
     * Get current user information
     *
     * @return Response object includes user information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see UserWithPowerInfoVo
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    fun getInfo(): ResponseResult<UserWithPowerInfoVo> =
        ResponseResult.databaseSuccess(data = userService.getInfo())

    /**
     * Get basic user information
     *
     * @param username Username
     * @return Response object includes user basic information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see UserWithInfoVo
     */
    @Operation(summary = "获取指定用户基本信息")
    @GetMapping("/info/{username}")
    fun getBasicInfo(@ProcessParam @ParamProcessor @PathVariable username: String): ResponseResult<UserWithInfoVo> =
        ResponseResult.databaseSuccess(data = userService.getBasicInfo(username))

    /**
     * Get basic user information by ID
     *
     * @param id User ID
     * @return Response object includes user basic information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see UserWithInfoVo
     */
    @Operation(summary = "获取指定用户基本信息")
    @GetMapping("/infoById/{id}")
    fun getBasicInfoById(@PathVariable id: Long): ResponseResult<UserWithInfoVo> =
        ResponseResult.databaseSuccess(data = userService.getBasicInfoById(id))

    /**
     * Update current user information
     *
     * @param userInfoUpdateParam Update user information parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserInfoUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "更新当前用户信息")
    @PatchMapping("/info")
    fun updateInfo(@ProcessParam @RequestBody @Valid userInfoUpdateParam: UserInfoUpdateParam): ResponseResult<Unit> {
        userService.updateInfo(userInfoUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Change password
     *
     * @param userChangePasswordParam User change password parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserChangePasswordParam
     * @see ResponseResult
     */
    @Operation(summary = "更改密码")
    @PostMapping("/info")
    fun password(@RequestBody @Valid userChangePasswordParam: UserChangePasswordParam): ResponseResult<Unit> {
        userService.password(userChangePasswordParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return Response object includes user information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see UserWithRoleInfoVo
     */
    @Operation(summary = "获取单个用户")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user:one:query')")
    fun getOne(@PathVariable id: Long): ResponseResult<UserWithRoleInfoVo> =
        ResponseResult.databaseSuccess(data = userService.getOne(id))

    /**
     * Get user paging information
     *
     * @param userGetParam Get user parameters
     * @return Response object includes user paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserGetParam
     * @see ResponseResult
     * @see PageVo
     * @see UserWithRoleInfoVo
     */
    @Operation(summary = "获取用户")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:user:all:query')")
    fun get(@ProcessParam @Valid userGetParam: UserGetParam?): ResponseResult<PageVo<UserWithRoleInfoVo>> =
        ResponseResult.databaseSuccess(
            data = userService.getPage(userGetParam)
        )

    /**
     * Add user
     *
     * @param userAddParam Add user parameters
     * @return Response object includes user information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserAddParam
     * @see ResponseResult
     * @see UserWithRoleInfoVo
     */
    @Operation(summary = "添加用户")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:user:one:add')")
    fun add(@ProcessParam @Valid @RequestBody userAddParam: UserAddParam): ResponseResult<UserWithRoleInfoVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.DATABASE_INSERT_SUCCESS, data = userService.add(userAddParam)
        )

    /**
     * Update user
     *
     * @param userUpdateParam Update user parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserUpdateParam
     * @see ResponseResult
     */
    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:user:one:modify')")
    fun update(@ProcessParam @Valid @RequestBody userUpdateParam: UserUpdateParam): ResponseResult<Unit> {
        userService.update(userUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update user password
     *
     * @param userUpdatePasswordParam Update user password parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserUpdatePasswordParam
     * @see ResponseResult
     */
    @Operation(summary = "修改密码")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:user:one:password')")
    fun password(@Valid @RequestBody userUpdatePasswordParam: UserUpdatePasswordParam): ResponseResult<Unit> {
        userService.password(userUpdatePasswordParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Delete user by ID
     *
     * @param id User ID
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user:one:remove')")
    fun delete(@PathVariable id: Long): ResponseResult<Unit> {
        userService.deleteOne(id)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Delete user by list
     *
     * @param userDeleteParam Delete user parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see UserDeleteParam
     * @see ResponseResult
     */
    @Operation(summary = "批量删除用户")
    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('system:user:all:remove')")
    fun deleteList(@Valid @RequestBody userDeleteParam: UserDeleteParam): ResponseResult<Unit> {
        userService.delete(userDeleteParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }
}
