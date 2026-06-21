package com.cobblepass.mixin;

import com.cobblepass.server.QuestListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {
    @Shadow @Final private PlayerEntity player;

    @Inject(method = "onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private void onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        System.out.println("[CobblePass Debug] onTakeItem called for: " + itemId + " x " + stack.getCount() + ", side: " + (player.getWorld().isClient() ? "CLIENT" : "SERVER"));
        if (player instanceof ServerPlayerEntity serverPlayer) {
            QuestListener.handleCraft(serverPlayer, itemId, stack.getCount());
        }
    }
}
