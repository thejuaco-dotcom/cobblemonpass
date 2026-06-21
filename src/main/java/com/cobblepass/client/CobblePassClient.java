package com.cobblepass.client;

import com.cobblepass.common.NetworkPackets;
import com.cobblepass.common.PlayerProgress;
import com.cobblepass.common.Quest;
import com.cobblepass.common.Reward;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CobblePassClient implements ClientModInitializer {
    public static List<Quest> quests;
    public static List<Quest> fullQuestsPool;
    public static List<Reward> rewards;
    public static PlayerProgress progress;
    public static String currencyType = "ADMIN_ONLY";
    public static String currencyTarget = "";
    public static int premiumCost = 0;
    public static long seasonStartTime = 0;
    public static long seasonEndTime = 0;
    public static boolean seasonActive = false;

    private static KeyBinding openBattlePassKey;
    private static final Gson GSON = new Gson();

    @Override
    public void onInitializeClient() {
        // 1. Register Client Keybinding
        openBattlePassKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cobblepass.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.cobblepass.keys"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBattlePassKey.wasPressed()) {
                if (client.player != null) {
                    if (quests != null && rewards != null && progress != null) {
                        client.setScreen(new BattlePassScreen());
                    } else {
                        client.player.sendMessage(Text.literal("§cEl pase de batalla aún no se ha sincronizado con el servidor."), false);
                    }
                }
            }
        });

        // 3. Register Network Packet Receiver for Sync
        ClientPlayNetworking.registerGlobalReceiver(NetworkPackets.SyncBattlePassPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                quests = GSON.fromJson(payload.questsJson(), new TypeToken<List<Quest>>() {}.getType());
                rewards = GSON.fromJson(payload.rewardsJson(), new TypeToken<List<Reward>>() {}.getType());
                progress = GSON.fromJson(payload.progressJson(), PlayerProgress.class);
                currencyType = payload.currencyType();
                currencyTarget = payload.currencyTarget();
                premiumCost = payload.premiumCost();
                seasonStartTime = payload.seasonStartTime();
                seasonEndTime = payload.seasonEndTime();
                seasonActive = payload.seasonActive();
            });
        });

        // Sync Full Quests Pool for Admins
        ClientPlayNetworking.registerGlobalReceiver(NetworkPackets.SyncFullQuestsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                fullQuestsPool = GSON.fromJson(payload.fullQuestsJson(), new TypeToken<List<Quest>>() {}.getType());
            });
        });

        // 4. Register Network Packet Receiver for Open GUI
        ClientPlayNetworking.registerGlobalReceiver(NetworkPackets.OpenBattlePassPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (quests != null && rewards != null && progress != null) {
                    context.client().setScreen(new BattlePassScreen());
                } else {
                    if (context.client().player != null) {
                        context.client().player.sendMessage(Text.literal("§cEl pase de batalla aún no se ha sincronizado con el servidor."), false);
                    }
                }
            });
        });
    }

    public static void claimReward(int level, boolean premium) {
        ClientPlayNetworking.send(new NetworkPackets.ClaimRewardPayload(level, premium));
    }

    public static void claimAll() {
        ClientPlayNetworking.send(new NetworkPackets.ClaimAllPayload());
    }

    public static void buyPremium() {
        ClientPlayNetworking.send(new NetworkPackets.BuyPremiumPayload());
    }
}
