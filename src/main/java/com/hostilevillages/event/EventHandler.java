package com.hostilevillages.event;

import com.hostilevillages.HostileVillages;
import com.hostilevillages.RandomVillageDataSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    private final static int MAX_VILLAGE_DISTANCE = 200 * 200;

    private static BlockPos             lastSpawn      = BlockPos.ZERO;
    private static RandomVillageDataSet villageDataSet = new RandomVillageDataSet();

    /**
     * Filled by worldgen threads, emptied by the server thread
     */
    private static List<Tuple<Entity, ServerLevel>> toAdd = Collections.synchronizedList(new ArrayList<>());

    /**
     * Longest time the queue waits for the chunk of an entity to finish generating
     */
    private final static long MAX_CHUNK_WAIT = TimeUnit.MINUTES.toNanos(5);

    private static Tuple<Entity, ServerLevel> waitingEntry = null;
    private static long                       waitingSince = 0;

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
        if (entity instanceof IronGolem || entity.getType() == EntityType.IRON_GOLEM)
        {
            if (entity.blockPosition().distSqr(lastSpawn) < MAX_VILLAGE_DISTANCE && villageDataSet != null && villageDataSet.isValid(entity.level()))
            {
                return true;
            }
        }

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
            while (!toAdd.isEmpty() && !shouldWaitForChunk(toAdd.get(0)))
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

    /**
     * The spawn setup looks up the difficulty at the entity, which reads its chunk whenever a player or a pregenerator has requested
     * that chunk. While the chunk is still generating the server thread would have to wait for it, so the entity stays queued until
     * the chunk is done or no longer requested.
     *
     * @param tuple next queued entity and its level
     * @return true if the queue should wait
     */
    private static boolean shouldWaitForChunk(final Tuple<Entity, ServerLevel> tuple)
    {
        final Entity entity = tuple.getA();
        final ServerLevel level = tuple.getB();
        final BlockPos pos = entity.blockPosition();

        if (!(entity instanceof Mob) || !level.hasChunkAt(pos) || level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null)
        {
            waitingEntry = null;
            return false;
        }

        if (waitingEntry != tuple)
        {
            waitingEntry = tuple;
            waitingSince = System.nanoTime();

            if (HostileVillages.config.getCommonConfig().debugLog)
            {
                HostileVillages.LOGGER.info("Delaying spawn until the chunk has finished generating for entity: " + entity);
            }

            return true;
        }

        if (System.nanoTime() - waitingSince >= MAX_CHUNK_WAIT)
        {
            HostileVillages.LOGGER.warn("Chunk did not finish generating within five minutes, spawning anyway: " + entity);
            waitingEntry = null;
            return false;
        }

        return true;
    }
}
