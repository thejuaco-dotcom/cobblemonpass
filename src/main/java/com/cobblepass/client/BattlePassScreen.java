package com.cobblepass.client;

import com.cobblepass.common.PlayerProgress;
import com.cobblepass.common.Quest;
import com.cobblepass.common.Reward;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

public class BattlePassScreen extends Screen {
    private String activeTab = "quests"; // "quests" or "rewards"
    private final int panelWidth = 400;
    private final int panelHeight = 220;
    private int startX;
    private int startY;
    private int selectedTimelineLevel = 1;
    private int timelineStartLevel = 1;
    private boolean isDraggingScrollbar = false;
    private String activeQuestTab = "daily"; // "daily" or "weekly" or "seasonal"
    private int questPage = 0;
    private final com.cobblemon.mod.common.client.render.models.blockbench.FloatingState freeFloatingState = 
        new com.cobblemon.mod.common.client.render.models.blockbench.FloatingState();
    private final com.cobblemon.mod.common.client.render.models.blockbench.FloatingState premiumFloatingState = 
        new com.cobblemon.mod.common.client.render.models.blockbench.FloatingState();
    private int freeFloatingStateAge = 0;
    private int premiumFloatingStateAge = 0;

    public BattlePassScreen() {
        super(Text.literal("CobblePass"));
    }

    @Override
    public void tick() {
        super.tick();
        if (freeFloatingState != null) {
            freeFloatingStateAge++;
            freeFloatingState.updateAge(freeFloatingStateAge);
        }
        if (premiumFloatingState != null) {
            premiumFloatingStateAge++;
            premiumFloatingState.updateAge(premiumFloatingStateAge);
        }
    }

    @Override
    protected void init() {
        this.startX = (this.width - this.panelWidth) / 2;
        this.startY = (this.height - this.panelHeight) / 2;

        PlayerProgress progress = CobblePassClient.progress;
        List<Reward> rewards = CobblePassClient.rewards;
        int currentLvl = progress != null ? progress.getLevel() : 0;
        int maxLvl = rewards != null ? rewards.size() : 5;
        this.selectedTimelineLevel = Math.max(1, Math.min(currentLvl, maxLvl));

        int baseLvl = Math.max(1, currentLvl);
        this.timelineStartLevel = Math.max(1, Math.min(((baseLvl - 1) / 5) * 5 + 1, 96));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 1. Draw outer panel background (Poké-styled dark slate)
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, 0xF5181A21);

        // 2. Draw Header Banner (Pokéball red accent)
        context.fill(startX, startY, startX + panelWidth, startY + 42, 0xFFE13B36);

        // Title text
        context.drawText(this.textRenderer, "COBBLEPASS", startX + 15, startY + 10, 0xFFFFFFFF, true);

        // XP Progress Bar text
        PlayerProgress progress = CobblePassClient.progress;
        List<Reward> rewards = CobblePassClient.rewards;

        int currentLvl = progress.getLevel();
        int maxLvl = rewards.size();

        String levelText = "NIVEL " + currentLvl;
        if (currentLvl >= maxLvl) {
            levelText += " (MÁX)";
        }
        context.drawText(this.textRenderer, levelText, startX + 15, startY + 26, 0xFFFEE2E2, false);

        // Draw XP Bar
        int barX = startX + 75;
        int barY = startY + 27;
        int barW = 80;
        int barH = 6;
        context.fill(barX, barY, barX + barW, barY + barH, 0xFF7F1D1D); // Dark red background

        if (currentLvl < maxLvl) {
            Reward nextReward = rewards.stream().filter(r -> r.getLevel() == (currentLvl + 1)).findFirst().orElse(null);
            int needed = nextReward != null ? nextReward.getXpRequired() : 100;
            int currentXp = progress.getCurrentXp();
            float pct = (float) currentXp / needed;
            context.fill(barX, barY, barX + (int)(barW * pct), barY + barH, 0xFF4ADE80); // Emerald green progress

            String xpText = currentXp + " / " + needed + " XP";
            context.drawText(this.textRenderer, xpText, barX + barW + 8, startY + 26, 0xFFFEE2E2, false);
        } else {
            context.fill(barX, barY, barX + barW, barY + barH, 0xFF4ADE80);
            context.drawText(this.textRenderer, "COMPLETADO", barX + barW + 8, startY + 26, 0xFFFEE2E2, false);
        }

        // 2.5 Draw Admin Gear button if player has OP permissions
        boolean isOp = this.client.player != null && this.client.player.hasPermissionLevel(2);
        if (isOp) {
            int gearX = startX + panelWidth - 20;
            int gearY = startY + 10;
            boolean hoverGear = mouseX >= gearX && mouseX <= gearX + 12 && mouseY >= gearY && mouseY <= gearY + 12;
            context.drawText(this.textRenderer, "⚙", gearX, gearY, hoverGear ? 0xFFFDE047 : 0xFFFFFFFF, false);
        }

        // 3. Draw Tabs (Misiones vs Premios)
        int tabQuestsX = startX + panelWidth - 140;
        int tabQuestsY = startY + 21;
        int tabW = 60;
        int tabH = 15;

