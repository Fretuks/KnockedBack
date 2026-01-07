package net.fretux.knockedback.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fretux.knockedback.KnockedBack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerAnimationHandler {
    private static final ResourceLocation GRIP_ANIMATION_ID = new ResourceLocation(KnockedBack.MOD_ID, "animation.model.gripping");
    private static final java.util.Map<UUID, ModifierLayer<IAnimation>> layers = new java.util.HashMap<>();
    private static final java.util.Map<UUID, Boolean> activeStates = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof AbstractClientPlayer player)) return;
        UUID playerId = player.getUUID();
        boolean shouldGrip = ClientGrippedState.isGripped(playerId);
        boolean wasGripping = activeStates.getOrDefault(playerId, false);
        if (shouldGrip == wasGripping) return;
        activeStates.put(playerId, shouldGrip);
        ModifierLayer<IAnimation> layer = layers.get(playerId);
        if (layer == null) {
            layer = new ModifierLayer<>();
            layers.put(playerId, layer);
            PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(0, layer);
        }
        if (shouldGrip) {
            IAnimation animation = PlayerAnimationRegistry.getAnimation(GRIP_ANIMATION_ID);
            layer.setAnimation(animation);
        } else {
            layer.setAnimation(null);
        }
    }
}
