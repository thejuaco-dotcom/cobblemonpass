package com.cobblepass.server;

import com.cobblepass.common.PlayerProgress;
import com.cobblepass.common.Quest;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.cobblemon.mod.common.api.events.pokemon.TradeEvent;
import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.api.events.pokemon.FossilRevivedEvent;
import com.cobblemon.mod.common.api.events.pokemon.CollectEggEvent;
import com.cobblemon.mod.common.api.events.pokemon.EvGainedEvent;
import com.cobblemon.mod.common.api.events.pokemon.LevelUpEvent;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.evolution.ContextEvolution;
import com.cobblemon.mod.common.pokemon.requirements.FriendshipRequirement;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;

public class QuestListener {

    public static void register() {
        // 1. Cobblemon: Pokemon Captured
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            ServerPlayerEntity player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            if (player != null && pokemon != null) {
                handleCapture(player, pokemon);
            }
            return kotlin.Unit.INSTANCE;
        });

        // 2. Cobblemon: Battle Victory
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            event.getWinners().forEach(winner -> {
                if (CobblePassServer.server != null && winner.getPlayerUUIDs() != null) {
                    for (java.util.UUID playerUuid : winner.getPlayerUUIDs()) {
                        ServerPlayerEntity player = CobblePassServer.server.getPlayerManager().getPlayer(playerUuid);
                        if (player != null) {
                            // Passive XP for winning a battle: +3 XP (action bar)
                            DataManager.addXp(player, 3);
                            player.sendMessage(Text.literal("§a+3 XP (Victoria de Batalla)"), true);
                        }
                    }
                }
            });
            return kotlin.Unit.INSTANCE;
        });

        // 3. Minecraft Vanilla: Block Break
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!DataManager.isSeasonActive()) return;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                handleQuestProgress(serverPlayer, Quest.Type.MINE_BLOCK, blockId.toString(), 1);
                
                // Passive XP for mining ores: +1 XP (action bar)
                String name = blockId.getPath().toLowerCase();
                if (name.contains("ore")) {
                    DataManager.addXp(serverPlayer, 1);
                    serverPlayer.sendMessage(Text.literal("§a+1 XP (Minería)"), true);
                }
            }
        });

        // 4. Cobblemon: Evolution Complete
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            Pokemon pokemon = event.getPokemon();
            if (pokemon != null && pokemon.getOwnerUUID() != null) {
                if (CobblePassServer.server != null) {
                    ServerPlayerEntity player = CobblePassServer.server.getPlayerManager().getPlayer(pokemon.getOwnerUUID());
                    if (player != null) {
                        handleEvolution(player, event);
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        // 5. Cobblemon: Trade Event Post
        CobblemonEvents.TRADE_EVENT_POST.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            if (CobblePassServer.server != null) {
                // Participant 1
                UUID uuid1 = event.getTradeParticipant1().getUuid();
                Pokemon pokemon1 = event.getTradeParticipant1Pokemon();
                ServerPlayerEntity player1 = CobblePassServer.server.getPlayerManager().getPlayer(uuid1);
                if (player1 != null && pokemon1 != null) {
                    handleTrade(player1, pokemon1);
                }

                // Participant 2
                UUID uuid2 = event.getTradeParticipant2().getUuid();
                Pokemon pokemon2 = event.getTradeParticipant2Pokemon();
                ServerPlayerEntity player2 = CobblePassServer.server.getPlayerManager().getPlayer(uuid2);
                if (player2 != null && pokemon2 != null) {
                    handleTrade(player2, pokemon2);
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        // 6. Cobblemon: Hatch Egg Post
        CobblemonEvents.HATCH_EGG_POST.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            ServerPlayerEntity player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            if (player != null && pokemon != null) {
                handleHatch(player, pokemon);
            }
            return kotlin.Unit.INSTANCE;
        });

        // 7. Cobblemon: Battle Fainted
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            BattlePokemon killed = event.getKilled();
            if (killed != null && killed.getOriginalPokemon() != null) {
                BattleActor faintedActor = killed.getActor();
                if (faintedActor != null && faintedActor.getSide() != null) {
                    BattleSide oppositeSide = faintedActor.getSide().getOppositeSide();
                    if (oppositeSide != null && oppositeSide.getActors() != null) {
                        for (BattleActor oppositeActor : oppositeSide.getActors()) {
                            if (oppositeActor != null && oppositeActor.getPlayerUUIDs() != null) {
                                for (java.util.UUID playerUuid : oppositeActor.getPlayerUUIDs()) {
                                    if (CobblePassServer.server != null) {
                                        ServerPlayerEntity player = CobblePassServer.server.getPlayerManager().getPlayer(playerUuid);
                                        if (player != null) {
                                            handleDefeat(player, killed.getOriginalPokemon());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        // 8. Cobblemon: Fossil Revived
        CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            ServerPlayerEntity player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            if (player != null && pokemon != null) {
                handleFossilRevived(player, pokemon);
            }
            return kotlin.Unit.INSTANCE;
        });

        // 9. Cobblemon: Collect Egg
        CobblemonEvents.COLLECT_EGG.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            ServerPlayerEntity player = event.getPlayer();
            Pokemon male = event.getMaleParent();
            Pokemon female = event.getFemaleParent();
            if (player != null) {
                handleCollectEgg(player, male, female);
            }
            return kotlin.Unit.INSTANCE;
        });

        // 10. Cobblemon: EV Gained
        CobblemonEvents.EV_GAINED_EVENT_POST.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            Pokemon pokemon = event.getPokemon();
            if (pokemon != null && pokemon.getOwnerUUID() != null) {
                if (CobblePassServer.server != null) {
                    ServerPlayerEntity player = CobblePassServer.server.getPlayerManager().getPlayer(pokemon.getOwnerUUID());
                    if (player != null) {
                        handleEvGained(player, pokemon);
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        // 11. Cobblemon: Level Up
        CobblemonEvents.LEVEL_UP_EVENT.subscribe(Priority.NORMAL, event -> {
            if (!DataManager.isSeasonActive()) return kotlin.Unit.INSTANCE;
            Pokemon pokemon = event.getPokemon();
            if (pokemon != null && pokemon.getOwnerUUID() != null) {
                if (CobblePassServer.server != null) {
                    ServerPlayerEntity player = CobblePassServer.server.getPlayerManager().getPlayer(pokemon.getOwnerUUID());
                    if (player != null) {
                        handleEvGained(player, pokemon);
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    private static boolean matchesPokemon(Quest quest, Pokemon pokemon) {
        String target = quest.getTarget().toLowerCase();
        if (target.equals("any")) {
            return true;
        }
        String species = pokemon.getSpecies().getName().toLowerCase();
        String primaryType = pokemon.getPrimaryType().getName().toLowerCase();
        String secondaryType = pokemon.getSecondaryType() != null ? pokemon.getSecondaryType().getName().toLowerCase() : "";
        
        if (target.equals(species) || target.equals(primaryType) || target.equals(secondaryType)) {
            return true;
        } else if (target.startsWith("regex:")) {
            String regex = target.substring(6);
            return species.matches(regex) || primaryType.matches(regex) || secondaryType.matches(regex);
        } else if (target.contains("*")) {
            String regex = target.replace(".", "\\.").replace("*", ".*");
            return species.matches(regex) || primaryType.matches(regex) || secondaryType.matches(regex);
        }
        return false;
    }

    private static void handleCapture(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.CAPTURE_POKEMON && matchesPokemon(quest, pokemon)) {
                incrementProgress(player, quest);
            }
        }
        checkPerfectIvs(player, pokemon);

        // Passive XP for capture: +5 XP (action bar)
        DataManager.addXp(player, 5);
        player.sendMessage(Text.literal("§a+5 XP (Captura)"), true);
    }

    private static void handleDefeat(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.DEFEAT_POKEMON && matchesPokemon(quest, pokemon)) {
                incrementProgress(player, quest);
            }
        }
    }

    private static void handleEvolution(ServerPlayerEntity player, EvolutionCompleteEvent event) {
        Pokemon pokemon = event.getPokemon();
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.EVOLVE_POKEMON && matchesPokemon(quest, pokemon)) {
                incrementProgress(player, quest);
            } else if (quest.getType() == Quest.Type.EVOLVE_STONE && matchesPokemon(quest, pokemon)) {
                if (event.getEvolution() instanceof ContextEvolution) {
                    Object context = ((ContextEvolution<?, ?>) event.getEvolution()).getRequiredContext();
                    if (context != null) {
                        String ctxStr = context.toString().toLowerCase();
                        if (ctxStr.contains("stone")) {
                            incrementProgress(player, quest);
                        }
                    }
                }
            } else if (quest.getType() == Quest.Type.EVOLVE_FRIENDSHIP && matchesPokemon(quest, pokemon)) {
                boolean hasFriendshipReq = false;
                for (com.cobblemon.mod.common.api.pokemon.requirement.Requirement req : event.getEvolution().getRequirements()) {
                    if (req instanceof FriendshipRequirement) {
                        hasFriendshipReq = true;
                        break;
                    }
                }
                if (hasFriendshipReq) {
                    incrementProgress(player, quest);
                }
            }
        }

        // Passive XP for evolution: +10 XP (action bar)
        DataManager.addXp(player, 10);
        player.sendMessage(Text.literal("§a+10 XP (Evolución)"), true);
    }

    private static void handleTrade(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.TRADE_POKEMON && matchesPokemon(quest, pokemon)) {
                incrementProgress(player, quest);
            }
        }

        // Passive XP for trading: +10 XP (action bar)
        DataManager.addXp(player, 10);
        player.sendMessage(Text.literal("§a+10 XP (Intercambio)"), true);
    }

    private static void handleHatch(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.HATCH_EGG && matchesPokemon(quest, pokemon)) {
                incrementProgress(player, quest);
            }
        }
        checkPerfectIvs(player, pokemon);

        // Passive XP for hatching: +15 XP (action bar)
        DataManager.addXp(player, 15);
        player.sendMessage(Text.literal("§a+15 XP (Eclosión)"), true);
    }

    private static void handleFossilRevived(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.RESURRECT_FOSSIL && matchesPokemon(quest, pokemon)) {
                incrementProgress(player, quest);
            }
        }
    }

    private static void handleCollectEgg(ServerPlayerEntity player, Pokemon male, Pokemon female) {
        if (male == null || female == null) return;
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.BREED_POKEMON) {
                String targetGroup = quest.getTarget().toLowerCase();
                boolean matches = false;
                if (targetGroup.equals("any")) {
                    matches = true;
                } else {
                    for (EggGroup eg : male.getSpecies().getEggGroups()) {
                        if (eg.name().toLowerCase().equals(targetGroup) || eg.getShowdownID().toLowerCase().equals(targetGroup)) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) {
                        for (EggGroup eg : female.getSpecies().getEggGroups()) {
                            if (eg.name().toLowerCase().equals(targetGroup) || eg.getShowdownID().toLowerCase().equals(targetGroup)) {
                                matches = true;
                                break;
                            }
                        }
                    }
                }
                if (matches) {
                    incrementProgress(player, quest);
                }
            }
        }
    }

    private static void handleEvGained(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.MAX_EV_POKEMON) {
                String statTarget = quest.getTarget().toLowerCase();
                boolean met = false;
                if (statTarget.equals("any")) {
                    for (Stats stat : Stats.values()) {
                        if (pokemon.getEvs().getOrDefault(stat) >= 252) {
                            met = true;
                            break;
                        }
                    }
                } else {
                    for (Stats stat : Stats.values()) {
                        if (stat.name().toLowerCase().equals(statTarget) || stat.getShowdownId().toLowerCase().equals(statTarget)) {
                            if (pokemon.getEvs().getOrDefault(stat) >= 252) {
                                met = true;
                                break;
                            }
                        }
                    }
                }
                if (met) {
                    incrementProgress(player, quest);
                }
            }
        }
    }

    private static void checkPerfectIvs(ServerPlayerEntity player, Pokemon pokemon) {
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        for (Quest quest : activeQuests) {
            if (quest.getType() == Quest.Type.PERFECT_IV_POKEMON) {
                int requiredPerfect = 1;
                try {
                    requiredPerfect = Integer.parseInt(quest.getTarget().trim());
                } catch (NumberFormatException e) {
                    // fallback if not a number
                }

                int perfectCount = 0;
                for (Stats stat : Stats.values()) {
                    if (pokemon.getIvs().getOrDefault(stat) >= 31) {
                        perfectCount++;
                    }
                }
                if (perfectCount >= requiredPerfect) {
                    incrementProgress(player, quest);
                }
            }
        }
    }

    public static void handleCraft(ServerPlayerEntity player, Identifier itemId, int amount) {
        if (!DataManager.isSeasonActive()) return;
        System.out.println("[CobblePass Debug] handleCraft called for: " + itemId + ", amount: " + amount);
        handleQuestProgress(player, Quest.Type.CRAFT_ITEM, itemId.toString(), amount);
        
        // Passive XP for crafting: +1 XP per item crafted (action bar)
        DataManager.addXp(player, amount);
        player.sendMessage(Text.literal("§a+" + amount + " XP (Fabricación)"), true);
    }

    private static void handleQuestProgress(ServerPlayerEntity player, Quest.Type type, String target, int amount) {
        System.out.println("[CobblePass Debug] handleQuestProgress: type=" + type + ", target=" + target + ", amount=" + amount);
        List<Quest> activeQuests = DataManager.getActiveQuestsForPlayer(player);
        System.out.println("[CobblePass Debug] Active quests count for player: " + activeQuests.size());
        for (Quest quest : activeQuests) {
            if (quest.getType() == type) {
                String questTarget = quest.getTarget().toLowerCase();
                boolean matches = false;
                if (questTarget.equals("any")) {
                    matches = true;
                } else if (questTarget.startsWith("regex:")) {
                    String regex = questTarget.substring(6);
                    matches = target.toLowerCase().matches(regex);
                } else if (questTarget.contains("*")) {
                    String regex = questTarget.replace(".", "\\.").replace("*", ".*");
                    matches = target.toLowerCase().matches(regex);
                } else {
                    matches = questTarget.equals(target.toLowerCase());
                }
                
                System.out.println("[CobblePass Debug] Quest: " + quest.getId() + " (" + quest.getTitle() + "), questTarget=" + questTarget + ", matches=" + matches);
                
                if (matches) {
                    for (int i = 0; i < amount; i++) {
                        incrementProgress(player, quest);
                    }
                }
            }
        }
    }

    private static void incrementProgress(ServerPlayerEntity player, Quest quest) {
        PlayerProgress progress = DataManager.getProgress(player.getUuid());
        int current = progress.getQuestProgressCount(quest.getId());
        int required = quest.getRequiredAmount();

        if (current < required) {
            progress.incrementQuestProgress(quest.getId(), 1, required);
            DataManager.saveProgress(progress);

            // Sync with client
            CobblePassServer.syncPlayerProgress(player);

            if (current + 1 >= required) {
                player.sendMessage(Text.literal("§6§l¡Misión Completada! §r" + quest.getTitle() + " (+" + quest.getXpReward() + " XP)"));
                DataManager.addXp(player, quest.getXpReward());
            }
        }
    }
}
