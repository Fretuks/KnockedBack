package net.fretux.knockedback;

import net.fretux.knockedback.client.ClientGrippedState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class GrippedStatePacket {
    private final UUID playerId;
    private final boolean gripped;

    public GrippedStatePacket(UUID playerId, boolean gripped) {
        this.playerId = playerId;
        this.gripped = gripped;
    }

    public static void encode(GrippedStatePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.gripped);
    }

    public static GrippedStatePacket decode(FriendlyByteBuf buf) {
        return new GrippedStatePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(GrippedStatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientGrippedState.setGripped(packet.playerId, packet.gripped));
        ctx.get().setPacketHandled(true);
    }
}
