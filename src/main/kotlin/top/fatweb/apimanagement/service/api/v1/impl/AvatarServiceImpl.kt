package top.fatweb.apimanagement.service.api.v1.impl

import org.springframework.stereotype.Service
import top.fatweb.avatargenerator.GitHubAvatar
import top.fatweb.avatargenerator.IdenticonAvatar
import top.fatweb.avatargenerator.SquareAvatar
import top.fatweb.avatargenerator.TriangleAvatar
import top.fatweb.avatargenerator.layer.background.ColorPaintBackgroundLayer
import top.fatweb.apimanagement.param.api.v1.avatar.AvatarBaseParam
import top.fatweb.apimanagement.param.api.v1.avatar.AvatarGitHubParam
import top.fatweb.apimanagement.service.api.v1.IAvatarService
import top.fatweb.apimanagement.util.getRandomLong
import top.fatweb.apimanagement.vo.api.v1.avatar.AvatarBase64Vo
import java.awt.Color
import kotlin.io.encoding.Base64

/**
 * Avatar service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IAvatarService
 */
@Service
class AvatarServiceImpl : IAvatarService {
    override fun random(avatarBaseParam: AvatarBaseParam?): ByteArray =
        when ((1..4).random()) {
            1 -> this.triangle(avatarBaseParam)
            2 -> this.square(avatarBaseParam)
            3 -> this.identicon(avatarBaseParam)
            else -> this.github(AvatarGitHubParam().apply {
                seed = avatarBaseParam?.seed
                size = avatarBaseParam?.size
                margin = avatarBaseParam?.margin
                padding = avatarBaseParam?.padding
                colors = avatarBaseParam?.colors
                background = avatarBaseParam?.background
            })
        }

    override fun randomBase64(avatarBaseParam: AvatarBaseParam?): AvatarBase64Vo = when ((1..4).random()) {
        1 -> this.triangleBase64(avatarBaseParam)
        2 -> this.squareBase64(avatarBaseParam)
        3 -> this.identiconBase64(avatarBaseParam)
        else -> this.githubBase64(AvatarGitHubParam().apply {
            seed = avatarBaseParam?.seed
            size = avatarBaseParam?.size
            margin = avatarBaseParam?.margin
            padding = avatarBaseParam?.padding
            colors = avatarBaseParam?.colors
            background = avatarBaseParam?.background
        })
    }

    override fun triangle(avatarBaseParam: AvatarBaseParam?): ByteArray {
        val avatar = (
                if (avatarBaseParam == null || avatarBaseParam.colors.isNullOrEmpty())
                    TriangleAvatar.newAvatarBuilder()
                else TriangleAvatar.newAvatarBuilder(
                    *avatarBaseParam.colors!!.map(::decodeColor).toTypedArray()
                )
                ).apply {
                avatarBaseParam?.size?.let(::size)
                avatarBaseParam?.margin?.let(::margin)
                avatarBaseParam?.padding?.let(::padding)
                avatarBaseParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
            }.build()

        return avatar.createAsPngBytes(avatarBaseParam?.seed ?: getRandomLong())
    }

    override fun triangleBase64(avatarBaseParam: AvatarBaseParam?) =
        AvatarBase64Vo(Base64.encode(triangle(avatarBaseParam)))

    override fun square(avatarBaseParam: AvatarBaseParam?): ByteArray {
        val avatar = (
                if (avatarBaseParam == null || avatarBaseParam.colors.isNullOrEmpty())
                    SquareAvatar.newAvatarBuilder()
                else SquareAvatar.newAvatarBuilder(
                    *avatarBaseParam.colors!!.map(::decodeColor).toTypedArray()
                )
                ).apply {
                avatarBaseParam?.size?.let(::size)
                avatarBaseParam?.margin?.let(::margin)
                avatarBaseParam?.padding?.let(::padding)
                avatarBaseParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
            }.build()

        return avatar.createAsPngBytes(avatarBaseParam?.seed ?: getRandomLong())
    }

    override fun squareBase64(avatarBaseParam: AvatarBaseParam?) =
        AvatarBase64Vo(Base64.encode(square(avatarBaseParam)))

    override fun identicon(avatarBaseParam: AvatarBaseParam?): ByteArray {
        val avatar = IdenticonAvatar.newAvatarBuilder().apply {
            avatarBaseParam?.size?.let(::size)
            avatarBaseParam?.margin?.let(::margin)
            avatarBaseParam?.padding?.let(::padding)
            if (avatarBaseParam != null && !avatarBaseParam.colors.isNullOrEmpty()) {
                color(decodeColor(avatarBaseParam.colors!!.random()))
            }
            avatarBaseParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
        }.build()

        return avatar.createAsPngBytes(avatarBaseParam?.seed ?: getRandomLong())
    }

    override fun identiconBase64(avatarBaseParam: AvatarBaseParam?) =
        AvatarBase64Vo(Base64.encode(identicon(avatarBaseParam)))

    override fun github(avatarGitHubParam: AvatarGitHubParam?): ByteArray {
        val avatar = (avatarGitHubParam?.let { GitHubAvatar.newAvatarBuilder(it.elementSize, it.precision) }
            ?: let { GitHubAvatar.newAvatarBuilder(400, 5) }).apply {
            avatarGitHubParam?.size?.let(::size)
            avatarGitHubParam?.margin?.let(::margin)
            avatarGitHubParam?.padding?.let(::padding)
            if (avatarGitHubParam != null && !avatarGitHubParam.colors.isNullOrEmpty()) {
                color(decodeColor(avatarGitHubParam.colors!!.random()))
            }
            avatarGitHubParam?.background?.let { layers(ColorPaintBackgroundLayer(decodeColor(it))) }
        }.build()

        return avatar.createAsPngBytes(avatarGitHubParam?.seed ?: getRandomLong())
    }

    override fun githubBase64(avatarGitHubParam: AvatarGitHubParam?) =
        AvatarBase64Vo(Base64.encode(github(avatarGitHubParam)))

    fun decodeColor(nm: String): Color {
        return if (Regex("^#[0-9a-fA-F]{6}$").matches(nm)) {
            Color.decode(nm)
        } else if (Regex("^#[0-9a-fA-F]{8}$").matches(nm)) {
            val intVal = Integer.decode(nm.substring(1..6).prependIndent("#"))
            val alpha = Integer.decode(nm.substring(7).prependIndent("#"))
            Color(intVal shr 16 and 0xFF, intVal shr 8 and 0XFF, intVal and 0xFF, alpha and 0xFF)
        } else {
            Color.WHITE
        }
    }
}
