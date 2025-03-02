package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoPack
import com.nexomc.nexo.api.events.resourcepack.NexoPackUploadEvent
import com.nexomc.nexo.api.events.resourcepack.NexoPostPackGenerateEvent
import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent
import com.nexomc.nexo.compatibilities.modelengine.ModelEngineCompatibility
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.converter.OraxenConverter
import com.nexomc.nexo.fonts.FontManager
import com.nexomc.nexo.fonts.Shift
import com.nexomc.nexo.fonts.ShiftTag
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
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
import com.nexomc.nexo.utils.prependIfMissing
import com.ticxo.modelengine.api.ModelEngineAPI
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import team.unnamed.creative.BuiltResourcePack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.TrueTypeFontProvider
import team.unnamed.creative.lang.Language
import team.unnamed.creative.sound.SoundEvent
import team.unnamed.creative.sound.SoundRegistry
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PackGenerator {
    private val packDownloader: PackDownloader = PackDownloader()
    private var resourcePack = ResourcePack.resourcePack()
    private val packObfuscator: PackObfuscator = PackObfuscator(resourcePack)
    private val packValidator: PackValidator = PackValidator(resourcePack)
    private var builtPack: BuiltResourcePack? = null
    private val trimsCustomArmor: TrimsCustomArmor?
    private val componentCustomArmor: ComponentCustomArmor?
    private val modelGenerator: ModelGenerator
    var packGenFuture: CompletableFuture<Void>? = null
        private set

    fun generatePack() {
        stopPackGeneration()
        NexoPrePackGenerateEvent(resourcePack).call()
        val packFolder = NexoPlugin.instance().dataFolder.resolve("pack")

        val futures = arrayOf(
            packDownloader.downloadRequiredPack(),
            packDownloader.downloadDefaultPack(),
            DefaultResourcePackExtractor.extractLatest(packReader),
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
                        NexoPack.mergePack(resourcePack, packReader.readFile(file))
                    }.onFailure {
                        Logs.logError("Failed to read ${file.path} to a ResourcePack")
                        if (Settings.DEBUG.toBool()) it.printStackTrace()
                        else Logs.logError(it.message!!)
                    }
                }
                Settings.PACK_IMPORT_FROM_URL.toStringList().forEach { url ->
                    runCatching {
                        Logs.logInfo("Importing pack from <aqua>${url}")
                        val pack = URI.create(url).toURL().openStream().use(packReader::readFromInputStream)
                        NexoPack.mergePack(resourcePack, pack)
                    }.onFailure {
                        Logs.logError("Failed to read $url to a ResourcePack")
                        if (Settings.DEBUG.toBool()) it.printStackTrace()
                        else Logs.logError(it.message!!)
                    }
                }

                if (NexoPlugin.instance().converter().oraxenConverter.convertResourcePack)
                    OraxenConverter.processPackFolder(packFolder)

                runCatching {
                    NexoPack.mergePack(resourcePack, packReader.readFile(packFolder))
                }.onFailure {
                    Logs.logError("Failed to read Nexo/pack/assets-folder to a ResourcePack")
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                    else Logs.logError(it.message!!)
                }

                CustomBlockFactory.instance()?.blockStates(resourcePack)
                CustomBlockFactory.instance()?.soundRegistries(resourcePack)
                addItemPackFiles()
                addGlyphFiles()
                addShiftProvider()
                addSoundFile()
                parseLanguageFiles()

                runCatching { SchedulerUtils.callSyncMethod {
                    trimsCustomArmor?.generateTrimAssets(resourcePack)
                }.get(4L, TimeUnit.SECONDS) }.printOnFailure()
                componentCustomArmor?.generatePackFiles(resourcePack)

                handleScoreboardTablist()

                removeExcludedFileExtensions()
                sortModelOverrides()

                runCatching {
                    SchedulerUtils.foliaScheduler.runNextTick {
                        resourcePack = NexoPostPackGenerateEvent(resourcePack).also { it.call() }.resourcePack
                    }.get(4L, TimeUnit.SECONDS)
                }

                packValidator.validatePack()
                if (resourcePack.packMeta() == null) resourcePack.packMeta(NMSHandlers.handler().resourcepackFormat(), "Nexo's default pack.")
                removeStandardItemModels()
                ModernVersionPatcher(resourcePack).patchPack()

                val initialHash = packWriter.build(resourcePack).hash().takeIf {
                    !packObfuscator.obfuscationType.isNone || Settings.PACK_USE_PACKSQUASH.toBool()
                } ?: ""

                resourcePack = packObfuscator.obfuscatePack(initialHash)
                resourcePack = NexoPackSquash.squashPack(resourcePack, initialHash)

                SchedulerUtils.foliaScheduler.runAsync {
                    val packZip = NexoPlugin.instance().dataFolder.resolve("pack", "pack.zip")
                    if (Settings.PACK_GENERATE_ZIP.toBool()) packWriter.writeToZipFile(packZip, resourcePack)
                }
                builtPack = packWriter.build(resourcePack)
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
                if (Settings.PACK_SEND_RELOAD.toBool()) Bukkit.getOnlinePlayers().forEach(packServer::sendPack)
            }
        }
    }

    private fun removeStandardItemModels() {
        // Remove standard ItemModel files as they are no longer needed
        resourcePack.unknownFiles().filterFast { it.key.startsWith("assets/minecraft/items/") }.forEach { (key, writable) ->
            runCatching {
                val itemModelObject = writable.toJsonObject() ?: return@forEach
                if (!ModernVersionPatcher.isStandardItemModel(key, itemModelObject)) return@forEach

                resourcePack.removeUnknownFile(key)
            }.printOnFailure(true)
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
        this.resourcePack = resourcePack
    }

    fun builtPack() = builtPack

    private fun addShiftProvider() {
        resourcePack.fonts().toList().forEach { font ->
            val trueTypes = font.providers().filterFastIsInstance<TrueTypeFontProvider>().mapFast { p ->
                FontProvider.trueType().size(p.size()).shift(p.shift()).file(p.file()).oversample(p.oversample())
                    .skip(p.skip().plus(Shift.fontProvider.advances().keys)).build()
            }

            font.providers(font.providers().filterFast { it !is TrueTypeFontProvider }
                .plus(FontProvider.reference(ShiftTag.FONT))
                .plus(trueTypes)
            ).addTo(resourcePack)
        }
        resourcePack.font(ShiftTag.FONT, Shift.fontProvider)
    }

    private fun addGlyphFiles() {
        val fontGlyphs = LinkedHashMap<Key, MutableList<FontProvider>>()
        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            if (glyph.hasBitmap()) return@forEach
            fontGlyphs.compute(glyph.font()) { _, providers ->
                (providers ?: mutableListOf()).also { it += glyph.fontProvider() }
            }
        }

        FontManager.glyphBitMaps.values.forEach { glyphBitMap ->
            fontGlyphs.compute(glyphBitMap.font) { _, providers: MutableList<FontProvider>? ->
                (providers ?: mutableListOf()).also { it += glyphBitMap.fontProvider() }
            }
        }

        fontGlyphs.forEach { (font, providers) ->
            (resourcePack.font(font)?.toBuilder() ?: Font.font().key(font)).apply {
                providers.forEach(::addProvider)
            }.build().addTo(resourcePack)
        }
    }

    private fun addSoundFile() {
        NexoPlugin.instance().soundManager().customSoundRegistries().forEach { customSoundRegistry ->
            val existingRegistry = resourcePack.soundRegistry(customSoundRegistry.namespace())
            if (existingRegistry == null) customSoundRegistry.addTo(resourcePack)
            else {
                val mergedEvents = LinkedHashSet<SoundEvent>()
                mergedEvents += existingRegistry.sounds()
                mergedEvents += customSoundRegistry.sounds()
                SoundRegistry.soundRegistry().namespace(existingRegistry.namespace()).sounds(mergedEvents).build().addTo(resourcePack)
            }
        }
        JukeboxPlayableDatapack().createDatapack()
    }

    private fun handleScoreboardTablist() {
        if (Settings.HIDE_SCOREBOARD_BACKGROUND.toBool() || Settings.HIDE_TABLIST_BACKGROUND.toBool())
            resourcePack.unknownFile("assets/minecraft/shaders/core/rendertype_gui.vsh", ScoreboardBackground.modernFile())
    }

    private fun importRequiredPack() {
        runCatching {
            val requiredPack = packReader.readFile(externalPacks.listFiles()?.firstOrNull { it.name.startsWith("RequiredPack_") } ?: return)
            if (VersionUtil.atleast("1.21.4")) requiredPack.unknownFiles().keys.filter { it.startsWith("assets/minecraft/items/") }.forEach {
                requiredPack.removeUnknownFile(it)
            }
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
            NexoPack.mergePack(resourcePack, packReader.readFile(defaultPack))
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
                        NexoPack.mergePack(resourcePack, packReader.readFile(it))
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
            NexoPack.mergePack(resourcePack, packReader.readFile(megPack).also { pack ->
                if (VersionUtil.atleast("1.21.4")) packObfuscator.skippedKeys += pack.models().map { it.key() }
                if (!Settings.PACK_EXCLUDE_MODEL_ENGINE_SHADERS.toBool()) return@also
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

    private fun addItemPackFiles() {
        modelGenerator.generateBaseItemModels()
        modelGenerator.generateItemModels()
        AtlasGenerator.generateAtlasFile(resourcePack)
    }

    private fun parseLanguageFiles() {
        parseGlobalLanguage()
        ArrayList(resourcePack.languages()).forEach { language ->
            LinkedHashSet<Map.Entry<String, String>>(language.translations().entries).forEach { (key, value) ->
                language.translations()[key] = parseLegacyThroughMiniMessage(value)
                language.translations().remove("DO_NOT_ALTER_THIS_LINE")
            }
            resourcePack.language(language)
        }
    }

    private fun parseGlobalLanguage() {
        val globalLanguage = resourcePack.language(Key.key("global")) ?: return
        Logs.logInfo("Converting global lang file to individual language files...")

        availableLanguageCodes.forEach { langKey ->
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

        DefaultResourcePackExtractor.extractLatest(packReader)

        packDownloader.downloadRequiredPack()
        packDownloader.downloadDefaultPack()

        trimsCustomArmor = TrimsCustomArmor().takeIf { setting == CustomArmorType.TRIMS }
        componentCustomArmor = ComponentCustomArmor.takeIf { setting == CustomArmorType.COMPONENT }
        modelGenerator = ModelGenerator(resourcePack)
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
        val packReader = NexoPackReader()
        val packWriter = NexoPackWriter()

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

        private val availableLanguageCodes = LinkedHashSet(
            linkedSetOf(
                "af_za", "ar_sa", "ast_es", "az_az", "ba_ru",
                "bar", "be_by", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz",
                "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca",
                "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy",
                "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan",
                "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr",
                "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il",
                "hi_in", "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is",
                "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr",
                "ksh", "kw_gb", "la_la", "lb_lu", "li_li", "lmo", "lol_us", "lt_lt",
                "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de",
                "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pt_br",
                "pt_pt", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "se_no", "sk_sk",
                "sl_si", "so_so", "sq_al", "sr_sp", "sv_se", "sxu", "szl", "ta_in",
                "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "uk_ua", "val_es",
                "vec_it", "vi_vn", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab"
            ).map(Key::key)
        )

        private val ignoredExtensions = LinkedHashSet(mutableListOf(".json", ".png", ".mcmeta"))
    }
}
