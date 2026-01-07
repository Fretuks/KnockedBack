package net.fretux.knockedback.client;

import net.fretux.knockedback.KnockedBack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KnockedBack.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LocalKnockedPoseHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (ClientKnockedState.isKnocked()) {
            if (player.getVehicle() != null) {
                if (player.getForcedPose() == Pose.SWIMMING) {
                    player.setForcedPose(null);
                }
            } else if (player.getForcedPose() != Pose.SWIMMING) {
                player.setForcedPose(Pose.SWIMMING);
            }
        } else if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
        }
    }
}
