package com.nexomc.nexo.utils

import org.apache.commons.lang3.EnumUtils

object EnumUtils {

    fun <E : Enum<E>> String.toEnumOrElse(
        enumClass: Class<E>,
        fallback: (String) -> E
    ): E = EnumUtils.getEnum(enumClass, this.uppercase()) ?: fallback(this)

    inline fun <reified T : Enum<T>> String.toEnumOrElse(fallback: (String) -> T): T {
        return enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) } ?: fallback(this)
    }
}