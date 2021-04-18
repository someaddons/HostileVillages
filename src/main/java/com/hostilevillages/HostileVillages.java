package com.hostilevillages;

import com.google.common.collect.ImmutableList;
import com.hostilevillages.config.Configuration;
import com.hostilevillages.event.EventHandler;
import com.hostilevillages.event.ModEventHandler;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.template.ProcessorLists;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;

import static com.hostilevillages.HostileVillages.MODID;
import static net.minecraft.world.gen.feature.template.ProcessorLists.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MODID)
public class HostileVillages
{
    public static final String MODID = "hostilevillages";

    public static final Random        rand   = new Random();
    public static final Logger        LOGGER = LogManager.getLogger();
    public static       Configuration config = new Configuration();

    static double percent = 0d;

    public HostileVillages()
    {
        ModLoadingContext.get()
          .registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> org.apache.commons.lang3.tuple.Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ModEventHandler.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        RandomVillageDataSet.parseFromConfig();
        LOGGER.info("Hostile Villages initialized");
    }

    /**
     * Predefined patterns
     */
    private static Map<ResourceLocation, JigsawPattern> patterns = new HashMap<>();
    static
    {
        percent = config.getCommonConfig().vanillaVillageChance.get() / 100d;

        JigsawPattern plains = new JigsawPattern(new ResourceLocation("village/plains/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(JigsawPiece.legacy("village/plains/town_centers/plains_fountain_01", ProcessorLists.MOSSIFY_20_PERCENT), (int) (percent * 50)),
            Pair.of(JigsawPiece.legacy("village/plains/town_centers/plains_meeting_point_1", ProcessorLists.MOSSIFY_20_PERCENT), (int) (percent * 50)),
            Pair.of(JigsawPiece.legacy("village/plains/town_centers/plains_meeting_point_2"), (int) (percent * 50)),
            Pair.of(JigsawPiece.legacy("village/plains/town_centers/plains_meeting_point_3", ProcessorLists.MOSSIFY_70_PERCENT), (int) (percent * 50)),
            Pair.of(JigsawPiece.legacy("village/plains/zombie/town_centers/plains_fountain_01", ZOMBIE_PLAINS), 1),
            Pair.of(JigsawPiece.legacy("village/plains/zombie/town_centers/plains_meeting_point_1", ZOMBIE_PLAINS), 1),
            Pair.of(JigsawPiece.legacy("village/plains/zombie/town_centers/plains_meeting_point_2", ZOMBIE_PLAINS), 1),
            Pair.of(JigsawPiece.legacy("village/plains/zombie/town_centers/plains_meeting_point_3", ZOMBIE_PLAINS), 1)),
          JigsawPattern.PlacementBehaviour.RIGID);

        JigsawPattern snowy = new JigsawPattern(new ResourceLocation("village/snowy/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(JigsawPiece.legacy("village/snowy/town_centers/snowy_meeting_point_1"), (int) (percent * 100)),
            Pair.of(JigsawPiece.legacy("village/snowy/town_centers/snowy_meeting_point_2"), (int) (percent * 50)),
            Pair.of(JigsawPiece.legacy("village/snowy/town_centers/snowy_meeting_point_3"), (int) (percent * 150)),
            Pair.of(JigsawPiece.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_1"), 2),
            Pair.of(JigsawPiece.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_2"), 1),
            Pair.of(JigsawPiece.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_3"), 3)),
          JigsawPattern.PlacementBehaviour.RIGID);

        JigsawPattern savanna = new JigsawPattern(new ResourceLocation("village/savanna/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(JigsawPiece.legacy("village/savanna/town_centers/savanna_meeting_point_1"), (int) (percent * 100)),
            Pair.of(JigsawPiece.legacy("village/savanna/town_centers/savanna_meeting_point_2"), (int) (percent * 50)),
            Pair.of(JigsawPiece.legacy("village/savanna/town_centers/savanna_meeting_point_3"), (int) (percent * 150)),
            Pair.of(JigsawPiece.legacy("village/savanna/town_centers/savanna_meeting_point_4"), (int) (percent * 150)),
            Pair.of(JigsawPiece.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_1", ZOMBIE_SAVANNA), 2),
            Pair.of(JigsawPiece.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_2", ZOMBIE_SAVANNA), 1),
            Pair.of(JigsawPiece.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_3", ZOMBIE_SAVANNA), 3),
            Pair.of(JigsawPiece.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_4", ZOMBIE_SAVANNA), 3)),
          JigsawPattern.PlacementBehaviour.RIGID);

        JigsawPattern desert = new JigsawPattern(new ResourceLocation("village/desert/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(JigsawPiece.legacy("village/desert/town_centers/desert_meeting_point_1"), (int) (percent * 98)),
            Pair.of(JigsawPiece.legacy("village/desert/town_centers/desert_meeting_point_2"), (int) (percent * 98)),
            Pair.of(JigsawPiece.legacy("village/desert/town_centers/desert_meeting_point_3"), (int) (percent * 49)),
            Pair.of(JigsawPiece.legacy("village/desert/zombie/town_centers/desert_meeting_point_1", ZOMBIE_DESERT), 2),
            Pair.of(JigsawPiece.legacy("village/desert/zombie/town_centers/desert_meeting_point_2", ZOMBIE_DESERT), 2),
            Pair.of(JigsawPiece.legacy("village/desert/zombie/town_centers/desert_meeting_point_3", ZOMBIE_DESERT), 1)),
          JigsawPattern.PlacementBehaviour.RIGID);

        JigsawPattern taiga = new JigsawPattern(new ResourceLocation("village/taiga/town_centers"),
          new ResourceLocation("empty"),
          newListOf(Pair.of(JigsawPiece.legacy("village/taiga/town_centers/taiga_meeting_point_1", ProcessorLists.MOSSIFY_10_PERCENT), (int) (percent * 49)),
            Pair.of(JigsawPiece.legacy("village/taiga/town_centers/taiga_meeting_point_2", ProcessorLists.MOSSIFY_10_PERCENT), (int) (percent * 49)),
            Pair.of(JigsawPiece.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_1", ZOMBIE_TAIGA), 1),
            Pair.of(JigsawPiece.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_2", ZOMBIE_TAIGA), 1)),
          JigsawPattern.PlacementBehaviour.RIGID);

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
    public static JigsawPattern getReplacement(final JigsawPattern pattern)
    {
        if (patterns.containsKey(pattern.getName()))
        {
            return patterns.get(pattern.getName());
        }

        return null;
    }

    public static ImmutableList<Pair<Function<JigsawPattern.PlacementBehaviour, ? extends JigsawPiece>, Integer>> newListOf(Pair<Function<JigsawPattern.PlacementBehaviour, ? extends JigsawPiece>, Integer>... args)
    {
        List<Pair<Function<JigsawPattern.PlacementBehaviour, ? extends JigsawPiece>, Integer>> list = new ArrayList<>(Arrays.asList(args));
        list.removeIf(element -> element.getSecond() <= 0);
        return ImmutableList.copyOf(list);
    }
}
