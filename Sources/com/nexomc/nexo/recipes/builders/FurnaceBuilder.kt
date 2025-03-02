package com.nexomc.nexo.recipes.builders

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class FurnaceBuilder(player: Player) : CookingBuilder(player, Component.text("furnace"))
