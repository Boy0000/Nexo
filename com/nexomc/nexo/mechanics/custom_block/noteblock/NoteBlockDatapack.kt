package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.printOnFailure
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.nexomc.nexo.utils.resolve
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.Tag
import java.nio.charset.StandardCharsets
import java.util.*

class NoteBlockDatapack {
    fun generateDatapack() {
        nexoDatapack.resolve("data").mkdirs()
        writeMcMeta()
        removeFromMineableTag()

        Bukkit.getDatapackManager().packs.firstOrNull { it.name == "file/nexo_custom_blocks" }?.isEnabled = true
    }

    private fun writeMcMeta() {
        runCatching {
            val packMeta = nexoDatapack.resolve("pack.mcmeta")
            val jsonObject = JsonObject()
            val packObject = JsonObject()

            packObject.addProperty("description", "Datapack for Nexo")
            packObject.addProperty("pack_format", 48)

            jsonObject.add("pack", packObject)

            FileUtils.writeStringToFile(packMeta, jsonObject.toString(), StandardCharsets.UTF_8)
        }.printOnFailure()
    }

    private fun removeFromMineableTag() {
        runCatching {
            val tagFile = nexoDatapack.resolve("data", "minecraft", "tags", "block", "mineable", "axe.json")
            tagFile.parentFile.mkdirs()
            tagFile.createNewFile()

            val content = FileUtils.readFileToString(tagFile, StandardCharsets.UTF_8)
            val tagObject =
                if (content.isEmpty()) JsonObject() else JsonParser.parseString(content).asJsonObject
            val tagArray = Optional.ofNullable(tagObject.getAsJsonArray("values")).orElseGet {
                JsonArray().apply {
                    if (VersionUtil.atleast("1.21")) {
                        RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK)
                            .getTag(TagKey.create(RegistryKey.BLOCK, Key.key("minecraft:mineable/axe")))
                            .values().forEach { add(it.key().asString()) }
                    } else Tag.MINEABLE_AXE.values.forEach { add(it.key().asString()) }
                }
            }

            tagArray.remove(JsonPrimitive("minecraft:note_block"))

            tagObject.addProperty("replace", true)
            tagObject.add("values", tagArray)

            FileUtils.writeStringToFile(tagFile, tagObject.toString(), StandardCharsets.UTF_8)
        }.printOnFailure()
    }

    companion object {
        private val defaultWorld = Bukkit.getWorlds().first()
        private val nexoDatapack = defaultWorld.worldFolder.resolve("datapacks", "nexo_custom_blocks")
    }
}
