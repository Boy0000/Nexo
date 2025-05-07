package com.nexomc.nexo.mechanics.furniture.bed

import org.bukkit.Location
import java.util.*

interface IFurnitureBedEntity {

    fun spawn(location: Location): UUID
}