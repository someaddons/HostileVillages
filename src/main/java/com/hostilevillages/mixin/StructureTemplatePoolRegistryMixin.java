package com.hostilevillages.mixin;

import com.hostilevillages.HostileVillages;
import net.minecraft.core.Holder;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pools.class)
/**
 * Redirect the registry to add replacements
 */
public class StructureTemplatePoolRegistryMixin
{
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void onregister(final StructureTemplatePool pattern, final CallbackInfoReturnable<Holder<StructureTemplatePool>> cir)
    {
        final StructureTemplatePool rpattern = HostileVillages.getReplacement(pattern);

        if (rpattern != null)
        {
            cir.setReturnValue(BuiltinRegistries.register(BuiltinRegistries.TEMPLATE_POOL, rpattern.getName(), rpattern));
        }
    }
}
