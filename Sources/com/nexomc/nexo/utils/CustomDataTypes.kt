package com.nexomc.nexo.utils

import com.jeff_media.morepersistentdatatypes.DataType

object CustomDataTypes {
    val UUID_LIST = DataType.asList(DataType.UUID)
    val STRING_LIST = DataType.asList(DataType.STRING)
}