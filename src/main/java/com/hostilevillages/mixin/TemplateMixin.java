package com.hostilevillages.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(StructureTemplate.class)
public class TemplateMixin
{
    @Inject(method = "createEntityIgnoreException", at = @At("RETURN"))
    private static void onEntitySpawn(final ServerLevelAccessor p_215382_0_, final CompoundTag p_215382_1_, final CallbackInfoReturnable<Optional<Entity>> cir)
    {
        if (cir.getReturnValue().isPresent())
        {
            final Entity entity = cir.getReturnValue().get();
            if (entity.getType() == EntityType.ZOMBIE_VILLAGER || entity.getType() == EntityType.VILLAGER)
            {
                if (entity instanceof Mob)
                {
                    ((Mob) entity).setPersistenceRequired();
                }
            }
        }
    }
}
