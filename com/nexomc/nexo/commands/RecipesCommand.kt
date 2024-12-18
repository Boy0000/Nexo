package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.recipes.builders.*
import com.nexomc.nexo.recipes.builders.RecipeBuilder.Companion.get
import com.nexomc.nexo.recipes.listeners.RecipeEventManager
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

internal fun CommandTree.recipesCommand() = literalArgument("recipes") {
    withPermission("nexo.command.recipes")
    literalArgument("show") {
        withPermission("nexo.command.recipes.show")
        textArgument("type") {
            replaceSuggestions(ArgumentSuggestions.stringsAsync {
                CompletableFuture.supplyAsync { RecipeEventManager.instance().permittedRecipeNames(it.sender) }
            })
            anyExecutor { sender, args ->
                if (sender is Player) {
                    var recipes = RecipeEventManager.instance().permittedRecipes(sender)
                    val param = args.get("type") as String
                    if ("all" != param) recipes = recipes.filter { it.name == param }
                    if (recipes.isEmpty()) {
                        Message.RECIPE_NO_RECIPE.send(sender)
                        return@anyExecutor
                    }
                    NexoPlugin.instance().invManager().recipesShowcase(0, recipes).show(sender)
                } else Message.NOT_PLAYER.send(sender)
            }
        }
    }
    literalArgument("builder") {
        withPermission("nexo.command.recipes.builder")
        literalArgument("shaped") {
            anyExecutor { sender, _ ->
                if (sender is Player) (get(sender.uniqueId) ?: ShapedBuilder(sender)).open()
                else Message.NOT_PLAYER.send(sender)
            }
        }
        literalArgument("shapeless") {
            anyExecutor { sender, _ ->
                if (sender is Player) (get(sender.uniqueId) ?: ShapelessBuilder(sender)).open()
                else Message.NOT_PLAYER.send(sender)
            }
        }
        literalArgument("furnace") {
            integerArgument("cookingtime") {
                integerArgument("experience", optional = true) {
                    anyExecutor { sender, arguments ->
                        if (sender is Player) {
                            val recipe = get(sender.uniqueId) ?: FurnaceBuilder(sender)
                            if (recipe is FurnaceBuilder) {
                                recipe.setCookingTime(arguments.get("cookingtime") as Int)
                                recipe.setExperience(arguments.getOptionalByClass("experience", Int::class.java).orElse(0))
                            }
                            recipe.open()
                        } else Message.NOT_PLAYER.send(sender)
                    }
                }
            }
        }
        literalArgument("blasting") {
            integerArgument("cookingtime") {
                integerArgument("experience", optional = true) {
                    anyExecutor { sender, arguments ->
                        if (sender is Player) {
                            val recipe = get(sender.uniqueId) ?: BlastingBuilder(sender)
                            if (recipe is BlastingBuilder) {
                                recipe.setCookingTime(arguments.get("cookingtime") as Int)
                                recipe.setExperience(arguments.getOptionalByClass("experience", Int::class.java).orElse(0))
                            }
                            recipe.open()
                        } else Message.NOT_PLAYER.send(sender)
                    }
                }
            }
        }
        literalArgument("smoking") {
            integerArgument("cookingtime") {
                integerArgument("experience", optional = true) {
                    anyExecutor { sender, arguments ->
                        if (sender is Player) {
                            val recipe = get(sender.uniqueId) ?: SmokingBuilder(sender)
                            if (recipe is SmokingBuilder) {
                                recipe.setCookingTime(arguments.get("cookingtime") as Int)
                                recipe.setExperience(arguments.getOptionalByClass("experience", Int::class.java).orElse(0))
                            }
                            recipe.open()
                        } else Message.NOT_PLAYER.send(sender)
                    }
                }
            }
        }
        literalArgument("stonecutting") {
            anyExecutor { sender, _ ->
                if (sender is Player) {
                    (get(sender.uniqueId) ?: StonecuttingBuilder(sender)).open()
                } else Message.NOT_PLAYER.send(sender)
            }
        }
        anyExecutor { sender, args ->
            if (sender is Player) get(sender.uniqueId)?.open() ?: Message.RECIPE_NO_BUILDER.send(sender)
            else Message.NOT_PLAYER.send(sender)
        }
    }

    literalArgument("save") {
        withPermission("nexo.command.recipes.save")
        textArgument("name") {
            stringArgument("permission", optional = true) {
                anyExecutor { sender, args ->
                    if (sender is Player) {
                        val recipe = get(sender.uniqueId) ?: return@anyExecutor Message.RECIPE_NO_BUILDER.send(sender)
                        val name = args.args().first() as String
                        val permission = args.getOptional("permission").orElse("") as String
                        recipe.saveRecipe(name, permission)
                        Message.RECIPE_SAVE.send(sender, tagResolver("name", name))
                    } else Message.NOT_PLAYER.send(sender)
                }
            }
        }

    }

}
