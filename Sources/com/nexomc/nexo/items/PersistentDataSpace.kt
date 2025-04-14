package com.nexomc.nexo.items

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

@JvmRecord
data class PersistentDataSpace<T, Z>(val key: NamespacedKey, val dataType: PersistentDataType<T, Z>)
