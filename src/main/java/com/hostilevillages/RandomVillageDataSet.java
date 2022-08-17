package com.hostilevillages;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represent a random village type
 */
public class RandomVillageDataSet
{
    /**
     * Parsed config list
     */
    public static List<DataEntry> possibleMonsters = new ArrayList<>();
    public static int             totalWeight;

    public static List<ResourceLocation> loottables = new ArrayList<>();

    /**
     * Selected entry
     */
    private DataEntry entry;

    /**
     * Loot stuff
     */
    private ItemStack mendingArmor;

    private int spawnedEntities = 0;

    private long worldTimeStart = 0;

    public RandomVillageDataSet()
    {
        final int chosen = HostileVillages.rand.nextInt(totalWeight);

        int currentWeight = 0;
        for (final DataEntry entry : possibleMonsters)
        {
            if (chosen < entry.weight + currentWeight)
            {
                this.entry = entry;
                break;
            }
            currentWeight += entry.weight;
        }

        if (entry == null)
        {
            entry = possibleMonsters.get(0);
        }

        mendingArmor = new ItemStack(Items.IRON_CHESTPLATE);
        EnchantmentHelper.setEnchantments(Collections.singletonMap(Enchantments.MENDING, 1), mendingArmor);
    }

    public EntityType getEntityReplacement()
    {
        return HostileVillages.rand.nextInt(entry.secondaryChance) > 0 ? entry.main : entry.secondary;
    }

    /**
     * On entity spawn add loot
     *
     * @param entity
     */
    public void onEntitySpawn(final Mob entity, final ServerLevelAccessor world)
    {
        spawnedEntities++;
        // Sun lotion
        if (entity.getMobType() == MobType.UNDEAD && entity.isPersistenceRequired())
        {
            entity.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
        }

        entity.finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.STRUCTURE, null, null);

        // Register the break door goal once, it wont persist but let them break intial doors
        entity.goalSelector.addGoal(0, new BreakDoorGoal(entity, difficulty -> true));
        if (GoalUtils.hasGroundPathNavigation(entity))
        {
            ((GroundPathNavigation) entity.getNavigation()).setCanOpenDoors(true);
        }

        if (!HostileVillages.config.getCommonConfig().generateLoot.get())
        {
            return;
        }

        if (entity.isPersistenceRequired() && spawnedEntities > 12 && mendingArmor != null && (entity.getMobType() == MobType.UNDEAD
                                                                                                 || entity.getMobType() == MobType.ILLAGER))
        {
            entity.setItemSlot(EquipmentSlot.CHEST, mendingArmor);
            entity.setGuaranteedDrop(EquipmentSlot.CHEST);
            mendingArmor = null;
        }

        if (entity.isPersistenceRequired() && spawnedEntities > 12 && spawnedEntities % 5 == 0 && spawnedEntities <= 20)
        {
            final MinecartChest en = EntityType.CHEST_MINECART.create(world.getLevel());
            en.setPos(entity.getX(), entity.getY(), entity.getZ());
            world.addFreshEntity(en);
            en.setLootTable(loottables.get(HostileVillages.rand.nextInt(loottables.size())), HostileVillages.rand.nextInt(509));

            if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get() && HostileVillages.config.getCommonConfig().villagesSpawnEggLoot.get())
            {
                for (int i = 0; i < 27; i++)
                {
                    if (en.getItem(i).getItem() == Items.AIR)
                    {
                        en.setItem(i, Items.VILLAGER_SPAWN_EGG.getDefaultInstance());
                        break;
                    }
                }
            }
        }
    }

    public boolean isValid(final Level world)
    {
        return (world.getGameTime() - worldTimeStart) < 20 * 120;
    }

    public void setWorldTimeStart(final long worldTimeStart)
    {
        this.worldTimeStart = worldTimeStart;
    }

    static class DataEntry
    {
        private DataEntry(final EntityType main, final EntityType secondary, final int secondaryChance, final int weight)
        {
            this.main = main;
            this.secondary = secondary;
            this.secondaryChance = secondaryChance;
            this.weight = weight;
        }

        final EntityType main;
        final EntityType secondary;
        final int        weight;
        final int        secondaryChance;
    }

    public static void parseFromConfig()
    {
        totalWeight = 0;
        possibleMonsters = new ArrayList<>();
        loottables = new ArrayList<>();
        for (final String entry : HostileVillages.config.getCommonConfig().villageEntityTypes.get())
        {
            final String[] splitEntry = entry.split(";");
            if (splitEntry.length != 4)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, wrong amount of parameters: " + entry);
                continue;
            }

            final ResourceLocation main = ResourceLocation.tryParse(splitEntry[0]);
            if (main == null)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, not a valid resource location " + splitEntry[0]);
                continue;
            }

            final EntityType mainType = ForgeRegistries.ENTITY_TYPES.getValue(main);
            if (mainType == null)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, not a valid entity type" + splitEntry[0]);
                continue;
            }

            final ResourceLocation secondary = ResourceLocation.tryParse(splitEntry[1]);
            if (secondary == null)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, not a valid resource location " + splitEntry[1]);
                continue;
            }

            final EntityType secondaryType = ForgeRegistries.ENTITY_TYPES.getValue(secondary);
            if (secondaryType == null)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, not a valid entity type" + splitEntry[1]);
                continue;
            }

            int secondaryChance;
            int weight;
            try
            {
                secondaryChance = Integer.parseInt(splitEntry[2]);
                weight = Integer.parseInt(splitEntry[3]);
            }
            catch (Exception e)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, not a number" + splitEntry[2] + splitEntry[3]);
                continue;
            }

            totalWeight += weight;
            possibleMonsters.add(new DataEntry(mainType, secondaryType, secondaryChance, weight));
        }

        for (final String entry : HostileVillages.config.getCommonConfig().loottables.get())
        {
            final ResourceLocation lootID = ResourceLocation.tryParse(entry);
            if (lootID == null)
            {
                HostileVillages.LOGGER.error("Config entry could not be parsed, not a valid resource location " + entry);
                continue;
            }

            loottables.add(lootID);
        }
    }
}
