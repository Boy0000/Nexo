package com.nexomc.nexo.utils

import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key

object KeyUtils {
    fun Key.removeSuffix(suffix: String) = Key.key(asString().removeSuffix(suffix))
    fun Key.appendSuffix(suffix: String) = Key.key(asString().appendIfMissing(suffix))

    @JvmField
    val MALFORMED_KEY_PLACEHOLDER = Key.key("item/barrier")

    @JvmStatic
    fun dropExtension(key: Key) = dropExtension(key.asString())

    @JvmStatic
    fun dropExtension(key: String): Key {
        if (!Key.parseable(key)) return MALFORMED_KEY_PLACEHOLDER
        val i = key.lastIndexOf(".")
        return if (i == -1) Key.key(key)
        else Key.key(key.substring(0, i))
    }

    private val KEY_REGEX = "[^a-z0-9._/-]".toRegex()

    @JvmStatic
    fun parseKey(key: String): Key {
        return parseKey(key.substringBefore(":", "minecraft"), key.substringAfter(":"))
    }

    @JvmStatic
    fun parseKey(namespace: String, key: String, prefix: String? = null): Key {
        var (namespace, key) = namespace to key
        val (oldNamespace, oldKey) = namespace to key

        if (!Key.parseable("$namespace:$key")) {
            namespace = namespace.lowercase().replace(KEY_REGEX, "_")
            key = key.lowercase().replace(KEY_REGEX, "_")

            Logs.logError("Invalid $prefix-key: $oldNamespace:$oldKey")
            Logs.logWarn("Keys must be all lower-case, without spaces and most special characters")
            Logs.logWarn("Example: $oldNamespace:$oldKey -> $namespace:$key")
        }

        return Key.key(namespace, key)
    }
}
