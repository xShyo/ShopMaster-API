package xshyo.us.shopMaster.managers;

import org.bukkit.entity.Player;
import xshyo.us.shopMaster.enums.CurrencyType;

import java.util.UUID;

public abstract class CurrencyManager {

    protected abstract boolean hasEnough(Player paramPlayer, int paramInt);
    public abstract boolean hasEnough(Player paramPlayer, double paramDouble);
    protected abstract boolean hasEnough(UUID paramUUID, double paramDouble);

    protected abstract boolean withdraw(Player paramPlayer, int paramInt);

    public abstract boolean withdraw(Player paramPlayer, double paramDouble);

    protected abstract boolean withdraw(UUID paramUUID, double paramDouble);

    protected abstract boolean add(Player paramPlayer, int paramInt);

    public abstract boolean add(Player paramPlayer, double paramDouble);

    protected abstract boolean add(UUID paramUUID, double paramDouble);

    protected abstract CurrencyType getType();

    public static CurrencyManager initializeManager(String costTypeString, CurrencyType defaultCostType) {
        return null;
    }
}
