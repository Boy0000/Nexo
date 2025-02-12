package com.nexomc.nexo.converter

import org.bukkit.Location
import org.spongepowered.configurate.ConfigurationNode

internal fun <N : ConfigurationNode, R> N.ifExists(block: (N) -> R): N? = this.takeUnless(ConfigurationNode::virtual)?.also { block(it) }
internal fun <N : ConfigurationNode, R> N.onExists(block: (N) -> R): R? = this.takeUnless(ConfigurationNode::virtual)?.let { block(it) }
internal fun <N : ConfigurationNode> N.renameNode(key: String) = parent()?.node(key)?.mergeFrom(this)?.also { parent()!!.removeChild(this.key()) }
internal fun <N : ConfigurationNode> N.renameNode(node: ConfigurationNode) = node.mergeFrom(this)?.also { parent()!!.removeChild(this.key()) }
internal fun <N : ConfigurationNode> N.removeChildNode(key: Any?) = node(key).copy().also { removeChild(key) }

internal val <N : ConfigurationNode> N.stringList get() = runCatching { getList(String::class.java) }.getOrDefault(listOf())
internal val <N : ConfigurationNode> N.stringListOrNull get() = runCatching { getList(String::class.java).takeUnless { virtual() || it.isNullOrEmpty() } }.getOrNull()

internal fun Location.fineString() = "$x,$y,$z | ${world.name}"
