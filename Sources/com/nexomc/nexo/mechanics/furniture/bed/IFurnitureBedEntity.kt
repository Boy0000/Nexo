package com.nexomc.nexo.mechanics.furniture.bed

import org.bukkit.Location

typealias BukkitEntity = org.bukkit.entity.Entity

interface IFurnitureBedEntity {

    fun spawn(location: Location): BukkitEntity
}