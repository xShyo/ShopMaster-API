package xshyo.us.shopMaster.economys;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.managers.CurrencyManager;

import java.util.UUID;

public class ExpPointsHook extends CurrencyManager {
    public boolean hasEnough(Player paramPlayer, int paramInt) {
        return (paramPlayer.getTotalExperience() >= paramInt);
    }

    public boolean hasEnough(Player paramPlayer, double paramDouble) {
        return (paramPlayer.getTotalExperience() >= paramDouble);
    }

    public boolean hasEnough(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        return (player != null && hasEnough(player, paramDouble));
    }

    public boolean withdraw(Player paramPlayer, int paramInt) {
        if (hasEnough(paramPlayer, paramInt)) {
            int currentExp = paramPlayer.getTotalExperience();
            paramPlayer.setTotalExperience(currentExp - paramInt);
            return true;
        }
        return false;
    }

    public boolean withdraw(Player paramPlayer, double paramDouble) {
        return withdraw(paramPlayer, (int)paramDouble);
    }

    public boolean withdraw(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        return (player != null && withdraw(player, paramDouble));
    }

    public boolean add(Player paramPlayer, int paramInt) {
        paramPlayer.setTotalExperience(paramPlayer.getTotalExperience() + paramInt);
        return true;
    }

    public boolean add(Player paramPlayer, double paramDouble) {
        return add(paramPlayer, (int)paramDouble);
    }

    public boolean add(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        return (player != null && add(player, paramDouble));
    }

    public CurrencyType getType() {
        return CurrencyType.EXP_POINTS;
    }
}