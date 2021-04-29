package com.hostilevillages.event;

import com.hostilevillages.HostileVillages;
import com.hostilevillages.RandomVillageDataSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.eventbus.api.Event.Result.DENY;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    private final static int MAX_VILLAGE_DISTANCE = 200 * 200;

    private static BlockPos             lastSpawn      = BlockPos.ZERO;
    private static RandomVillageDataSet villageDataSet = null;

    private static List<Tuple<Entity, World>> toAdd = new ArrayList<>();

    private static Entity excludedZombieVillager;

    @SubscribeEvent
    public static void onLivingSpawn(final LivingSpawnEvent.CheckSpawn event)
    {
        if (event.getEntity().getType() != EntityType.ZOMBIE_VILLAGER)
        {
            return;
        }

        if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get())
        {
            event.setResult(DENY);
            return;
        }

        // Exclude natural spawn from village mechanics
        excludedZombieVillager = event.getEntity();
    }

    @SubscribeEvent
    public static void onVillagerKill(final LivingConversionEvent.Pre event)
    {
        if (!HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get())
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onVillagerKill(final LivingConversionEvent.Post event)
    {
        if (event.getOutcome().getType() != EntityType.ZOMBIE_VILLAGER)
        {
            return;
        }

        // Exclude natural spawn from village mechanics
        excludedZombieVillager = event.getOutcome();
    }

    @SubscribeEvent
    public static void onEntityAdd(final EntityJoinWorldEvent event)
    {
        if (event.getEntity() == excludedZombieVillager)
        {
            excludedZombieVillager = null;
            return;
        }

        if (event.getEntity().getType() == EntityType.VILLAGER || event.getEntity().getType() == EntityType.ZOMBIE_VILLAGER)
        {
            if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get() && event.getEntity().getType() == EntityType.VILLAGER)
            {
                return;
            }

            if (event.getEntity().blockPosition().distSqr(lastSpawn) > MAX_VILLAGE_DISTANCE)
            {
                if (HostileVillages.rand.nextInt(100) < HostileVillages.config.getCommonConfig().vanillaVillageChance.get())
                {
                    villageDataSet = null;
                }
                else
                {
                    villageDataSet = new RandomVillageDataSet();
                }
            }

            lastSpawn = event.getEntity().blockPosition();

            if (villageDataSet == null)
            {
                return;
            }

            event.setCanceled(true);
            event.getEntity().remove();

            for (int i = 0; i < HostileVillages.config.getCommonConfig().hostilePopulationSize.get(); i++)
            {
                final Entity entity = villageDataSet.getEntityReplacement().create(event.getWorld());

                if (entity == null)
                {
                    return;
                }

                entity.setPos(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ());
                toAdd.add(new Tuple<>(entity, event.getWorld()));
            }
        }
    }

    @SubscribeEvent
    public static void addToWorld(final TickEvent.WorldTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            return;
        }

        if (!toAdd.isEmpty())
        {
            Tuple<Entity, World> tuple = toAdd.remove(0);
            tuple.getB().addFreshEntity(tuple.getA());

            if (villageDataSet != null && tuple.getA() instanceof MobEntity)
            {
                ((MobEntity) tuple.getA()).setPersistenceRequired();
                villageDataSet.onEntitySpawn((MobEntity) tuple.getA());
            }
        }
    }
}
