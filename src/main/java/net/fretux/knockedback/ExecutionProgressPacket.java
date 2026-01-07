package net.fretux.knockedback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class ExecutionProgressPacket {
    private final int timeLeft;
    @Nullable
    private final UUID executorId;

    public ExecutionProgressPacket(int timeLeft, @Nullable UUID executorId) {
        this.timeLeft = timeLeft;
        this.executorId = executorId;
    }

    public static void encode(ExecutionProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.timeLeft);
        buf.writeBoolean(msg.executorId != null);
        if (msg.executorId != null) {
            buf.writeUUID(msg.executorId);
        }
    }

    public static ExecutionProgressPacket decode(FriendlyByteBuf buf) {
        int timeLeft = buf.readInt();
        UUID executorId = buf.readBoolean() ? buf.readUUID() : null;
        return new ExecutionProgressPacket(timeLeft, executorId);
    }

    public static void handle(ExecutionProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.fretux.knockedback.client.ClientExecutionState.setExecution(msg.timeLeft, msg.executorId);
        });
        ctx.get().setPacketHandled(true);
    }
}
