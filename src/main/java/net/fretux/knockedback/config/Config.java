package net.fretux.knockedback.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue knockedDuration;
        public final ForgeConfigSpec.IntValue executionTime;
        public final ForgeConfigSpec.BooleanValue mobExecutionEnabled;
        public final ForgeConfigSpec.BooleanValue totemPreventsKnockdown;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> mobExecutionAllowlist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> knockedPotionEffects;
        public final ForgeConfigSpec.BooleanValue removeOtherPotionEffectsWhileKnocked;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("general");

            knockedDuration = builder
                    .comment("How long (in ticks) a player remains knocked before recovering. Default: 400 (20 seconds).")
                    .defineInRange("knockedDuration", 400, 100, 1200);

            executionTime = builder
                    .comment("How long (in ticks) it takes to execute a knocked player. Default: 60 (3 seconds).")
                    .defineInRange("executionTime", 60, 20, 200);

            mobExecutionEnabled = builder
                    .comment("If true, mobs can execute knocked players. Default: true.")
                    .define("mobExecutionEnabled", true);

            mobExecutionAllowlist = builder
                    .comment("List of entity ids allowed to execute knocked players.",
                            "If empty, any hostile/aggro mob can execute.",
                            "Example: [\"minecraft:zombie\", \"modid:custom_mob\"]")
                    .defineListAllowEmpty("mobExecutionAllowlist", List.of(), o -> o instanceof String);

            knockedPotionEffects = builder
                    .comment("Potion effects to apply while a player is knocked.",
                            "Format: effect_id,duration,amplifier[,ambient][,visible][,showIcon]",
                            "Example: [\"minecraft:slowness,200,1\", \"minecraft:weakness,200,0,true,false,false\"]")
                    .defineListAllowEmpty("knockedPotionEffects", List.of(), o -> o instanceof String);

            removeOtherPotionEffectsWhileKnocked = builder
                    .comment("If true, remove any potion effects not listed in knockedPotionEffects while knocked. Default: false.")
                    .define("removeOtherPotionEffectsWhileKnocked", false);

            builder.pop().push("damage_behavior");

            totemPreventsKnockdown = builder
                    .comment("If true, Totems of Undying prevent entering the knocked state. Default: true.")
                    .define("totemPreventsKnockdown", true);

            builder.pop();
        }
    }
}
