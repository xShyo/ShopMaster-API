package xshyo.us.shopMaster.utilities.menu.controls.categories;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.CurrencyFormatter;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class InformationControls extends Controls {

    private final ShopMaster plugin = ShopMaster.getInstance();
    private final String path;
    private final int amount;
    private final ShopItem shopItem;
    private final TypeService typeService;

    @Override
    public ItemStack getButtonItem(Player player) {
        Section itemConfig = plugin.getLayouts().getSection(path);

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
        int amount = itemConfig.getInt("amount", 1);
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



}
