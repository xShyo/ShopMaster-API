package xshyo.us.shopMaster.utilities.menu;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

public abstract class Controls {

    private final ShopMaster shopMaster = ShopMaster.getInstance();


    public static Controls placeholder(ItemStack itemStack) {
        return new Controls() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return itemStack;
            }

        };
    }


    public ItemStack buildItem(Section itemConfig, Player player) {
        if (itemConfig == null) {
            return new ItemStack(Material.STONE);
        }

        String material = itemConfig.getString("material");
        if (material == null || material.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        String displayName = itemConfig.getString("display_name");
        displayName = Utils.setPAPI(player, displayName);
        List<String> lore = itemConfig.getStringList("lore");
        List<String> replacedLore = new ArrayList<>();
        for (String line : lore) {
            line = Utils.setPAPI(player, line);
            line = Utils.translate(line);
            replacedLore.add(line);
        }
        int amount = itemConfig.getInt("amount", 1);
        boolean glowing = itemConfig.getBoolean("glowing", false);
        int modelData = itemConfig.getInt("model_data", 0);
        boolean hide_attributes = itemConfig.getBoolean("hide_attributes", true);

        return new ItemBuilder(material)
                .setName(Utils.translate(displayName))
                .setLore(replacedLore)
                .setAmount(amount)
                .setEnchanted(glowing)
                .setCustomModelData(modelData)
                .build();
    }



    public abstract ItemStack getButtonItem(Player player);

    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
    }

    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return false;
    }

    public boolean shouldCancel(Player player, int slot, ClickType clickType) {
        return true;
    }

    public boolean shouldShift(Player player, int slot, ClickType clickType) {
        return true;
    }

    public void close(Player player) {
        player.closeInventory();
    }

}