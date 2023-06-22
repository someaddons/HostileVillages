package com.hostilevillages.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

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
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        final CommandSourceStack source = context.getSource();
        if (source.getEntity() == null)
        {
            return 0;
        }

        searchAndTP(source);

        return 0;
    }

    private void searchAndTP(final CommandSourceStack source)
    {
        // KIll all with leather cap?/mending armor
        // KIll all non-undead with leather cap
        // Check chest minecarts
        final Player player = (Player) source.getEntity();

        int killedEntities = 0;
        for (final Entity searchEntity : ((ServerLevel) player.level()).getAllEntities())
        {
            if (searchEntity instanceof MinecartChest && !visited.contains(searchEntity))
            {
                visited.add(searchEntity);
                player.teleportTo(searchEntity.getX(), searchEntity.getY(), searchEntity.getZ());
                source.sendSystemMessage(Component.literal("Found entity:" + searchEntity.getDisplayName().getString()));
                break;
            }

            if (!(searchEntity instanceof Mob) || searchEntity instanceof Npc)
            {
                continue;
            }

            // Instakill entities
            if (searchEntity.getY() < 60 && ((Mob) searchEntity).isPersistenceRequired())
            {
                if (((Mob) searchEntity).getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.LEATHER_HELMET ||
                      (((Mob) searchEntity).getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.IRON_CHESTPLATE
                         && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, ((Mob) searchEntity).getItemBySlot(EquipmentSlot.CHEST)) > 0))
                {
                    searchEntity.remove(Entity.RemovalReason.DISCARDED);
                    killedEntities++;
                    continue;
                }
            }

            // Skip animals at the sky
            if ((searchEntity instanceof Animal && searchEntity.level().canSeeSky(searchEntity.blockPosition())))
            {
                continue;
            }

            if (((Mob) searchEntity).isPersistenceRequired() && !visited.contains(searchEntity))
            {
                boolean isInStructure = false;
                for (final Map.Entry<Structure, StructureStart> entry : ((ServerLevel) searchEntity.level()).getChunk(searchEntity.getBlockX() >> 4,
                    searchEntity.getBlockZ() >> 4)
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
                    source.sendSystemMessage(Component.literal("Found entity:" + searchEntity.getDisplayName().getString()));
                    source.sendSystemMessage(Component.literal("Killed entities:" + killedEntities));
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
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return
          IMCCommand.newLiteral(getName())
            .then(IMCCommand.newArgument("clear", StringArgumentType.string()).executes(this::clearVisited)).executes(this::checkPreConditionAndExecute);
    }

    private int clearVisited(final CommandContext<CommandSourceStack> context)
    {
        if (!checkPreCondition(context))
        {
            return 0;
        }

        context.getSource().sendSystemMessage(Component.literal("Cleared visited entity list"));
        visited = new HashSet<>();
        return 0;
    }
}
