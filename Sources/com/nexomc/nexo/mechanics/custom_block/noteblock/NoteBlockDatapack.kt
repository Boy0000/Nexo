package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.google.gson.JsonPrimitive
import com.nexomc.nexo.NexoBootstrapper
import com.nexomc.nexo.mechanics.custom_block.noteblock.beacon.BeaconTagDatapack
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.JsonBuilder.toJsonArray
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
        if (NexoBootstrapper.bootsStrung) return
        writeMCMeta()
        removeFromMineableTag()

        if (NoteBlockMechanicFactory.instance()?.BLOCK_PER_VARIATION?.any { it.value.isBeaconBaseBlock() } == true)
            BeaconTagDatapack.writeTagFile(datapackFile)
        enableDatapack(true)
    }

    private fun removeFromMineableTag() {
        runCatching {
            val tagArray = when {
                VersionUtil.atleast("1.21") -> RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK)
                    .getTag(TagKey.create(RegistryKey.BLOCK, Key.key("minecraft:mineable/axe")))
                    .values().map { JsonPrimitive(it.key().asString()) }
                else -> Tag.MINEABLE_AXE.values.map { JsonPrimitive(it.key().asString()) }
            }.minus(JsonPrimitive("minecraft:note_block")).toJsonArray()

            datapackFile.resolve("data", "minecraft", "tags", "block", "mineable", "axe.json").apply {
                parentFile.mkdirs()
                createNewFile()
            }.writeText(JsonBuilder.jsonObject.plus("replace", true).plus("values", tagArray).toString())
        }.printOnFailure()
    }
}
