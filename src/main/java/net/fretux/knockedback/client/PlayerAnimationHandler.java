package net.fretux.knockedback.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerAnimationHandler {
    private static final ResourceLocation GRIP_ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(KnockedBack.MOD_ID, "animation.model.gripping");
    private static final Map<UUID, ModifierLayer<IAnimation>> layers = new HashMap<>();
    private static final Map<UUID, Boolean> activeStates = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof AbstractClientPlayer player)) return;
        UUID playerId = player.getUUID();
        boolean shouldGrip = ClientGrippedState.isGripped(playerId);
        boolean wasGripping = activeStates.getOrDefault(playerId, false);
        if (shouldGrip == wasGripping) return;
        activeStates.put(playerId, shouldGrip);
        ModifierLayer<IAnimation> layer = layers.computeIfAbsent(playerId, id -> {
            ModifierLayer<IAnimation> newLayer = new ModifierLayer<>();
            PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(0, newLayer);
            return newLayer;
        });
        if (shouldGrip) {
            var animation = PlayerAnimationRegistry.getAnimation(GRIP_ANIMATION_ID);
            if (animation != null) {
                layer.setAnimation(new KeyframeAnimationPlayer(animation));
            }
        } else {
            layer.setAnimation(null);
        }
    }
}