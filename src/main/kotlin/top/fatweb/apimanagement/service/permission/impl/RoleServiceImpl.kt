package top.fatweb.apimanagement.service.permission.impl

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.component.storage.RedisProvider
import top.fatweb.apimanagement.converter.permission.toEntity
import top.fatweb.apimanagement.converter.permission.toVo
import top.fatweb.apimanagement.converter.permission.toVoWithPower
import top.fatweb.apimanagement.entity.permission.RPowerRole
import top.fatweb.apimanagement.entity.permission.Role
import top.fatweb.apimanagement.mapper.permission.RoleMapper
import top.fatweb.apimanagement.param.permission.role.*
import top.fatweb.apimanagement.properties.ServerProperties
import top.fatweb.apimanagement.service.permission.*
import top.fatweb.apimanagement.util.*
import top.fatweb.apimanagement.vo.PageVo
import top.fatweb.apimanagement.vo.permission.RoleWithPowerVo
import top.fatweb.apimanagement.vo.permission.base.RoleVo

/**
 * Role service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServerProperties
 * @see RedisProvider
 * @see IRPowerRoleService
 * @see IScopeService
 * @see IMenuService
 * @see IUserService
 * @see ServiceImpl
 * @see RoleMapper
 * @see Role
 * @see IRoleService
 */
@Service
class RoleServiceImpl(
    private val serverProperties: ServerProperties,
    private val redisProvider: RedisProvider,
    private val rPowerRoleService: IRPowerRoleService,
    private val menuService: IMenuService,
    private val userService: IUserService
) : ServiceImpl<RoleMapper, Role>(), IRoleService {
    override fun getPage(roleGetParam: RoleGetParam?): PageVo<RoleWithPowerVo> {
        val roleIdsPage = Page<Long>(roleGetParam?.currentPage ?: 1, roleGetParam?.pageSize ?: 20)

        setPageSort(roleGetParam, roleIdsPage)

        val roleIdsIPage =
            baseMapper.selectPage(roleIdsPage, roleGetParam?.searchName, roleGetParam?.searchRegex ?: false)

        val rolePage = Page<Role>(roleIdsPage.current, roleIdsIPage.size, roleIdsIPage.total)
        if (roleIdsIPage.total > 0) {
            rolePage.records = baseMapper.selectListWithPowerByIds(roleIdsIPage.records)
        }


        return rolePage.toVoWithPower()
    }

    override fun getOne(id: Long): RoleWithPowerVo =
        queryOrThrowException { baseMapper.selectOneById(id) }.let(Role::toVoWithPower)

    override fun getList(): List<RoleVo> = list().map(Role::toVo)

    @Transactional
    override fun add(roleAddParam: RoleAddParam): RoleVo {
        val fullPowerIds = roleAddParam.powerIds?.let(::getFullPowerIds)

        val role = roleAddParam.toEntity()
        saveOrThrowException { save(role) }

        if (fullPowerIds.isNullOrEmpty()) {
            return role.toVo()
        }

        saveOrThrowException {
            rPowerRoleService.saveBatch(fullPowerIds.map {
                RPowerRole().apply {
                    roleId = role.id
                    powerId = it
                }
            })
        }
        return role.toVo()
    }

    @Transactional
    override fun update(roleUpdateParam: RoleUpdateParam) {
        val fullPowerIds = roleUpdateParam.powerIds?.let(::getFullPowerIds)

        val role = roleUpdateParam.toEntity()

        updateOrThrowException { updateById(role) }

        val oldPowerList = rPowerRoleService.list(
            KtQueryWrapper(RPowerRole()).select(RPowerRole::powerId).eq(RPowerRole::roleId, roleUpdateParam.id)
        ).map(RPowerRole::powerId)
        val addPowerIds = HashSet<Long>()
        val removePowerIds = HashSet<Long>()
        fullPowerIds?.forEach(addPowerIds::add)
        oldPowerList.forEach {
            it?.let(removePowerIds::add)
        }
        removePowerIds.removeAll(addPowerIds)
        oldPowerList.toSet().let(addPowerIds::removeAll)

        removePowerIds.forEach {
            rPowerRoleService.remove(
                KtQueryWrapper(RPowerRole()).eq(
                    RPowerRole::roleId, roleUpdateParam.id
                ).eq(RPowerRole::powerId, it)
            )
        }

        addPowerIds.forEach {
            saveOrThrowException {
                rPowerRoleService.save(RPowerRole().apply {
                    roleId = roleUpdateParam.id
                    powerId = it
                })
            }
        }

        roleUpdateParam.id?.let { offlineUser(it) }
    }

    override fun status(roleUpdateStatusParam: RoleUpdateStatusParam) {
        updateById(roleUpdateStatusParam.toEntity()).let {
            updateOrThrowException { it }

            if (!roleUpdateStatusParam.enable) {
                roleUpdateStatusParam.id?.let { id -> offlineUser(id) }
            }
        }
    }

    @Transactional
    override fun deleteOne(id: Long) {
        delete(RoleDeleteParam(listOf(id)))
    }

    @Transactional
    override fun delete(roleDeleteParam: RoleDeleteParam) {
        removeBatchByIds(roleDeleteParam.ids)
        rPowerRoleService.remove(KtQueryWrapper(RPowerRole()).`in`(RPowerRole::roleId, roleDeleteParam.ids))
        offlineUser(*roleDeleteParam.ids!!.toLongArray())
    }

    private fun getFullPowerIds(powerIds: List<Long>): Set<Long> {
        val fullPowerIds: HashSet<Long> = hashSetOf()
        powerIds.forEach {
            fullPowerIds.add(it)
            fullPowerIds.add(it / 100 * 100)
            getMenuParent(it / 10000 * 10000, fullPowerIds)
            fullPowerIds.add(it / 1000000 * 1000000)
        }

        return fullPowerIds
    }

    private fun getMenuParent(id: Long, parentIds: HashSet<Long>) {
        parentIds.add(id)
        menuService.getById(id)?.parentId?.let {
            getMenuParent(it, parentIds)
        }
    }

    private fun offlineUser(vararg roleIds: Long) {
        val userIds = userService.getIdsByRoleIds(roleIds.toList())
        offlineUser(serverProperties = serverProperties, redisProvider = redisProvider, *userIds.toLongArray())
    }
}
