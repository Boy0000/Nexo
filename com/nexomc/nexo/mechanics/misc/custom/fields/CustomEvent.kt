package com.nexomc.nexo.mechanics.misc.custom.fields

import com.nexomc.nexo.utils.actions.ClickAction

class CustomEvent(action: String, oneUsage: Boolean) {
    val type: CustomEventType
    private val params = mutableListOf<String>()
    val isOneUsage: Boolean

    init {
        val actionParams = action.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        type = CustomEventType.valueOf(actionParams[0])
        params += listOf(*actionParams).subList(1, actionParams.size)
        this.isOneUsage = oneUsage
    }

    fun getParams() = params

    fun getListener(itemID: String?, cooldown: Long, clickAction: ClickAction) =
        type.constructor.create(itemID, cooldown, this, clickAction)

}
