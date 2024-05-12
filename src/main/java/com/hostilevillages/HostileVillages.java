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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
        villages.add(new ResourceLocation("worldgen/template_pool/village/plains/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/snowy/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/savanna/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/desert/town_centers.json"));
        villages.add(new ResourceLocation("worldgen/template_pool/village/taiga/town_centers.json"));
    }
    public HostileVillages()
    {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (c, b) -> true));
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().addListener(this::serverStart);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().addListener(this::onCommandsRegister);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        //PlainVillagePools.bootstrap(null);
    }

    public static void adjustVillageSpawns(final JsonArray elements)
    {
        int villageChance = config.getCommonConfig().vanillaVillageChance;
        int zombieChance = 100 - villageChance;

        for (final JsonElement entry : elements)
        {
            if (entry instanceof JsonObject jsonObject)
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
              event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).get().get(new ResourceLocation("minecraft:village/" + name + "/zombie/houses")).templates;

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
