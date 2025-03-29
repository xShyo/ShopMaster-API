package xshyo.us.shopMaster.services;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.superclass.CurrencyManager;
import xshyo.us.shopMaster.utilities.PluginUtils;

import java.util.Map;
import java.util.TreeMap;

public class PurchaseService {

    /**
     * Procesa una compra para un jugador
     *
     * @param player   El jugador que realiza la compra
     * @param item     El ítem de la tienda a comprar
     * @param quantity La cantidad a comprar
     */
    public static void processPurchase(Player player, ShopItem item, int quantity) {
        double pricePerUnit = (double) item.getBuyPrice() / item.getAmount(); // Ajusta el precio por unidad correctamente
        double totalPrice = pricePerUnit * quantity;

        CurrencyManager currencyManager = ShopMaster.getInstance().getCurrencyMap()
                .get(CurrencyType.getType(item.getEconomy(), CurrencyType.VAULT));

        if (currencyManager == null) {
            player.closeInventory();
            PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.INVALID");
            return;
        }

        if (!currencyManager.hasEnough(player, totalPrice)) {
            player.closeInventory();
            PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.NOT_ENOUGH", totalPrice);
            return;
        }

        ItemStack sampleItem = item.createItemStack();
        if (sampleItem == null) {
            PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.ERROR");
            return;
        }

        int maxStackSize = sampleItem.getType().getMaxStackSize();
        int totalStacksNeeded = (quantity / maxStackSize) + (quantity % maxStackSize == 0 ? 0 : 1);
        int freeSlots = countFreeSlots(player);

        if (freeSlots < totalStacksNeeded) {
            player.closeInventory();
            PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.NO_SPACE");
            return;
        }

        if (!currencyManager.withdraw(player, totalPrice)) {
            player.closeInventory();
            PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.ERROR");
            return;
        }

        try {
            giveItemsToPlayer(player, item, quantity);
        } catch (Exception e) {
            currencyManager.add(player, totalPrice);
            player.closeInventory();
            PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.ERROR");
            return;
        }

        PluginUtils.sendMessage(player, "MESSAGES.GUI.PURCHASE.SUCCESS", quantity, item.getDisplayName(), totalPrice);
        player.closeInventory();
    }



    private static int countFreeSlots(Player player) {
        int freeSlots = 0;
        for (int i = 0; i < 36; i++) { // Solo contar los slots principales (0-35), no la armadura
            if (player.getInventory().getItem(i) == null) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    private static void giveItemsToPlayer(Player player, ShopItem item, int quantity) {
        // Verificar si item o createItemStack() retorna null
        if (item == null) {
            return;
        }

        ItemStack sampleItem = item.createItemStack();
        if (sampleItem == null) {
            return;
        }

        int maxStackSize = sampleItem.getType().getMaxStackSize();

        // Calcular los stacks
        int fullStacks = quantity / maxStackSize;
        int remainingItems = quantity % maxStackSize;

        // Dar los stacks completos
        for (int i = 0; i < fullStacks; i++) {
            ItemStack stackItem = item.createItemStack();
            if (stackItem == null) {
                return;
            }

            stackItem.setAmount(maxStackSize);

            // Guardar los resultados para verificar si hubo problemas
            java.util.HashMap<Integer, ItemStack> results = player.getInventory().addItem(stackItem);
            if (!results.isEmpty()) {
                // Intentar tirar los ítems al suelo
                for (ItemStack remaining : results.values()) {
                    player.getWorld().dropItem(player.getLocation(), remaining);
                }
            }
        }

        // Dar los ítems restantes
        if (remainingItems > 0) {
            ItemStack remainingItem = item.createItemStack();
            if (remainingItem == null) {
                return;
            }

            remainingItem.setAmount(remainingItems);

            // Guardar los resultados para verificar si hubo problemas
            java.util.HashMap<Integer, ItemStack> results = player.getInventory().addItem(remainingItem);
            if (!results.isEmpty()) {
                // Intentar tirar los ítems al suelo
                for (ItemStack remaining : results.values()) {
                    player.getWorld().dropItem(player.getLocation(), remaining);
                }
            }
        }

    }



}