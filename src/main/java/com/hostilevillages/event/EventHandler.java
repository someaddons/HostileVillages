package com.hostilevillages.event;

import com.hostilevillages.HostileVillages;
import com.hostilevillages.RandomVillageDataSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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

    private static List<Tuple<Entity, ServerLevel>> toAdd = new ArrayList<>();

    @SubscribeEvent
    public static void onEntityAdd(final EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide)
        {
            return;
        }

        if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn)
        {
            return;
        }

        if (event.getEntity().getType() == EntityType.ZOMBIE_VILLAGER)
        {
            return;
        }

        if (replaceEntityOnSpawn(event.getEntity(), (ServerLevelAccessor) event.getLevel()))
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
    public static boolean replaceEntityOnSpawn(final Entity entity, final ServerLevelAccessor world)
    {
        if (entity.getType() == EntityType.VILLAGER || entity.getType() == EntityType.ZOMBIE_VILLAGER)
        {
            if (HostileVillages.config.getCommonConfig().allowVanillaVillagerSpawn && entity.getType() == EntityType.VILLAGER)
            {
                return false;
            }

            if (entity.blockPosition().distSqr(lastSpawn) > MAX_VILLAGE_DISTANCE || (villageDataSet != null && !villageDataSet.isValid(entity.level())))
            {
                villageDataSet = new RandomVillageDataSet();
                villageDataSet.setWorldTimeStart(entity.level().getGameTime());
            }

            lastSpawn = entity.blockPosition();

            if (villageDataSet == null)
            {
                return false;
            }

            if (entity.getType() == EntityType.VILLAGER && entity.getTags().contains("feywild_librarian"))
            {
                return false;
            }

            entity.remove(Entity.RemovalReason.DISCARDED);

            boolean requirePersistance = entity instanceof Mob && ((Mob) entity).isPersistenceRequired();

            for (int i = 0; i < HostileVillages.config.getCommonConfig().hostilePopulationSize; i++)
            {
                final Entity replacementEntity = villageDataSet.getEntityReplacement().create(world.getLevel());

                if (replacementEntity.getType() == EntityType.VILLAGER)
                {
                    // Do not circle spawn entities
                    continue;
                }

                if (!(replacementEntity instanceof Mob))
                {
                    continue;
                }

                if (requirePersistance)
                {
                    ((Mob) replacementEntity).setPersistenceRequired();
                }
                else if (replacementEntity.getType().getCategory().isPersistent() || ((Mob) replacementEntity).isPersistenceRequired())
                {
                    continue;
                }

                if (HostileVillages.config.getCommonConfig().debugLog)
                {
                    HostileVillages.LOGGER.info(
                      "Replacing entity: " + entity + " with entity: " + replacementEntity + " persistence:" + ((Mob) replacementEntity).isPersistenceRequired());
                }

                replacementEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                toAdd.add(new Tuple<>(replacementEntity, world.getLevel()));
            }

            return true;
        }

        return false;
    }

    @SubscribeEvent
    public static void addToWorld(final TickEvent.LevelTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START || event.level.isClientSide)
        {
            return;
        }

        if (!toAdd.isEmpty())
        {
            while (!toAdd.isEmpty())
            {
                Tuple<Entity, ServerLevel> tuple = toAdd.remove(0);
                tuple.getB().addFreshEntity(tuple.getA());

                if (villageDataSet != null && tuple.getA() instanceof Mob)
                {
                    villageDataSet.onEntitySpawn((Mob) tuple.getA(), tuple.getB());
                }
            }
        }
    }
}
