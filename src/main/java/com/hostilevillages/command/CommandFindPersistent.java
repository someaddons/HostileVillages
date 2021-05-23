package com.hostilevillages.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.INPC;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.minecart.ChestMinecartEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Teleport to a persistent entity outside of a dungeon
 */
public class CommandFindPersistent implements IMCOPCommand
{
    private static Set<Entity> visited = new HashSet<>();

    @Override
    public int onExecute(final CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        if (source.getEntity() == null)
        {
            return 0;
        }

        searchAndTP(source);

        return 0;
    }

    private void searchAndTP(final CommandSource source)
    {
        // KIll all with leather cap?/mending armor
        // KIll all non-undead with leather cap
        // Check chest minecarts
        final PlayerEntity player = (PlayerEntity) source.getEntity();

        int killedEntities = 0;
        for (final Entity searchEntity : ((ServerWorld) player.level).getAllEntities())
        {
            if (searchEntity instanceof ChestMinecartEntity && !visited.contains(searchEntity))
            {
                visited.add(searchEntity);
                player.teleportTo(searchEntity.getX(), searchEntity.getY(), searchEntity.getZ());
                source.sendSuccess(new StringTextComponent("Found entity:" + searchEntity.getDisplayName().getString()), false);
                break;
            }

            if (!(searchEntity instanceof MobEntity) || searchEntity instanceof INPC)
            {
                continue;
            }

            // Instakill entities
            if ((((MobEntity) searchEntity).getMobType() != CreatureAttribute.UNDEAD) && ((MobEntity) searchEntity).isPersistenceRequired())
            {
                if (ItemStack.matches(((MobEntity) searchEntity).getItemBySlot(EquipmentSlotType.HEAD), Items.LEATHER_HELMET.getDefaultInstance()))
                {
                    searchEntity.remove();
                    killedEntities++;
                    continue;
                }
            }

            // Skip animals at the sky
            if ((searchEntity instanceof AnimalEntity && searchEntity.level.canSeeSky(searchEntity.blockPosition())))
            {
                continue;
            }

            if (((MobEntity) searchEntity).isPersistenceRequired() && !visited.contains(searchEntity))
            {
                boolean isInStructure = false;
                for (final Map.Entry<Structure<?>, StructureStart<?>> entry : ((ServerWorld) searchEntity.level).getChunk(searchEntity.xChunk, searchEntity.zChunk)
                                                                                .getAllStarts()
                                                                                .entrySet())
                {
                    if (entry.getValue().getBoundingBox().isInside(searchEntity.blockPosition()))
                    {
                        isInStructure = true;
                        break;
                    }
                }

                if (!isInStructure)
                {
                    visited.add(searchEntity);
                    player.teleportTo(searchEntity.getX(), searchEntity.getY(), searchEntity.getZ());
                    source.sendSuccess(new StringTextComponent("Found entity:" + searchEntity.getDisplayName().getString()), false);
                    source.sendSuccess(new StringTextComponent("Killed entities:" + killedEntities), false);
                    break;
                }
            }
        }
    }

    @Override
    public String getName()
    {
        return "tpToPersistent";
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> build()
    {
        return
          IMCCommand.newLiteral(getName())
            .then(IMCCommand.newArgument("clear", StringArgumentType.string()).executes(this::clearVisited)).executes(this::checkPreConditionAndExecute);
    }

    private int clearVisited(final CommandContext<CommandSource> context)
    {
        if (!checkPreCondition(context))
        {
            return 0;
        }

        context.getSource().sendSuccess(new StringTextComponent("Cleared visited entity list"), false);
        visited = new HashSet<>();
        return 0;
    }
}
