package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.nexomc.nexo.mechanics.custom_block.noteblock.beacon.BeaconTagDatapack
import com.nexomc.nexo.utils.JsonBuilder
import com.nexomc.nexo.utils.JsonBuilder.array
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.NexoDatapack
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.resolve
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Tag

class NoteBlockDatapack : NexoDatapack("nexo_custom_blocks", "Datapack for allowing Custom Blocks in Beacons") {

    init {
        datapackFile.deleteRecursively()
    }

    fun createDatapack() {
        writeMCMeta()
        removeFromMineableTag()

        if (NoteBlockMechanicFactory.instance()?.BLOCK_PER_VARIATION?.any { it.value.isBeaconBaseBlock() } == true)
            BeaconTagDatapack.writeTagFile(datapackFile)
        enableDatapack(true)
    }

    private fun removeFromMineableTag() {
        runCatching {
            val tagFile = datapackFile.resolve("data", "minecraft", "tags", "block", "mineable", "axe.json")
            tagFile.parentFile.mkdirs()
            tagFile.createNewFile()

            val content = tagFile.readText()
            val tagObject = if (content.isEmpty()) JsonBuilder.jsonObject else JsonParser.parseString(content).asJsonObject
            val tagArray = tagObject.array("values") ?: JsonBuilder.jsonArray.apply {
                if (VersionUtil.atleast("1.21")) {
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK)
                        .getTag(TagKey.create(RegistryKey.BLOCK, Key.key("minecraft:mineable/axe")))
                        .values().forEach { add(it.key().asString()) }
                } else Tag.MINEABLE_AXE.values.forEach { add(it.key().asString()) }
            }

            tagArray.remove(JsonPrimitive("minecraft:note_block"))

            tagObject.plus("replace", true).plus("values", tagArray)
            tagFile.writeText(tagObject.toString())
        }.printOnFailure()
    }
}
