package xshyo.us.shopMaster.utilities.menu.controls.categories;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.shop.data.ShopItem;
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
        displayName = replacePlaceholders(displayName);

        List<String> lore = itemConfig.getStringList("lore");
        List<String> replacedLore = new ArrayList<>();
        for (String line : lore) {
            line = Utils.setPAPI(player, line);
            line = replacePlaceholders(line);
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

    private String replacePlaceholders(String text) {
        // Ajuste el precio por unidad
        double pricePerUnit = (double) shopItem.getBuyPrice() / shopItem.getAmount();
        double totalPrice = pricePerUnit * amount;

        // Obtener el displayName, y si es nulo, usar el nombre del material
        ItemStack itemStack = shopItem.createItemStack();
        String item = itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName()
                ? itemStack.getItemMeta().getDisplayName()
                : itemStack.getType().toString(); // Si no tiene displayName, usar el material

        String display = shopItem.getDisplayName() != null ? shopItem.getDisplayName() : item; // Si no tiene displayName, usar el nombre por defecto
        String material = itemStack.getType().toString(); // Nombre del material

        return text.replace("{amount}", String.valueOf(amount))
                .replace("{price}", String.valueOf(pricePerUnit))
                .replace("{totalPrice}", String.valueOf(totalPrice))
                .replace("{item}", item)
                .replace("{displayName}", display)
                .replace("{material}", material);
    }




}
