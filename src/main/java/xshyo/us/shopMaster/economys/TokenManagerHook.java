package xshyo.us.shopMaster.economys;

import me.realized.tokenmanager.TokenManagerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.superclass.CurrencyManager;

import java.util.OptionalLong;
import java.util.UUID;

public class TokenManagerHook extends CurrencyManager {
    private final TokenManagerPlugin tokenManager;

    public TokenManagerHook(TokenManagerPlugin paramTokenManagerPlugin) {
        this.tokenManager = paramTokenManagerPlugin;
    }

    public boolean hasEnough(Player paramPlayer, int paramInt) {
        OptionalLong optionalLong = this.tokenManager.getTokens(paramPlayer);
        return (optionalLong.isPresent() && optionalLong.getAsLong() >= paramInt);
    }

    public boolean hasEnough(Player paramPlayer, double paramDouble) {
        OptionalLong optionalLong = this.tokenManager.getTokens(paramPlayer);
        return (optionalLong.isPresent() && optionalLong.getAsLong() >= paramDouble);
    }

    public boolean hasEnough(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        if (player == null)
            return false;
        OptionalLong optionalLong = this.tokenManager.getTokens(player);
        return (optionalLong.isPresent() && optionalLong.getAsLong() >= paramDouble);
    }

    public boolean withdraw(Player paramPlayer, int paramInt) {
        return this.tokenManager.removeTokens(paramPlayer, paramInt);
    }

    public boolean withdraw(Player paramPlayer, double paramDouble) {
        return this.tokenManager.removeTokens(paramPlayer, (int)paramDouble);
    }

    public boolean withdraw(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        if (player == null)
            return false;
        return this.tokenManager.removeTokens(player, (int)paramDouble);
    }

    public boolean add(Player paramPlayer, int paramInt) {
        return this.tokenManager.addTokens(paramPlayer, paramInt);
    }

    public boolean add(Player paramPlayer, double paramDouble) {
        return this.tokenManager.addTokens(paramPlayer, (int)paramDouble);
    }

    public boolean add(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        if (player == null)
            return false;
        return this.tokenManager.addTokens(player, (int)paramDouble);
    }

    public CurrencyType getType() {
        return CurrencyType.TOKEN_MANAGER;
    }
}
