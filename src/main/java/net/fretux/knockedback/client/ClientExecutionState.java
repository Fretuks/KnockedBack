package net.fretux.knockedback.client;

import javax.annotation.Nullable;
import java.util.UUID;

public class ClientExecutionState {
    private static int timeLeft = 0;
    @Nullable
    private static UUID executorId;

    public static void setExecution(int ticks, @Nullable UUID executor) {
        timeLeft = ticks;
        executorId = ticks > 0 ? executor : null;
    }

    public static int getTimeLeft() {
        return timeLeft;
    }

    public static boolean isExecuting() {
        return timeLeft > 0;
    }

    public static boolean isExecutor(UUID playerId) {
        return timeLeft > 0 && executorId != null && executorId.equals(playerId);
    }
}
