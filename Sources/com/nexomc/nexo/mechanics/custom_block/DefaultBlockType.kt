package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.mechanics.MechanicFactory

@JvmRecord
data class DefaultBlockType(private val name: String, private val factory: MechanicFactory?) : CustomBlockType {
    override fun name() = name

    override fun factory() = factory
}
