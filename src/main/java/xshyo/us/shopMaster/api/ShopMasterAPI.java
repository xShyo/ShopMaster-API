package xshyo.us.shopMaster.api;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;

@Getter
@Setter
public class ShopMasterAPI {

    private final ShopMaster shopMaster = ShopMaster.getInstance();


    public boolean isSellable(Player player, ItemStack item) {
        return shopMaster.getSellService().isSellable(player, item);
    }


    /**
     * Get detailed information about a sellable item
     * @param player The player attempting to sell
     * @param item The item to get information about
     * @return SellableItemInfo object or null if the item cannot be sold
     */
    public SellService.SellableItemInfo getSellableInfo(Player player, ItemStack item) {
        return shopMaster.getSellService().getSellableShopItem(player, item);
    }


    /**
     * Sell a specific amount of items from player's inventory
     * @param player The player selling items
     * @param item The item to sell
     * @param amount The amount to sell
     * @param searchEntireInventory Whether to search the entire inventory (true) or just main hand (false)
     * @return SellResult containing status and price details
     */
    public SellResult sellItem(Player player, ItemStack item, int amount, boolean searchEntireInventory) {
        return shopMaster.getSellService().sellItem(player, item, amount, searchEntireInventory);
    }

    /**
     * Sell an item from a GUI (does not remove items from inventory)
     * @param player The player selling items
     * @param item The item to sell
     * @param amount The amount to sell
     * @return SellResult containing status and price details
     */
    public SellResult sellGuiItem(Player player, ItemStack item, int amount) {
        return shopMaster.getSellService().sellGuiItem(player, item, amount);
    }

    /**
     * Sell all sellable items in a player's inventory
     * @param player The player selling items
     * @return SellAllResult containing status and detailed sale information
     */
    public SellAllResult sellAllItems(Player player) {
        return shopMaster.getSellService().sellAllItems(player);
    }



    /**
     * Sell all items of a specific type from a player's inventory
     * @param player The player selling items
     * @param referenceItem The type of item to sell
     * @return SellAllResult containing status and detailed sale information
     */
    public SellAllResult sellAllItemOfType(Player player, ItemStack referenceItem) {
        return shopMaster.getSellService().sellAllItemOfType(player, referenceItem);
    }


    /**
     * Generate and send summary messages to a player based on a sell operation
     * @param player The player who performed the sale
     * @param result The result of the sell operation
     */
    public void sendSaleSummary(Player player, SellAllResult result) {
        result.generateSummaryMessages(player);
    }

    /**
     * Check if a world is blacklisted for selling
     * @param worldName The name of the world to check
     * @return true if the world is blacklisted, false otherwise
     */
    public boolean isWorldBlacklistedForSelling(String worldName) {
        return shopMaster.getConf().getStringList("config.command.sell.black-list.world").contains(worldName);
    }

    /**
     * Check if a gamemode is blacklisted for selling
     * @param gameMode The gamemode to check
     * @return true if the gamemode is blacklisted, false otherwise
     */
    public boolean isGameModeBlacklistedForSelling(String gameMode) {
        return shopMaster.getConf().getStringList("config.command.sell.black-list.gameModes").contains(gameMode);
    }




}
