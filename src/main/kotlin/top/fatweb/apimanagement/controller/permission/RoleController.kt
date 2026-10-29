package top.fatweb.apimanagement.controller.permission

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.permission.role.RoleAddParam
import top.fatweb.apimanagement.param.permission.role.RoleDeleteParam
import top.fatweb.apimanagement.param.permission.role.RoleGetParam
import top.fatweb.apimanagement.param.permission.role.RoleUpdateParam
import top.fatweb.apimanagement.param.permission.role.RoleUpdateStatusParam
import top.fatweb.apimanagement.service.permission.IRoleService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.RoleWithPowerVo
import top.fatweb.apimanagement.vo.permission.base.RoleVo

/**
 * Role management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@BaseController(path = ["/system/role"], name = "角色管理", description = "角色管理相关接口")
class RoleController(private val roleService: IRoleService) {
    /**
     * Get role by ID
     *
     * @param id Role ID
     * @return Response object includes role information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取单个角色")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:role:one:query')")
    fun getOne(@PathVariable id: Long): ResponseResult<RoleWithPowerVo> =
        ResponseResult.databaseSuccess(data = roleService.getOne(id))

    /**
     * Get role paging information
     *
     * @param roleGetParam Get role parameters
     * @return Response object includes role paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取角色")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:role:all:query')")
    fun get(@Valid @ProcessParam roleGetParam: RoleGetParam?): ResponseResult<PageVo<RoleWithPowerVo>> =
        ResponseResult.databaseSuccess(
            data = roleService.getPage(roleGetParam),
        )

    /**
     * Get role list
     *
     * @return Response object includes role list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    @PreAuthorize(
        "hasAnyAuthority('system:role:all:list', 'system:group:one:add', 'system:group:one:modify', 'system:user:one:add', 'system:user:one:modify')",
    )
    fun list(): ResponseResult<List<RoleVo>> = ResponseResult.databaseSuccess(
        data = roleService.getList(),
    )

    /**
     * Add role
     *
     * @param roleAddParam Add role parameters
     * @return Response object includes role information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "添加角色")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:role:one:add')")
    fun add(@ProcessParam @Valid @RequestBody roleAddParam: RoleAddParam): ResponseResult<RoleVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.DATABASE_INSERT_SUCCESS,
            data = roleService.add(roleAddParam),
        )

    /**
     * Update role
     *
     * @param roleUpdateParam Update role parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:role:one:modify')")
    fun update(@ProcessParam @Valid @RequestBody roleUpdateParam: RoleUpdateParam): ResponseResult<Unit> {
        roleService.update(roleUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update status of role
     *
     * @param roleUpdateStatusParam Update status of role parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "修改角色状态")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:role:one:status')")
    fun status(@Valid @RequestBody roleUpdateStatusParam: RoleUpdateStatusParam): ResponseResult<Unit> {
        roleService.status(roleUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Delete role by ID
     *
     * @param id
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:role:one:remove')")
    fun delete(@PathVariable id: Long): ResponseResult<Unit> {
        roleService.deleteOne(id)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Delete role by list
     *
     * @param roleDeleteParam Delete role parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "批量删除角色")
    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('system:role:all:remove')")
    fun deleteList(@Valid @RequestBody roleDeleteParam: RoleDeleteParam): ResponseResult<Unit> {
        roleService.delete(roleDeleteParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }
}
