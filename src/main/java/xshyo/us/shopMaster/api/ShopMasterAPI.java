package xshyo.us.shopMaster.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;
import xshyo.us.shopMaster.shop.data.ShopItem;

/**
 * API for ShopMaster plugin that provides access to shop functionality.
 * This API allows developers to interact with the shop system to check if items are sellable
 * and retrieve shop item information.
 */

public class ShopMasterAPI {

    private static ShopMaster shopMaster;
    /**
     * Calculates the sell price for a stack of items.
     *
     * @param itemStack The stack of items to calculate the sell price for.
     * @return The total sell price for the item stack, or -1.0 if there is a problem.
     */
    public static double getItemStackPriceSell(ItemStack itemStack) {
        // Check if itemStack is null
        if (itemStack == null) {
            return -1.0;
        }

        ShopItem shopItem = getShopItem(itemStack);
        // Check if the item exists in the shop
        if (shopItem == null) {
            return -1.0;
        }

        try {
            int amount = itemStack.getAmount();

            double unitPrice = shopItem.getSellPrice();
            double baseAmount = shopItem.getAmount(); // Minimum quantity in the shop
            if (baseAmount > 1) {
                unitPrice = unitPrice / baseAmount; // Adjust unit price
            }

            return unitPrice * amount;
        } catch (Exception e) {
            // Any other unexpected error
            return -1.0;
        }
    }

    /**
     * Calculates the sell price for a stack of items specific to a player.
     * This version considers player-specific permissions or discounts.
     *
     * @param player The player for whom the price is calculated.
     * @param itemStack The stack of items to calculate the sell price for.
     * @return The total sell price for the item stack, or -1.0 if there is a problem.
     */
    public static double getItemStackPriceSell(Player player, ItemStack itemStack) {
        // Check if player or itemStack are null
        if (player == null || itemStack == null) {
            return -1.0;
        }

        ShopItem shopItem = getShopItemPlayer(player, itemStack);
        // Check if the item exists in the shop for this player
        if (shopItem == null) {
            return -1.0;
        }

        try {
            int amount = itemStack.getAmount();

            double unitPrice = shopItem.getSellPrice();
            double baseAmount = shopItem.getAmount(); // Minimum quantity in the shop
            if (baseAmount > 1) {
                unitPrice = unitPrice / baseAmount; // Adjust unit price
            }

            return unitPrice * amount;
        } catch (Exception e) {
            // Any other unexpected error
            return -1.0;
        }
    }

    /**
     * Calculates the buy price for a stack of items.
     *
     * @param itemStack The stack of items to calculate the buy price for.
     * @return The total buy price for the item stack, or -1.0 if there is a problem.
     */
    public static double getItemStackPriceBuy(ItemStack itemStack) {
        // Check if itemStack is null
        if (itemStack == null) {
            return -1.0;
        }

        ShopItem shopItem = getShopItem(itemStack);
        // Check if the item exists in the shop
        if (shopItem == null) {
            return -1.0;
        }

        try {
            int amount = itemStack.getAmount();

            double unitPrice = shopItem.getBuyPrice();
            double baseAmount = shopItem.getAmount(); // Minimum quantity in the shop
            if (baseAmount > 1) {
                unitPrice = unitPrice / baseAmount; // Adjust unit price
            }

            return unitPrice * amount;
        } catch (Exception e) {
            // Any other unexpected error
            return -1.0;
        }
    }

    /**
     * Calculates the buy price for a stack of items specific to a player.
     * This version considers player-specific permissions or discounts.
     *
     * @param player The player for whom the price is calculated.
     * @param itemStack The stack of items to calculate the buy price for.
     * @return The total buy price for the item stack, or -1.0 if there is a problem.
     */
    public static double getItemStackPriceBuy(Player player, ItemStack itemStack) {
        // Check if player or itemStack are null
        if (player == null || itemStack == null) {
            return -1.0;
        }

        ShopItem shopItem = getShopItemPlayer(player, itemStack);
        // Check if the item exists in the shop for this player
        if (shopItem == null) {
            return -1.0;
        }

        try {
            int amount = itemStack.getAmount();

            double unitPrice = shopItem.getBuyPrice();
            double baseAmount = shopItem.getAmount(); // Minimum quantity in the shop
            if (baseAmount > 1) {
                unitPrice = unitPrice / baseAmount; // Adjust unit price
            }

            return unitPrice * amount;
        } catch (Exception e) {
            // Any other unexpected error
            return -1.0;
        }
    }


