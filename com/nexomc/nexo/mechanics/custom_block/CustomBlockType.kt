package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.mechanics.MechanicFactory

interface CustomBlockType {
    fun name(): String
    fun factory(): MechanicFactory?
}


