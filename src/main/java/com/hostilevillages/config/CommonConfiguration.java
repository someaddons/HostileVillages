package com.hostilevillages.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonConfiguration
{
    public final ForgeConfigSpec.ConfigValue<Integer>                vanillaVillageChance;
    public final ForgeConfigSpec.ConfigValue<Integer>                hostilePopulationSize;
    public final ForgeConfigSpec.ConfigValue<Integer>                additionalStructuresWeight;
    public final ForgeConfigSpec.ConfigValue<Boolean>                generateLoot;
    public final ForgeConfigSpec.ConfigValue<Boolean>                villagesSpawnEggLoot;
    public final ForgeConfigSpec.ConfigValue<Boolean>                debugLog;
    public final ForgeConfigSpec.ConfigValue<Boolean>                disableNoEntityDespawnWhenPickingItem;
    public final ForgeConfigSpec.ConfigValue<Boolean>                allowVanillaVillagerSpawn;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageEntityTypes;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> additionalStructures;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> loottables;
    public final ForgeConfigSpec                                     ForgeConfigSpecBuilder;

    protected CommonConfiguration(final ForgeConfigSpec.Builder builder)
    {
        builder.push("Hostile Villages settings");
        builder.comment("Percentage of how likely normal,non-zombie villages are to spawn. default: 0");
        vanillaVillageChance = builder.defineInRange("vanillaVillageChance", 0, 0, 100);

        builder.comment("Set higher to increase the generated population of the hostile village, default: 5");
        hostilePopulationSize = builder.defineInRange("hostilePopulationSize", 5, 1, 100);

        builder.comment("Whether to generate extra loot for the village, default: true");
        generateLoot = builder.define("generateLoot", true);

        builder.comment("Turn on debug messages for spawning, default: false");
        debugLog = builder.define("debugLog", false);

        builder.comment("Disables entities beeing unable to despawn after they get an item equipped, default: true");
        disableNoEntityDespawnWhenPickingItem = builder.define("disableNoEntityDespawnWhenPickingItem", true);

        builder.comment("List of loottables to use, default: minecraft:chests/simple_dungeon");
        loottables = builder.defineList("loottables",
          Arrays.asList(
            "minecraft:chests/simple_dungeon"
          )
          , e -> e instanceof String && ((String) e).contains(":"));

        builder.comment("Whether to allow vanilla villagers to spawn at all. default: false");
        allowVanillaVillagerSpawn = builder.define("allowVanillaVillagerSpawn", false);

        builder.comment("If enabled and vanilla villager spawn is enabled then hostile villages will get a villager spawn egg added to their loot. default: true");
        villagesSpawnEggLoot = builder.define("villagesSpawnEggLoot", true);

        builder.comment(
          "List of entity pairs which spawn in villages. Format = entity1;entity2;5;6   5 is the chance of entity2(one in five), 6 is the total weight of this whole entry to be chosen");
        villageEntityTypes = builder.defineList("villageEntityTypes",
          Arrays.asList(
            "minecraft:zombie;minecraft:stray;3;14",
            "minecraft:skeleton;minecraft:panda;5;6",
            "minecraft:husk;minecraft:spider;3;15",
            "minecraft:creeper;minecraft:cave_spider;3;11",
            "minecraft:slime;minecraft:rabbit;2;9",
            "minecraft:stray;minecraft:wither_skeleton;3;8",
            "minecraft:zombified_piglin;minecraft:pig;3;7",
            "minecraft:snow_golem;minecraft:sheep;3;5",
            "minecraft:witch;minecraft:bat;3;15",
            "minecraft:vindicator;minecraft:illusioner;5;3",
            "minecraft:pillager;minecraft:evoker;7;7")
          , e -> e instanceof String && ((String) e).contains(":"));

        builder.comment(
          "Additional structures to add as houses for spawning zombie villages, default: []. Example for bountiful and waystones support: [\"bountiful:village/common/bounty_gazebo\", \"waystones:village/common/waystone\"]");
        additionalStructures = builder.defineList("additionalStructures",
          ArrayList::new
          , e -> e instanceof String && ResourceLocation.tryParse((String) e) != null);

        builder.comment("Set higher to increase the amount of additional structures generated, note those replace houses, default: 2");
        additionalStructuresWeight = builder.defineInRange("additionalStructuresWeight", 2, 1, 100);

        // Escapes the current category level
        builder.pop();
        ForgeConfigSpecBuilder = builder.build();
    }
}
