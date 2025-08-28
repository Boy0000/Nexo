package com.nexomc.nexo.recipes

enum class RecipeType(val id: String) {
    SHAPED("shaped"), SHAPELESS("shapeless"),
    FURNACE("furnace"), BLASTING("blasting"), SMOKER("smoking"),
    BREWING("brewing"), STONECUTTING("stonecutting"), CAMPFIRE("campfire")
}