package com.nexomc.nexo.mechanics.trident

import org.bukkit.Location
import org.bukkit.entity.Entity

interface ICustomTridentEntity {

    fun spawn(location: Location): Entity
}