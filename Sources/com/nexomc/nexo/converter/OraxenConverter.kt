package com.nexomc.nexo.converter

import com.jeff_media.persistentdataserializer.PersistentDataSerializer
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteMechanicHelpers
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringMechanicHelpers
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.customarmor.CustomArmorType
import com.nexomc.nexo.utils.logs.Logs
import io.leangen.geantyref.TypeToken
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

object OraxenConverter {

    private val nexoFolder = NexoPlugin.instance().dataFolder
    private val oraxenFolder = nexoFolder.parentFile.resolve("Oraxen")
    private val tempFolder = oraxenFolder.resolveSibling("OraxenTemporary")

    fun convert() {
        val oraxenConverter = NexoPlugin.instance().converter().oraxenConverter
        if (!oraxenFolder.exists()) {
            oraxenConverter.hasBeenConverted = true
            NexoPlugin.instance().converter().save()
            return
        }

        Logs.logInfo("Starting conversion of Oraxen-setup...")
        tempFolder.deleteRecursively()
        oraxenFolder.copyRecursively(tempFolder)

        tempFolder.listFiles { file -> file.isFile && file.extension == "yml" }
            ?.takeUnless { oraxenConverter.hasBeenConverted }
            ?.forEach { file ->
                file.copyTo(nexoFolder.resolve(file.relativeTo(tempFolder)), overwrite = true)
                Logs.logInfo("Copied ${file.name} from Oraxen to Nexo...")
            }

        tempFolder.resolve("languages").listFiles { file -> file.isFile && file.extension == "yml" }
            ?.takeUnless { oraxenConverter.hasBeenConverted }
            ?.forEach { lang ->
                lang.writeText(lang.readText()
                    .replace("<gradient:#9055FF:#13E2DA>", "<gradient:#46C392:#7FC794>")
                    .replace("Oraxen", "Nexo")
                    .replace("/o", "/nexo")
                    .replace("oraxen", "nexo")

                )
            }

        if (oraxenConverter.convertSettings) processSettings(nexoFolder)

        arrayOf("sound.yml", "hud.yml", "gestures", "font.yml").forEach {
            tempFolder.resolve(it).deleteRecursively()
            nexoFolder.resolve(it).deleteRecursively()
        }

        if (oraxenConverter.convertItems) {
            tempFolder.resolve("items").walkBottomUp().filter { it.extension == "yml" }.forEach {
                processItemConfigs(it)
                Logs.logSuccess("Finished converting item-config ${it.name}")
                it.copyTo(it.parentFile.resolve("oraxen_items").resolve(it.name))
                it.delete()
            }
            tempFolder.resolve("recipes").listFiles()?.filter { it.extension == "yml" && it.readText().isNotEmpty() }?.forEach {
                processRecipes(it)
                Logs.logSuccess("Finished converting recipe-config ${it.name}")
            }
        }

        tempFolder.resolve("glyphs").walkBottomUp().filter { it.extension == "yml" }.forEach {
            val target = it.parentFile.resolve("oraxen_glyphs").resolve(it.name)
            when (it.name) {
                "required.yml", "shifts.yml" -> {
                    Logs.logWarn("Skipped ${it.path} due to Nexo providing this")
                    target.delete()
                }
                "interface.yml" -> {
                    NexoYaml.loadConfiguration(it).apply {
                        set("menu_items", null)
                        save(it)
                    }
                    it.copyTo(target)
                }
                else -> it.copyTo(target)
            }
            it.delete()
        }

        if (oraxenConverter.convertResourcePack) {
            val packFolder = tempFolder.resolve("pack")
            processPackFolder(packFolder)
            packFolder.resolve("pack.mcmeta").delete()
            packFolder.resolve("pack.png").delete()
            Logs.logSuccess("Finished converting Resourcepack folders")
        }

        tempFolder.copyRecursively(nexoFolder, false) { file, _ ->
            val oraxenFile = oraxenFolder.resolve(file.relativeTo(nexoFolder))
            if (!oraxenFile.path.startsWith("plugins\\Oraxen\\pack"))
                Logs.logWarn("Skipping copying of <gold>${oraxenFile.path}</gold> as it already exists...")
            OnErrorAction.SKIP
        }

        tempFolder.deleteRecursively()
        oraxenConverter.hasBeenConverted = true
        NexoPlugin.instance().converter().save()
        Logs.logSuccess("Finished conversion of Oraxen- to Nexo-setup!.")
    }

