package com.nexomc.nexo.mechanics.custom_block.noteblock.beacon

import com.nexomc.nexo.utils.JsonBuilder
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.printOnFailure
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit

object BeaconTagDatapack {
    private val defaultWorld = Bukkit.getWorlds().first()
    private val customBlocksDatapack = defaultWorld.worldFolder.resolve("datapacks/nexo_custom_blocks")


    fun generateDatapack() {
        customBlocksDatapack.resolve("data").mkdirs()
        writeMCMeta()
        writeTagFile()
    }

    private fun writeTagFile() {
        runCatching {
            val tagFile = customBlocksDatapack.resolve("data/minecraft/tags/blocks/beacon_base_blocks.json")
            tagFile.parentFile.mkdirs()

            val content = JsonBuilder.jsonObject
                .plus("replace", false)
                .plus("values", JsonBuilder.jsonArray.plus("minecraft:note_block"))

            tagFile.writeText(content.toString())
        }.printOnFailure()
    }

    private fun writeMCMeta() {
        runCatching {
            val packMeta = customBlocksDatapack.resolve("pack.mcmeta")
            val json = JsonBuilder.jsonObject.plus("pack", JsonBuilder.jsonObject
                .plus("description", "Datapack for Nexos Custom Blocks")
                .plus("pack_format", 26)
            )
            packMeta.writeText(json.toString())
        }.printOnFailure()
    }
}
