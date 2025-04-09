package xshyo.us.shopMaster.utilities.menu.controls.purchase;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class StackPurchaseControls extends Controls {

    private final Section section;
    private final ShopItem shopItem;
    private final int amount;

    @Override
    public ItemStack getButtonItem(Player player) {
        Section itemConfig = section;

        if (itemConfig == null) {
            return new ItemStack(Material.STONE);
        }
        String material = itemConfig.getString("material");
        if (material == null || material.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        String displayName = itemConfig.getString("display_name");
        displayName = Utils.setPAPI(player, displayName);
        displayName = PluginUtils.replacePlaceholders(displayName, shopItem, amount);

        List<String> lore = itemConfig.getStringList("lore");
        List<String> replacedLore = new ArrayList<>();
        for (String line : lore) {
            line = Utils.setPAPI(player, line);
            line = PluginUtils.replacePlaceholders(line, shopItem, amount);
            replacedLore.add(line);
        }

        // Usar la cantidad predeterminada del ShopItem si está configurada
        int configuredAmount = Math.max(1, shopItem.getAmount());
        int amount = itemConfig.getInt("amount", configuredAmount);

        boolean glowing = itemConfig.getBoolean("glowing", false);
        int modelData = itemConfig.getInt("model_data", 0);
        List<String> itemFlagsStr = itemConfig.getStringList("item_flags");

        return new ItemBuilder(material)
                .setName(displayName)
                .setLore(replacedLore)
                .setAmount(amount)
                .setEnchanted(glowing)
                .addFlagsFromConfig(new HashSet<>(itemFlagsStr))
                .setCustomModelData(modelData)
                .build();
    }


//    @Override
//    public ItemStack getButtonItem(Player player) {
//        return buildItem(section, player);
//    }

}
