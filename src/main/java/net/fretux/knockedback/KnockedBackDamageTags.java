package net.fretux.knockedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class KnockedBackDamageTags {
    public static final TagKey<DamageType> BYPASS_KNOCKDOWN = TagKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(KnockedBack.MOD_ID, "bypass_knockdown")
    );

    private KnockedBackDamageTags() {
    }
}
