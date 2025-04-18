package xshyo.us.shopMaster.services;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.managers.ShopManager;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.managers.CurrencyManager;

public class SellService {

    public SellService(ShopMaster plugin, ShopManager shopManager) {
    }


    private static class CacheKey {

    }

    private record CustomItemEntry(Shop shop, ShopItem shopItem) {
    }


    public record SellableItemInfo(Shop shop, ShopItem shopItem, ItemStack itemStack) {
    }

    public boolean isSellable(ItemStack item) {
        return false;

    }


    public boolean isSellable(Player player, ItemStack item) {
        return false;
    }


    private SellableItemInfo findStandardItem(ItemStack item) {
        return null;
    }


    public void buildSellableItemsIndex() {

    }

    private SellableItemInfo findCustomItem(ItemStack item) {
        return null;
    }


    public SellableItemInfo getSellableShopItem(ItemStack item) {
        return null;
    }


    public SellableItemInfo getSellableShopItem(Player player, ItemStack item) {
        return null;
    }


    public SellResult sellGuiItem(Player player, ItemStack item, int amount) {
        return null;
    }


    public SellResult sellItem(Player player, ItemStack item, int amount, boolean searchEntireInventory) {
        return null;
    }


    private int countItemsInEntireInventory(PlayerInventory inventory, ItemStack referenceItem) {
        return 0;
    }

    private void removeItems(PlayerInventory inventory, ItemStack referenceItem, int amountToRemove, boolean searchEntireInventory) {

    }



    public SellAllResult sellAllItems(Player player) {
        return null;
    }


    public SellAllResult sellAllItemOfType(Player player, ItemStack referenceItem) {
        return null;
    }

    private class SellAllProcessor {

        SellAllProcessor(Player player) {
            this(player, null);
        }

        SellAllProcessor(Player player, ItemStack referenceItem) {
        }

        SellAllResult processInventory() {
            return null;
        }

        private void processInventorySlot(ItemStack item, int slot) {


        }


        private void updateSalesTracking(String currency, Material material, ShopItem shopItem, int amount, double itemTotal) {

        }


        private SellAllResult finalizeSale() {
            return null;
        }

        private void processEarnings() {

        }
    }

    private CurrencyManager getCurrencyManager(SellableItemInfo info) {
        return null;
    }

    private SellAllResult createFailedSellResult(SellStatus status) {
        return null;
    }


    private boolean isWorldBlacklisted(String worldName) {
        return false;
    }


    private boolean isGameModeBlacklisted(String gameMode) {
      return false;
    }


    public void reload() {
        buildSellableItemsIndex();
    }
}