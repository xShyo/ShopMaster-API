package xshyo.us.shopMaster.utilities.menu.controls;

import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.gui.ShopCategoryMenu;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class CategoryControls extends Controls {
    private final Shop shop;


    @Override
    public ItemStack getButtonItem(Player player) {

        if (shop == null) {
            return new ItemStack(Material.STONE);
        }

        String material = shop.getCategory().getMaterial().toString();
        if (material == null || material.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        String displayName = shop.getCategory().getDisplayName();
        List<String> lore = shop.getCategory().getLore();
        List<String> replacedLore = new ArrayList<>();
        for (String line : lore) {
            replacedLore.add(line);
        }
        int amount = shop.getCategory().getAmount();
        boolean glowing = shop.getCategory().isGlowing();
        int modelData = shop.getCategory().getModelData();
        List<String> item_flags = shop.getCategory().getItemFlags();

        return new ItemBuilder(material)
                .setName(displayName)
                .setLore(replacedLore)
                .setAmount(amount)
                .setEnchanted(glowing)
                .addFlagsFromConfig(new HashSet<>(item_flags))
                .setCustomModelData(modelData)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        new ShopCategoryMenu(player, shop).openMenu(1);
    }


}
