package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoPack
import com.nexomc.nexo.api.events.resourcepack.NexoPackUploadEvent
import com.nexomc.nexo.api.events.resourcepack.NexoPostPackGenerateEvent
import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent
import com.nexomc.nexo.compatibilities.modelengine.ModelEngineCompatibility
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.fonts.NexoTranslator
import com.nexomc.nexo.glyphs.ReferenceGlyph
import com.nexomc.nexo.glyphs.Shift
import com.nexomc.nexo.glyphs.ShiftTag
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.pack.ShaderUtils.ScoreboardBackground
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.pack.creative.NexoPackWriter
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.AdventureUtils.parseLegacyThroughMiniMessage
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.customarmor.ComponentCustomArmor
import com.nexomc.nexo.utils.customarmor.CustomArmorType
import com.nexomc.nexo.utils.customarmor.CustomArmorType.Companion.setting
import com.nexomc.nexo.utils.customarmor.TrimsCustomArmor
import com.nexomc.nexo.utils.jukebox_playable.JukeboxPlayableDatapack
import com.nexomc.nexo.utils.logs.Logs
import com.ticxo.modelengine.api.ModelEngineAPI
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import team.unnamed.creative.BuiltResourcePack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.ReferenceFontProvider
import team.unnamed.creative.lang.Language
import team.unnamed.creative.metadata.pack.PackFormat
import team.unnamed.creative.sound.SoundRegistry
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PackGenerator {
    private val packDownloader: PackDownloader = PackDownloader()
    private val resourcePack = ResourcePack.resourcePack()
    private val packObfuscator: PackObfuscator = PackObfuscator(resourcePack)
    private val packSquasher: NexoPackSquash = NexoPackSquash(resourcePack)
    private val packValidator: PackValidator = PackValidator(resourcePack)
    private var builtPack: BuiltResourcePack? = null
    private val trimsCustomArmor: TrimsCustomArmor?
    private val componentCustomArmor: ComponentCustomArmor?
    private val modelGenerator: ModelGenerator
    private val atlasGenerator: AtlasGenerator
    private val gifGenerator: GifGenerator
    var packGenFuture: CompletableFuture<Void>? = null
        private set

    init {
        NexoPackReader.resetReader()
        NexoPackWriter.resetWriter()
    }

    fun regeneratePack() {
        generatePack(Settings.PACK_SEND_RELOAD.toBool())
    }

    fun generatePack(sendToOnline: Boolean = true) {
        stopPackGeneration()
        NexoPrePackGenerateEvent(resourcePack).call()
        val packFolder = NexoPlugin.instance().dataFolder.resolve("pack")

        val futures = arrayOf(
            packDownloader.downloadRequiredPack(),
            packDownloader.downloadDefaultPack(),
            VanillaResourcePack.extractLatest(),
            ModelEngineCompatibility.modelEngineFuture()
        )

        packGenFuture = CompletableFuture.allOf(*futures).thenRunAsync {
            runCatching {
                Logs.logInfo("Generating resourcepack...")
                importRequiredPack()
                if (Settings.PACK_IMPORT_DEFAULT.toBool()) importDefaultPack()
                if (Settings.PACK_IMPORT_EXTERNAL.toBool()) importExternalPacks()
                if (Settings.PACK_IMPORT_MODEL_ENGINE.toBool()) importModelEnginePack()

                Settings.PACK_IMPORT_FROM_LOCATION.toStringList().mapNotNull {
                    NexoPlugin.instance().dataFolder.absoluteFile.parentFile?.parentFile?.resolve(it)
                }.forEach { file ->
                    runCatching {
                        Logs.logInfo("Importing pack from <aqua>${file.path}")
                        NexoPack.mergePack(resourcePack, NexoPackReader.INSTANCE.readFile(file))
                    }.onFailure {
                        Logs.logError("Failed to read ${file.path} to a ResourcePack")
                        if (Settings.DEBUG.toBool()) it.printStackTrace()
                        else Logs.logError(it.message!!)
                    }
                }
                Settings.PACK_IMPORT_FROM_URL.toStringList().forEach { url ->
                    runCatching {
                        Logs.logInfo("Importing pack from <aqua>${url}")
                        val pack = URI.create(url).toURL().openStream().use(NexoPackReader.INSTANCE::readFromInputStream)
                        NexoPack.mergePack(resourcePack, pack)
                    }.onFailure {
                        Logs.logError("Failed to read $url to a ResourcePack")
                        if (Settings.DEBUG.toBool()) it.printStackTrace()
                        else Logs.logError(it.message!!)
                    }
                }

                resourcePack.packMeta(PackFormat.format(NMSHandlers.handler().resourcepackFormat(), 22, 99), "Nexo's default pack.".deserialize())

                runCatching {
                    NexoPack.mergePack(resourcePack, NexoPackReader.INSTANCE.readFile(packFolder))
                }.onFailure {
                    Logs.logError("Failed to read Nexo/pack/assets-folder to a ResourcePack")
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                    else Logs.logError(it.message!!)
                }

                CustomBlockFactory.instance()?.blockStates(resourcePack)
                CustomBlockFactory.instance()?.soundRegistries(resourcePack)
                FurnitureFactory.instance()?.soundRegistries(resourcePack)
                modelGenerator.generateModels()
                atlasGenerator.generateAtlasFile()
                gifGenerator.generateGifFiles()
                addGlyphFiles()
                addShiftProvider()
                addSoundFile()
                parseLanguageFiles()

                trimsCustomArmor?.generateTrimAssets(resourcePack)
                componentCustomArmor?.generatePackFiles()

                handleScoreboardTablist()
                Overlays.addToResourcepack(resourcePack)
                removeExcludedFileExtensions()
                sortModelOverrides()

                runCatching {
                    SchedulerUtils.foliaScheduler.runNextTick {
                        NexoPostPackGenerateEvent(resourcePack).callEvent()
                    }.get(4L, TimeUnit.SECONDS)
                }

                packValidator.validatePack()
                ModernVersionPatcher.convertResources(resourcePack)
                resourcePack.items().removeIf { ModernVersionPatcher.standardItemModels.containsValue(it) }

                val initialHash = NexoPackWriter.INSTANCE.build(resourcePack).hash().takeIf {
                    !packObfuscator.obfuscationType.isNone || Settings.PACK_USE_PACKSQUASH.toBool()
                } ?: ""

                packObfuscator.obfuscatePack(initialHash)

                val packZip = NexoPlugin.instance().dataFolder.resolve("pack", "pack.zip")
                if (Settings.PACK_USE_PACKSQUASH.toBool() && packSquasher.squashPack(initialHash)) {
                    val squashedZip = packSquasher.packSquashCache.resolve("$initialHash.zip")
                    if (Settings.PACK_GENERATE_ZIP.toBool()) squashedZip.copyTo(packZip, true)
                    builtPack = BuiltResourcePack.of(Writable.file(packZip), FileUtils.getSha1Hash(packZip))
                } else {
                    if (Settings.PACK_GENERATE_ZIP.toBool()) SchedulerUtils.foliaScheduler.runAsync {
                        NexoPackWriter.INSTANCE.writeToZipFile(packZip, resourcePack)
                    }
                    builtPack = NexoPackWriter.INSTANCE.build(resourcePack)
                }
            }.onFailure {
                Logs.logError("Failed to generate ResourcePack...")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
                else Logs.logWarn(it.message!!)
            }
        }.thenRunAsync {
            Logs.logSuccess("Finished generating resourcepack!", true)
            val packServer = NexoPlugin.instance().packServer()
            packServer.uploadPack().thenRun {
                SchedulerUtils.foliaScheduler.runNextTick {
                    NexoPackUploadEvent(builtPack!!.hash(), packServer.packUrl()).call()
                }
                if (sendToOnline) Bukkit.getOnlinePlayers().forEach(packServer::sendPack)
            }
            NexoTranslator.registerTranslations()
        }
    }

    private fun sortModelOverrides() {
        resourcePack.models().toMutableList().forEach { model ->
            val sortedOverrides = LinkedHashSet(model.overrides()).sortedWith(Comparator.comparingInt { o ->
                    o.predicate().firstOrNull { p -> p.name() == "custom_model_data" }?.value().toString().toIntOrNull() ?: 0
                })
            model.toBuilder().overrides(sortedOverrides).build().addTo(resourcePack)
        }
    }

    fun packObfuscator() = packObfuscator

    fun resourcePack() = resourcePack

    fun resourcePack(resourcePack: ResourcePack) {
        NexoPack.overwritePack(this.resourcePack, resourcePack)
    }

    fun builtPack() = builtPack

    private fun addShiftProvider() {
        val shiftFontReferences = resourcePack.font(ShiftTag.FONT)?.providers()?.mapNotNull { (it as? ReferenceFontProvider)?.id() } ?: emptyList()
        resourcePack.fonts().toList().forEach { font ->
            if (font.key() == ShiftTag.FONT || font.key() in shiftFontReferences) font.toBuilder().addProvider(Shift.fontProvider).build().addTo(resourcePack)
            else font.toBuilder().addProvider(FontProvider.reference(ShiftTag.FONT)).build().addTo(resourcePack)
        }
        Font.font(ShiftTag.FONT, (resourcePack.font(ShiftTag.FONT)?.providers()?.plus(Shift.fontProvider) ?: listOf(Shift.fontProvider))).addTo(resourcePack)
    }

    private fun addGlyphFiles() {
        NexoPlugin.instance().fontManager().glyphs().groupBy { it.font }.forEach { (font, glyphs) ->
            val providers = resourcePack.font(font)?.providers() ?: emptyList()
            val glyphProviders = glyphs.filterNot { it is ReferenceGlyph }.flatMap { it.fontProviders.toList() }
            resourcePack.font(font, providers.plus(glyphProviders))
        }
    }

    private fun addSoundFile() {
        NexoPlugin.instance().soundManager().customSoundRegistries().forEach { customSoundRegistry ->
            (resourcePack.soundRegistry(customSoundRegistry.namespace())?.let { existing ->
                SoundRegistry.soundRegistry(existing.namespace(), existing.sounds().plus(customSoundRegistry.sounds()))
            } ?: customSoundRegistry).addTo(resourcePack)
        }
        JukeboxPlayableDatapack().createDatapack()
    }

    private fun handleScoreboardTablist() {
        if (!Settings.HIDE_SCOREBOARD_BACKGROUND.toBool() && !Settings.HIDE_TABLIST_BACKGROUND.toBool()) return
        resourcePack.unknownFile("assets/minecraft/shaders/core/rendertype_gui.vsh", ScoreboardBackground.modernFile())
    }

    private fun importRequiredPack() {
        runCatching {
            val requiredPack = NexoPackReader.INSTANCE.readFile(externalPacks.listFiles()?.firstOrNull { it.name.startsWith("RequiredPack_") } ?: return)
            if (VersionUtil.atleast("1.21.4")) requiredPack.items().map { it.key() }.forEach(requiredPack::removeItem)
            NexoPack.mergePack(resourcePack, requiredPack)
        }.onFailure {
            if (!Settings.DEBUG.toBool()) Logs.logError(it.message!!)
            else it.printStackTrace()
        }
    }

    private fun importDefaultPack() {
        val defaultPack = externalPacks.listFiles()?.firstOrNull { it.name.startsWith("DefaultPack_") } ?: return
        Logs.logInfo("Importing DefaultPack...")

        runCatching {
            NexoPack.mergePack(resourcePack, NexoPackReader.INSTANCE.readFile(defaultPack))
        }.onFailure {
            Logs.logError("Failed to read Nexo's DefaultPack...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            else Logs.logError(it.message!!)
        }
    }

    private val defaultRegex = "(Default|Required)Pack_.*".toRegex()
    private fun importExternalPacks() {
        val externalPacks = externalPacks.listFiles() ?: return
        val externalOrder = Settings.PACK_IMPORT_EXTERNAL_PACK_ORDER.toStringList()
        externalPacks.sortedWith(Comparator.comparingInt<File> {
            externalOrder.indexOf(it.name).takeIf { it != -1 } ?: Int.MAX_VALUE
        }.thenComparing(File::getName)).asSequence().filter { !it.name.matches(defaultRegex) }.forEach {
                if (it.isDirectory || it.name.endsWith(".zip")) {
                    Logs.logInfo("Importing external-pack <aqua>${it.name}</aqua>...")
                    runCatching {
                        NexoPack.mergePack(resourcePack, NexoPackReader.INSTANCE.readFile(it))
                    }.onFailure { e ->
                        Logs.logError("Failed to read ${it.path} to a ResourcePack...")
                        if (!Settings.DEBUG.toBool()) Logs.logError(e.message!!)
                        else e.printStackTrace()
                    }
                } else {
                    Logs.logError("Skipping unknown file ${it.name} in imports folder")
                    Logs.logError("File is neither a directory nor a zip file")
                }
            }
    }

    private fun importModelEnginePack() {
        if (!PluginUtils.isModelEngineEnabled) return
        val megPack = ModelEngineAPI.getAPI().dataFolder.resolve("resource pack.zip").takeIf(File::exists)
            ?: ModelEngineAPI.getAPI().dataFolder.resolve("resource pack").takeIf(File::exists)
            ?: return Logs.logWarn("Could not find ModelEngine ZIP-resourcepack...")

        runCatching {
            NexoPack.mergePack(resourcePack, NexoPackReader.INSTANCE.readFile(megPack).also { pack ->
                if (VersionUtil.atleast("1.21.4")) pack.models().forEach { packObfuscator.skippedKeys += it.key() }
                if (!Settings.PACK_EXCLUDE_MODEL_ENGINE_SHADERS.toBool()) return@also
                pack.icon(null)
                pack.unknownFiles().keys.filter { "assets/minecraft/shaders/core" in it }.forEach(pack::removeUnknownFile)
                Logs.logInfo("Removed core-shaders from ModelEngine-ResourcePack...")
            })
        }.onFailure {
            Logs.logError("Failed to read ModelEngine-ResourcePack...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            else Logs.logWarn(it.message!!)
        }.onSuccess {
            Logs.logSuccess("Imported ModelEngine pack successfully!")
        }
    }

    private fun parseLanguageFiles() {
        NexoPlugin.instance().configsManager().languages.childSections().forEach { langKey, section ->
            val key = Key.key(langKey.lowercase())
            val language = resourcePack.language(key)?.translations() ?: mutableMapOf()

            language += section.getValues(true).filterValuesInstanceOf<String>()

            resourcePack.language(Language.language(key, language))
        }

        parseGlobalLanguage()
        resourcePack.languages().toList().forEach { language ->
            LinkedHashSet<Map.Entry<String, String>>(language.translations().entries).forEach { (key, value) ->
                language.translations()[key] = when {
                    key in escapeMenuLangKeys -> parseLegacyThroughMiniMessage(value)
                    else -> value
                }
                language.translations().remove("DO_NOT_ALTER_THIS_LINE")
            }
            resourcePack.language(language)
        }
    }
    private val escapeMenuLangKeys = ObjectArrayList.of(
        "menu.game", "menu.disconnect", "menu.feedback", "menu.options", "menu.playerReporting",
        "menu.reportBugs", "menu.returnToGame", "menu.sendFeedback", "gui.stats", "gui.advancements", "modmenu.title"
    )

    private fun parseGlobalLanguage() {
        val globalLanguage = resourcePack.language(Key.key("global")) ?: return
        Logs.logInfo("Converting global lang file to individual language files...")

        VanillaResourcePack.vanillaLangKeys.forEach { langKey ->
            val language = resourcePack.language(langKey) ?: Language.language(langKey, mapOf())
            val newTranslations = LinkedHashMap(language.translations())

            LinkedHashSet<Map.Entry<String, String>>(globalLanguage.translations().entries).forEach { (key, value) ->
                newTranslations.putIfAbsent(key, value)
            }
            resourcePack.language(Language.language(langKey, newTranslations))
        }
        resourcePack.removeLanguage(Key.key("global"))
    }

    init {
        generateDefaultPaths()

        VanillaResourcePack.extractLatest()

        packDownloader.downloadRequiredPack()
        packDownloader.downloadDefaultPack()

        trimsCustomArmor = TrimsCustomArmor().takeIf { setting == CustomArmorType.TRIMS }
        componentCustomArmor = ComponentCustomArmor(resourcePack).takeIf { setting == CustomArmorType.COMPONENT }
        modelGenerator = ModelGenerator(resourcePack)
        atlasGenerator = AtlasGenerator(resourcePack)
        gifGenerator = GifGenerator(resourcePack)
    }

    private fun removeExcludedFileExtensions() {
        Settings.PACK_EXCLUDED_FILE_EXTENSIONS.toStringList().map { it.prependIfMissing(".") }.forEach { extension ->
            if (extension in ignoredExtensions) return@forEach
            resourcePack.unknownFiles().keys.toMutableSet().forEach { key ->
                if (key.endsWith(extension)) resourcePack.removeUnknownFile(key)
            }
        }
    }

    companion object {
        var externalPacks = NexoPlugin.instance().dataFolder.resolve("pack/external_packs")

        private val assetsFolder = NexoPlugin.instance().dataFolder.resolve("pack/assets")
        fun stopPackGeneration() {
            runCatching {
                NexoPackSquash.stopPackSquash()
                NexoPlugin.instance().packGenerator().let {
                    it.packGenFuture?.also { future ->
                        if (future.isDone) return@also
                        future.cancel(true)
                        Logs.logError("Cancelling generation of Nexo ResourcePack...")
                    }
                    it.packGenFuture = null
                }
            }
        }

        private fun generateDefaultPaths() {
            externalPacks.mkdirs()
            assetsFolder.resolve("minecraft", "textures").mkdirs()
            assetsFolder.resolve("minecraft", "models").mkdirs()
            assetsFolder.resolve("minecraft", "sounds").mkdirs()
            assetsFolder.resolve("minecraft", "font").mkdirs()
            assetsFolder.resolve("minecraft", "lang").mkdirs()
            NexoPlugin.instance().resourceManager().extractConfigsInFolder("pack/assets/minecraft/lang", "json")
        }

        private val ignoredExtensions = LinkedHashSet(mutableListOf(".json", ".png", ".mcmeta"))
    }
}
