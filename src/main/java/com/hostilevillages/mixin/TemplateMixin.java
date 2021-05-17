package com.hostilevillages.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.feature.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Template.class)
public class TemplateMixin
{
    @Inject(method = "createEntityIgnoreException", at = @At("RETURN"))
    private static void onEntitySpawn(final IServerWorld p_215382_0_, final CompoundNBT p_215382_1_, final CallbackInfoReturnable<Optional<Entity>> cir)
    {
        if (cir.getReturnValue().isPresent())
        {
            final Entity entity = cir.getReturnValue().get();
            if (entity.getType() == EntityType.ZOMBIE_VILLAGER || entity.getType() == EntityType.VILLAGER)
            {
                if (entity instanceof MobEntity)
                {
                    ((MobEntity) entity).setPersistenceRequired();
                }
            }
        }
    }
}
