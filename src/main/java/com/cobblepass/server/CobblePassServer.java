package com.cobblepass.server;

import com.cobblepass.common.NetworkPackets;
import com.cobblepass.common.PlayerProgress;
import com.cobblepass.common.Reward;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CobblePassServer implements ModInitializer {
    public static MinecraftServer server = null;

    @Override
    public void onInitialize() {
        // 1. Initialize data (configs and folders)
        DataManager.initialize();

        // Initialize server instance
        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);

        // Reflection check for BattleActor class methods
        try {
            Class<?> clazz = Class.forName("com.cobblemon.mod.common.api.battles.model.actor.BattleActor");
            System.out.println("=== BATTLE ACTOR REFLECTION START ===");
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                System.out.println("  [Method] " + method.getName() + " -> " + method.getReturnType().getName());
            }
            System.out.println("=== BATTLE ACTOR REFLECTION END ===");
        } catch (Exception e) {
            System.out.println("Could not reflect BattleActor: " + e.getMessage());
        }

        // 2. Register Network Payloads
        PayloadTypeRegistry.playS2C().register(NetworkPackets.SyncBattlePassPayload.TYPE, NetworkPackets.SyncBattlePassPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkPackets.SyncFullQuestsPayload.TYPE, NetworkPackets.SyncFullQuestsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkPackets.OpenBattlePassPayload.TYPE, NetworkPackets.OpenBattlePassPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkPackets.ClaimRewardPayload.TYPE, NetworkPackets.ClaimRewardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkPackets.ClaimAllPayload.TYPE, NetworkPackets.ClaimAllPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkPackets.BuyPremiumPayload.TYPE, NetworkPackets.BuyPremiumPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkPackets.SaveQuestsPayload.TYPE, NetworkPackets.SaveQuestsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkPackets.SaveRewardsPayload.TYPE, NetworkPackets.SaveRewardsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkPackets.SaveConfigPayload.TYPE, NetworkPackets.SaveConfigPayload.CODEC);

        // 3. Register Network Packet Receivers
        ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.ClaimRewardPayload.TYPE, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int level = payload.level();
            boolean premium = payload.premium();

            context.server().execute(() -> {
                claimReward(player, level, premium);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.ClaimAllPayload.TYPE, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                claimAllRewards(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.BuyPremiumPayload.TYPE, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                buyPremiumPass(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.SaveQuestsPayload.TYPE, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                context.server().execute(() -> {
                    DataManager.saveQuestsJson(payload.questsJson());
                    DataManager.reloadConfigs(context.server());
                });
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.SaveRewardsPayload.TYPE, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                context.server().execute(() -> {
                    DataManager.saveRewardsJson(payload.rewardsJson());
                    DataManager.reloadConfigs(context.server());
                });
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.SaveConfigPayload.TYPE, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player.hasPermissionLevel(2)) {
                context.server().execute(() -> {
                    DataManager.saveConfigJson(payload.configJson());
                    DataManager.reloadConfigs(context.server());
                });
            }
        });

        // 4. Register Event Listeners
        QuestListener.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            syncPlayerProgress(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            DataManager.unloadPlayer(player.getUuid());
        });

        // 5. Register Admin and Player Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("cobblepass")
                    .requires(source -> source.hasPermissionLevel(2)) // Admin command
                    .then(CommandManager.literal("reload")
                            .executes(context -> {
                                DataManager.reloadConfigs(context.getSource().getServer());
                                context.getSource().sendFeedback(() -> Text.literal("§aConfiguraciones recargadas con éxito."), true);
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("season")
                            .then(CommandManager.literal("start")
                                    .executes(context -> {
                                        DataManager.startSeason();
                                        DataManager.reloadConfigs(context.getSource().getServer());
                                        context.getSource().sendFeedback(() -> Text.literal("§aTemporada iniciada con éxito."), true);
                                        return 1;
                                    })
                            )
                            .then(CommandManager.literal("stop")
                                    .executes(context -> {
                                        DataManager.stopSeason();
                                        DataManager.reloadConfigs(context.getSource().getServer());
                                        context.getSource().sendFeedback(() -> Text.literal("§aTemporada detenida con éxito."), true);
                                        return 1;
                                    })
                            )
                            .then(CommandManager.literal("create")
                                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                int duration = IntegerArgumentType.getInteger(context, "duration");
                                                DataManager.createSeason(context.getSource().getServer(), duration);
                                                context.getSource().sendFeedback(() -> Text.literal("§aNueva temporada creada con duración de " + duration + " días. Todo el progreso fue reiniciado."), true);
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(CommandManager.literal("addxp")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                                DataManager.addXp(target, amount);
                                                context.getSource().sendFeedback(() -> Text.literal("§aSe añadieron " + amount + " XP a " + target.getName().getString()), true);
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(CommandManager.literal("setpremium")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("premium", BoolArgumentType.bool())
                                            .executes(context -> {
                                                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                boolean premium = BoolArgumentType.getBool(context, "premium");
                                                PlayerProgress progress = DataManager.getProgress(target.getUuid());
                                                progress.setPremium(premium);
                                                DataManager.saveProgress(progress);
                                                syncPlayerProgress(target);
                                                context.getSource().sendFeedback(() -> Text.literal("§aPase premium de " + target.getName().getString() + " establecido en " + premium), true);
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(CommandManager.literal("testcraft")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("itemId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                    .executes(context -> {
                                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                        String itemIdStr = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "itemId");
                                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                                        QuestListener.handleCraft(target, Identifier.of(itemIdStr), amount);
                                                        context.getSource().sendFeedback(() -> Text.literal("§aSimulated crafting: " + itemIdStr + " x" + amount + " for " + target.getName().getString()), true);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
            );

            dispatcher.register(CommandManager.literal("battlepass")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            if (ServerPlayNetworking.canSend(player, NetworkPackets.OpenBattlePassPayload.TYPE)) {
                                ServerPlayNetworking.send(player, new NetworkPackets.OpenBattlePassPayload());
                            } else {
                                player.sendMessage(Text.literal("§cNecesitas tener instalado el mod CobblePass en tu cliente para abrir la interfaz del pase de batalla."));
                            }
                        }
                        return 1;
                    })
            );
        });
    }

    public static void syncPlayerProgress(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, NetworkPackets.SyncBattlePassPayload.TYPE)) {
            return;
        }
        String quests = DataManager.getActiveQuestsJsonForPlayer(player);
        String rewards = DataManager.getRewardsJson();
        String progress = DataManager.getProgressJson(player.getUuid());
        DataManager.Config cfg = DataManager.config;

        ServerPlayNetworking.send(player, new NetworkPackets.SyncBattlePassPayload(
                quests, rewards, progress, cfg.currencyType, cfg.currencyTarget, cfg.premiumCost,
                cfg.seasonStartTime, cfg.seasonEndTime, cfg.seasonActive
        ));

        if (player.hasPermissionLevel(2) && ServerPlayNetworking.canSend(player, NetworkPackets.SyncFullQuestsPayload.TYPE)) {
            ServerPlayNetworking.send(player, new NetworkPackets.SyncFullQuestsPayload(DataManager.getQuestsJson()));
        }
    }

    private static void buyPremiumPass(ServerPlayerEntity player) {
        PlayerProgress progress = DataManager.getProgress(player.getUuid());
        if (progress.isPremium()) {
            player.sendMessage(Text.literal("§cYa posees el Pase Premium."));
            return;
        }

        DataManager.Config cfg = DataManager.config;
        boolean success = false;

        if (cfg.currencyType.equalsIgnoreCase("SCOREBOARD")) {
            success = checkAndDeductScoreboard(player, cfg.currencyTarget, cfg.premiumCost);
        } else if (cfg.currencyType.equalsIgnoreCase("ITEM")) {
            success = checkAndDeductItem(player, cfg.currencyTarget, cfg.premiumCost);
        }

        if (success) {
            progress.setPremium(true);
            DataManager.saveProgress(progress);
            syncPlayerProgress(player);
            player.sendMessage(Text.literal("§a§l¡Felicitaciones! Has comprado el Pase Premium con éxito."));
        } else {
            if (cfg.currencyType.equalsIgnoreCase("SCOREBOARD")) {
                player.sendMessage(Text.literal("§cNo tienes suficiente dinero (requiere " + cfg.premiumCost + " en la scoreboard '" + cfg.currencyTarget + "')."));
            } else if (cfg.currencyType.equalsIgnoreCase("ITEM")) {
                String itemName = cfg.currencyTarget;
                if (itemName.contains(":")) {
                    itemName = itemName.substring(itemName.indexOf(":") + 1);
                }
                itemName = itemName.replace("_", " ");
                player.sendMessage(Text.literal("§cNo tienes suficientes ítems en tu inventario (requiere " + cfg.premiumCost + "x " + itemName + ")."));
            } else {
                player.sendMessage(Text.literal("§cEl Pase Premium solo puede ser otorgado por administradores."));
            }
        }
    }

    private static boolean checkAndDeductScoreboard(ServerPlayerEntity player, String objectiveName, int cost) {
        var scoreboard = player.getServer().getScoreboard();
        var objective = scoreboard.getNullableObjective(objectiveName);
        if (objective == null) {
            System.err.println("[CobblePass] Warning: Scoreboard objective '" + objectiveName + "' configured for premium purchases does not exist!");
            return false;
        }

        var score = scoreboard.getScore(player, objective);
        if (score == null || score.getScore() < cost) return false;

        // Deduct
        var mutableScore = scoreboard.getOrCreateScore(player, objective);
        mutableScore.setScore(score.getScore() - cost);
        return true;
    }

    private static boolean checkAndDeductItem(ServerPlayerEntity player, String itemIdStr, int cost) {
        Identifier targetId = Identifier.of(itemIdStr);
        Item targetItem = Registries.ITEM.get(targetId);
        if (targetItem == null) return false;

        int totalCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(targetItem)) {
                totalCount += stack.getCount();
            }
        }

        if (totalCount < cost) return false;

        // Deduct items
        int remaining = cost;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(targetItem)) {
                int count = stack.getCount();
                if (count <= remaining) {
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                    remaining -= count;
                } else {
                    stack.setCount(count - remaining);
                    remaining = 0;
                }
                if (remaining == 0) break;
            }
        }
        player.getInventory().markDirty();
        return true;
    }

    private static void claimReward(ServerPlayerEntity player, int level, boolean premium) {
        PlayerProgress progress = DataManager.getProgress(player.getUuid());

        if (progress.getLevel() < level) {
            player.sendMessage(Text.literal("§cNo has alcanzado el nivel " + level + " todavía."));
            return;
        }

        if (progress.isRewardClaimed(level, premium)) {
            player.sendMessage(Text.literal("§cYa has reclamado esta recompensa."));
            return;
        }

        if (premium && !progress.isPremium()) {
            player.sendMessage(Text.literal("§cNecesitas el Pase Premium para reclamar esta recompensa."));
            return;
        }

        Reward reward = DataManager.getRewards().stream()
                .filter(r -> r.getLevel() == level)
                .findFirst()
                .orElse(null);

        if (reward == null) {
            player.sendMessage(Text.literal("§cEsta recompensa no existe."));
            return;
        }

        Reward.Action action = premium ? reward.getPremiumReward() : reward.getFreeReward();
        if (action == null) {
            player.sendMessage(Text.literal("§cNo hay recompensa configurada para este nivel."));
            return;
        }

        executeRewardAction(player, action);

        progress.setRewardClaimed(level, premium, true);
        DataManager.saveProgress(progress);

        syncPlayerProgress(player);

        player.sendMessage(Text.literal("§a¡Recompensa del nivel " + level + " reclamada!"));
    }

    private static void claimAllRewards(ServerPlayerEntity player) {
        PlayerProgress progress = DataManager.getProgress(player.getUuid());
        int currentLvl = progress.getLevel();
        int claimedCount = 0;

        for (Reward reward : DataManager.getRewards()) {
            int lvl = reward.getLevel();
            if (lvl > currentLvl) continue;

            // Claim Free Reward
            if (reward.getFreeReward() != null && !progress.isRewardClaimed(lvl, false)) {
                executeRewardAction(player, reward.getFreeReward());
                progress.setRewardClaimed(lvl, false, true);
                claimedCount++;
            }

            // Claim Premium Reward
            if (reward.getPremiumReward() != null && progress.isPremium() && !progress.isRewardClaimed(lvl, true)) {
                executeRewardAction(player, reward.getPremiumReward());
                progress.setRewardClaimed(lvl, true, true);
                claimedCount++;
            }
        }

        if (claimedCount > 0) {
            DataManager.saveProgress(progress);
            syncPlayerProgress(player);
            player.sendMessage(Text.literal("§a¡Se han reclamado todas las recompensas pendientes (" + claimedCount + " en total)!"));
        } else {
            player.sendMessage(Text.literal("§cNo tienes recompensas pendientes por reclamar."));
        }
    }

    private static void executeRewardAction(ServerPlayerEntity player, Reward.Action action) {
        switch (action.getType()) {
            case ITEM:
                Identifier itemId = Identifier.of(action.getValue());
                net.minecraft.item.Item item = Registries.ITEM.get(itemId);
                if (item != null) {
                    ItemStack stack = new ItemStack(item, action.getAmount());
                    if (action.getNbt() != null && !action.getNbt().isEmpty()) {
                        try {
                            net.minecraft.nbt.NbtCompound nbt = net.minecraft.nbt.StringNbtReader.parse(action.getNbt());
                            if (!nbt.contains("id")) {
                                nbt.putString("id", action.getValue());
                            }
                            if (!nbt.contains("count")) {
                                nbt.putInt("count", action.getAmount());
                            }
                            ItemStack customStack = ItemStack.fromNbt(player.getRegistryManager(), nbt).orElse(ItemStack.EMPTY);
                            if (!customStack.isEmpty()) {
                                stack = customStack;
                            }
                        } catch (Exception e) {
                            System.err.println("[CobblePass] Failed to parse custom NBT for reward: " + action.getNbt());
                            e.printStackTrace();
                        }
                    }
                    player.getInventory().offerOrDrop(stack);
                }
                break;
            case COMMAND:
                String command = action.getValue().replace("%player%", player.getName().getString());
                player.getServer().getCommandManager().executeWithPrefix(player.getServer().getCommandSource(), command);
                break;
            case POKEMON:
                String pokeCmd = "pokegive " + player.getName().getString() + " " + action.getValue();
                player.getServer().getCommandManager().executeWithPrefix(player.getServer().getCommandSource(), pokeCmd);
                break;
        }
    }
}
