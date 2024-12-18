package com.nexomc.nexo.utils

import org.apache.commons.lang3.EnumUtils

object EnumUtils {

    fun <E : Enum<E>> String.toEnumOrElse(
        enumClass: Class<E>,
        fallback: (String) -> E
    ): E = EnumUtils.getEnum(enumClass, this.uppercase()) ?: fallback(this)

}