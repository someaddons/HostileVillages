package com.hostilevillages.event;

import com.hostilevillages.HostileVillages;
import com.hostilevillages.RandomVillageDataSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    private final static int MAX_VILLAGE_DISTANCE = 200 * 200;

    private static BlockPos             lastSpawn      = BlockPos.ZERO;
    private static RandomVillageDataSet villageDataSet = new RandomVillageDataSet();

    private static List<Tuple<Entity, World>> toAdd = new ArrayList<>();

    private static EntityType excludedZombieVillager;

    @SubscribeEvent
    public static void onLivingSpawn(final LivingSpawnEvent.CheckSpawn event)
    {
        if (event.getEntity().getType() != EntityType.ZOMBIE_VILLAGER || event.getEntity().level.isClientSide)
        {
            return;
        }

        excludedZombieVillager = event.getEntity().getType();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preLivingConversionEvent(final LivingConversionEvent.Pre event)
    {
        // Exclude natural spawn from village mechanics
        if (!event.getEntity().level.isClientSide && event.getOutcome() == EntityType.ZOMBIE_VILLAGER)
        {
            excludedZombieVillager = event.getOutcome();
        }
    }

    @SubscribeEvent
    public static void onEntityAdd(final EntityJoinWorldEvent event)
    {
        if (event.getWorld().isClientSide)
        {
            return;
        }

        if (event.getEntity().getType() == excludedZombieVillager)
        {
            excludedZombieVillager = null;

            if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get())
            {
                return;
            }
        }

        if (replaceEntityOnSpawn(event.getEntity(), (IServerWorld) event.getWorld()))
        {
            event.setCanceled(true);
        }
    }

    /**
     * Replaces the spawning entity with a different entity
     *
     * @param entity entity to spawn
     * @param world  world ot spawn in
     * @return true if replaced
     */
    private static boolean replaceEntityOnSpawn(final Entity entity, final IServerWorld world)
    {
        if (entity.getType() == EntityType.VILLAGER || entity.getType() == EntityType.ZOMBIE_VILLAGER)
        {
            if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn.get() && entity.getType() == EntityType.VILLAGER)
            {
                return false;
            }

            if (entity.blockPosition().distSqr(lastSpawn) > MAX_VILLAGE_DISTANCE || (villageDataSet != null && !villageDataSet.isValid(entity.level)))
            {
                villageDataSet = new RandomVillageDataSet();
                villageDataSet.setWorldTimeStart(entity.level.getGameTime());
            }

            lastSpawn = entity.blockPosition();

            if (villageDataSet == null)
            {
                return false;
            }

            entity.remove();

            boolean requirePersistance = entity instanceof MobEntity && ((MobEntity) entity).isPersistenceRequired();

            for (int i = 0; i < HostileVillages.config.getCommonConfig().hostilePopulationSize.get(); i++)
            {
                final Entity replacementEntity = villageDataSet.getEntityReplacement().create(world.getLevel());

                if (!(replacementEntity instanceof MobEntity))
                {
                    continue;
                }

                if (requirePersistance)
                {
                    ((MobEntity) replacementEntity).setPersistenceRequired();
                }
                else if (replacementEntity.getType().getCategory().isPersistent() || ((MobEntity) replacementEntity).isPersistenceRequired())
                {
                    continue;
                }

                if (HostileVillages.config.getCommonConfig().debugLog.get())
                {
                    HostileVillages.LOGGER.info(
                      "Replacing entity: " + entity + " with entity: " + replacementEntity + " persistence:" + ((MobEntity) replacementEntity).isPersistenceRequired());
                }

                replacementEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                toAdd.add(new Tuple<>(replacementEntity, world.getLevel()));
            }

            return true;
        }

        return false;
    }

    @SubscribeEvent
    public static void addToWorld(final TickEvent.WorldTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START || event.world.isClientSide)
        {
            return;
        }

        if (!toAdd.isEmpty())
        {
            Tuple<Entity, World> tuple = toAdd.remove(0);
            tuple.getB().addFreshEntity(tuple.getA());

            if (villageDataSet != null && tuple.getA() instanceof MobEntity)
            {
                villageDataSet.onEntitySpawn((MobEntity) tuple.getA(), (IServerWorld) event.world);
            }
        }
    }
}
