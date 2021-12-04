package com.hostilevillages;

import com.google.common.collect.ImmutableList;
import com.hostilevillages.command.CommandFindPersistent;
import com.hostilevillages.config.Configuration;
import com.hostilevillages.event.EventHandler;
import com.hostilevillages.event.ModEventHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
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
import java.util.function.Supplier;

import static com.hostilevillages.HostileVillages.MODID;
import static net.minecraft.core.Registry.TEMPLATE_POOL_REGISTRY;
import static net.minecraft.data.worldgen.ProcessorLists.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MODID)
public class HostileVillages
{
    public static final String MODID = "hostilevillages";

    public static final Random        rand   = new Random();
    public static final Logger        LOGGER = LogManager.getLogger();
    public static       Configuration config = new Configuration();

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
        for (final String name : Arrays.asList("plains", "savanna", "snowy", "taiga", "desert"))
        {
            final List<StructurePoolElement> list =
              event.getServer().registryAccess().registry(TEMPLATE_POOL_REGISTRY).get().get(new ResourceLocation("minecraft:village/" + name + "/zombie/houses")).templates;

            for (final String structure : HostileVillages.config.getCommonConfig().additionalStructures.get())
            {
                for (int i = 0; i < config.getCommonConfig().additionalStructuresWeight.get(); i++)
                {
                    list.add(StructurePoolElement.legacy(structure).apply(StructureTemplatePool.Projection.RIGID));
                }
            }
        }
    }

    /**
     * Predefined patterns
     */
    private static Map<ResourceLocation, StructureTemplatePool> patterns = new HashMap<>();
    static
    {
        int villageChance = config.getCommonConfig().vanillaVillageChance.get();
        int zombieChance = 100 - villageChance;

        int villageMin = villageChance > 0 ? 1 : 0;

        StructureTemplatePool plains = new StructureTemplatePool(new ResourceLocation("village/plains/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_fountain_01", ProcessorLists.MOSSIFY_20_PERCENT),
              Math.max(villageMin, villageChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_1", ProcessorLists.MOSSIFY_20_PERCENT), Math.max(villageMin, villageChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_2"), Math.max(villageMin, villageChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_3", ProcessorLists.MOSSIFY_70_PERCENT), Math.max(villageMin, villageChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_fountain_01", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_1", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_2", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4)),
            Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_3", ZOMBIE_PLAINS), Math.max(1, zombieChance / 4))),
          StructureTemplatePool.Projection.RIGID);

        StructureTemplatePool snowy = new StructureTemplatePool(new ResourceLocation("village/snowy/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_1"), Math.max(villageMin, villageChance / 3)),
            Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_2"), Math.max(villageMin, villageChance / 6)),
            Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_3"), Math.max(villageMin, villageChance / 2)),
            Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_1"), Math.max(1, zombieChance / 3)),
            Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_2"), Math.max(1, zombieChance / 6)),
            Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_3"), Math.max(1, zombieChance / 2))),
          StructureTemplatePool.Projection.RIGID);

        StructureTemplatePool savanna = new StructureTemplatePool(new ResourceLocation("village/savanna/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_1"), (int) (Math.max(villageMin, villageChance / 4.5))),
            Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_2"), (int) (Math.max(villageMin, villageChance / 9))),
            Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_3"), (int) (Math.max(villageMin, villageChance / 3))),
            Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_4"), (int) (Math.max(villageMin, villageChance / 3))),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_1", ZOMBIE_SAVANNA), Math.max(1, (int) (zombieChance / 4.5))),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_2", ZOMBIE_SAVANNA), Math.max(1, zombieChance / 9)),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_3", ZOMBIE_SAVANNA), Math.max(1, zombieChance / 3)),
            Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_4", ZOMBIE_SAVANNA), Math.max(1, zombieChance / 3))),
          StructureTemplatePool.Projection.RIGID);

        StructureTemplatePool desert = new StructureTemplatePool(new ResourceLocation("village/desert/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_1"), (int) (Math.max(villageMin, villageChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_2"), (int) (Math.max(villageMin, villageChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_3"), (int) (Math.max(villageMin, villageChance / 5))),
            Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_1", ZOMBIE_DESERT), Math.max(1, (int) (zombieChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_2", ZOMBIE_DESERT), Math.max(1, (int) (zombieChance / 2.5))),
            Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_3", ZOMBIE_DESERT), Math.max(1, zombieChance / 5))),
          StructureTemplatePool.Projection.RIGID);

        StructureTemplatePool taiga = new StructureTemplatePool(new ResourceLocation("village/taiga/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(StructurePoolElement.legacy("village/taiga/town_centers/taiga_meeting_point_1", ProcessorLists.MOSSIFY_10_PERCENT),
              (int) Math.max(villageMin, (villageChance / 2))),
            Pair.of(StructurePoolElement.legacy("village/taiga/town_centers/taiga_meeting_point_2", ProcessorLists.MOSSIFY_10_PERCENT),
              (int) Math.max(villageMin, (villageChance / 2))),
            Pair.of(StructurePoolElement.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_1", ZOMBIE_TAIGA), Math.max(1, (int) (zombieChance / 2))),
            Pair.of(StructurePoolElement.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_2", ZOMBIE_TAIGA), Math.max(1, (int) (zombieChance / 2)))),
          StructureTemplatePool.Projection.RIGID);

        patterns.put(plains.getName(), plains);
        patterns.put(snowy.getName(), snowy);
        patterns.put(savanna.getName(), savanna);
        patterns.put(desert.getName(), desert);
        patterns.put(taiga.getName(), taiga);
    }
    /**
     * Get the replacement pattern for the given pattern
     *
     * @param pattern given
     * @return possible replacement
     */
    public static StructureTemplatePool getReplacement(final StructureTemplatePool pattern)
    {
        if (patterns.containsKey(pattern.getName()))
        {
            return patterns.get(pattern.getName());
        }

        return null;
    }

    public static ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> newListOf(Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>... args)
    {
        List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> list = new ArrayList<>(Arrays.asList(args));
        list.removeIf(element -> element.getSecond() <= 0);
        return ImmutableList.copyOf(list);
    }

    /**
     * Put here instead of in the interface, due to java 11 breaking with interface lambdas
     *
     * @param iTagCollection
     * @param <T>
     * @return
     */
    public static <T> Codec<Tag<T>> tagCodec(Supplier<TagCollection<T>> iTagCollection)
    {
        return ResourceLocation.CODEC.flatXmap((p_232949_1_) -> {
            return Optional.ofNullable(iTagCollection.get().getTag(p_232949_1_)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown tag: " + p_232949_1_);
            });
        }, (tag) -> {

            ResourceLocation id = iTagCollection.get().getId(tag);
            if (id == null)
            {
                // Fallback id lookup
                Collection<ResourceLocation> tags = iTagCollection.get().getMatchingTags(tag.getValues().get(0));

                for (final ResourceLocation currentID : tags)
                {
                    final Tag compare = iTagCollection.get().getTag(currentID);
                    if (compare != null)
                    {
                        if (compare.getValues().equals(tag.getValues()))
                        {
                            id = currentID;
                            break;
                        }
                    }
                }
            }

            return Optional.ofNullable(id).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown tag: " + tag);
            });
        });
    }
}
