package us.timinc.mc.cobblemon.counter

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.storage.player.PlayerDataExtensionRegistry
import com.cobblemon.mod.common.command.argument.PokemonArgumentType
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.util.getPlayer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import us.timinc.mc.cobblemon.counter.command.*
import us.timinc.mc.cobblemon.counter.config.CounterConfig
import us.timinc.mc.cobblemon.counter.store.CaptureCount
import us.timinc.mc.cobblemon.counter.store.CaptureStreak
import us.timinc.mc.cobblemon.counter.store.KoCount
import us.timinc.mc.cobblemon.counter.store.KoStreak
import java.util.*

object Counter : ModInitializer {
    @Suppress("unused")
    const val MOD_ID = "cobbled_counter"

    private var logger: Logger = LogManager.getLogger(MOD_ID)
    private var config: CounterConfig = CounterConfig.Builder.load()

    override fun onInitialize() {
        PlayerDataExtensionRegistry.register(KoCount.NAME, KoCount::class.java)
        PlayerDataExtensionRegistry.register(KoStreak.NAME, KoStreak::class.java)
        PlayerDataExtensionRegistry.register(CaptureCount.NAME, CaptureCount::class.java)
        PlayerDataExtensionRegistry.register(CaptureStreak.NAME, CaptureStreak::class.java)

        CobblemonEvents.POKEMON_CAPTURED.subscribe { handlePokemonCapture(it) }
        CobblemonEvents.BATTLE_VICTORY.subscribe { handleWildDefeat(it) }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(literal<CommandSourceStack>("counter").then(literal<CommandSourceStack>("ko").then(
                literal<CommandSourceStack>("count").then(argument<CommandSourceStack, Species>(
                    "species", PokemonArgumentType.pokemon()
                ).then(argument<CommandSourceStack, EntitySelector>(
                    "player", EntityArgument.player()
                ).executes { KoCountCommand.withPlayer(it) })
                    .executes { KoCountCommand.withoutPlayer(it) })
            )
                .then(literal<CommandSourceStack>("streak").then(argument<CommandSourceStack?, EntitySelector?>(
                    "player", EntityArgument.player()
                ).executes { KoStreakCommand.withPlayer(it) }).executes { KoStreakCommand.withoutPlayer(it) })
                .then(literal<CommandSourceStack>("total").then(argument<CommandSourceStack?, EntitySelector?>(
                    "player", EntityArgument.player()
                ).executes { KoTotalCommand.withPlayer(it) }).executes { KoTotalCommand.withoutPlayer(it) })
                .then(literal<CommandSourceStack>("reset").requires { source -> source.hasPermission(2) }
                    .then(
                        literal<CommandSourceStack>("count").then(argument<CommandSourceStack?, EntitySelector?>(
                            "player", EntityArgument.player()
                        ).executes { KoResetCommand.resetCount(it) })
                    )
                    .then(
                        literal<CommandSourceStack>("streak").then(argument<CommandSourceStack?, EntitySelector?>(
                            "player", EntityArgument.player()
                        ).executes { KoResetCommand.resetStreak(it) })
                    )
                    .then(
                        literal<CommandSourceStack>("all").then(argument<CommandSourceStack?, EntitySelector?>(
                            "player", EntityArgument.player()
                        ).executes { KoResetCommand.reset(it) })
                    )
                )
            )
                .then(literal<CommandSourceStack>("capture").then(
                    literal<CommandSourceStack>("count").then(argument<CommandSourceStack?, Species?>(
                        "species", PokemonArgumentType.pokemon()
                    ).then(argument<CommandSourceStack?, EntitySelector?>(
                        "player", EntityArgument.player()
                    ).executes { CaptureCountCommand.withPlayer(it) })
                        .executes { CaptureCountCommand.withoutPlayer(it) })
                )
                    .then(literal<CommandSourceStack>("streak").then(argument<CommandSourceStack?, EntitySelector?>(
                        "player", EntityArgument.player()
                    ).executes { CaptureStreakCommand.withPlayer(it) })
                        .executes { CaptureStreakCommand.withoutPlayer(it) })
                    .then(literal<CommandSourceStack>("total").then(argument<CommandSourceStack?, EntitySelector?>(
                        "player", EntityArgument.player()
                    ).executes { CaptureTotalCommand.withPlayer(it) })
                        .executes { CaptureTotalCommand.withoutPlayer(it) })
                    .then(literal<CommandSourceStack>("reset").requires { source -> source.hasPermission(2) }
                        .then(
                            literal<CommandSourceStack>("count").then(argument<CommandSourceStack?, EntitySelector?>(
                                "player", EntityArgument.player()
                            ).executes { CaptureResetCommand.resetCount(it) })
                        )
                        .then(
                            literal<CommandSourceStack>("streak").then(argument<CommandSourceStack?, EntitySelector?>(
                                "player", EntityArgument.player()
                            ).executes { CaptureResetCommand.resetStreak(it) })
                        )
                        .then(
                            literal<CommandSourceStack>("all").then(argument<CommandSourceStack?, EntitySelector?>(
                                "player", EntityArgument.player()
                            ).executes { CaptureResetCommand.reset(it) })
                        )
                    )
                )
            )
        }
    }

    private fun handlePokemonCapture(event: PokemonCapturedEvent) {
        val species = event.pokemon.species.name.lowercase()

        val data = Cobblemon.playerData.get(event.player)

        val captureCount: CaptureCount = data.extraData.getOrPut(CaptureCount.NAME) { CaptureCount() } as CaptureCount
        captureCount.add(species)

        val captureStreak: CaptureStreak =
            data.extraData.getOrPut(CaptureStreak.NAME) { CaptureStreak() } as CaptureStreak
        captureStreak.add(species)

        info(
            "Player ${event.player.displayName.string} captured a $species streak(${captureStreak.count}) count(${
                captureCount.get(
                    species
                )
            })"
        )

        Cobblemon.playerData.saveSingle(data)
    }

    private fun handleWildDefeat(battleVictoryEvent: BattleVictoryEvent) {
        val wildPokemons = battleVictoryEvent.battle.actors.flatMap { it.pokemonList }.map { it.originalPokemon }
            .filter { !it.isPlayerOwned() }

        battleVictoryEvent.winners.flatMap { it.getPlayerUUIDs().mapNotNull(UUID::getPlayer) }.forEach { player ->
            val data = Cobblemon.playerData.get(player)

            val koCount: KoCount = data.extraData.getOrPut(KoCount.NAME) { KoCount() } as KoCount
            val koStreak: KoStreak = data.extraData.getOrPut(KoStreak.NAME) { KoStreak() } as KoStreak

            wildPokemons.forEach { wildPokemon ->
                val species = wildPokemon.species.name.lowercase()

                koCount.add(species)
                koStreak.add(species)

                info(
                    "Player ${player.displayName.string} KO'd a $species streak(${koStreak.count}) count(${
                        koCount.get(
                            species
                        )
                    })"
                )
            }

            Cobblemon.playerData.saveSingle(data)
        }
    }

    fun info(msg: String) {
        if (!config.debug) return
        logger.info(msg)
    }
}

fun LiteralArgumentBuilder<CommandSourceStack>.register(dispatcher: CommandDispatcher<CommandSourceStack>) {
    dispatcher.register(this)
}