    private var customArmorType = if (VersionUtil.atleast("1.21.2")) CustomArmorType.COMPONENT else CustomArmorType.TRIMS

    fun processSettings(oraxenFolder: File) {
        val settingsLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(oraxenFolder.resolve("settings.yml")).build()
        runCatching { settingsLoader.load() }.printOnFailure().getOrNull()?.let { settings ->
            settings.removeChild("CustomBlocks")
            settings.node("ConfigsTools").renameNode("ConfigTools")?.node("error_item", "injectID")?.renameNode("injectId")

            settings.node("Pack", "server", "polymath").let {
                it.node("server").set("atlas.nexomc.com")
                it.node("secret").set("nexomc")
            }

            val customArmorNode = settings.node("CustomArmor", "type")
            if (customArmorNode.string == "SHADER") customArmorNode.set(customArmorType.name)
            else customArmorType = CustomArmorType.fromString(customArmorNode.getString(customArmorType.name))

            settings.removeChildNode("oraxen_inventory")

            settings.node("Pack").also { pack ->
                pack.removeChild("upload")
                pack.removeChild("receive")
                pack.removeChild("import")
                pack.removeChild("generation")
                pack.node("dispatch", "prompt").let {
                    it.set(it.string?.replace("Oraxen", "Nexo"))
                }
            }

            settingsLoader.save(settings)
        }

        oraxenFolder.resolve("sound.yml").renameTo(oraxenFolder.resolve("sounds.yml"))
        val soundsLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(oraxenFolder.resolve("sounds.yml")).build()
        runCatching { soundsLoader.load() }.printOnFailure().getOrNull()?.let { sounds ->
            sounds.node("settings", "automatically_generate").renameNode("generate_sounds")
            sounds.node("sounds").childrenMap().keys.filter {
                it.toString().removePrefix("block.").removePrefix("required.").let { s ->
                    s.startsWith("stone") || s.startsWith("wood")
                }
            }.forEach {
                sounds.node("sounds").removeChild(it)
            }
            soundsLoader.save(sounds)
        }
    }

