package com.cobblepass.client;

import com.cobblepass.common.NetworkPackets;
import com.cobblepass.common.Quest;
import com.cobblepass.common.Reward;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelScreen extends Screen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Screen parent;
    private String activeTab = "season"; // "season", "quests", "rewards"
    private final int panelWidth = 400;
    private final int panelHeight = 220;
    private int startX;
    private int startY;

    // Config Fields (Season Tab)
    private TextFieldWidget seasonDurationField;
    private TextFieldWidget premiumCostField;
    private TextFieldWidget currencyTypeField;
    private TextFieldWidget currencyTargetField;

    // Quest Fields & Selection
    private List<Quest> localQuests = new ArrayList<>();
    private int selectedQuestIndex = -1;
    private int questScrollIndex = 0;
    private TextFieldWidget questTitleField;
    private TextFieldWidget questDescField;
    private TextFieldWidget questTypeField;
    private TextFieldWidget questTargetField;
    private TextFieldWidget questReqField;
    private TextFieldWidget questXpField;
    private TextFieldWidget questCatField;

    // Reward Fields & Selection
    private List<Reward> localRewards = new ArrayList<>();
    private int selectedRewardLevel = 1;
    private int rewardScrollIndex = 0;
    private TextFieldWidget levelXpField;
    private TextFieldWidget freeTypeField;
    private TextFieldWidget freeValField;
    private TextFieldWidget freeAmtField;
    private TextFieldWidget freeTempField;
    private TextFieldWidget premTypeField;
    private TextFieldWidget premValField;
    private TextFieldWidget premAmtField;
    private TextFieldWidget premTempField;

    public AdminPanelScreen(Screen parent) {
        super(Text.literal("CobblePass Admin Panel"));
        this.parent = parent;

        // Clone quests and rewards locally so modifications aren't permanent until saved
        if (CobblePassClient.quests != null) {
            for (Quest q : CobblePassClient.quests) {
                this.localQuests.add(new Quest(q.getId(), q.getTitle(), q.getDescription(), q.getType(), q.getTarget(), q.getRequiredAmount(), q.getXpReward(), q.getCategory()));
            }
        }
        if (CobblePassClient.rewards != null) {
            for (Reward r : CobblePassClient.rewards) {
                // deep clone reward
                Reward.Action freeClone = r.getFreeReward() != null ? new Reward.Action(r.getFreeReward().getType(), r.getFreeReward().getValue(), r.getFreeReward().getAmount(), r.getFreeReward().getNbt()) : null;
                if (freeClone != null && r.getFreeReward().getTemplateRef() != null) {
                    freeClone.setTemplateRef(r.getFreeReward().getTemplateRef());
                }
                Reward.Action premClone = r.getPremiumReward() != null ? new Reward.Action(r.getPremiumReward().getType(), r.getPremiumReward().getValue(), r.getPremiumReward().getAmount(), r.getPremiumReward().getNbt()) : null;
                if (premClone != null && r.getPremiumReward().getTemplateRef() != null) {
                    premClone.setTemplateRef(r.getPremiumReward().getTemplateRef());
                }
                this.localRewards.add(new Reward(r.getLevel(), r.getXpRequired(), freeClone, premClone));
            }
        }
    }

    @Override
    protected void init() {
        this.startX = (this.width - this.panelWidth) / 2;
        this.startY = (this.height - this.panelHeight) / 2;

        clearAndInitWidgets();
    }

    private void clearAndInitWidgets() {
        this.clearChildren();

        if (activeTab.equals("season")) {
            seasonDurationField = new TextFieldWidget(this.textRenderer, startX + 130, startY + 50, 60, 12, Text.literal("Duration"));
            seasonDurationField.setText(String.valueOf(CobblePassClient.premiumCost > 0 || CobblePassClient.quests != null ? getLocalSeasonDuration() : 60));
            seasonDurationField.setMaxLength(5);
            this.addDrawableChild(seasonDurationField);

            premiumCostField = new TextFieldWidget(this.textRenderer, startX + 130, startY + 70, 60, 12, Text.literal("Premium Cost"));
            premiumCostField.setText(String.valueOf(CobblePassClient.premiumCost));
            premiumCostField.setMaxLength(8);
            this.addDrawableChild(premiumCostField);

            currencyTypeField = new TextFieldWidget(this.textRenderer, startX + 130, startY + 90, 80, 12, Text.literal("Currency Type"));
            currencyTypeField.setText(CobblePassClient.currencyType);
            currencyTypeField.setMaxLength(32);
            this.addDrawableChild(currencyTypeField);

            currencyTargetField = new TextFieldWidget(this.textRenderer, startX + 130, startY + 110, 120, 12, Text.literal("Currency Target"));
            currencyTargetField.setText(CobblePassClient.currencyTarget);
            currencyTargetField.setMaxLength(128);
            this.addDrawableChild(currencyTargetField);
        } else if (activeTab.equals("quests")) {
            if (selectedQuestIndex >= 0 && selectedQuestIndex < localQuests.size()) {
                Quest q = localQuests.get(selectedQuestIndex);

                questTitleField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 50, 140, 12, Text.literal("Title"));
                questTitleField.setText(q.getTitle());
                questTitleField.setMaxLength(64);
                this.addDrawableChild(questTitleField);

                questDescField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 70, 140, 12, Text.literal("Description"));
                questDescField.setText(q.getDescription());
                questDescField.setMaxLength(128);
                this.addDrawableChild(questDescField);

                questTypeField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 90, 140, 12, Text.literal("Type"));
                questTypeField.setText(q.getType().name());
                questTypeField.setMaxLength(32);
                this.addDrawableChild(questTypeField);

                questTargetField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 110, 140, 12, Text.literal("Target"));
                questTargetField.setText(q.getTarget());
                questTargetField.setMaxLength(128);
                this.addDrawableChild(questTargetField);

                questReqField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 130, 60, 12, Text.literal("Required Amount"));
                questReqField.setText(String.valueOf(q.getRequiredAmount()));
                questReqField.setMaxLength(8);
                this.addDrawableChild(questReqField);

                questXpField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 150, 60, 12, Text.literal("XP Reward"));
                questXpField.setText(String.valueOf(q.getXpReward()));
                questXpField.setMaxLength(8);
                this.addDrawableChild(questXpField);

                questCatField = new TextFieldWidget(this.textRenderer, startX + 245, startY + 170, 60, 12, Text.literal("Category"));
                questCatField.setText(q.getCategory());
                questCatField.setMaxLength(16);
                this.addDrawableChild(questCatField);
            }
        } else if (activeTab.equals("rewards")) {
            Reward r = localRewards.stream().filter(x -> x.getLevel() == selectedRewardLevel).findFirst().orElse(null);
            if (r != null) {
                levelXpField = new TextFieldWidget(this.textRenderer, startX + 185, startY + 50, 50, 12, Text.literal("Level XP"));
                levelXpField.setText(String.valueOf(r.getXpRequired()));
                levelXpField.setMaxLength(8);
                this.addDrawableChild(levelXpField);

                Reward.Action free = r.getFreeReward();
                freeTypeField = new TextFieldWidget(this.textRenderer, startX + 135, startY + 80, 80, 12, Text.literal("Free Type"));
                freeTypeField.setText(free != null ? free.getType().name() : "");
                freeTypeField.setMaxLength(16);
                this.addDrawableChild(freeTypeField);

                freeValField = new TextFieldWidget(this.textRenderer, startX + 135, startY + 100, 80, 12, Text.literal("Free Value"));
                freeValField.setText(free != null ? free.getValue() : "");
                freeValField.setMaxLength(256);
                this.addDrawableChild(freeValField);

                freeAmtField = new TextFieldWidget(this.textRenderer, startX + 135, startY + 120, 35, 12, Text.literal("Free Amount"));
                freeAmtField.setText(free != null ? String.valueOf(free.getAmount()) : "0");
                freeAmtField.setMaxLength(8);
                this.addDrawableChild(freeAmtField);

                freeTempField = new TextFieldWidget(this.textRenderer, startX + 135, startY + 140, 80, 12, Text.literal("Free Template"));
                freeTempField.setText(free != null && free.getTemplateRef() != null ? free.getTemplateRef() : "");
                freeTempField.setMaxLength(64);
                this.addDrawableChild(freeTempField);

                Reward.Action prem = r.getPremiumReward();
                premTypeField = new TextFieldWidget(this.textRenderer, startX + 270, startY + 80, 110, 12, Text.literal("Prem Type"));
                premTypeField.setText(prem != null ? prem.getType().name() : "");
                premTypeField.setMaxLength(16);
                this.addDrawableChild(premTypeField);

                premValField = new TextFieldWidget(this.textRenderer, startX + 270, startY + 100, 110, 12, Text.literal("Prem Value"));
                premValField.setText(prem != null ? prem.getValue() : "");
                premValField.setMaxLength(256);
                this.addDrawableChild(premValField);

                premAmtField = new TextFieldWidget(this.textRenderer, startX + 270, startY + 120, 35, 12, Text.literal("Prem Amount"));
                premAmtField.setText(prem != null ? String.valueOf(prem.getAmount()) : "0");
                premAmtField.setMaxLength(8);
                this.addDrawableChild(premAmtField);

                premTempField = new TextFieldWidget(this.textRenderer, startX + 270, startY + 140, 110, 12, Text.literal("Prem Template"));
                premTempField.setText(prem != null && prem.getTemplateRef() != null ? prem.getTemplateRef() : "");
                premTempField.setMaxLength(64);
                this.addDrawableChild(premTempField);
            }
        }
    }

    private int getLocalSeasonDuration() {
        return 60;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 1. Draw outer panel background (Slate dark)
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, 0xF5111318);

        // 2. Draw Header Banner (Pokéball red accent)
        context.fill(startX, startY, startX + panelWidth, startY + 42, 0xFF991B1B);

        // Title text
        context.drawText(this.textRenderer, "⚙ COBBLEPASS - ADMIN PANEL", startX + 15, startY + 10, 0xFFFFFFFF, true);

        // Back button (< Volver)
        int backX = startX + panelWidth - 70;
        int backY = startY + 10;
        boolean hoverBack = mouseX >= backX && mouseX <= backX + 55 && mouseY >= backY && mouseY <= backY + 12;
        context.drawText(this.textRenderer, "< Volver", backX, backY, hoverBack ? 0xFFFDE047 : 0xFFE2E8F0, false);

        // Sub tabs
        int tabY = startY + 28;
        int tabW = 75;
        int tabH = 14;

        // Tab Season
        int tabSeasonX = startX + 15;
        boolean hoverSeason = mouseX >= tabSeasonX && mouseX <= tabSeasonX + tabW && mouseY >= tabY && mouseY <= tabY + tabH;
        int seasonCol = activeTab.equals("season") ? 0xFF1E293B : (hoverSeason ? 0xAA1E293B : 0x44FFFFFF);
        context.fill(tabSeasonX, tabY, tabSeasonX + tabW, tabY + tabH, seasonCol);
        context.drawText(this.textRenderer, "Temporada", tabSeasonX + 10, tabY + 3, 0xFFFFFFFF, false);

        // Tab Quests
        int tabQuestsX = tabSeasonX + tabW + 5;
        boolean hoverQuests = mouseX >= tabQuestsX && mouseX <= tabQuestsX + tabW && mouseY >= tabY && mouseY <= tabY + tabH;
        int questsCol = activeTab.equals("quests") ? 0xFF1E293B : (hoverQuests ? 0xAA1E293B : 0x44FFFFFF);
        context.fill(tabQuestsX, tabY, tabQuestsX + tabW, tabY + tabH, questsCol);
        context.drawText(this.textRenderer, "Misiones", tabQuestsX + 14, tabY + 3, 0xFFFFFFFF, false);

        // Tab Rewards
        int tabRewardsX = tabQuestsX + tabW + 5;
        boolean hoverRewards = mouseX >= tabRewardsX && mouseX <= tabRewardsX + tabW && mouseY >= tabY && mouseY <= tabY + tabH;
        int rewardsCol = activeTab.equals("rewards") ? 0xFF1E293B : (hoverRewards ? 0xAA1E293B : 0x44FFFFFF);
        context.fill(tabRewardsX, tabY, tabRewardsX + tabW, tabY + tabH, rewardsCol);
        context.drawText(this.textRenderer, "Premios", tabRewardsX + 16, tabY + 3, 0xFFFFFFFF, false);

        // Draw Tab Contents
        if (activeTab.equals("season")) {
            renderSeasonTab(context, mouseX, mouseY);
        } else if (activeTab.equals("quests")) {
            renderQuestsTab(context, mouseX, mouseY);
        } else if (activeTab.equals("rewards")) {
            renderRewardsTab(context, mouseX, mouseY);
        }
    }

    private void renderSeasonTab(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, "Duración (días):", startX + 15, startY + 52, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Costo Premium:", startX + 15, startY + 72, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Moneda Tipo:", startX + 15, startY + 92, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Moneda Target:", startX + 15, startY + 112, 0xFF9CA3AF, false);

        // Draw helper text
        context.drawText(this.textRenderer, "Tipos: ITEM, SCOREBOARD, ADMIN_ONLY", startX + 15, startY + 130, 0xFF6B7280, false);

        // Season Commands Buttons
        int btnW = 85;
        int btnH = 14;
        int btnY = startY + 155;

        // Iniciar
        int btnStartX = startX + 15;
        boolean hoverStart = mouseX >= btnStartX && mouseX <= btnStartX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(btnStartX, btnY, btnStartX + btnW, btnY + btnH, hoverStart ? 0xFF15803D : 0xFF166534);
        context.drawText(this.textRenderer, "Iniciar Temp.", btnStartX + 10, btnY + 3, 0xFFFFFFFF, false);

        // Detener
        int btnStopX = btnStartX + btnW + 5;
        boolean hoverStop = mouseX >= btnStopX && mouseX <= btnStopX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(btnStopX, btnY, btnStopX + btnW, btnY + btnH, hoverStop ? 0xFFB91C1C : 0xFF991B1B);
        context.drawText(this.textRenderer, "Detener Temp.", btnStopX + 10, btnY + 3, 0xFFFFFFFF, false);

        // Nueva Temp.
        int btnNewX = btnStopX + btnW + 5;
        boolean hoverNew = mouseX >= btnNewX && mouseX <= btnNewX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(btnNewX, btnY, btnNewX + btnW, btnY + btnH, hoverNew ? 0xFFD97706 : 0xFFB45309);
        context.drawText(this.textRenderer, "Reiniciar Pase", btnNewX + 10, btnY + 3, 0xFFFFFFFF, false);

        // Guardar Ajustes Config
        int saveW = 100;
        int saveX = startX + panelWidth - 115;
        int saveY = startY + 185;
        boolean hoverSave = mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= saveY && mouseY <= saveY + 14;
        context.fill(saveX, saveY, saveX + saveW, saveY + 14, hoverSave ? 0xFF2563EB : 0xFF1D4ED8);
        context.drawText(this.textRenderer, "Guardar Config", saveX + 12, saveY + 3, 0xFFFFFFFF, false);
    }

    private void renderQuestsTab(DrawContext context, int mouseX, int mouseY) {
        // Left Side: scrollable list of quests (7 visible items)
        int listX = startX + 15;
        int listY = startY + 50;
        int listW = 190;
        int listH = 133; // 7 * 19 px
        int rowH = 19;

        context.fill(listX, listY, listX + listW, listY + listH, 0xFF111318);

        // Draw Quest items (7 visible)
        for (int i = 0; i < 7; i++) {
            int idx = questScrollIndex + i;
            if (idx >= localQuests.size()) break;

            Quest q = localQuests.get(idx);
            int rowY = listY + (i * rowH);
            boolean isSelected = (idx == selectedQuestIndex);

            context.fill(listX, rowY, listX + listW - 14, rowY + rowH - 1, isSelected ? 0xFF1E293B : 0xFF1E2026);
            context.fill(listX, rowY, listX + 2, rowY + rowH - 1, q.getCategory().equalsIgnoreCase("DAILY") ? 0xFF3B82F6 : (q.getCategory().equalsIgnoreCase("WEEKLY") ? 0xFF10B981 : 0xFFF59E0B));

            String title = q.getTitle();
            if (this.textRenderer.getWidth(title) > listW - 25) {
                title = this.textRenderer.trimToWidth(title, listW - 35) + "...";
            }
            context.drawText(this.textRenderer, title, listX + 6, rowY + 2, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, q.getId(), listX + 6, rowY + 11, 0xFF6B7280, false);
        }

        // Dedicated Scroll Track on the right of the list
        int scrollTrackX = listX + listW - 12;
        int scrollTrackW = 10;
        context.fill(scrollTrackX, listY, scrollTrackX + scrollTrackW, listY + listH, 0xFF0B0C0E);

        // Up arrow "▲"
        int upY = listY + 2;
        boolean hoverUp = mouseX >= scrollTrackX && mouseX <= scrollTrackX + scrollTrackW && mouseY >= upY && mouseY <= upY + 10;
        context.drawText(this.textRenderer, "▲", scrollTrackX + 2, upY, hoverUp ? 0xFFFDE047 : 0xFF9CA3AF, false);

        // Down arrow "▼"
        int downY = listY + listH - 10;
        boolean hoverDown = mouseX >= scrollTrackX && mouseX <= scrollTrackX + scrollTrackW && mouseY >= downY && mouseY <= downY + 10;
        context.drawText(this.textRenderer, "▼", scrollTrackX + 2, downY, hoverDown ? 0xFFFDE047 : 0xFF9CA3AF, false);

        // Scrollbar thumb
        int trackH = listH - 24;
        if (localQuests.size() > 7) {
            int thumbH = Math.max(10, trackH * 7 / localQuests.size());
            int thumbY = listY + 12 + (trackH - thumbH) * questScrollIndex / (localQuests.size() - 7);
            context.fill(scrollTrackX + 1, thumbY, scrollTrackX + scrollTrackW - 1, thumbY + thumbH, 0xFF475569);
        } else {
            context.fill(scrollTrackX + 1, listY + 12, scrollTrackX + scrollTrackW - 1, listY + 12 + trackH, 0xFF334155);
        }

        // Edit Labels on right
        if (selectedQuestIndex >= 0 && selectedQuestIndex < localQuests.size()) {
            int editX = startX + 215;
            context.drawText(this.textRenderer, "Tít:", editX, startY + 52, 0xFF9CA3AF, false);
            context.drawText(this.textRenderer, "Des:", editX, startY + 72, 0xFF9CA3AF, false);
            context.drawText(this.textRenderer, "Tip:", editX, startY + 92, 0xFF9CA3AF, false);
            context.drawText(this.textRenderer, "Tar:", editX, startY + 112, 0xFF9CA3AF, false);
            context.drawText(this.textRenderer, "Req:", editX, startY + 132, 0xFF9CA3AF, false);
            context.drawText(this.textRenderer, "XP:", editX, startY + 152, 0xFF9CA3AF, false);
            context.drawText(this.textRenderer, "Cat:", editX, startY + 172, 0xFF9CA3AF, false);

            // Save/Delete Quest item buttons (moved down to Y + 195)
            int qSaveW = 75;
            int qSaveX = startX + 215;
            int qSaveY = startY + 195;
            boolean hoverQSave = mouseX >= qSaveX && mouseX <= qSaveX + qSaveW && mouseY >= qSaveY && mouseY <= qSaveY + 14;
            context.fill(qSaveX, qSaveY, qSaveX + qSaveW, qSaveY + 14, hoverQSave ? 0xFF15803D : 0xFF166534);
            context.drawText(this.textRenderer, "Aplicar Misión", qSaveX + 6, qSaveY + 3, 0xFFFFFFFF, false);

            int qDelW = 50;
            int qDelX = qSaveX + qSaveW + 5;
            boolean hoverQDel = mouseX >= qDelX && mouseX <= qDelX + qDelW && mouseY >= qSaveY && mouseY <= qSaveY + 14;
            context.fill(qDelX, qSaveY, qDelX + qDelW, qSaveY + 14, hoverQDel ? 0xFFB91C1C : 0xFF991B1B);
            context.drawText(this.textRenderer, "Eliminar", qDelX + 6, qSaveY + 3, 0xFFFFFFFF, false);
        }

        // Global Quests Buttons (New, Send to Server) (moved down to Y + 195)
        int cmdW = 60;
        int cmdX = startX + 15;
        int cmdY = startY + 195;

        boolean hoverNewQ = mouseX >= cmdX && mouseX <= cmdX + cmdW && mouseY >= cmdY && mouseY <= cmdY + 14;
        context.fill(cmdX, cmdY, cmdX + cmdW, cmdY + 14, hoverNewQ ? 0xFF1D4ED8 : 0xFF1E3A8A);
        context.drawText(this.textRenderer, "+ Nueva", cmdX + 10, cmdY + 3, 0xFFFFFFFF, false);

        int sendW = 100;
        int sendX = cmdX + cmdW + 5;
        boolean hoverSend = mouseX >= sendX && mouseX <= sendX + sendW && mouseY >= cmdY && mouseY <= cmdY + 14;
        context.fill(sendX, cmdY, sendX + sendW, cmdY + 14, hoverSend ? 0xFF2563EB : 0xFF1D4ED8);
        context.drawText(this.textRenderer, "Enviar Servidor", sendX + 10, cmdY + 3, 0xFFFFFFFF, false);
    }

    private void renderRewardsTab(DrawContext context, int mouseX, int mouseY) {
        // Left side: Levels list (8 visible)
        int listX = startX + 15;
        int listY = startY + 50;
        int listW = 75;
        int listH = 120; // 8 * 15 px
        int rowH = 15;

        context.fill(listX, listY, listX + listW, listY + listH, 0xFF111318);

        for (int i = 0; i < 8; i++) {
            int lvl = rewardScrollIndex + 1 + i;
            if (lvl > localRewards.size()) break;

            int rowY = listY + (i * rowH);
            boolean isSelected = (lvl == selectedRewardLevel);

            context.fill(listX, rowY, listX + listW - 14, rowY + rowH - 1, isSelected ? 0xFF1E293B : 0xFF1E2026);
            context.drawText(this.textRenderer, "Lvl " + lvl, listX + 4, rowY + 3, 0xFFFFFFFF, false);
        }

        // Dedicated Scroll Track on the right of the levels list
        int scrollTrackX = listX + listW - 12;
        int scrollTrackW = 10;
        context.fill(scrollTrackX, listY, scrollTrackX + scrollTrackW, listY + listH, 0xFF0B0C0E);

        // Up arrow "▲"
        int upY = listY + 2;
        boolean hoverUp = mouseX >= scrollTrackX && mouseX <= scrollTrackX + scrollTrackW && mouseY >= upY && mouseY <= upY + 10;
        context.drawText(this.textRenderer, "▲", scrollTrackX + 2, upY, hoverUp ? 0xFFFDE047 : 0xFF9CA3AF, false);

        // Down arrow "▼"
        int downY = listY + listH - 10;
        boolean hoverDown = mouseX >= scrollTrackX && mouseX <= scrollTrackX + scrollTrackW && mouseY >= downY && mouseY <= downY + 10;
        context.drawText(this.textRenderer, "▼", scrollTrackX + 2, downY, hoverDown ? 0xFFFDE047 : 0xFF9CA3AF, false);

        // Scrollbar thumb
        int trackH = listH - 24;
        if (localRewards.size() > 8) {
            int thumbH = Math.max(10, trackH * 8 / localRewards.size());
            int thumbY = listY + 12 + (trackH - thumbH) * rewardScrollIndex / (localRewards.size() - 8);
            context.fill(scrollTrackX + 1, thumbY, scrollTrackX + scrollTrackW - 1, thumbY + thumbH, 0xFF475569);
        } else {
            context.fill(scrollTrackX + 1, listY + 12, scrollTrackX + scrollTrackW - 1, listY + 12 + trackH, 0xFF334155);
        }

        // Edit Area (Free on Left, Premium on Right) (no overlaps)
        context.drawText(this.textRenderer, "XP req:", startX + 105, startY + 52, 0xFF9CA3AF, false);

        // Columns Headers
        context.drawText(this.textRenderer, "GRATIS", startX + 135, startY + 70, 0xFF3B82F6, false);
        context.drawText(this.textRenderer, "PREMIUM", startX + 270, startY + 70, 0xFFF59E0B, false);

        // Labels Column 1 (Free)
        int col1X = startX + 100;
        context.drawText(this.textRenderer, "Tipo:", col1X, startY + 82, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Valor:", col1X, startY + 102, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Cant:", col1X, startY + 122, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Plant:", col1X, startY + 142, 0xFF9CA3AF, false);

        // Labels Column 2 (Premium)
        int col2X = startX + 230;
        context.drawText(this.textRenderer, "Tipo:", col2X, startY + 82, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Valor:", col2X, startY + 102, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Cant:", col2X, startY + 122, 0xFF9CA3AF, false);
        context.drawText(this.textRenderer, "Plant:", col2X, startY + 142, 0xFF9CA3AF, false);

        // Apply Level reward button (aligned to X + 135)
        int saveW = 85;
        int saveX = startX + 135;
        int saveY = startY + 160;
        boolean hoverLvlSave = mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= saveY && mouseY <= saveY + 14;
        context.fill(saveX, saveY, saveX + saveW, saveY + 14, hoverLvlSave ? 0xFF15803D : 0xFF166534);
        context.drawText(this.textRenderer, "Aplicar Nivel", saveX + 10, saveY + 3, 0xFFFFFFFF, false);

        // Save Rewards button
        int sendW = 100;
        int sendX = startX + panelWidth - 115;
        int sendY = startY + 185;
        boolean hoverSend = mouseX >= sendX && mouseX <= sendX + sendW && mouseY >= sendY && mouseY <= sendY + 14;
        context.fill(sendX, sendY, sendX + sendW, sendY + 14, hoverSend ? 0xFF2563EB : 0xFF1D4ED8);
        context.drawText(this.textRenderer, "Enviar Servidor", sendX + 10, sendY + 3, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int backX = startX + panelWidth - 70;
        int backY = startY + 10;
        if (mouseX >= backX && mouseX <= backX + 55 && mouseY >= backY && mouseY <= backY + 12) {
            playClickSound();
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }

        // Tab click detection
        int tabY = startY + 28;
        int tabW = 75;
        int tabH = 14;

        int tabSeasonX = startX + 15;
        if (mouseX >= tabSeasonX && mouseX <= tabSeasonX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
            if (!activeTab.equals("season")) {
                activeTab = "season";
                playClickSound();
                clearAndInitWidgets();
            }
            return true;
        }

        int tabQuestsX = tabSeasonX + tabW + 5;
        if (mouseX >= tabQuestsX && mouseX <= tabQuestsX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
            if (!activeTab.equals("quests")) {
                activeTab = "quests";
                selectedQuestIndex = localQuests.isEmpty() ? -1 : 0;
                playClickSound();
                clearAndInitWidgets();
            }
            return true;
        }

        int tabRewardsX = tabQuestsX + tabW + 5;
        if (mouseX >= tabRewardsX && mouseX <= tabRewardsX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
            if (!activeTab.equals("rewards")) {
                activeTab = "rewards";
                selectedRewardLevel = 1;
                playClickSound();
                clearAndInitWidgets();
            }
            return true;
        }

        // Content Tab click handlers
        if (activeTab.equals("season")) {
            int btnW = 85;
            int btnH = 14;
            int btnY = startY + 155;

            // Iniciar Temp
            int btnStartX = startX + 15;
            if (mouseX >= btnStartX && mouseX <= btnStartX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                playClickSound();
                MinecraftClient.getInstance().player.networkHandler.sendCommand("cobblepass season start");
                return true;
            }

            // Detener Temp
            int btnStopX = btnStartX + btnW + 5;
            if (mouseX >= btnStopX && mouseX <= btnStopX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                playClickSound();
                MinecraftClient.getInstance().player.networkHandler.sendCommand("cobblepass season stop");
                return true;
            }

            // Nueva Temp
            int btnNewX = btnStopX + btnW + 5;
            if (mouseX >= btnNewX && mouseX <= btnNewX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                playClickSound();
                int duration = 60;
                try {
                    duration = Integer.parseInt(seasonDurationField.getText());
                } catch (Exception e) {}
                MinecraftClient.getInstance().player.networkHandler.sendCommand("cobblepass season create " + duration);
                return true;
            }

            // Guardar Config Settings
            int saveW = 100;
            int saveX = startX + panelWidth - 115;
            int saveY = startY + 185;
            if (mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= saveY && mouseY <= saveY + 14) {
                playClickSound();
                saveConfigLocallyAndToServer();
                return true;
            }
        } else if (activeTab.equals("quests")) {
            int listX = startX + 15;
            int listY = startY + 50;
            int listW = 190;
            int listH = 133;
            int rowH = 19;

            // Click list items (excluding the scrollbar area on the right)
            if (mouseX >= listX && mouseX <= listX + listW - 14 && mouseY >= listY && mouseY <= listY + listH) {
                int clickedIndex = questScrollIndex + (int)((mouseY - listY) / rowH);
                if (clickedIndex >= 0 && clickedIndex < localQuests.size()) {
                    selectedQuestIndex = clickedIndex;
                    playClickSound();
                    clearAndInitWidgets();
                    return true;
                }
            }

            // Scroll arrows
            int scrollTrackX = listX + listW - 12;
            if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 10 && mouseY >= listY + 2 && mouseY <= listY + 12) {
                if (questScrollIndex > 0) {
                    questScrollIndex--;
                    playClickSound();
                }
                return true;
            }
            if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 10 && mouseY >= listY + listH - 10 && mouseY <= listY + listH) {
                if (questScrollIndex < localQuests.size() - 7) {
                    questScrollIndex++;
                    playClickSound();
                }
                return true;
            }

            // Applying current quest modifications (updated to Y + 195)
            if (selectedQuestIndex >= 0 && selectedQuestIndex < localQuests.size()) {
                int qSaveW = 75;
                int qSaveX = startX + 215;
                int qSaveY = startY + 195;
                if (mouseX >= qSaveX && mouseX <= qSaveX + qSaveW && mouseY >= qSaveY && mouseY <= qSaveY + 14) {
                    playClickSound();
                    applyQuestChanges();
                    return true;
                }

                int qDelW = 50;
                int qDelX = qSaveX + qSaveW + 5;
                if (mouseX >= qDelX && mouseX <= qDelX + qDelW && mouseY >= qSaveY && mouseY <= qSaveY + 14) {
                    playClickSound();
                    localQuests.remove(selectedQuestIndex);
                    selectedQuestIndex = localQuests.isEmpty() ? -1 : 0;
                    clearAndInitWidgets();
                    return true;
                }
            }

            // New/Send Global Buttons (updated to Y + 195)
            int cmdX = startX + 15;
            int cmdY = startY + 195;
            if (mouseX >= cmdX && mouseX <= cmdX + 60 && mouseY >= cmdY && mouseY <= cmdY + 14) {
                playClickSound();
                Quest newQuest = new Quest("quest_" + System.currentTimeMillis(), "Nueva Misión", "Detalle de la misión", Quest.Type.CAPTURE_POKEMON, "any", 10, 150, "DAILY");
                localQuests.add(newQuest);
                selectedQuestIndex = localQuests.size() - 1;
                clearAndInitWidgets();
                return true;
            }

            int sendX = cmdX + 65;
            if (mouseX >= sendX && mouseX <= sendX + 100 && mouseY >= cmdY && mouseY <= cmdY + 14) {
                playClickSound();
                sendQuestsToServer();
                return true;
            }
        } else if (activeTab.equals("rewards")) {
            int listX = startX + 15;
            int listY = startY + 50;
            int listW = 75;
            int listH = 120;
            int rowH = 15;

            // Click levels list (excluding the scrollbar area on the right)
            if (mouseX >= listX && mouseX <= listX + listW - 14 && mouseY >= listY && mouseY <= listY + listH) {
                int clickedLvl = rewardScrollIndex + 1 + (int)((mouseY - listY) / rowH);
                if (clickedLvl >= 1 && clickedLvl <= localRewards.size()) {
                    selectedRewardLevel = clickedLvl;
                    playClickSound();
                    clearAndInitWidgets();
                    return true;
                }
            }

            // Scroll arrows
            int scrollTrackX = listX + listW - 12;
            if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 10 && mouseY >= listY + 2 && mouseY <= listY + 12) {
                if (rewardScrollIndex > 0) {
                    rewardScrollIndex--;
                    playClickSound();
                }
                return true;
            }
            if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 10 && mouseY >= listY + listH - 10 && mouseY <= listY + listH) {
                if (rewardScrollIndex < localRewards.size() - 8) {
                    rewardScrollIndex++;
                    playClickSound();
                }
                return true;
            }

            // Apply Level Reward Changes (aligned to X + 135)
            int saveW = 85;
            int saveX = startX + 135;
            int saveY = startY + 160;
            if (mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= saveY && mouseY <= saveY + 14) {
                playClickSound();
                applyRewardLevelChanges();
                return true;
            }

            // Send Rewards to Server
            int sendX = startX + panelWidth - 115;
            int sendY = startY + 185;
            if (mouseX >= sendX && mouseX <= sendX + 100 && mouseY >= sendY && mouseY <= sendY + 14) {
                playClickSound();
                sendRewardsToServer();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab.equals("quests")) {
            int listX = startX + 15;
            int listY = startY + 50;
            int listW = 190;
            int listH = 133;
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                if (verticalAmount > 0) {
                    if (questScrollIndex > 0) {
                        questScrollIndex--;
                        playClickSound();
                        return true;
                    }
                } else if (verticalAmount < 0) {
                    if (questScrollIndex < localQuests.size() - 7) {
                        questScrollIndex++;
                        playClickSound();
                        return true;
                    }
                }
            }
        } else if (activeTab.equals("rewards")) {
            int listX = startX + 15;
            int listY = startY + 50;
            int listW = 75;
            int listH = 120;
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                if (verticalAmount > 0) {
                    if (rewardScrollIndex > 0) {
                        rewardScrollIndex--;
                        playClickSound();
                        return true;
                    }
                } else if (verticalAmount < 0) {
                    if (rewardScrollIndex < localRewards.size() - 8) {
                        rewardScrollIndex++;
                        playClickSound();
                        return true;
                    }
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.master(
                        SoundEvents.UI_BUTTON_CLICK, 1.0F
                )
        );
    }

    // Save Logic implementations
    private void saveConfigLocallyAndToServer() {
        try {
            com.google.gson.JsonObject cfg = new com.google.gson.JsonObject();
            cfg.addProperty("currencyType", currencyTypeField.getText().trim());
            cfg.addProperty("currencyTarget", currencyTargetField.getText().trim());
            cfg.addProperty("premiumCost", Integer.parseInt(premiumCostField.getText().trim()));
            cfg.addProperty("seasonDurationDays", Integer.parseInt(seasonDurationField.getText().trim()));
            
            cfg.addProperty("seasonStartTime", CobblePassClient.seasonStartTime);
            cfg.addProperty("seasonEndTime", CobblePassClient.seasonEndTime);
            cfg.addProperty("seasonActive", CobblePassClient.seasonActive);

            String json = GSON.toJson(cfg);
            ClientPlayNetworking.send(new NetworkPackets.SaveConfigPayload(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyQuestChanges() {
        if (selectedQuestIndex >= 0 && selectedQuestIndex < localQuests.size()) {
            Quest q = localQuests.get(selectedQuestIndex);
            q.setTitle(questTitleField.getText());
            q.setDescription(questDescField.getText());
            try {
                q.setType(Quest.Type.valueOf(questTypeField.getText().toUpperCase().trim()));
            } catch (Exception e) {}
            q.setTarget(questTargetField.getText().trim());
            try {
                q.setRequiredAmount(Integer.parseInt(questReqField.getText().trim()));
            } catch (Exception e) {}
            try {
                q.setXpReward(Integer.parseInt(questXpField.getText().trim()));
            } catch (Exception e) {}
            q.setCategory(questCatField.getText().toUpperCase().trim());
        }
    }

    private void sendQuestsToServer() {
        try {
            String json = GSON.toJson(localQuests);
            ClientPlayNetworking.send(new NetworkPackets.SaveQuestsPayload(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyRewardLevelChanges() {
        Reward r = localRewards.stream().filter(x -> x.getLevel() == selectedRewardLevel).findFirst().orElse(null);
        if (r != null) {
            try {
                r.setXpRequired(Integer.parseInt(levelXpField.getText().trim()));
            } catch (Exception e) {}

            // Free Reward
            String freeType = freeTypeField.getText().trim().toUpperCase();
            if (!freeType.isEmpty()) {
                try {
                    Reward.Type t = Reward.Type.valueOf(freeType);
                    String val = freeValField.getText().trim();
                    int amt = Integer.parseInt(freeAmtField.getText().trim());
                    Reward.Action action = new Reward.Action(t, val, amt);
                    String temp = freeTempField.getText().trim();
                    if (!temp.isEmpty()) {
                        action.setTemplateRef(temp);
                    }
                    r.setFreeReward(action);
                } catch (Exception e) {}
            } else {
                r.setFreeReward(null);
            }

            // Premium Reward
            String premType = premTypeField.getText().trim().toUpperCase();
            if (!premType.isEmpty()) {
                try {
                    Reward.Type t = Reward.Type.valueOf(premType);
                    String val = premValField.getText().trim();
                    int amt = Integer.parseInt(premAmtField.getText().trim());
                    Reward.Action action = new Reward.Action(t, val, amt);
                    String temp = premTempField.getText().trim();
                    if (!temp.isEmpty()) {
                        action.setTemplateRef(temp);
                    }
                    r.setPremiumReward(action);
                } catch (Exception e) {}
            } else {
                r.setPremiumReward(null);
            }
        }
    }

    private void sendRewardsToServer() {
        try {
            String json = GSON.toJson(localRewards);
            ClientPlayNetworking.send(new NetworkPackets.SaveRewardsPayload(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
