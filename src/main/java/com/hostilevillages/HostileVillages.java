package com.hostilevillages;

import com.google.common.collect.ImmutableList;
import com.hostilevillages.command.CommandFindPersistent;
import com.hostilevillages.config.Configuration;
import com.hostilevillages.event.EventHandler;
import com.hostilevillages.event.ModEventHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;

import static com.hostilevillages.HostileVillages.MODID;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MODID)
public class HostileVillages
{
    public static final String MODID = "hostilevillages";

    public static final Random                               rand     = new Random();
    public static final Logger                               LOGGER   = LogManager.getLogger();
    public static       Configuration                        config   = new Configuration();
    public static       Map<ResourceLocation, ImmutableList> villages = new HashMap<>();

    public HostileVillages()
    {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (c, b) -> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, config.getCommonConfig().ForgeConfigSpecBuilder);
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ModEventHandler.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().addListener(this::serverStart);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().addListener(this::onCommandsRegister);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
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
        loadWorldgen(event.getServer());
        for (final String name : Arrays.asList("plains", "savanna", "snowy", "taiga", "desert"))
        {
            final List<StructurePoolElement> list =
              event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).get().get(new ResourceLocation("minecraft:village/" + name + "/zombie/houses")).templates;

            for (final String structure : HostileVillages.config.getCommonConfig().additionalStructures.get())
            {
                for (int i = 0; i < config.getCommonConfig().additionalStructuresWeight.get(); i++)
                {
                    list.add(StructurePoolElement.legacy(structure).apply(StructureTemplatePool.Projection.RIGID));
                }
            }
        }
    }

    public static void loadWorldgen(final MinecraftServer server)
    {
        HolderGetter<StructureProcessorList> holdergetter1 = server.registryAccess().lookup(Registries.PROCESSOR_LIST).get();
        Holder<StructureProcessorList> MOSSIFY_20_PERCENT = holdergetter1.getOrThrow(ProcessorLists.MOSSIFY_20_PERCENT);
        Holder<StructureProcessorList> MOSSIFY_10_PERCENT = holdergetter1.getOrThrow(ProcessorLists.MOSSIFY_10_PERCENT);
        Holder<StructureProcessorList> MOSSIFY_70_PERCENT = holdergetter1.getOrThrow(ProcessorLists.MOSSIFY_70_PERCENT);
        Holder<StructureProcessorList> ZOMBIE_PLAINS = holdergetter1.getOrThrow(ProcessorLists.ZOMBIE_PLAINS);
        Holder<StructureProcessorList> ZOMBIE_SAVANNA = holdergetter1.getOrThrow(ProcessorLists.ZOMBIE_SAVANNA);
        Holder<StructureProcessorList> ZOMBIE_DESERT = holdergetter1.getOrThrow(ProcessorLists.ZOMBIE_DESERT);
        Holder<StructureProcessorList> ZOMBIE_TAIGA = holdergetter1.getOrThrow(ProcessorLists.ZOMBIE_TAIGA);

        int villageChance = config.getCommonConfig().vanillaVillageChance.get();
        int zombieChance = 100 - villageChance;

        int villageMin = villageChance > 0 ? 1 : 0;

        ImmutableList plains = HostileVillages.newListOf(Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_fountain_01",
              MOSSIFY_20_PERCENT),
            Math.max(villageMin, villageChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_1", MOSSIFY_20_PERCENT),
            Math.max(villageMin, villageChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_2"), Math.max(villageMin, villageChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_3", MOSSIFY_70_PERCENT),
            Math.max(villageMin, villageChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_fountain_01", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_1", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_2", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)),
          Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_3", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)));

        villages.put(new ResourceLocation("village/plains/terminators"), plains);


        ImmutableList snowy = newListOf(Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_1"), Math.max(villageMin, villageChance / 3)),
          Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_2"), Math.max(villageMin, villageChance / 6)),
          Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_3"), Math.max(villageMin, villageChance / 2)),
          Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_1"), Math.max(1, zombieChance / 3)),
          Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_2"), Math.max(1, zombieChance / 6)),
          Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_3"), Math.max(1, zombieChance / 2)));

        villages.put(new ResourceLocation("asd"), snowy);

        ImmutableList savanna =
          newListOf(Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_1"), (int) (Math.max(villageMin, villageChance / 4.5))),
            Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_2"), (int) (Math.max(villageMin, villageChance / 9))),
            Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_3"), (int) (Math.max(villageMin, villageChance / 3))),
            Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_4"), (int) (Math.max(villageMin, villageChance / 3))),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_1", ZOMBIE_SAVANNA), Math.max(1, (int) (zombieChance / 4.5))),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_2", ZOMBIE_SAVANNA), Math.max(1, zombieChance / 9)),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_3", ZOMBIE_SAVANNA), Math.max(1, zombieChance / 3)),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_4", ZOMBIE_SAVANNA), Math.max(1, zombieChance / 3)));

        villages.put(new ResourceLocation("asd"), savanna);

        ImmutableList desert =
          newListOf(Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_1"), (int) (Math.max(villageMin, villageChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_2"), (int) (Math.max(villageMin, villageChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_3"), (int) (Math.max(villageMin, villageChance / 5))),
            Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_1", ZOMBIE_DESERT), Math.max(1, (int) (zombieChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_2", ZOMBIE_DESERT), Math.max(1, (int) (zombieChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_3", ZOMBIE_DESERT), Math.max(1, zombieChance / 5)));

        villages.put(new ResourceLocation("asd"), desert);

        ImmutableList taiga =
          newListOf(Pair.of(StructurePoolElement.legacy("village/taiga/town_centers/taiga_meeting_point_1", MOSSIFY_10_PERCENT),
              (int) Math.max(villageMin, (villageChance / 2))),
            Pair.of(StructurePoolElement.legacy("village/taiga/town_centers/taiga_meeting_point_2", MOSSIFY_10_PERCENT),
              (int) Math.max(villageMin, (villageChance / 2))),
            Pair.of(StructurePoolElement.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_1", ZOMBIE_TAIGA), Math.max(1, (int) (zombieChance / 2))),
            Pair.of(StructurePoolElement.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_2", ZOMBIE_TAIGA), Math.max(1, (int) (zombieChance / 2))));

        villages.put(new ResourceLocation("asd"), taiga);
    }

    public static ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> newListOf(Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>... args)
    {
        List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> list = new ArrayList<>(Arrays.asList(args));
        list.removeIf(element -> element.getSecond() <= 0);
        return ImmutableList.copyOf(list);
    }
}
