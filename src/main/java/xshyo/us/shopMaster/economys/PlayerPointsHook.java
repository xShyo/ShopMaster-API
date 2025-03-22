package xshyo.us.shopMaster.economys;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.superclass.CurrencyManager;

import java.util.UUID;

public class PlayerPointsHook extends CurrencyManager {
    private final PlayerPointsAPI api;

    public PlayerPointsHook(PlayerPointsAPI paramPlayerPointsAPI) {
        this.api = paramPlayerPointsAPI;
    }

    public boolean hasEnough(Player paramPlayer, int paramInt) {
        return (this.api.look(paramPlayer.getUniqueId()) >= paramInt);
    }

    public boolean hasEnough(Player paramPlayer, double paramDouble) {
        return (this.api.look(paramPlayer.getUniqueId()) >= paramDouble);
    }

    public boolean hasEnough(UUID paramUUID, double paramDouble) {
        return (this.api.look(paramUUID) >= paramDouble);
    }

    public boolean withdraw(Player paramPlayer, int paramInt) {
        return this.api.take(paramPlayer.getUniqueId(), paramInt);
    }

    public boolean withdraw(Player paramPlayer, double paramDouble) {
        return this.api.take(paramPlayer.getUniqueId(), (int)paramDouble);
    }

    public boolean withdraw(UUID paramUUID, double paramDouble) {
        return this.api.take(paramUUID, (int)paramDouble);
    }

    public boolean add(Player paramPlayer, int paramInt) {
        return this.api.give(paramPlayer.getUniqueId(), paramInt);
    }

    public boolean add(Player paramPlayer, double paramDouble) {
        return this.api.give(paramPlayer.getUniqueId(), (int)paramDouble);
    }

    public boolean add(UUID paramUUID, double paramDouble) {
        return this.api.give(paramUUID, (int)paramDouble);
    }

    public CurrencyType getType() {
        return CurrencyType.PLAYER_POINTS;
    }
}
