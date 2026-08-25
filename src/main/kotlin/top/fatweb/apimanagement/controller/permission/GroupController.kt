package top.fatweb.apimanagement.controller.permission

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseCode
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.param.permission.group.*
import top.fatweb.apimanagement.service.permission.IGroupService
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.GroupWithRoleVo
import top.fatweb.apimanagement.vo.permission.base.GroupVo

/**
 * Group management controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IGroupService
 */
@BaseController(path = ["/system/group"], name = "用户组管理", description = "用户组管理相关接口")
class GroupController(
    val groupService: IGroupService
) {
    /**
     * Get group by ID
     *
     * @param id Group ID
     * @return Response object includes group information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see GroupWithRoleVo
     */
    @Operation(summary = "获取单个用户组")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:group:one:query')")
    fun getOne(@PathVariable id: Long): ResponseResult<GroupWithRoleVo> =
        ResponseResult.databaseSuccess(data = groupService.getOne(id))

    /**
     * Get group paging information
     *
     * @param groupGetParam Get group parameters
     * @return Response object includes group paging information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupGetParam
     * @see ResponseResult
     * @see PageVo
     * @see GroupWithRoleVo
     */
    @Operation(summary = "获取用户组")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:group:all:query')")
    fun get(@ProcessParam @Valid groupGetParam: GroupGetParam?): ResponseResult<PageVo<GroupWithRoleVo>> =
        ResponseResult.databaseSuccess(
            data = groupService.getPage(groupGetParam)
        )

    /**
     * Get group list
     *
     * @return Response object includes group list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     * @see GroupVo
     */
    @Operation(summary = "获取用户组列表")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('system:group:all:list', 'system:user:one:add', 'system:user:one:modify')")
    fun list(): ResponseResult<List<GroupVo>> =
        ResponseResult.databaseSuccess(
            data = groupService.getList()
        )

    /**
     * Add group
     *
     * @param groupAddParam Add group parameters
     * @return Response object includes group information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupAddParam
     * @see ResponseResult
     * @see GroupVo
     */
    @Operation(summary = "添加用户组")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:group:one:add')")
    fun add(@ProcessParam @Valid @RequestBody groupAddParam: GroupAddParam): ResponseResult<GroupVo> =
        ResponseResult.databaseSuccess(
            ResponseCode.DATABASE_INSERT_SUCCESS, data = groupService.add(groupAddParam)
        )

    /**
     * Update group
     *
     * @param groupUpdateParam Update group parameters
     * @return Response object includes group information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupUpdateParam
     * @see ResponseResult
     * @see GroupVo
     */
    @Operation(summary = "修改用户组")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:group:one:modify')")
    fun update(@ProcessParam @Valid @RequestBody groupUpdateParam: GroupUpdateParam): ResponseResult<Unit> {
        groupService.update(groupUpdateParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Update status of group
     *
     * @param groupUpdateStatusParam Update status of group parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupUpdateStatusParam
     * @see ResponseResult
     */
    @Operation(summary = "修改用户组状态")
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('system:group:one:status')")
    fun status(@Valid @RequestBody groupUpdateStatusParam: GroupUpdateStatusParam): ResponseResult<Unit> {
        groupService.status(groupUpdateStatusParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_UPDATE_SUCCESS)
    }

    /**
     * Delete group by ID
     *
     * @param id Group ID
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ResponseResult
     */
    @Operation(summary = "删除用户组")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:group:one:remove')")
    fun delete(@PathVariable id: Long): ResponseResult<Unit> {
        groupService.deleteOne(id)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }

    /**
     * Delete group by list
     *
     * @param groupDeleteParam Delete group parameters
     * @return Response object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see GroupDeleteParam
     * @see ResponseResult
     */
    @Operation(summary = "批量删除用户组")
    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('system:group:all:remove')")
    fun deleteList(@Valid @RequestBody groupDeleteParam: GroupDeleteParam): ResponseResult<Unit> {
        groupService.delete(groupDeleteParam)

        return ResponseResult.databaseSuccess(ResponseCode.DATABASE_DELETE_SUCCESS)
    }
}