        // Quests Tab Button
        boolean hoverQuests = mouseX >= tabQuestsX && mouseX <= tabQuestsX + tabW && mouseY >= tabQuestsY && mouseY <= tabQuestsY + tabH;
        int questsTabCol = activeTab.equals("quests") ? 0xFF181A21 : (hoverQuests ? 0xAA181A21 : 0x66181A21);
        context.fill(tabQuestsX, tabQuestsY, tabQuestsX + tabW, tabQuestsY + tabH, questsTabCol);
        context.drawText(this.textRenderer, "Misiones", tabQuestsX + 8, tabQuestsY + 4, 0xFFFFFFFF, false);

        // Rewards Tab Button
        int tabRewardsX = startX + panelWidth - 75;
        boolean hoverRewards = mouseX >= tabRewardsX && mouseX <= tabRewardsX + tabW && mouseY >= tabQuestsY && mouseY <= tabQuestsY + tabH;
        int rewardsTabCol = activeTab.equals("rewards") ? 0xFF181A21 : (hoverRewards ? 0xAA181A21 : 0x66181A21);
        context.fill(tabRewardsX, tabQuestsY, tabRewardsX + tabW, tabQuestsY + tabH, rewardsTabCol);
        context.drawText(this.textRenderer, "Premios", tabRewardsX + 11, tabQuestsY + 4, 0xFFFFFFFF, false);

