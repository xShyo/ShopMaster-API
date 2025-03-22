package xshyo.us.shopMaster.economys;

import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beasttokens.api.handlers.BTTokensManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.superclass.CurrencyManager;

import java.util.UUID;

public class BeastTokensHook extends CurrencyManager {
    private final BTTokensManager api = BeastTokensAPI.getTokensManager();


    public boolean hasEnough(Player paramPlayer, int paramInt) {
        return (this.api.getTokens(paramPlayer) >= paramInt);
    }

    public boolean hasEnough(Player paramPlayer, double paramDouble) {
        return (this.api.getTokens(paramPlayer) >= paramDouble);
    }

    public boolean hasEnough(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        return player != null && (this.api.getTokens(player) >= paramDouble);
    }

    public boolean withdraw(Player paramPlayer, int paramInt) {
        double currentTokens = this.api.getTokens(paramPlayer);
        if (currentTokens >= paramInt) {
            this.api.removeTokens(paramPlayer, paramInt);
            return true; // Se removieron los tokens correctamente
        }
        return false; // No se pudo remover porque no hay suficientes tokens
    }

    public boolean withdraw(Player paramPlayer, double paramDouble) {
        double currentTokens = this.api.getTokens(paramPlayer);
        if (currentTokens >= paramDouble) {
            this.api.removeTokens(paramPlayer, paramDouble);
            return true; // Se removieron los tokens correctamente
        }
        return false; // No se pudo remover porque no hay suficientes tokens
    }

    public boolean withdraw(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        if (player != null) {
            double currentTokens = this.api.getTokens(player);
            if (currentTokens >= paramDouble) {
                this.api.removeTokens(player, paramDouble);
                return true; // Se removieron los tokens correctamente
            }
        }
        return false; // No se pudo remover porque el jugador no está en línea o no hay suficientes tokens
    }

    public boolean add(Player paramPlayer, int paramInt) {
        double currentTokens = this.api.getTokens(paramPlayer);
        this.api.addTokens(paramPlayer, paramInt);
        return this.api.getTokens(paramPlayer) > currentTokens; // Verifica si los tokens aumentaron
    }

    public boolean add(Player paramPlayer, double paramDouble) {
        double currentTokens = this.api.getTokens(paramPlayer);
        this.api.addTokens(paramPlayer, paramDouble);
        return this.api.getTokens(paramPlayer) > currentTokens; // Verifica si los tokens aumentaron
    }

    public boolean add(UUID paramUUID, double paramDouble) {
        Player player = Bukkit.getPlayer(paramUUID);
        if (player != null) {
            double currentTokens = this.api.getTokens(player);
            this.api.addTokens(player, paramDouble);
            return this.api.getTokens(player) > currentTokens; // Verifica si los tokens aumentaron
        }
        return false; // No se pudo añadir porque el jugador no está en línea
    }

    public CurrencyType getType() {
        return CurrencyType.BEAST_TOKENS;
    }
}