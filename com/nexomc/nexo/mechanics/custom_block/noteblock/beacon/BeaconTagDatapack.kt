package com.nexomc.nexo.mechanics.custom_block.noteblock.beacon

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.key.Key
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import java.nio.charset.StandardCharsets

object BeaconTagDatapack {
    private val defaultWorld = Bukkit.getWorlds().get(0)
    private val datapackKey = Key.key("minecraft:file/nexo_custom_blocks")
    private val customBlocksDatapack =
        defaultWorld.worldFolder.toPath().resolve("datapacks/nexo_custom_blocks").toFile()


    fun generateDatapack() {
        customBlocksDatapack.toPath().resolve("data").toFile().mkdirs()
        writeMCMeta()
        writeTagFile()
    }

    private fun writeTagFile() {
        try {
            val tagFile =
                customBlocksDatapack.toPath().resolve("data/minecraft/tags/blocks/beacon_base_blocks.json").toFile()
            tagFile.parentFile.mkdirs()
            tagFile.createNewFile()

            val tagObject = JsonObject()
            val values = JsonArray()

            tagObject.addProperty("replace", false)
            values.add("minecraft:note_block")
            tagObject.add("values", values)

            FileUtils.writeStringToFile(tagFile, tagObject.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeMCMeta() {
        try {
            val packMeta = customBlocksDatapack.toPath().resolve("pack.mcmeta").toFile()
            packMeta.createNewFile()

            val datapackMeta = JsonObject()
            val data = JsonObject()
            data.addProperty("description", "Datapack for Nexos Custom Blocks")
            data.addProperty("pack_format", 26)
            datapackMeta.add("pack", data)
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
