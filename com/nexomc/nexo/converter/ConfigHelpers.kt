package com.nexomc.nexo.converter

import org.bukkit.Location
import org.spongepowered.configurate.ConfigurationNode

internal fun <N : ConfigurationNode, R> N.alsoVirtual(block: (N) -> R): N? = this.takeUnless(ConfigurationNode::virtual)?.also { block(it) }
internal fun <N : ConfigurationNode, R> N.letVirtual(block: (N) -> R): R? = this.takeUnless(ConfigurationNode::virtual)?.let { block(it) }
internal fun <N : ConfigurationNode> N.renameNode(key: String) = parent()?.node(key)?.mergeFrom(this)?.also { parent()!!.removeChild(this.key()) }
internal fun <N : ConfigurationNode> N.renameNode(node: ConfigurationNode) = node.mergeFrom(this)?.also { parent()!!.removeChild(this.key()) }
internal fun <N : ConfigurationNode> N.removeChildNode(key: Any?) = node(key).copy().also { removeChild(key) }

internal fun Location.fineString() = "$x,$y,$z | ${world.name}"
