package com.hostilevillages;

import com.cupboard.config.CupboardConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hostilevillages.command.CommandFindPersistent;
import com.hostilevillages.config.CommonConfiguration;
import com.hostilevillages.event.EventHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.hostilevillages.HostileVillages.MODID;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MODID)
public class HostileVillages
{
    public static final String MODID = "hostilevillages";

    public static final Random                              rand     = new Random();
    public static final Logger                              LOGGER   = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config   =
      new CupboardConfig<>(MODID, new CommonConfiguration());
    public static       Set<ResourceLocation>               villages = new HashSet<>();
    static
    {
        villages.add(ResourceLocation.withDefaultNamespace("village/plains/town_centers"));
        villages.add(ResourceLocation.withDefaultNamespace("village/snowy/town_centers"));
        villages.add(ResourceLocation.withDefaultNamespace("village/savanna/town_centers"));
        villages.add(ResourceLocation.withDefaultNamespace("village/desert/town_centers"));
        villages.add(ResourceLocation.withDefaultNamespace("village/taiga/town_centers"));
    }
    public HostileVillages(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.register(EventHandler.class);
        NeoForge.EVENT_BUS.addListener(this::serverStart);
        NeoForge.EVENT_BUS.addListener(this::onCommandsRegister);
        modEventBus.addListener(this::setup);
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

    private void setup(final FMLCommonSetupEvent event)
    {
        RandomVillageDataSet.parseFromConfig();
        LOGGER.info("Hostile Villages initialized");
    }

    public void onCommandsRegister(final RegisterCommandsEvent event)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal("hostilevillages");
        // Adds all command trees to the dispatcher to register the commands.
        event.getDispatcher().register(root.then(new CommandFindPersistent().build()));
    }

    private void serverStart(final ServerAboutToStartEvent event)
    {
        //loadWorldgen(event.getServer());
        for (final String name : Arrays.asList("plains", "savanna", "snowy", "taiga", "desert"))
        {
            final List<StructurePoolElement> list =
              event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).get().get(ResourceLocation.tryParse("minecraft:village/" + name + "/zombie/houses")).templates;

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
