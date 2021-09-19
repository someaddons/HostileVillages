package com.hostilevillages.mixin;

import com.hostilevillages.HostileVillages;
import com.mojang.serialization.Codec;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Supplier;

@Mixin(value = Tag.class, priority = 900)
/**
 * Do a reverse lookup for missing tags, there is some sp bugs where reloading the world leaves some old tag references which then fail when saving structures. So we update those
 */
public interface ITagMixin
{
    @Overwrite
    public static <T> Codec<Tag<T>> codec(Supplier<TagCollection<T>> iTagCollection)
    {
        return HostileVillages.tagCodec(iTagCollection);
    }
}