    /**
     * Checks if an item can be sold by a specific player.
     * This method verifies player-specific restrictions including:
     * - World blacklist
     * - Game mode blacklist
     * - Shop permissions
     *
     * @param player The player attempting to sell the item
     * @param item   The item to check
     * @return true if the player can sell this item, false otherwise
     */
    public static boolean isSellablePlayer(Player player, ItemStack item) {
        return shopMaster.getSellService().isSellable(player, item);
    }

    /**
     * Gets the ShopItem for a specific player and item.
     * This method considers player-specific restrictions including:
     * - World blacklist
     * - Game mode blacklist
     * - Shop permissions
     *
     * @param player    The player requesting the shop item
     * @param itemStack The item to get shop information for
     * @return The ShopItem if the player has access to it, null otherwise
     */
    public static ShopItem getShopItemPlayer(Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        SellService.SellableItemInfo info = shopMaster.getSellService().getSellableShopItem(player, itemStack);
        return info != null ? info.shopItem() : null;
    }

    /**
     * Checks if an item exists in any shop, regardless of player restrictions.
     * This method only verifies if the item is registered in the shop system.
     * It does not consider world blacklists, game mode restrictions, or permissions.
     *
     * @param item The item to check
     * @return true if the item exists in any shop, false otherwise
     */
    public static boolean isSellable(ItemStack item) {
        return shopMaster.getSellService().isSellable(item);
    }

    /**
     * Gets the ShopItem for an item, regardless of player restrictions.
     * This method retrieves shop information for an item without considering
     * world blacklists, game mode restrictions, or permissions.
     *
     * @param itemStack The item to get shop information for
     * @return The ShopItem if it exists in any shop, null otherwise
     */
    public static ShopItem getShopItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        SellService.SellableItemInfo info = shopMaster.getSellService().getSellableShopItem(itemStack);
        return info != null ? info.shopItem() : null;
    }


    /**
     * Get detailed information about a sellable item
     *
     * @param player The player attempting to sell
     * @param item   The item to get information about
     * @return SellableItemInfo object or null if the item cannot be sold
     */
    public static SellService.SellableItemInfo getSellableInfo(Player player, ItemStack item) {
        return shopMaster.getSellService().getSellableShopItem(player, item);
    }


    /**
     * Sell a specific amount of items from player's inventory
     *
     * @param player                The player selling items
     * @param item                  The item to sell
     * @param amount                The amount to sell
     * @param searchEntireInventory Whether to search the entire inventory (true) or just main hand (false)
     * @return SellResult containing status and price details
     */
    public static SellResult sellItem(Player player, ItemStack item, int amount, boolean searchEntireInventory) {
        return shopMaster.getSellService().sellItem(player, item, amount, searchEntireInventory);
    }

    /**
     * Sell an item from a GUI (does not remove items from inventory)
     *
     * @param player The player selling items
     * @param item   The item to sell
     * @param amount The amount to sell
     * @return SellResult containing status and price details
     */
    public static SellResult sellGuiItem(Player player, ItemStack item, int amount) {
        return shopMaster.getSellService().sellGuiItem(player, item, amount);
    }

    /**
     * Sell all sellable items in a player's inventory
     *
     * @param player The player selling items
     * @return SellAllResult containing status and detailed sale information
     */
    public static SellAllResult sellAllItems(Player player) {
        return shopMaster.getSellService().sellAllItems(player);
    }


    /**
     * Sell all items of a specific type from a player's inventory
     *
     * @param player        The player selling items
     * @param referenceItem The type of item to sell
     * @return SellAllResult containing status and detailed sale information
     */
    public static SellAllResult sellAllItemOfType(Player player, ItemStack referenceItem) {
        return shopMaster.getSellService().sellAllItemOfType(player, referenceItem);
    }


    /**
     * Generate and send summary messages to a player based on a sell operation
     *
     * @param player The player who performed the sale
     * @param result The result of the sell operation
     */
    public static void sendSaleSummary(Player player, SellAllResult result) {
        result.generateSummaryMessages(player);
    }



}
