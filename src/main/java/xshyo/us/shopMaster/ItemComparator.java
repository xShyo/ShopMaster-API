package xshyo.us.shopMaster;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemComparator {

    public ItemComparator() {
        reload();
    }

    public void reload() {
    }

    public boolean areItemsSimilar(ItemStack item1, ItemStack item2) {
        return false;
    }

    private boolean hasSameDamage(ItemStack item1, ItemStack item2) {
        return false;
    }

    private boolean compareDisplay(ItemMeta meta1, ItemMeta meta2) {

        return false;
    }

    private boolean compareLore(ItemMeta meta1, ItemMeta meta2) {
        return true;
    }

    private boolean compareModelData(ItemMeta meta1, ItemMeta meta2) {
        return true;
    }

    private boolean compareNbtData(ItemStack item1, ItemStack item2) {
        return false;
    }

    private String normalize(String input) {
        return null;
    }

    private List<String> normalizeList(List<String> list) {
        return null;
    }

}
