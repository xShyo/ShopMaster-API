package xshyo.us.shopMaster.services.records;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.shop.data.ShopItem;

import java.util.List;
import java.util.Map;


public record SellAllResult(
        SellStatus status,
        double totalEarnings,
        Map<String, Double> earningsByCurrency,
        Map<Material, Integer> soldItems,
        Map<String, Map<Material, Integer>> itemsByCurrency,
        Map<Material, Double> earningsByMaterial,
        List<ItemStack> skippedItems,
        Map<Material, ShopItem> soldShopItems,
        int totalItemsSold) {



    public void generateSummaryMessages(Player player) {
    }



}