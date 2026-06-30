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
                List<Quest> oldQuests = quests;
                PlayerProgress oldProgress = progress;

                quests = GSON.fromJson(payload.questsJson(), new TypeToken<List<Quest>>() {}.getType());
                rewards = GSON.fromJson(payload.rewardsJson(), new TypeToken<List<Reward>>() {}.getType());
                progress = GSON.fromJson(payload.progressJson(), PlayerProgress.class);
                currencyType = payload.currencyType();
                currencyTarget = payload.currencyTarget();
                premiumCost = payload.premiumCost();
                seasonStartTime = payload.seasonStartTime();
                seasonEndTime = payload.seasonEndTime();
                seasonActive = payload.seasonActive();

                // Trigger Toasts if progress increased
                if (oldProgress != null && progress != null && quests != null) {
                    for (Quest q : quests) {
                        int oldVal = oldProgress.getQuestProgressCount(q.getId());
                        int newVal = progress.getQuestProgressCount(q.getId());
                        if (newVal > oldVal && oldVal < q.getRequiredAmount()) {
                            boolean completed = newVal >= q.getRequiredAmount();
                            String title = completed ? "¡Misión Completada!" : "Progreso de Misión";
                            String desc = q.getTitle() + " (" + newVal + "/" + q.getRequiredAmount() + ")";
                            context.client().getToastManager().add(new BattlePassToast(title, desc, completed));
                        }
                    }
                }
            });
        });

        // Sync Full Quests Pool for Admins
        ClientPlayNetworking.registerGlobalReceiver(NetworkPackets.SyncFullQuestsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                fullQuestsPool = GSON.fromJson(payload.fullQuestsJson(), new TypeToken<List<Quest>>() {}.getType());
                if (context.client().currentScreen instanceof com.cobblepass.client.AdminPanelScreen adminScreen) {
                    adminScreen.refreshQuests(fullQuestsPool);
                }
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

        // 5. Register HUD Tracker Overlay
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.currentScreen != null) {
                return;
            }
            if (progress == null || quests == null || progress.getPinnedQuests().isEmpty()) {
                return;
            }

            int cardW = 140;
            int cardH = 22;
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // Calculate scale based on screenHeight to prevent overlapping in reduced windows
            float scale = 1.0f;
            if (screenHeight < 220) {
                scale = 0.6f;
            } else if (screenHeight < 280) {
                scale = 0.75f;
            } else if (screenHeight < 340) {
                scale = 0.85f;
            }

            drawContext.getMatrices().push();
            drawContext.getMatrices().scale(scale, scale, 1.0f);

            float sWidth = screenWidth / scale;
            float sHeight = screenHeight / scale;
            float x = sWidth - cardW - (8 / scale);
            float totalH = progress.getPinnedQuests().size() * (cardH + 4) - 4;
            float y = sHeight - (25 / scale) - totalH;

            for (String qId : progress.getPinnedQuests()) {
                Quest quest = null;
                for (Quest q : quests) {
                    if (q.getId().equals(qId)) {
                        quest = q;
                        break;
                    }
                }
                if (quest == null) continue;

                int current = progress.getQuestProgressCount(qId);
                int required = quest.getRequiredAmount();

                int ix = (int) x;
                int iy = (int) y;

                // Translucent slate background with blue left border
                drawContext.fill(ix, iy, ix + cardW, iy + cardH, 0x88111318);
                drawContext.fill(ix, iy, ix + 2, iy + cardH, 0xFF3B82F6);

                // Progress text
                String progText = current + "/" + required;
                int textW = client.textRenderer.getWidth(progText);

                // Quest Title text (ping-pong marquee scroll if it overflows to prevent overlap)
                String titleText = quest.getTitle();
                int maxTitleW = cardW - textW - 12;
                int titleW = client.textRenderer.getWidth(titleText);
                
                if (titleW > maxTitleW) {
                    int maxScroll = titleW - maxTitleW;
                    int speed = 20; // pixels per second
                    long pauseMs = 1500;
                    long oneWayTimeMs = (maxScroll * 1000L) / speed;
                    long totalCycleMs = (oneWayTimeMs * 2) + (pauseMs * 2);
                    
                    long timeInCycle = System.currentTimeMillis() % totalCycleMs;
                    int scrollOffset = 0;
                    
                    if (timeInCycle < pauseMs) {
                        scrollOffset = 0;
                    } else if (timeInCycle < pauseMs + oneWayTimeMs) {
                        float progress = (float)(timeInCycle - pauseMs) / oneWayTimeMs;
                        scrollOffset = (int)(progress * maxScroll);
                    } else if (timeInCycle < pauseMs * 2 + oneWayTimeMs) {
                        scrollOffset = maxScroll;
                    } else {
                        float progress = (float)(timeInCycle - (pauseMs * 2 + oneWayTimeMs)) / oneWayTimeMs;
                        scrollOffset = maxScroll - (int)(progress * maxScroll);
                    }
                    
                    // Enable scissor box using scale to smoothly clip text
                    drawContext.enableScissor(
                        (int) ((ix + 6) * scale),
                        (int) (iy * scale),
                        (int) ((ix + 6 + maxTitleW) * scale),
                        (int) ((iy + cardH) * scale)
                    );
                    drawContext.drawText(client.textRenderer, titleText, ix + 6 - scrollOffset, iy + 3, 0xFFFFFFFF, true);
                    drawContext.disableScissor();
                } else {
                    drawContext.drawText(client.textRenderer, titleText, ix + 6, iy + 3, 0xFFFFFFFF, true);
                }

                // Draw progress text
                drawContext.drawText(client.textRenderer, progText, ix + cardW - textW - 6, iy + 3, 0xFF4ADE80, true);

                // Progress bar
                int barW = cardW - 12;
                int barH = 2;
                int barX = ix + 6;
                int barY = iy + 14;
                drawContext.fill(barX, barY, barX + barW, barY + barH, 0x44FFFFFF);

                float pct = Math.min(1.0f, (float) current / required);
                int fillW = (int) (barW * pct);
                if (fillW > 0) {
                    drawContext.fill(barX, barY, barX + fillW, barY + barH, 0xFF4ADE80);
                }

                y += cardH + 4;
            }

            drawContext.getMatrices().pop();
        });
    }

    public static void togglePinQuest(String questId) {
        ClientPlayNetworking.send(new NetworkPackets.TogglePinQuestPayload(questId));
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
