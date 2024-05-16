package com.hostilevillages.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hostilevillages.HostileVillages;
import com.mojang.serialization.Decoder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin
{
    @Inject(method = "loadRegistryContents", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static <E> void onLoad(
      final RegistryOps.RegistryInfoLookup registryInfoLookup,
      final ResourceManager resourceManager,
      final ResourceKey<? extends Registry<E>> resourceKey,
      final WritableRegistry<E> writableRegistry,
      final Decoder<E> decoder,
      final Map<ResourceKey<?>, Exception> map,
      final CallbackInfo ci,
      final String string,
      final FileToIdConverter fileToIdConverter,
      final RegistryOps registryOps,
      final Iterator var9,
      final Map.Entry entry,
      final ResourceLocation resourceLocation,
      final ResourceKey resourceKey2,
      final Resource resource,
      final Reader reader,
      final JsonElement jsonElement)
    {
        if (HostileVillages.villages.contains(resourceLocation))
        {
            if (jsonElement instanceof JsonObject)
            {
                HostileVillages.adjustVillageSpawns(((JsonObject) jsonElement).getAsJsonArray("elements"));
            }
        }
    }
}
