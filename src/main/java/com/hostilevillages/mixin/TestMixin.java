package com.hostilevillages.mixin;

import com.hostilevillages.HostileVillages;
import net.minecraft.advancements.criterion.PlayerGeneratesContainerLootTrigger;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerGeneratesContainerLootTrigger.class)
public class TestMixin
{
    @Inject(method = "trigger", at = @At("HEAD"))
    public void test(final ServerPlayerEntity playerEntity, final ResourceLocation resourceLocation, final CallbackInfo ci)
    {
        playerEntity.sendMessage(new StringTextComponent("Generating loot for: " + resourceLocation), playerEntity.getUUID());
        HostileVillages.LOGGER.warn("Generating loot for: " + resourceLocation);
    }
}
