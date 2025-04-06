package xshyo.us.shopMaster.utilities.menu.controls.sell;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.CurrencyFormatter;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class SellInventoryControls extends Controls {

    private final ShopMaster plugin = ShopMaster.getInstance();
    private final String path;
    private final int amount;
    private final ShopItem shopItem;

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

        // Asegurarse de que display no sea null
        String displayName = shopItem.getDisplayName();
        String display = displayName != null ? displayName : item; // Si no tiene displayName, usar el nombre por defecto
        String material = itemStack.getType().toString(); // Nombre del material

        // Asegurarse de que ningún valor de reemplazo sea null
        String amountStr = String.valueOf(amount);
        String priceStr =  CurrencyFormatter.formatCurrency(pricePerUnit, shopItem.getEconomy());
        String totalPriceStr = CurrencyFormatter.formatCurrency(totalPrice, shopItem.getEconomy());

        // Garantizar que item no sea null (aunque ya debería estar cubierto arriba)
        if (item == null) item = "Unknown Item";

        // Garantizar que display no sea null
        if (display == null) display = "Unknown Item";

        return text.replace("{amount}", amountStr)
                .replace("{price}", priceStr)
                .replace("{totalPrice}", totalPriceStr)
                .replace("{currency}", shopItem.getEconomy())
                .replace("{item}", item)
                .replace("{displayName}", display)
                .replace("{material}", material);
    }

}
