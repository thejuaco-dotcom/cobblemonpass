package com.cobblepass.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;

public class BattlePassToast implements Toast {
    private final String title;
    private final String description;
    private final boolean completed;
    private long startTime = -1;

    public BattlePassToast(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
    }

    @Override
    public Toast.Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        if (this.startTime == -1) {
            this.startTime = startTime;
        }

        int w = this.getWidth();
        int h = this.getHeight();

        // Premium dark background panel
        context.fill(0, 0, w, h, 0xF5111318);
        // Left color indicator bar: gold for completed, blue for progress
        context.fill(0, 0, 3, h, completed ? 0xFFEAB308 : 0xFF3B82F6);

        // Titles and descriptions
        int titleColor = completed ? 0xFFFDE047 : 0xFFFFFFFF;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        context.drawText(mc.textRenderer, this.title, 8, 7, titleColor, false);
        context.drawText(mc.textRenderer, this.description, 8, 18, 0xFF9CA3AF, false);

        // Display for 5 seconds (5000 milliseconds)
        return startTime - this.startTime < 5000L ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
