package xshyo.us.shopMaster.economys;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.managers.CurrencyManager;

import java.util.UUID;


public class VaultHook extends CurrencyManager {
    private final Economy economy;

    public VaultHook(Economy economy) {
        this.economy = economy;
    }

    public boolean hasEnough(Player paramPlayer, int paramInt) {
        return this.economy.has((OfflinePlayer)paramPlayer, paramInt);
    }

    public boolean hasEnough(Player paramPlayer, double paramDouble) {
        return this.economy.has((OfflinePlayer)paramPlayer, paramDouble);
    }

    public boolean hasEnough(UUID paramUUID, double paramDouble) {
        return this.economy.has((OfflinePlayer)Bukkit.getPlayer(paramUUID), paramDouble);
    }

    public boolean withdraw(Player paramPlayer, int paramInt) {
        return this.economy.withdrawPlayer((OfflinePlayer)paramPlayer, paramInt).transactionSuccess();
    }

    public boolean withdraw(Player paramPlayer, double paramDouble) {
        return this.economy.withdrawPlayer((OfflinePlayer)paramPlayer, paramDouble).transactionSuccess();
    }

    public boolean withdraw(UUID paramUUID, double paramDouble) {
        return this.economy.withdrawPlayer((OfflinePlayer)Bukkit.getPlayer(paramUUID), paramDouble).transactionSuccess();
    }

    public boolean add(Player paramPlayer, int paramInt) {
        return this.economy.depositPlayer((OfflinePlayer)paramPlayer, paramInt).transactionSuccess();
    }

    public boolean add(Player paramPlayer, double paramDouble) {
        return this.economy.depositPlayer((OfflinePlayer)paramPlayer, paramDouble).transactionSuccess();
    }

    public boolean add(UUID paramUUID, double paramDouble) {
        return this.economy.depositPlayer((OfflinePlayer)Bukkit.getPlayer(paramUUID), paramDouble).transactionSuccess();
    }

    public CurrencyType getType() {
        return CurrencyType.VAULT;
    }
}
