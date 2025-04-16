package xshyo.us.shopMaster.services;

import org.bukkit.entity.Player;
import xshyo.us.shopMaster.services.records.PurchaseResult;
import xshyo.us.shopMaster.shop.data.ShopItem;

public class PurchaseService {

    /**
     * Procesa una compra para un jugador
     *
     * @param player   El jugador que realiza la compra
     * @param item     El ítem de la tienda a comprar
     * @param quantity La cantidad a comprar
     */
    public PurchaseResult processPurchase(Player player, ShopItem item, int quantity) {
        return null;

    }

    private int countFreeSlots(Player player) {
        return 0;
    }

    private void giveItemsToPlayer(Player player, ShopItem item, int quantity) {

    }
}