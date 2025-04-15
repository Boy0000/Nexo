package com.nexomc.nexo.mechanics.furniture.bed

import java.util.UUID
import org.bukkit.Location

interface IFurnitureBedEntity {

    fun spawn(location: Location): UUID
}