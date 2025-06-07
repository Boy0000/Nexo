package com.nexomc.nexo.glyphs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.KeyUtils.replace
import com.nexomc.nexo.utils.appendIfMissing
import com.nexomc.nexo.utils.deserialize
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.printOnFailure
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.configuration.ConfigurationSection
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import team.unnamed.creative.texture.Texture
import javax.imageio.ImageIO
import kotlin.math.roundToInt

data class AnimatedGlyph(val section: ConfigurationSection) : Glyph(section) {
    var frameCount = section.getInt("frame_count")
    private var aspectRatio: Float = 0f
    val offset by lazy { section.getInt("offset",(-(height * aspectRatio) - 1).roundToInt()) }

    private val gifKey = Key.key(section.getString("gif", id)!!.appendIfMissing(".gif"))
    override val texture: Key = gifKey.replace(".gif", ".png")
    override val font: Key = Key.key("nexo", "gifs/$id")
    override val defaultColor: TextColor = GIF_COLOR
    override val unicodes: MutableList<String> = mutableListOf() // Altered when calculating frame-count
    override val component by lazy {
        val placeholder = placeholders.firstOrNull()
        val hoverText = Settings.GLYPH_HOVER_TEXT.toString().let {
            if (placeholder != null) it.replace("<glyph_placeholder>", placeholder) else it
        }.replace("<glyph_id>", id).takeIf { it.isNotEmpty() }?.deserialize()?.let { HoverEvent.showText(it) }

        Component.text(0.until(frameCount).joinToString(unicode(frameCount), transform = ::unicode))
            .font(font).color(GIF_COLOR).hoverEvent(hoverText)
    }

    private val fontProvider: SpaceFontProvider by lazy { FontProvider.space().advance(unicode(frameCount), offset).build() }
    override val fontProviders: Array<FontProvider> by lazy { super.fontProviders + fontProvider }

    companion object {
        const val PRIVATE_USE_FIRST = 57344
        val GIF_COLOR = TextColor.color(16711422)

        fun unicode(index: Int): String = Character.toChars(PRIVATE_USE_FIRST + index).first().toString()
    }

    fun generateSplitGif(resourcePack: ResourcePack) {
        runCatching {
            val gifTexture = resourcePack.texture(gifKey) ?: return Logs.logWarn("No .gif found for $id at ${gifKey.asString()}")
            val spritesheet = GifConverter.splitGif(gifTexture, calculateFramecount(gifTexture))

            resourcePack.texture(spritesheet)
            resourcePack.removeTexture(gifKey)
        }.onFailure {
            Logs.logError("Could not generate split gif for ${id}.gif: ${it.message}")
        }.printOnFailure(true)
    }

    private fun calculateFramecount(gifTexture: Texture): Int {
        val (fc, ar) = runCatching {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            reader.input = gifTexture.data().toByteArray().inputStream().use(ImageIO::createImageInputStream)
            reader.getNumImages(true) to reader.getAspectRatio(0)
        }.onFailure {
            Logs.logError("Could not get frame count for ${id}.gif")
        }.printOnFailure(true).getOrDefault(0 to 0f)

        if (frameCount <= 0) frameCount = fc
        if (aspectRatio <= 0) aspectRatio = ar

        if (unicodes.isEmpty()) {
            unicodes += 0.until(frameCount).joinToString("", transform = ::unicode)
            NexoPlugin.instance().fontManager().registerGlyph(this)
        }

        return frameCount
    }
}