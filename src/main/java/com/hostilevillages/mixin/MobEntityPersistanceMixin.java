package com.hostilevillages.mixin;

import com.hostilevillages.HostileVillages;
import net.minecraft.entity.MobEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobEntityPersistanceMixin
{
    @Inject(method = "setItemSlotAndDropWhenKilled", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;setGuaranteedDrop(Lnet/minecraft/inventory/EquipmentSlotType;)V", shift = At.Shift.AFTER), cancellable = true)
    private void noPersisting(final EquipmentSlotType p_233657_1_, final ItemStack p_233657_2_, final CallbackInfo ci)
    {
        if (HostileVillages.config.getCommonConfig().disableNoEntityDespawnWhenPickingItem.get())
        {
            ci.cancel();
        }
    }
}
