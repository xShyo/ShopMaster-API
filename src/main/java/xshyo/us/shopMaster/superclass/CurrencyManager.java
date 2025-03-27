package xshyo.us.shopMaster.superclass;

import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.realized.tokenmanager.TokenManagerPlugin;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.economys.*;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.theAPI.utilities.Utils;
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
        CurrencyType costType;
        try {
            costType = CurrencyType.valueOf(costTypeString.toUpperCase());
        } catch (Throwable e) {
            costType = defaultCostType;
        }
        if (costType == null || costType == CurrencyType.DISABLED) {
            Bukkit.getConsoleSender().sendMessage(Utils.translate("&c[ShopMaster] Currency Type is not valid or is 'DISABLED'. Disabling economy support.."));
            return null;
        }
        if (costType == CurrencyType.VAULT) {
            if (ShopMaster.getInstance().setupVaultEconomy()) {
                Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency Type 'VAULT' has been enabled!"));

            } else {
                costType = defaultCostType;
                if (costType == null || costType == CurrencyType.DISABLED) {
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] Vault is not available! Disabling economy support.."));

                    return null;
                }
                Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] Vault is not available! Changing cost type to " + costType.name() + "' type!"));

            }
        } else if (costType == CurrencyType.TOKEN_MANAGER) {
            if (Bukkit.getPluginManager().isPluginEnabled("TokenManager")) {
                try {
                    TokenManagerPlugin.getInstance().getConfig();
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency Type 'TOKEN_MANAGER' has been enabled!"));

                } catch (Throwable e) {
                    costType = defaultCostType;
                    if (costType == null || costType == CurrencyType.DISABLED) {
                        Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] TokenManager is not available! Disabling economy support.."));
                        return null;
                    }
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] TokenManager is not available! Changing cost type to " + costType.name() + "' type!"));

                }
            } else {
                costType = defaultCostType;
                if (costType == null || costType == CurrencyType.DISABLED) {
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] TokenManager is not available! Disabling economy support.."));

                    return null;
                }
                Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] TokenManager is not available! Changing cost type to " + costType.name() + "' type!"));

            }
        } else if (costType == CurrencyType.PLAYER_POINTS) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
                try {
                    PlayerPoints.getInstance().getAPI();
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency Type 'PLAYER_POINTS' has been enabled!"));

                } catch (Throwable e) {
                    costType = defaultCostType;
                    if (costType == null || costType == CurrencyType.DISABLED) {
                        Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] PlayerPoints is not available! Disabling economy support.."));
                        return null;
                    }
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] PlayerPoints is not available! Changing cost type to " + costType.name() + "' type!"));
                }
            } else {
                costType = defaultCostType;
                if (costType == null || costType == CurrencyType.DISABLED) {
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] PlayerPoints is not available! Disabling economy support.."));
                    return null;
                }
                Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] PlayerPoints is not available! Changing cost type to " + costType.name() + "' type!"));
            }
        }else if (costType == CurrencyType.BEAST_TOKENS) {
            if (Bukkit.getPluginManager().isPluginEnabled("BeastTokens")) {
                try {
                    BeastTokensAPI.getTokensManager();
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency Type 'BEAST_TOKENS' has been enabled!"));

                } catch (Throwable e) {
                    costType = defaultCostType;
                    if (costType == null || costType == CurrencyType.DISABLED) {
                        Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Disabling economy support.."));
                        return null;
                    }
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Changing cost type to " + costType.name() + "' type!"));
                }
            } else {
                costType = defaultCostType;
                if (costType == null || costType == CurrencyType.DISABLED) {
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Disabling economy support.."));
                    return null;
                }
                Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Changing cost type to " + costType.name() + "' type!"));
            }
        }else if (costType == CurrencyType.LEVEL) {
            if (Bukkit.getPluginManager().isPluginEnabled("L")) {
                try {
                    BeastTokensAPI.getTokensManager();
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency Type 'BEAST_TOKENS' has been enabled!"));

                } catch (Throwable e) {
                    costType = defaultCostType;
                    if (costType == null || costType == CurrencyType.DISABLED) {
                        Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Disabling economy support.."));
                        return null;
                    }
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Changing cost type to " + costType.name() + "' type!"));
                }
            } else {
                costType = defaultCostType;
                if (costType == null || costType == CurrencyType.DISABLED) {
                    Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Disabling economy support.."));
                    return null;
                }
                Bukkit.getConsoleSender().sendMessage(Utils.translate("&e[ShopMaster] BeastTokens is not available! Changing cost type to " + costType.name() + "' type!"));
            }
        }
        switch (costType) {
            case PLAYER_POINTS:
                return new PlayerPointsHook(PlayerPoints.getInstance().getAPI());
            case TOKEN_MANAGER:
                return new TokenManagerHook((TokenManagerPlugin)Bukkit.getPluginManager().getPlugin("TokenManager"));
            case VAULT:
                return new VaultHook(ShopMaster.getInstance().getEconomy());
            case BEAST_TOKENS:
                return new BeastTokensHook();
            case LEVEL:
                return new LevelsHook();
            case EXP_POINTS:
                return new ExpPointsHook();
        }
        return null;
    }
}
