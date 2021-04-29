package com.hostilevillages;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.BreakDoorGoal;
import net.minecraft.entity.item.minecart.ChestMinecartEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.GroundPathHelper;
import net.minecraft.util.ResourceLocation;
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
    public void onEntitySpawn(final MobEntity entity)
    {
        // Sun lotion
        entity.equipItemIfPossible(Items.LEATHER_HELMET.getDefaultInstance());

        entity.setPersistenceRequired();

        // Register the break door goal once, it wont persist but let them break intial doors
        entity.goalSelector.addGoal(0, new BreakDoorGoal(entity, difficulty -> true));
        if (GroundPathHelper.hasGroundPathNavigation(entity))
        {
            ((GroundPathNavigator) entity.getNavigation()).setCanOpenDoors(true);
        }

        if (!HostileVillages.config.getCommonConfig().generateLoot.get())
        {
            return;
        }

        if (mendingArmor != null && entity.equipItemIfPossible(mendingArmor))
        {
            entity.setGuaranteedDrop(EquipmentSlotType.CHEST);
            mendingArmor = null;
        }

        if (HostileVillages.rand.nextInt(20) == 0)
        {
            final ChestMinecartEntity en = EntityType.CHEST_MINECART.create(entity.level);
            en.setPos(entity.getX(), entity.getY(), entity.getZ());
            entity.level.addFreshEntity(en);
            en.setLootTable(loottables.get(HostileVillages.rand.nextInt(loottables.size())), HostileVillages.rand.nextInt(509));

            if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get())
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

            final EntityType mainType = ForgeRegistries.ENTITIES.getValue(main);
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

            final EntityType secondaryType = ForgeRegistries.ENTITIES.getValue(secondary);
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
