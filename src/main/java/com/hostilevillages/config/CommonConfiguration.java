package com.hostilevillages.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hostilevillages.RandomVillageDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonConfiguration implements ICommonConfig
{
    public int          vanillaVillageChance                  = 0;
    public int          hostilePopulationSize                 = 5;
    public int          additionalStructuresWeight            = 2;
    public boolean      generateLoot                          = true;
    public boolean      villagesSpawnEggLoot                  = true;
    public boolean      debugLog                              = false;
    public boolean      disableNoEntityDespawnWhenPickingItem = true;
    public boolean      allowVanillaVillagerSpawn             = false;
    public List<String> villageEntityTypes;
    public List<String> additionalStructures                  = new ArrayList<>();
    public List<String> loottables;

    public CommonConfiguration()
    {
        loottables = Arrays.asList("minecraft:chests/simple_dungeon");

        villageEntityTypes =
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
            "minecraft:pillager;minecraft:evoker;7;7");
    }

    @Override
    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry = new JsonObject();
        entry.addProperty("desc:", "Percentage of how likely normal,non-zombie villages are to spawn. default: 0");
        entry.addProperty("vanillaVillageChance", vanillaVillageChance);
        root.add("vanillaVillageChance", entry);

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "Set higher to increase the generated population of the hostile village, default: 5");
        entry2.addProperty("hostilePopulationSize", hostilePopulationSize);
        root.add("hostilePopulationSize", entry2);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "Whether to generate extra loot for the village, default: true");
        entry3.addProperty("generateLoot", generateLoot);
        root.add("generateLoot", entry3);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "Turn on debug messages for spawning, default: false");
        entry4.addProperty("debugLog", debugLog);
        root.add("debugLog", entry4);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "Disables entities beeing unable to despawn after they get an item equipped, default: true");
        entry5.addProperty("disableNoEntityDespawnWhenPickingItem", disableNoEntityDespawnWhenPickingItem);
        root.add("disableNoEntityDespawnWhenPickingItem", entry5);

        final JsonObject entry6 = new JsonObject();
        entry6.addProperty("desc:", "List of loottables to use, default: minecraft:chests/simple_dungeon");
        final JsonArray array = new JsonArray();
        for (final String id : loottables)
        {
            array.add(id);
        }
        entry6.add("loottables", array);
        root.add("loottables", entry6);

        final JsonObject entry7 = new JsonObject();
        entry7.addProperty("desc:", "Whether to allow vanilla villagers to spawn at all. default: false");
        entry7.addProperty("allowVanillaVillagerSpawn", allowVanillaVillagerSpawn);
        root.add("allowVanillaVillagerSpawn", entry7);

        final JsonObject entry8 = new JsonObject();
        entry8.addProperty("desc:", "If enabled and vanilla villager spawn is enabled then hostile villages will get a villager spawn egg added to their loot. default: true");
        entry8.addProperty("villagesSpawnEggLoot", villagesSpawnEggLoot);
        root.add("villagesSpawnEggLoot", entry8);


        final JsonObject entry9 = new JsonObject();
        entry9.addProperty("desc:",
          "List of entity pairs which spawn in villages. Format = entity1;entity2;5;6   5 is the chance of entity2(one in five), 6 is the total weight of this whole entry to be chosen");
        final JsonArray types = new JsonArray();
        for (final String id : villageEntityTypes)
        {
            types.add(id);
        }
        entry9.add("villageEntityTypes", types);
        root.add("villageEntityTypes", entry9);

        final JsonObject entry10 = new JsonObject();
        entry10.addProperty("desc:",
          "Additional structures to add as houses for spawning zombie villages, default: []");
        final JsonArray example = new JsonArray();
        example.add("bountiful:village/common/bounty_gazebo");
        example.add("waystones:village/common/waystone");

        entry10.add("Example for bountiful and waystones support(this bracket does nothing, modify the additionalStructures one further below) ", example);

        final JsonArray structures = new JsonArray();
        for (final String id : additionalStructures)
        {
            structures.add(id);
        }
        entry10.add("additionalStructures", structures);
        root.add("additionalStructures", entry10);


        final JsonObject entry11 = new JsonObject();
        entry11.addProperty("desc:", "Set higher to increase the amount of additional structures generated, note those replace houses, default: 2");
        entry11.addProperty("additionalStructuresWeight", additionalStructuresWeight);
        root.add("additionalStructuresWeight", entry11);

        return root;
    }

    @Override
    public void deserialize(JsonObject data)
    {
        vanillaVillageChance = Math.min(100, data.get("vanillaVillageChance").getAsJsonObject().get("vanillaVillageChance").getAsInt());
        hostilePopulationSize = Math.min(100, data.get("hostilePopulationSize").getAsJsonObject().get("hostilePopulationSize").getAsInt());
        generateLoot = data.get("generateLoot").getAsJsonObject().get("generateLoot").getAsBoolean();
        debugLog = data.get("debugLog").getAsJsonObject().get("debugLog").getAsBoolean();
        disableNoEntityDespawnWhenPickingItem = data.get("disableNoEntityDespawnWhenPickingItem").getAsJsonObject().get("disableNoEntityDespawnWhenPickingItem").getAsBoolean();
        final JsonArray lootableData = data.get("loottables").getAsJsonObject().get("loottables").getAsJsonArray();

        loottables = new ArrayList<>();
        for (final JsonElement entry : lootableData)
        {
            loottables.add(entry.getAsString());
        }

        allowVanillaVillagerSpawn = data.get("allowVanillaVillagerSpawn").getAsJsonObject().get("allowVanillaVillagerSpawn").getAsBoolean();
        villagesSpawnEggLoot = data.get("villagesSpawnEggLoot").getAsJsonObject().get("villagesSpawnEggLoot").getAsBoolean();
        additionalStructuresWeight = Math.min(100, data.get("additionalStructuresWeight").getAsJsonObject().get("additionalStructuresWeight").getAsInt());

        JsonArray villageTypes = data.get("villageEntityTypes").getAsJsonObject().get("villageEntityTypes").getAsJsonArray();
        villageEntityTypes = new ArrayList<>();
        for (final JsonElement entry : villageTypes)
        {
            villageEntityTypes.add(entry.getAsString());
        }

        final JsonArray structureData = data.get("additionalStructures").getAsJsonObject().get("additionalStructures").getAsJsonArray();
        additionalStructures = new ArrayList<>();
        for (final JsonElement entry : structureData)
        {
            additionalStructures.add(entry.getAsString());
        }

        RandomVillageDataSet.parseFromConfig();
    }
}