        // 4. Draw Tab Content
        if (activeTab.equals("quests")) {
            renderQuests(context, mouseX, mouseY);
        } else {
            renderRewards(context, mouseX, mouseY, delta);
        }
    }

    private void renderQuests(DrawContext context, int mouseX, int mouseY) {
        List<Quest> filteredQuests = getFilteredQuests();
        PlayerProgress progress = CobblePassClient.progress;

        // Draw sub-tabs
        int subW = 68;
        int subH = 12;
        int subY = startY + 48;

        // Diarias
        int dailyX = startX + 15;
        boolean hoverDaily = mouseX >= dailyX && mouseX <= dailyX + subW && mouseY >= subY && mouseY <= subY + subH;
        int dailyCol = activeQuestTab.equals("daily") ? 0xFF3B82F6 : (hoverDaily ? 0xAA3B82F6 : 0x33FFFFFF);
        context.fill(dailyX, subY, dailyX + subW, subY + subH, dailyCol);
        int textW1 = this.textRenderer.getWidth("Diarias");
        context.drawText(this.textRenderer, "Diarias", dailyX + (subW - textW1) / 2, subY + 2, 0xFFFFFFFF, false);

        // Semanales
        int weeklyX = startX + 15 + subW + 5;
        boolean hoverWeekly = mouseX >= weeklyX && mouseX <= weeklyX + subW && mouseY >= subY && mouseY <= subY + subH;
        int weeklyCol = activeQuestTab.equals("weekly") ? 0xFF3B82F6 : (hoverWeekly ? 0xAA3B82F6 : 0x33FFFFFF);
        context.fill(weeklyX, subY, weeklyX + subW, subY + subH, weeklyCol);
        int textW2 = this.textRenderer.getWidth("Semanales");
        context.drawText(this.textRenderer, "Semanales", weeklyX + (subW - textW2) / 2, subY + 2, 0xFFFFFFFF, false);

        // Temporada
        int seasonalX = startX + 15 + (subW + 5) * 2;
        boolean hoverSeasonal = mouseX >= seasonalX && mouseX <= seasonalX + subW && mouseY >= subY && mouseY <= subY + subH;
        int seasonalCol = activeQuestTab.equals("seasonal") ? 0xFF3B82F6 : (hoverSeasonal ? 0xAA3B82F6 : 0x33FFFFFF);
        context.fill(seasonalX, subY, seasonalX + subW, subY + subH, seasonalCol);
        int textW3 = this.textRenderer.getWidth("Temporada");
        context.drawText(this.textRenderer, "Temporada", seasonalX + (subW - textW3) / 2, subY + 2, 0xFFFFFFFF, false);

        // Reset Timers
        if (activeQuestTab.equals("daily")) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
            java.time.Duration duration = java.time.Duration.between(now, nextMidnight);
            long seconds = duration.getSeconds();
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            String timerText = String.format("Reset: %02d:%02d:%02d", hours, minutes, secs);
            context.drawText(this.textRenderer, timerText, startX + 235, subY + 2, 0xFFFDE047, false);
        } else if (activeQuestTab.equals("weekly")) {
            java.time.LocalDate today = java.time.LocalDate.now();
            int daysUntilMonday = java.time.DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue();
            if (daysUntilMonday <= 0) {
                daysUntilMonday += 7;
            }
            java.time.LocalDateTime nextMondayMidnight = today.plusDays(daysUntilMonday).atStartOfDay();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(now, nextMondayMidnight);
            long seconds = duration.getSeconds();
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            String timerText = String.format("Reset: %dd %02dh", days, hours);
            context.drawText(this.textRenderer, timerText, startX + 235, subY + 2, 0xFFFDE047, false);
        } else if (activeQuestTab.equals("seasonal")) {
            if (!CobblePassClient.seasonActive) {
                context.drawText(this.textRenderer, "Temporada Inactiva", startX + 230, subY + 2, 0xFFEF4444, false);
            } else {
                long now = System.currentTimeMillis();
                long endTime = CobblePassClient.seasonEndTime;
                if (now > endTime) {
                    context.drawText(this.textRenderer, "Temporada Finalizada", startX + 230, subY + 2, 0xFFEF4444, false);
                } else {
                    long diffMs = endTime - now;
                    long totalSecs = diffMs / 1000;
                    long days = totalSecs / 86400;
                    long hours = (totalSecs % 86400) / 3600;
                    String timerText = String.format("Reset: %dd %02dh", days, hours);
                    context.drawText(this.textRenderer, timerText, startX + 230, subY + 2, 0xFFFDE047, false);
                }
            }
        }

        // Pagination
        int maxPage = Math.max(0, (filteredQuests.size() - 1) / 4);
        if (questPage > maxPage) {
            questPage = maxPage;
        }

        int navX = startX + panelWidth - 75;
        boolean hoverPrev = mouseX >= navX && mouseX <= navX + 15 && mouseY >= subY && mouseY <= subY + subH;
        int prevColor = (questPage > 0) ? (hoverPrev ? 0xFFFDE047 : 0xFFFFFFFF) : 0xFF4B5563;
        context.drawText(this.textRenderer, "<", navX + 4, subY + 2, prevColor, false);

        String pageStr = (questPage + 1) + "/" + (maxPage + 1);
        int pageStrW = this.textRenderer.getWidth(pageStr);
        context.drawText(this.textRenderer, pageStr, navX + 18 + (25 - pageStrW) / 2, subY + 2, 0xFF9CA3AF, false);

        boolean hoverNext = mouseX >= navX + 45 && mouseX <= navX + 60 && mouseY >= subY && mouseY <= subY + subH;
        int nextColor = (questPage < maxPage) ? (hoverNext ? 0xFFFDE047 : 0xFFFFFFFF) : 0xFF4B5563;
        context.drawText(this.textRenderer, ">", navX + 49, subY + 2, nextColor, false);

        // Cards list
        int startIdx = questPage * 4;
        int cardY = startY + 65;
        int cardHeight = 32;
        int cardMargin = 4;

        for (int i = 0; i < 4; i++) {
            int idx = startIdx + i;
            if (idx >= filteredQuests.size()) break;

            Quest quest = filteredQuests.get(idx);
            int currentProgress = progress.getQuestProgressCount(quest.getId());
            int required = quest.getRequiredAmount();

            int cardX = startX + 15;
            int cardW = panelWidth - 30;

            // Draw card background
            context.fill(cardX, cardY, cardX + cardW, cardY + cardHeight, 0xFF222530);
            context.fill(cardX, cardY, cardX + 3, cardY + cardHeight, 0xFFE13B36);

            // Quest Title
            String title = truncateText(quest.getTitle(), 290);
            context.drawText(this.textRenderer, title, cardX + 10, cardY + 3, 0xFFFFFFFF, false);
            // Quest Description
            String desc = truncateText(quest.getDescription(), 290);
            context.drawText(this.textRenderer, desc, cardX + 10, cardY + 12, 0xFF9CA3AF, false);

            // Progress text
            String progressStr = currentProgress + "/" + required;
            context.drawText(this.textRenderer, progressStr, cardX + cardW - 60, cardY + 3, 0xFF4ADE80, false);

            // XP Reward text
            String xpStr = "+" + quest.getXpReward() + " XP";
            context.drawText(this.textRenderer, xpStr, cardX + cardW - 60, cardY + 12, 0xFFEAB308, false);

            // Progress Bar inside card
            int barW = cardW - 20;
            int barH = 2;
            int barX = cardX + 10;
            int barY = cardY + 23;
            context.fill(barX, barY, barX + barW, barY + barH, 0xFF2A2D3A);

            float pct = (float) currentProgress / required;
            int fillW = (int) (barW * pct);
            if (fillW > 0) {
                context.fill(barX, barY, barX + fillW, barY + barH, 0xFF4ADE80);
            }

            cardY += cardHeight + cardMargin;
        }
    }

    private void renderRewards(DrawContext context, int mouseX, int mouseY, float delta) {
        List<Reward> rewards = CobblePassClient.rewards;
        PlayerProgress progress = CobblePassClient.progress;

        int numLevels = rewards.size();
        int timelineY = startY + 65;
        int timelineX = startX + 50;
        int timelineWidth = panelWidth - 100;
        int currentLvl = progress.getLevel();

        // 1. Draw timeline connecting line
        context.fill(timelineX, timelineY, timelineX + timelineWidth, timelineY + 2, 0xFF4B5563);
        if (currentLvl >= timelineStartLevel) {
            int reachedIndex = Math.min(currentLvl - timelineStartLevel, 4);
            int endX = timelineX + reachedIndex * timelineWidth / 4;
            if (reachedIndex > 0) {
                context.fill(timelineX, timelineY, endX, timelineY + 2, 0xFF4ADE80);
            }
        }

        // 2. Draw timeline level nodes
        for (int i = 0; i < 5; i++) {
            int lvl = timelineStartLevel + i;
            if (lvl > numLevels) break;

            int nodeX = timelineX + i * timelineWidth / 4;
            boolean isSelected = (lvl == selectedTimelineLevel);
            boolean isUnlocked = (lvl <= currentLvl);

            int bgCol = isUnlocked ? 0xFFE13B36 : 0xFF4B5563;
            if (isSelected) {
                context.fill(nodeX - 11, timelineY - 10, nodeX + 11, timelineY + 12, 0xFFFFFFFF);
            }
            context.fill(nodeX - 9, timelineY - 8, nodeX + 9, timelineY + 10, bgCol);

            String lbl = String.valueOf(lvl);
            int textW = this.textRenderer.getWidth(lbl);
            context.drawText(this.textRenderer, lbl, nodeX - textW / 2, timelineY - 4, 0xFFFFFFFF, false);
        }

        // 3. Draw left and right navigation arrows
        boolean hoverLeft = mouseX >= startX + 15 && mouseX <= startX + 35 && mouseY >= timelineY - 10 && mouseY <= timelineY + 10;
        int leftColor = (timelineStartLevel > 1) ? (hoverLeft ? 0xFFFDE047 : 0xFFFFFFFF) : 0xFF4B5563;
        context.drawText(this.textRenderer, "<", startX + 22, timelineY - 4, leftColor, false);

        boolean hoverRight = mouseX >= startX + panelWidth - 35 && mouseX <= startX + panelWidth - 15 && mouseY >= timelineY - 10 && mouseY <= timelineY + 10;
        int rightColor = (timelineStartLevel < 96) ? (hoverRight ? 0xFFFDE047 : 0xFFFFFFFF) : 0xFF4B5563;
        context.drawText(this.textRenderer, ">", startX + panelWidth - 28, timelineY - 4, rightColor, false);

        // 3.5 Draw Scrollbar
        int trackX = startX + 50;
        int trackWidth = panelWidth - 100;
        int trackY = timelineY + 18;
        int thumbW = 30;
        int thumbH = 4;
        
        context.fill(trackX, trackY, trackX + trackWidth, trackY + 2, 0xFF4B5563);
        
        int thumbRange = trackWidth - thumbW;
        int thumbX = trackX + (timelineStartLevel - 1) * thumbRange / 95;
        
        boolean hoverThumb = mouseX >= thumbX && mouseX <= thumbX + thumbW && mouseY >= trackY - 2 && mouseY <= trackY + 4;
        int thumbColor = (isDraggingScrollbar || hoverThumb) ? 0xFFFDE047 : 0xFFE13B36;
        context.fill(thumbX, trackY - 1, thumbX + thumbW, trackY + thumbH - 1, thumbColor);

        // 3.6 Draw "Reclamar Todo" Button
        int claimAllW = 85;
        int claimAllH = 14;
        int claimAllX = startX + panelWidth - 100;
        int claimAllY = startY + 204;
        
        boolean hasUnclaimed = hasUnclaimedRewards();
        boolean hoverBtn = mouseX >= claimAllX && mouseX <= claimAllX + claimAllW && mouseY >= claimAllY && mouseY <= claimAllY + claimAllH;
        
        int btnBg = hasUnclaimed ? (hoverBtn ? 0xFFE13B36 : 0xFF374151) : 0xFF1F2937;
        int btnTextCol = hasUnclaimed ? 0xFFFFFFFF : 0xFF9CA3AF;
        int btnBorderCol = hasUnclaimed ? (hoverBtn ? 0xFFF87171 : 0xFF4B5563) : 0xFF374151;
        
        context.fill(claimAllX, claimAllY, claimAllX + claimAllW, claimAllY + claimAllH, btnBorderCol);
        context.fill(claimAllX + 1, claimAllY + 1, claimAllX + claimAllW - 1, claimAllY + claimAllH - 1, btnBg);
        
        String claimAllText = "Reclamar todo";
        int claimAllTextW = this.textRenderer.getWidth(claimAllText);
        context.drawText(this.textRenderer, claimAllText, claimAllX + (claimAllW - claimAllTextW) / 2, claimAllY + 3, btnTextCol, false);

        // 3.7 Draw "Comprar Premium" Button or Active Status
        if (progress.isPremium()) {
            context.drawText(this.textRenderer, "★ Pase Premium Activo", startX + 15, startY + 207, 0xFFFDE047, false);
        } else if (!CobblePassClient.currencyType.equalsIgnoreCase("ADMIN_ONLY")) {
            int buyW = 140;
            int buyH = 14;
            int buyX = startX + 15;
            int buyY = startY + 204;
            
            boolean hoverBuy = mouseX >= buyX && mouseX <= buyX + buyW && mouseY >= buyY && mouseY <= buyY + buyH;
            int buyBg = hoverBuy ? 0xFFEAB308 : 0xFFCA8A04;
            int buyBorder = hoverBuy ? 0xFFFEF08A : 0xFF854D0E;
            
            context.fill(buyX, buyY, buyX + buyW, buyY + buyH, buyBorder);
            context.fill(buyX + 1, buyY + 1, buyX + buyW - 1, buyY + buyH - 1, buyBg);
            
            String buyText = getBuyButtonText();
            int buyTextW = this.textRenderer.getWidth(buyText);
            context.drawText(this.textRenderer, buyText, buyX + (buyW - buyTextW) / 2, buyY + 3, 0xFF000000, false);
        }

        // 4. Draw selected level details card
        Reward reward = rewards.stream().filter(r -> r.getLevel() == selectedTimelineLevel).findFirst().orElse(null);
        if (reward == null) return;

        int cardX = startX + 15;
        int cardY = startY + 92;
        int cardW = panelWidth - 30;
        int cardHeight = 110;

        context.fill(cardX, cardY, cardX + cardW, cardY + cardHeight, 0xFF181A21);
        
        int halfW = (cardW - 10) / 2;
        
        // --- Left Half: Free Reward ---
        int leftX = cardX + 5;
        context.fill(leftX, cardY + 5, leftX + halfW, cardY + cardHeight - 5, 0xFF222530);
        
        String freeTitle = "GRATIS (LVL " + selectedTimelineLevel + ")";
        int freeTitleW = this.textRenderer.getWidth(freeTitle);
        context.drawText(this.textRenderer, freeTitle, leftX + (halfW - freeTitleW) / 2, cardY + 10, 0xFF9CA3AF, false);
        
        Reward.Action freeAction = reward.getFreeReward();
        if (freeAction != null && freeAction.getType() == Reward.Type.POKEMON) {
            drawPokemonModel(context, freeAction.getValue(), leftX + (halfW) / 2, cardY + 28, delta, freeFloatingState);
        } else {
            net.minecraft.item.ItemStack freeStack = getItemStackForAction(freeAction);
            if (!freeStack.isEmpty()) {
                context.drawItem(freeStack, leftX + (halfW - 16) / 2, cardY + 24);
            }
        }
        
        String freeText = freeAction != null ? (freeAction.getType() == Reward.Type.POKEMON ? "" : freeAction.getAmount() + "x ") + formatRewardName(freeAction) : "Ninguno";
        freeText = truncateText(freeText, 170);
        int freeTextW = this.textRenderer.getWidth(freeText);
        context.drawText(this.textRenderer, freeText, leftX + (halfW - freeTextW) / 2, cardY + 48, 0xFFD1D5DB, false);

        if (selectedTimelineLevel <= currentLvl && freeAction != null) {
            boolean claimed = progress.isRewardClaimed(selectedTimelineLevel, false);
            if (claimed) {
                String statusText = "Reclamado";
                int statusTextW = this.textRenderer.getWidth(statusText);
                context.drawText(this.textRenderer, statusText, leftX + (halfW - statusTextW) / 2, cardY + 76, 0xFF10B981, false);
            } else {
                int btnW = 80;
                int btnH = 20;
                int btnX = leftX + (halfW - btnW) / 2;
                int btnY = cardY + 70;
                boolean hover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hover ? 0xFF2563EB : 0xFF3B82F6);
                int btnTextW = this.textRenderer.getWidth("Cobrar");
                context.drawText(this.textRenderer, "Cobrar", btnX + (btnW - btnTextW) / 2, btnY + 6, 0xFFFFFFFF, false);
            }
        } else if (freeAction != null) {
            String statusText = "Bloqueado";
            int statusTextW = this.textRenderer.getWidth(statusText);
            context.drawText(this.textRenderer, statusText, leftX + (halfW - statusTextW) / 2, cardY + 76, 0xFF6B7280, false);
        } else {
            String statusText = "Ninguno";
            int statusTextW = this.textRenderer.getWidth(statusText);
            context.drawText(this.textRenderer, statusText, leftX + (halfW - statusTextW) / 2, cardY + 76, 0xFF6B7280, false);
        }

        // --- Right Half: Premium Reward ---
        int rightX = cardX + 5 + halfW + 5;
        context.fill(rightX, cardY + 5, rightX + halfW, cardY + cardHeight - 5, 0xFF2A2823);
        context.fill(rightX, cardY + 5, rightX + halfW, cardY + 7, 0xFFEAB308);

        String premTitle = "PREMIUM (LVL " + selectedTimelineLevel + ")";
        int premTitleW = this.textRenderer.getWidth(premTitle);
        context.drawText(this.textRenderer, premTitle, rightX + (halfW - premTitleW) / 2, cardY + 10, 0xFFFDE047, false);

        Reward.Action premAction = reward.getPremiumReward();
        if (premAction != null && premAction.getType() == Reward.Type.POKEMON) {
            drawPokemonModel(context, premAction.getValue(), rightX + (halfW) / 2, cardY + 28, delta, premiumFloatingState);
        } else {
            net.minecraft.item.ItemStack premStack = getItemStackForAction(premAction);
            if (!premStack.isEmpty()) {
                context.drawItem(premStack, rightX + (halfW - 16) / 2, cardY + 24);
            }
        }

        String premText = premAction != null ? (premAction.getType() == Reward.Type.POKEMON ? "" : premAction.getAmount() + "x ") + formatRewardName(premAction) : "Ninguno";
        premText = truncateText(premText, 170);
        int premTextW = this.textRenderer.getWidth(premText);
        context.drawText(this.textRenderer, premText, rightX + (halfW - premTextW) / 2, cardY + 48, 0xFFFDE047, false);

        if (selectedTimelineLevel <= currentLvl && premAction != null) {
            boolean claimed = progress.isRewardClaimed(selectedTimelineLevel, true);
            if (claimed) {
                String statusText = "Reclamado";
                int statusTextW = this.textRenderer.getWidth(statusText);
                context.drawText(this.textRenderer, statusText, rightX + (halfW - statusTextW) / 2, cardY + 76, 0xFF10B981, false);
            } else if (progress.isPremium()) {
                int btnW = 80;
                int btnH = 20;
                int btnX = rightX + (halfW - btnW) / 2;
                int btnY = cardY + 70;
                boolean hover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hover ? 0xFFCA8A04 : 0xFFEAB308);
                int btnTextW = this.textRenderer.getWidth("Cobrar");
                context.drawText(this.textRenderer, "Cobrar", btnX + (btnW - btnTextW) / 2, btnY + 6, 0xFF000000, false);
            } else {
                String statusText = "Requiere Premium";
                int statusTextW = this.textRenderer.getWidth(statusText);
                context.drawText(this.textRenderer, statusText, rightX + (halfW - statusTextW) / 2, cardY + 76, 0xFFF97316, false);
            }
        } else if (premAction != null) {
            String statusText = "Bloqueado";
            int statusTextW = this.textRenderer.getWidth(statusText);
            context.drawText(this.textRenderer, statusText, rightX + (halfW - statusTextW) / 2, cardY + 76, 0xFF6B7280, false);
        } else {
            String statusText = "Ninguno";
            int statusTextW = this.textRenderer.getWidth(statusText);
            context.drawText(this.textRenderer, statusText, rightX + (halfW - statusTextW) / 2, cardY + 76, 0xFF6B7280, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean isOp = this.client.player != null && this.client.player.hasPermissionLevel(2);
        if (isOp) {
            int gearX = startX + panelWidth - 20;
            int gearY = startY + 10;
            if (mouseX >= gearX && mouseX <= gearX + 12 && mouseY >= gearY && mouseY <= gearY + 12) {
                playClickSound();
                this.client.setScreen(new AdminPanelScreen(this));
                return true;
            }
        }

        int tabQuestsX = startX + panelWidth - 140;
        int tabQuestsY = startY + 21;
        int tabW = 60;
        int tabH = 15;

        if (mouseX >= tabQuestsX && mouseX <= tabQuestsX + tabW && mouseY >= tabQuestsY && mouseY <= tabQuestsY + tabH) {
            activeTab = "quests";
            playClickSound();
            return true;
        }

        int tabRewardsX = startX + panelWidth - 75;
        if (mouseX >= tabRewardsX && mouseX <= tabRewardsX + tabW && mouseY >= tabQuestsY && mouseY <= tabQuestsY + tabH) {
            activeTab = "rewards";
            playClickSound();
            return true;
        }

        if (activeTab.equals("quests")) {
            int subW = 68;
            int subH = 12;
            int subY = startY + 48;

            // Click Diarias
            int dailyX = startX + 15;
            if (mouseX >= dailyX && mouseX <= dailyX + subW && mouseY >= subY && mouseY <= subY + subH) {
                activeQuestTab = "daily";
                questPage = 0;
                playClickSound();
                return true;
            }

            // Click Semanales
            int weeklyX = startX + 15 + subW + 5;
            if (mouseX >= weeklyX && mouseX <= weeklyX + subW && mouseY >= subY && mouseY <= subY + subH) {
                activeQuestTab = "weekly";
                questPage = 0;
                playClickSound();
                return true;
            }

            // Click Temporada
            int seasonalX = startX + 15 + (subW + 5) * 2;
            if (mouseX >= seasonalX && mouseX <= seasonalX + subW && mouseY >= subY && mouseY <= subY + subH) {
                activeQuestTab = "seasonal";
                questPage = 0;
                playClickSound();
                return true;
            }

            // Click pagination arrows
            int navX = startX + panelWidth - 75;

            // Click Prev
            if (mouseX >= navX && mouseX <= navX + 15 && mouseY >= subY && mouseY <= subY + subH) {
                if (questPage > 0) {
                    questPage--;
                    playClickSound();
                }
                return true;
            }

            // Click Next
            if (mouseX >= navX + 45 && mouseX <= navX + 60 && mouseY >= subY && mouseY <= subY + subH) {
                List<Quest> filtered = getFilteredQuests();
                int maxPage = Math.max(0, (filtered.size() - 1) / 4);
                if (questPage < maxPage) {
                    questPage++;
                    playClickSound();
                }
                return true;
            }
        }

        if (activeTab.equals("rewards")) {
            int claimAllW = 85;
            int claimAllH = 14;
            int claimAllX = startX + panelWidth - 100;
            int claimAllY = startY + 204;
            if (mouseX >= claimAllX && mouseX <= claimAllX + claimAllW && mouseY >= claimAllY && mouseY <= claimAllY + claimAllH) {
                if (hasUnclaimedRewards()) {
                    CobblePassClient.claimAll();
                    playClickSound();
                    return true;
                }
            }

            // Click on "Comprar Premium" Button
            if (CobblePassClient.progress != null && !CobblePassClient.progress.isPremium() && !CobblePassClient.currencyType.equalsIgnoreCase("ADMIN_ONLY")) {
                int buyW = 140;
                int buyH = 14;
                int buyX = startX + 15;
                int buyY = startY + 204;
                if (mouseX >= buyX && mouseX <= buyX + buyW && mouseY >= buyY && mouseY <= buyY + buyH) {
                    CobblePassClient.buyPremium();
                    playClickSound();
                    return true;
                }
            }

            List<Reward> rewards = CobblePassClient.rewards;
            PlayerProgress progress = CobblePassClient.progress;
            int numLevels = rewards.size();

            int timelineY = startY + 65;
            int timelineX = startX + 50;
            int timelineWidth = panelWidth - 100;

            // 0. Scrollbar Click Handler
            int trackY = timelineY + 18;
            if (mouseX >= timelineX && mouseX <= timelineX + timelineWidth && mouseY >= trackY - 4 && mouseY <= trackY + 8) {
                isDraggingScrollbar = true;
                updateScroll(mouseX);
                playClickSound();
                return true;
            }

            // 1. Navigation Arrows Click Handler
            if (timelineStartLevel > 1 && mouseX >= startX + 15 && mouseX <= startX + 35 && mouseY >= timelineY - 10 && mouseY <= timelineY + 10) {
                timelineStartLevel = Math.max(1, timelineStartLevel - 5);
                playClickSound();
                return true;
            }

            if (timelineStartLevel < 96 && mouseX >= startX + panelWidth - 35 && mouseX <= startX + panelWidth - 15 && mouseY >= timelineY - 10 && mouseY <= timelineY + 10) {
                timelineStartLevel = Math.min(96, timelineStartLevel + 5);
                playClickSound();
                return true;
            }

            // 2. Timeline Nodes Click Handler
            for (int i = 0; i < 5; i++) {
                int lvl = timelineStartLevel + i;
                if (lvl > numLevels) break;

                int nodeX = timelineX + i * timelineWidth / 4;
                if (mouseX >= nodeX - 12 && mouseX <= nodeX + 12 && mouseY >= timelineY - 12 && mouseY <= timelineY + 12) {
                    selectedTimelineLevel = lvl;
                    playClickSound();
                    return true;
                }
            }

            // 3. Claim Buttons Click Handler
            Reward reward = rewards.stream().filter(r -> r.getLevel() == selectedTimelineLevel).findFirst().orElse(null);
            if (reward != null) {
                int lvl = reward.getLevel();
                boolean isUnlocked = progress.getLevel() >= lvl;

                if (isUnlocked) {
                    int cardX = startX + 15;
                    int cardW = panelWidth - 30;
                    int cardY = startY + 92;
                    int halfW = (cardW - 10) / 2;

                    int leftX = cardX + 5;
                    int btnW = 80;
                    int btnH = 20;
                    int freeBtnX = leftX + (halfW - btnW) / 2;
                    int freeBtnY = cardY + 70;

                    boolean freeClaimed = progress.isRewardClaimed(lvl, false);
                    if (reward.getFreeReward() != null && !freeClaimed && mouseX >= freeBtnX && mouseX <= freeBtnX + btnW && mouseY >= freeBtnY && mouseY <= freeBtnY + btnH) {
                        CobblePassClient.claimReward(lvl, false);
                        playClickSound();
                        return true;
                    }

                    int rightX = cardX + 5 + halfW + 5;
                    int premBtnX = rightX + (halfW - btnW) / 2;
                    int premBtnY = cardY + 70;

                    boolean premClaimed = progress.isRewardClaimed(lvl, true);
                    if (reward.getPremiumReward() != null && progress.isPremium() && !premClaimed && mouseX >= premBtnX && mouseX <= premBtnX + btnW && mouseY >= premBtnY && mouseY <= premBtnY + btnH) {
                        CobblePassClient.claimReward(lvl, true);
                        playClickSound();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (activeTab.equals("rewards") && isDraggingScrollbar && button == 0) {
            updateScroll(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab.equals("rewards")) {
            if (verticalAmount > 0) {
                timelineStartLevel = Math.max(1, timelineStartLevel - 1);
                playClickSound();
                return true;
            } else if (verticalAmount < 0) {
                timelineStartLevel = Math.min(96, timelineStartLevel + 1);
                playClickSound();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateScroll(double mouseX) {
        int trackX = startX + 50;
        int trackWidth = panelWidth - 100;
        int thumbW = 30;
        int thumbRange = trackWidth - thumbW;
        int relativeX = (int) mouseX - trackX - (thumbW / 2);
        relativeX = Math.max(0, Math.min(relativeX, thumbRange));
        
        int nextStartLvl = 1 + (relativeX * 95 / thumbRange);
        if (nextStartLvl != timelineStartLevel) {
            timelineStartLevel = nextStartLvl;
        }
    }

    private void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.master(
                        SoundEvents.UI_BUTTON_CLICK, 1.0F
                )
        );
    }

    private net.minecraft.item.ItemStack getItemStackForAction(Reward.Action action) {
        if (action == null) return net.minecraft.item.ItemStack.EMPTY;
        
        if (action.getType() == Reward.Type.POKEMON) {
            String ballId = action.getValue().contains("shiny=true") || action.getValue().contains("shiny") ? "cobblemon:cherish_ball" : "cobblemon:poke_ball";
            net.minecraft.util.Identifier itemId = net.minecraft.util.Identifier.of(ballId);
            net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(itemId);
            if (item != null) {
                return new net.minecraft.item.ItemStack(item, 1);
            }
        } else if (action.getType() == Reward.Type.ITEM) {
            String val = action.getValue();
            net.minecraft.util.Identifier itemId = net.minecraft.util.Identifier.of(val);
            net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(itemId);
            if (item != null) {
                return new net.minecraft.item.ItemStack(item, action.getAmount());
            }
        } else if (action.getType() == Reward.Type.COMMAND) {
            if (action.getValue().contains("diamond")) {
                net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft:diamond"));
                if (item != null) return new net.minecraft.item.ItemStack(item, 1);
            }
        }
        return net.minecraft.item.ItemStack.EMPTY;
    }

    private String formatRewardName(Reward.Action action) {
        if (action == null) return "Ninguno";
        if (action.getType() == Reward.Type.POKEMON) {
            String val = action.getValue();
            String pokeName = val.split(" ")[0];
            pokeName = Character.toUpperCase(pokeName.charAt(0)) + pokeName.substring(1);
            
            boolean isShiny = val.contains("shiny=true") || val.contains("shiny");
            String lvlStr = "";
            if (val.contains("level=")) {
                String[] parts = val.split("level=");
                lvlStr = " Nv." + parts[parts.length - 1].split(" ")[0];
            }
            
            return (isShiny ? "★ " : "") + pokeName + lvlStr;
        } else {
            return formatItemName(action.getValue());
        }
    }

    private String formatItemName(String itemId) {
        String[] parts = itemId.split(":");
        String name = parts[parts.length - 1];
        name = name.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean hasUnclaimedRewards() {
        PlayerProgress progress = CobblePassClient.progress;
        List<Reward> rewards = CobblePassClient.rewards;
        if (progress == null || rewards == null) return false;
        
        int currentLvl = progress.getLevel();
        for (Reward r : rewards) {
            int lvl = r.getLevel();
            if (lvl > currentLvl) continue;
            
            if (r.getFreeReward() != null && !progress.isRewardClaimed(lvl, false)) {
                return true;
            }
            if (r.getPremiumReward() != null && progress.isPremium() && !progress.isRewardClaimed(lvl, true)) {
                return true;
            }
        }
        return false;
    }

    private String getBuyButtonText() {
        if (CobblePassClient.currencyType.equalsIgnoreCase("SCOREBOARD")) {
            return "Premium: " + CobblePassClient.premiumCost + " P$";
        } else if (CobblePassClient.currencyType.equalsIgnoreCase("ITEM")) {
            return "Premium: " + CobblePassClient.premiumCost + "x " + formatItemName(CobblePassClient.currencyTarget);
        }
        return "Premium";
    }

    private List<Quest> getFilteredQuests() {
        List<Quest> quests = CobblePassClient.quests;
        if (quests == null) return List.of();

        String targetCategory = "DAILY";
        if (activeQuestTab.equals("weekly")) {
            targetCategory = "WEEKLY";
        } else if (activeQuestTab.equals("seasonal")) {
            targetCategory = "SEASONAL";
        }

        final String cat = targetCategory;
        return quests.stream()
                .filter(q -> q.getCategory().equalsIgnoreCase(cat))
                .toList();
    }

    private String truncateText(String text, int maxWidth) {
        if (text == null) return "";
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixW = this.textRenderer.getWidth(suffix);
        String trimmed = this.textRenderer.trimToWidth(text, maxWidth - suffixW);
        return trimmed + suffix;
    }

    private void drawPokemonModel(DrawContext context, String value, int x, int y, float delta, com.cobblemon.mod.common.client.render.models.blockbench.FloatingState floatingState) {
        net.minecraft.client.util.math.MatrixStack matrices = context.getMatrices();
        boolean pushed = false;
        try {
            String species = value.split(" ")[0].toLowerCase();
            boolean shiny = value.contains("shiny=true") || value.contains("shiny");
            net.minecraft.util.Identifier speciesId = net.minecraft.util.Identifier.of("cobblemon", species);
            
            float yaw = (System.currentTimeMillis() / 40F) % 360F;
            org.joml.Quaternionf rotation = com.cobblemon.mod.common.util.math.QuaternionUtilsKt.fromEulerXYZDegrees(
                new org.joml.Quaternionf(), 
                new org.joml.Vector3f(13f, yaw, 0f)
            );
            
            java.util.HashSet<String> aspects = new java.util.HashSet<>();
            if (shiny) {
                aspects.add("shiny");
            }
            floatingState.setCurrentAspects(aspects);
            
            matrices.push();
            pushed = true;
            matrices.translate((float)x, (float)y, 100.0f);
            
            com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt.drawProfilePokemon(
                speciesId,
                matrices,
                rotation,
                com.cobblemon.mod.common.entity.PoseType.PROFILE,
                floatingState,
                delta,
                6.0f, // scale
                false, // useProfileScale
                false, // useBaseScale
                false, // doQuirks
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                0.0f,
                0.0f
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pushed) {
                matrices.pop();
            }
        }
    }
}
