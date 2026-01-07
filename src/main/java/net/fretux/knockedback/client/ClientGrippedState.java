package net.fretux.knockedback.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClientGrippedState {
    private static final Set<UUID> grippedPlayers = new HashSet<>();

    public static void setGripped(UUID playerId, boolean gripped) {
        if (gripped) {
            grippedPlayers.add(playerId);
        } else {
            grippedPlayers.remove(playerId);
        }
    }

    public static boolean isGripped(UUID playerId) {
        return grippedPlayers.contains(playerId);
    }
}
