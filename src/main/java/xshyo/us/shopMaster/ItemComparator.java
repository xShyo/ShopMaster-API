package xshyo.us.shopMaster;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemComparator {
    private boolean compareMeta;
    private boolean compareModel;
    private boolean compareDamage;
    private boolean compareAll;

    public ItemComparator() {
        reload();
    }

    public void reload() {
        this.compareMeta = ShopMaster.getInstance().getConf().getBoolean("config.item-sell.compare-meta", true);
        this.compareDamage = ShopMaster.getInstance().getConf().getBoolean("config.item-sell.compare-damage", false);
        this.compareModel = ShopMaster.getInstance().getConf().getBoolean("config.item-sell.compare-model", true);
        this.compareAll = ShopMaster.getInstance().getConf().getBoolean("config.item-sell.compare-all", true);
    }

    public boolean areItemsSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;

        if (item1.getType() != item2.getType()) return false;

        if (compareDamage && !hasSameDamage(item1, item2)) return false;

        if (compareMeta || compareModel) {
            ItemMeta meta1 = item1.getItemMeta();
            ItemMeta meta2 = item2.getItemMeta();

            if ((meta1 == null) != (meta2 == null)) return false;
            if (meta1 != null) {
                if (compareMeta && !compareDisplay(meta1, meta2)) return false;
                if (compareMeta && !compareLore(meta1, meta2)) return false;
                if (compareModel && !compareModelData(meta1, meta2)) return false;
            }
        }

        return !compareAll || compareNbtData(item1, item2);
    }

    private boolean hasSameDamage(ItemStack item1, ItemStack item2) {
        if (!(item1.getItemMeta() instanceof Damageable d1) ||
                !(item2.getItemMeta() instanceof Damageable d2)) return true;
        return d1.getDamage() == d2.getDamage();
    }

    private boolean compareDisplay(ItemMeta meta1, ItemMeta meta2) {
        boolean hasName1 = meta1.hasDisplayName();
        boolean hasName2 = meta2.hasDisplayName();
        if (hasName1 != hasName2) return false;

        if (hasName1) {
            String name1 = normalize(meta1.getDisplayName());
            String name2 = normalize(meta2.getDisplayName());
            return name1.equalsIgnoreCase(name2);
        }

        return true;
    }

    private boolean compareLore(ItemMeta meta1, ItemMeta meta2) {
        boolean hasLore1 = meta1.hasLore();
        boolean hasLore2 = meta2.hasLore();
        if (hasLore1 != hasLore2) return false;

        if (hasLore1) {
            List<String> lore1 = normalizeList(meta1.getLore());
            List<String> lore2 = normalizeList(meta2.getLore());
            return lore1.equals(lore2);
        }

        return true;
    }

    private boolean compareModelData(ItemMeta meta1, ItemMeta meta2) {
        boolean hasModel1 = meta1.hasCustomModelData();
        boolean hasModel2 = meta2.hasCustomModelData();
        if (hasModel1 != hasModel2) return false;

        if (hasModel1) {
            return meta1.getCustomModelData() == meta2.getCustomModelData();
        }

        return true;
    }

    private boolean compareNbtData(ItemStack item1, ItemStack item2) {
        return item1.isSimilar(item2);
    }

    private String normalize(String input) {
        return input.replaceAll("§[0-9a-fklmnor]", "").trim().toLowerCase();
    }

    private List<String> normalizeList(List<String> list) {
        List<String> normalized = new ArrayList<>();
        if (list == null) return normalized;

        for (String line : list) {
            normalized.add(normalize(line));
        }
        return normalized;
    }

}
