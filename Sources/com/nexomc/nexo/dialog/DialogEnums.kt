package com.nexomc.nexo.dialog

enum class DialogTypes {
    CONFIRM, LIST, MULTI, NOTICE, LINK
}

enum class DialogBodyTypes {
    MESSAGE, ITEM
}

enum class DialogInputTypes {
    TEXT, BOOL, NUMBER, SINGLE
}

enum class DialogActionTypes {
    OPEN_URL, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE, CLIPBOARD, SHOW_DIALOG, CUSTOM, DYNAMIC_RUN_COMMAND, DYNAMIC_CUSTOM
}
