package com.hostilevillages;

import com.cupboard.config.CupboardConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hostilevillages.command.CommandFindPersistent;
import com.hostilevillages.config.CommonConfiguration;
import com.hostilevillages.event.EventHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class HostileVillages implements ModInitializer {

    public static final String MOD_ID = "hostilevillages";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(MOD_ID, new CommonConfiguration());
    public static Random rand = new Random();

    public static Set<ResourceLocation> villages = new HashSet<>();
    static
    {
        villages.add(new ResourceLocation("worldgen/template_pool/village/plains/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/snowy/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/savanna/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/desert/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/taiga/town_centers.json"));
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((c, o, b) -> c.register(createCommands()));
        ServerTickEvents.START_SERVER_TICK.register(this::serverStart);
        ServerTickEvents.END_SERVER_TICK.register(EventHandler::addToWorld);
    }

    public static void adjustVillageSpawns(final JsonArray elements)
    {
        int villageChance = config.getCommonConfig().vanillaVillageChance;
        int zombieChance = 100 - villageChance;

        for (final JsonElement entry : elements)
        {
            if (entry instanceof JsonObject jsonObject && jsonObject.has("element") && jsonObject.get("element").getAsJsonObject().has("location"))
            {
                if (jsonObject.get("element").getAsJsonObject().get("location").getAsString().contains("zombie"))
                {
                    jsonObject.addProperty("weight", Math.max(1, zombieChance / 4));
                }
                else
                {
                    jsonObject.addProperty("weight", (Math.max(1, villageChance / 4)));
                }
            }
        }
    }

    public LiteralArgumentBuilder<CommandSourceStack> createCommands()
    {
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal("hostilevillages");
        // Adds all command trees to the dispatcher to register the commands.
        return root.then(new CommandFindPersistent().build());
    }

    private void serverStart(final MinecraftServer server)
    {
        //loadWorldgen(event.getServer());
        for (final String name : Arrays.asList("plains", "savanna", "snowy", "taiga", "desert"))
        {
            final List<StructurePoolElement> list =
              server.registryAccess().registry(Registries.TEMPLATE_POOL).get().get(new ResourceLocation("minecraft:village/" + name + "/zombie/houses")).templates;

            for (final String structure : HostileVillages.config.getCommonConfig().additionalStructures)
            {
                for (int i = 0; i < config.getCommonConfig().additionalStructuresWeight; i++)
                {
                    list.add(StructurePoolElement.legacy(structure).apply(StructureTemplatePool.Projection.RIGID));
                }
            }
        }
    }
}
