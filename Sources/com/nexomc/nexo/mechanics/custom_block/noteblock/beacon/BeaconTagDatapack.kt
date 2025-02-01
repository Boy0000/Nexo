package com.nexomc.nexo.mechanics.custom_block.noteblock.beacon

import com.nexomc.nexo.utils.JsonBuilder
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.printOnFailure
import java.io.File

object BeaconTagDatapack {

    fun writeTagFile(datapackFile: File) {
        runCatching {
            val tagFile = datapackFile.resolve("data/minecraft/tags/blocks/beacon_base_blocks.json")
            tagFile.parentFile.mkdirs()

            val content = JsonBuilder.jsonObject
                .plus("replace", false)
                .plus("values", JsonBuilder.jsonArray.plus("minecraft:note_block"))

            tagFile.writeText(content.toString())
        }.printOnFailure()
    }
}
