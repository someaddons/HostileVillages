package com.hostilevillages.mixin;

import com.hostilevillages.HostileVillages;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MobEntityPersistanceMixin
{
    @Inject(method = "setItemSlotAndDropWhenKilled", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setGuaranteedDrop(Lnet/minecraft/world/entity/EquipmentSlot;)V", shift = At.Shift.AFTER), cancellable = true)
    private void noPersisting(final EquipmentSlot p_233657_1_, final ItemStack p_233657_2_, final CallbackInfo ci)
    {
        if (HostileVillages.config.getCommonConfig().disableNoEntityDespawnWhenPickingItem.get())
        {
            ci.cancel();
        }
    }
}