    fun processRecipes(recipeFile: File) {
        val recipeLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(recipeFile).build()
        val recipeYaml = runCatching { recipeLoader.load() }.onFailure {
            Logs.logError("Failed to load ${recipeFile.name} for parsing...")
            it.printStackTrace()
        }.getOrNull() ?: return

        recipeYaml.childrenMap().values.forEach { recipeNode ->
            recipeNode.node("result")?.removeChildNode("oraxen_item")?.string?.let {
                recipeNode.node("result", "nexo_item").set(it)
            }
            recipeNode.node("input")?.removeChildNode("oraxen_item")?.string?.let {
                recipeNode.node("input", "nexo_item").set(it)
            }
            recipeNode.node("ingredients").childrenMap().forEach { (_, ingredientNode) ->
                ingredientNode.removeChildNode("oraxen_item")?.string?.let {
                    ingredientNode.node("nexo_item").set(it)
                }
            }

        }

        runCatching {
            recipeLoader.save(recipeYaml)
        }.onFailure {
            Logs.logWarn("Failed to save ${recipeFile.name} changes...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }
    }

    fun processItemConfigs(itemFile: File) {
        if (itemFile.extension != "yml") return
        val itemLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(itemFile).build()
        val itemYaml = runCatching { itemLoader.load() }.onFailure {
            Logs.logError("Failed to load ${itemFile.name} for parsing...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }.getOrNull() ?: return

        itemYaml.childrenMap().mapKeys { it.key.toString() }.forEach { (itemId, itemNode) ->
            if (itemNode.empty()) return@forEach

            runCatching {
                itemNode.node("displayname").renameNode("itemname")
                itemNode.node("injectID").renameNode("injectId")
                itemNode.node("trim_pattern")?.let { it.set(it.string?.replace("oraxen", "nexo")) }
                itemNode.node("Components", "equippable", "model")?.let { it.set(it.string?.replace("oraxen", "nexo")) }
                itemNode.node("AttributeModifiers").childrenList().forEach { attributeNode ->
                    attributeNode.removeChild("name")
                    attributeNode.removeChild("key")
                    attributeNode.removeChild("uuid")
                }

                val componentNode = itemNode.node("Components")
                val mechanicsNode = itemNode.node("Mechanics")

                if (customArmorType == CustomArmorType.TRIMS && itemId.matches(CustomArmorType.itemIdRegex)) {
                    itemNode.node("material").takeIf { "_" in it.getString("") }?.let { it.set("CHAINMAIL_${it.string?.substringAfter("_")}") }
                }

                if (VersionUtil.atleast("1.20.5")) mechanicsNode.node("food").renameNode(componentNode.node("food"))?.onExists { foodNode ->
                    foodNode.node("hunger").renameNode("nutrition")
                    val effectProbability = foodNode.removeChildNode("effect_probability").getDouble(1.0)
                    //TODO Convert to Consumable-Component for 1.21.2+ servers
                    foodNode.node("effects").childrenMap().values.forEach { effect ->
                        effect.node("probability").set(effectProbability)
                        effect.node("is_ambient").renameNode("ambient")
                        effect.node("has_particles").renameNode("show_particles")
                        effect.node("has_icon").renameNode("show_icon")
                    }
                }

                mechanicsNode.removeChildNode("durability")?.node("value")?.renameNode(componentNode.node("durability"))

                mechanicsNode.removeChildNode("music_disc").childrenMap().values.forEach { musicdisc ->
                    componentNode.node("jukebox_playable", "song_key").set(musicdisc.node("song").string)
                    componentNode.node("jukebox_playable", "show_in_tooltip").set(true)
                }

                mechanicsNode.node("noteblock")?.ifExists { noteNode ->
                    noteNode.node("type").set("NOTEBLOCK")
                    noteNode.node("drop").removeChildNode("best_tools").getList(TypeToken.get(String::class.java))?.firstOrNull()?.let {
                        noteNode.node("drop", "best_tool").set(it)
                    }
                    noteNode.node("drop", "loots").childrenList().forEach { lootNode ->
                        lootNode.removeChildNode("oraxen_item").string?.let {
                            lootNode.node("nexo_item").set(it)
                        }
                    }
                    noteNode.node("custom_variation").also { cv ->
                        val legacyData = NoteMechanicHelpers.legacyBlockData(cv.int)
                        // We plus1 here because Oraxen had bad logic so it was CV1 = note 2...
                        val modernVariation = legacyData?.let(NoteMechanicHelpers::modernCustomVariation)?.plus(1) ?: return@also
                        cv.set(modernVariation)
                    }
                }?.renameNode("custom_block")

                mechanicsNode.node("stringblock")?.ifExists { stringNode ->
                    stringNode.node("type").set("STRINGBLOCK")
                    stringNode.node("drop").removeChildNode("best_tools").getList(TypeToken.get(String::class.java))?.firstOrNull()?.let {
                        stringNode.node("drop", "best_tool").set(it)
                    }
                    stringNode.node("drop", "loots").childrenList().forEach { lootNode ->
                        lootNode.removeChildNode("oraxen_item").string?.let {
                            lootNode.node("nexo_item").set(it)
                        }
                    }

                    stringNode.node("sapling").onExists { sapling ->
                        sapling.node("canGrowNaturally").renameNode("grows_naturally")
                        sapling.node("naturalGrowthTime").renameNode("natural_growth_time")
                        sapling.node("canGrowFromBoneMeal").renameNode("grows_from_bonemeal")
                        sapling.node("boneMealGrowthSpeedup").renameNode("bonemeal_growth_speedup")
                        sapling.node("growSound").renameNode("grow_sound")
                        sapling.node("minLightLevel").renameNode("min_light_level")
                        sapling.node("requiresWaterSource").renameNode("requires_water_source")
                        sapling.node("schematicName").renameNode("schematic")
                        sapling.node("shouldCopyBiomes").renameNode("copy_biomes")
                        sapling.node("shouldCopyEntities").renameNode("copy_entities")
                        sapling.node("shouldReplaceBlocks").renameNode("replace_blocks")
                    }

                    val legacyBlockData = StringMechanicHelpers.legacyBlockData(stringNode.node("custom_variation").int)
                    val modernVariation = StringMechanicHelpers.modernCustomVariation(legacyBlockData)
                    stringNode.node("custom_variation").set(modernVariation)
                }?.renameNode("custom_block")

                mechanicsNode.node("furniture").ifExists { furnitureNode ->
                    furnitureNode.removeChildNode("type").string?.takeUnless { it == "DISPLAY_ENTITY" }?.also {
                        Logs.logWarn("    Furniture <gold>$itemId</gold> is using ITEM_FRAME type furniture")
                        Logs.logWarn("    This might cause issues due to Nexo only supporting DISPLAY_ENTITY")
                        Logs.logError(" Be sure to back-up your world folders and run the world-converter")

                        furnitureNode.node("properties", "display_transform").set("FIXED")
                    }

                    furnitureNode.node("drop", "loots").childrenList().forEach { lootNode ->
                        lootNode.removeChildNode("oraxen_item").string?.let {
                            lootNode.node("nexo_item").set(it)
                        }
                    }

                    furnitureNode.node("display_entity_properties").renameNode("properties")?.onExists { propertiesNode ->
                        propertiesNode.node("displayWidth").renameNode("display_width")
                        propertiesNode.node("displayHeight").renameNode("display_height")
                        propertiesNode.node("scale").takeIf(ConfigurationNode::virtual)?.let { scaleNode ->
                            val isFixed = propertiesNode.node("display_transform").string == "FIXED"
                            val scaleString = listOf("x", "y", "z").joinToString(",") { scaleNode.childrenMap()[it]?.getDouble(if (isFixed) 0.5 else 1.0)?.toString()?.replace(".0", "") ?: if (isFixed) "0.5" else "1" }
                            scaleNode.set(scaleString)
                        }
                    }

                    furnitureNode.node("farmblock_required").renameNode("farmland_required")
                    furnitureNode.node("evolution").onExists { evoNode ->
                        evoNode.node("rain_boost", "boost_tick").renameNode(evoNode.node("rain_boost_tick"))
                        evoNode.node("bone_meal", "chance").renameNode(evoNode.node("bone_meal_chance"))
                    }

                    val hitboxNode = furnitureNode.node("hitbox")
                    if (hitboxNode.node("interactions").virtual()) hitboxNode.node("interactionHitboxes").renameNode("interactions")
                    else hitboxNode.removeChild("interactionHitboxes")
                    if (hitboxNode.node("barriers").virtual()) hitboxNode.node("barrierHitboxes").renameNode("barriers")
                    else hitboxNode.removeChild("barrierHitboxes")

                    val barriers = mutableSetOf<String>()
                    if (furnitureNode.removeChildNode("barrier").boolean) barriers += "0,0,0"
                    furnitureNode.removeChildNode("barriers").onExists { barriersNode ->
                        barriersNode.childrenList().forEach barriers@{ barrierNode ->
                            barriers += when {
                                barrierNode.string == "origin" -> "0,0,0"
                                barrierNode.isMap -> listOf("x", "y", "z").joinToString(",") { barrierNode.childrenMap()[it]?.getInt(0)?.toString() ?: "0" }
                                else -> return@barriers
                            }
                        }
                    }
                    if (barriers.isNotEmpty()) furnitureNode.node("hitbox", "barriers").set(barriers)

                    val templateHasHitbox = itemNode.node("template").string?.let { itemNode.parent()?.node(it) }
                        ?.node("Mechanics", "furniture", "hitbox")?.onExists {
                            it.node("barriers").childrenList().isNullOrEmpty() &&
                                    it.node("interactions").childrenList().isNullOrEmpty()
                        }?.not() ?: false
                    val (width, height) = furnitureNode.node("hitbox").let { it.removeChildNode("width").getDouble(1.0) to it.removeChildNode("height").getDouble(1.0) }
                    if (!templateHasHitbox) if (width != 1.0 || height != 1.0  || (barriers.isEmpty() && furnitureNode.node("hitbox", "shulkers").childrenList().isEmpty())) {
                        hitboxNode.node("interactions").takeIf {
                            it.virtual() && hitboxNode.node("barriers").virtual()
                        }?.set(listOf("0,0,0 $width,$height"))
                    }

                    furnitureNode.removeChildNode("light")?.takeUnless { barriers.isEmpty() || it.int == 0 }?.ifExists { lightNode ->
                        furnitureNode.node("lights").setList(String::class.java, barriers.map { it.plus(" ${lightNode.raw()}") })
                    }

                    furnitureNode.removeChildNode("seat").node("height")?.takeUnless { barriers.isEmpty() }?.ifExists { seatNode ->
                        val seatOffset = (seatNode.raw().toString().toDoubleOrNull() ?: 0.0).minus(0.6)
                        val seats = barriers.map { FurnitureSeat(it).apply { offset.y -= seatOffset }.offset.toString() }
                        furnitureNode.node("seats").setList(String::class.java, seats)
                    }

                    furnitureNode.node("drop").removeChildNode("best_tools").getList(TypeToken.get(String::class.java))?.firstOrNull()?.let {
                        furnitureNode.node("drop", "best_tool").set(it)
                    }
                }

                if (mechanicsNode.empty()) itemNode.removeChild("Mechanics")
                if (componentNode.empty()) itemNode.removeChild("Components")
            }.onFailure {
                Logs.logError("Failed to convert $itemId: ${it.message}")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }

        }
        runCatching {
            itemLoader.save(itemYaml)
        }.onFailure {
            Logs.logWarn("Failed to save ${itemFile.name} changes...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }
    }

    fun processPackFolder(packFolder: File) {
        runCatching {
            if (!packFolder.exists() || !packFolder.isDirectory) return
            val namespaceFolder = packFolder.resolve("assets").resolve("minecraft")

            packFolder.resolve("textures", "required").walkBottomUp().firstOrNull { it.name == "menu_items.png" }?.delete()
            packFolder.resolve("textures", "models", "armor").walkBottomUp().forEach { file ->
                if (file.name.startsWith("chainmail_")) return@forEach
                else file.delete()
            }

            setOf("models", "textures", "sounds", "font", "lang").associateFastWith { packFolder.resolve(it) }.forEach {
                val dest = namespaceFolder.resolve(it.key)
                it.value.copyRecursively(dest, false) { _, _ ->
                    OnErrorAction.SKIP
                }
                it.value.deleteRecursively()
            }
        }.onFailure {
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            else Logs.logWarn(it.message!!)
        }
    }

    fun convertOraxenPDCEntries(pdc: PersistentDataContainer) {
        val oraxenKeys = mutableListOf<NamespacedKey>()
        PersistentDataSerializer.fromMapList(
            PersistentDataSerializer.toMapList(pdc).map { map ->
                map.mapValues { e ->
                    if (e.key.toString() != "key") return@mapValues e.value
                    val value = e.value as? String ?: return@mapValues e.value
                    value.takeIf { "oraxen" in it }?.apply { NamespacedKey.fromString(this)?.apply(oraxenKeys::add) }?.replace("oraxen", "nexo") ?: e.value
                }
            }, pdc
        )
        oraxenKeys.forEach(pdc::remove)
    }

}