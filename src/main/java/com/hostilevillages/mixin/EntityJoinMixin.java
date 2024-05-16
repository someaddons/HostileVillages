package com.hostilevillages.mixin;

import com.hostilevillages.event.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
public class EntityJoinMixin <T extends EntityAccess>
{
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void onJoin(final T entityAccess, final boolean bl, final CallbackInfoReturnable<Boolean> cir)
    {
        if (EventHandler.onEntityAdd((Entity) entityAccess))
        {
            cir.setReturnValue(false);
        }
    }
}
