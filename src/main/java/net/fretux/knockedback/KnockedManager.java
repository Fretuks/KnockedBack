package net.fretux.knockedback;

import net.fretux.knockedback.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KnockedManager {
    private static final Map<UUID, Integer> knockedEntities = new HashMap<>();
    private static final Set<UUID> grippedEntities = new HashSet<>();
    private static final Set<UUID> killingNow = new HashSet<>();

    public static void markKillInProgress(LivingEntity e) {
        killingNow.add(e.getUUID());
    }

    public static void unmarkKillInProgress(LivingEntity e) {
        killingNow.remove(e.getUUID());
    }


    public static int getKnockedDuration() {
        return Config.COMMON.knockedDuration.get();
    }

    public static void applyKnockedState(LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return;
        }
        if (!isKnocked(entity)) {
            knockedEntities.put(entity.getUUID(), getKnockedDuration());
            entity.setHealth(1.0F);
            applyKnockedEffects((Player) entity);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (KnockedManager.isKnocked(sp)) {
            applyKnockedEffects(sp);
            boolean grounded = sp.onGround() || sp.isInWater();
            sp.setNoGravity(false);
            if (!grounded) {
                sp.setDeltaMovement(0, sp.getDeltaMovement().y() - 0.08, 0); 
            }
        }

    }


    public static boolean isKnocked(LivingEntity entity) {
        return knockedEntities.containsKey(entity.getUUID());
    }

    public static void removeKnockedState(LivingEntity entity) {
        knockedEntities.remove(entity.getUUID());
        setGripped(entity, false);
        MobKillHandler.clearKillAttempt(entity.getUUID());
        entity.setPose(Pose.STANDING);
        if (entity instanceof ServerPlayer sp) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new KnockedTimePacket(0)
            );
        }
    }


    public static void setGripped(LivingEntity entity, boolean isGripped) {
        if (isGripped) {
            grippedEntities.add(entity.getUUID());
        } else {
            grippedEntities.remove(entity.getUUID());
        }
        if (entity instanceof ServerPlayer sp) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new GrippedStatePacket(sp.getUUID(), isGripped)
            );
        }
    }

    public static Collection<UUID> getKnockedUuids() {
        return new HashSet<>(knockedEntities.keySet());
    }

    public static void tickKnockedStates() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        Iterator<Map.Entry<UUID, Integer>> it = knockedEntities.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID playerId = entry.getKey();
            int timeLeft = entry.getValue();
            if (grippedEntities.contains(playerId)
                    || CarryManager.isBeingCarried(playerId)) {
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    applyKnockedEffects(p);
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(timeLeft)
                    );
                }
                continue;
            }
            if (MobKillHandler.isBeingMobExecuted(playerId)
                    || PlayerExecutionHandler.isBeingPlayerExecuted(playerId)) {
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    applyKnockedEffects(p);
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(timeLeft)
                    );
                }
                continue;
            }
            timeLeft--;
            if (timeLeft <= 0) {
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(0)
                    );
                }
                it.remove();
            } else {
                entry.setValue(timeLeft);
                ServerPlayer p = NetworkHandlerHelper.getPlayerByUuid(server, playerId);
                if (p != null) {
                    applyKnockedEffects(p);
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> p),
                            new KnockedTimePacket(timeLeft)
                    );
                }
            }
        }
    }

    private static void killAndRemove(LivingEntity entity) {
        removeKnockedState(entity); 
        entity.setHealth(0.0F);
        PlayerExecutionHandler.cancelExecution(entity.getUUID());
    }
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource src = event.getSource();
        if (!(entity instanceof Player player)) return;
        if (killingNow.contains(player.getUUID())) return;
        boolean isFatal = player.getHealth() - event.getAmount() <= 0;
        if (isKnocked(player)) {
            if (src.is(KnockedBackDamageTags.BYPASS_KNOCKDOWN)) {
                if (isFatal) {
                    killAndRemove(player);
                }
                return;
            }
            knockedEntities.put(player.getUUID(), getKnockedDuration());
            applyKnockedEffects(player);
            event.setCanceled(true);
            return;
        }
        if (MobKillHandler.isBeingMobExecuted(player.getUUID())
                || PlayerExecutionHandler.isBeingPlayerExecuted(player.getUUID())) {
            event.setCanceled(true);
            return;
        }
        if (isFatal) {
            if (src.is(KnockedBackDamageTags.BYPASS_KNOCKDOWN)) {
                killAndRemove(player);
                return;
            }
        }
        boolean hasTotem = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        if (isFatal) {
            if (!hasTotem || !Config.COMMON.totemPreventsKnockdown.get()) {
                event.setCanceled(true);
                applyKnockedState(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID playerId = sp.getUUID();
        knockedEntities.remove(playerId);
        grippedEntities.remove(playerId);
        MobKillHandler.clearKillAttempt(playerId);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> sp),
                new KnockedTimePacket(0)
        );
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> sp),
                new ExecutionProgressPacket(0, null)
        );
        NetworkHandler.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                new GrippedStatePacket(playerId, false)
        );
    }

    private static void applyKnockedEffects(Player player) {
        List<? extends String> configured = Config.COMMON.knockedPotionEffects.get();
        Map<MobEffect, MobEffectInstance> desiredEffects = new HashMap<>();
        for (String entry : configured) {
            MobEffectInstance instance = parseEffect(entry);
            if (instance == null) {
                continue;
            }
            MobEffect effect = instance.getEffect();
            MobEffectInstance existing = desiredEffects.get(effect);
            if (existing == null || existing.getAmplifier() < instance.getAmplifier()) {
                desiredEffects.put(effect, instance);
            }
        }
        if (Config.COMMON.removeOtherPotionEffectsWhileKnocked.get()) {
            for (MobEffectInstance current : new ArrayList<>(player.getActiveEffects())) {
                MobEffect currentEffect = current.getEffect();
                if (!desiredEffects.containsKey(currentEffect)) {
                    player.removeEffect(currentEffect);
                }
            }
        }
        for (MobEffectInstance instance : desiredEffects.values()) {
            MobEffect effect = instance.getEffect();
            MobEffectInstance existing = player.getEffect(effect);
            if (existing == null || existing.getDuration() < 20 || existing.getAmplifier() < instance.getAmplifier()) {
                player.addEffect(instance);
            }
        }
    }

    private static MobEffectInstance parseEffect(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }
        String[] parts = entry.split(",");
        if (parts.length < 3) {
            return null;
        }
        ResourceLocation effectId = ResourceLocation.tryParse(parts[0].trim());
        if (effectId == null) {
            return null;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            return null;
        }
        Integer duration = parseInt(parts[1].trim());
        Integer amplifier = parseInt(parts[2].trim());
        if (duration == null || amplifier == null) {
            return null;
        }
        boolean ambient = parts.length > 3 ? Boolean.parseBoolean(parts[3].trim()) : false;
        boolean visible = parts.length > 4 ? Boolean.parseBoolean(parts[4].trim()) : true;
        boolean showIcon = parts.length > 5 ? Boolean.parseBoolean(parts[5].trim()) : true;
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon);
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
