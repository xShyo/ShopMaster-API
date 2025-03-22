package xshyo.us.shopMaster.utilities.menu.controls.categories;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DisplayControls extends Controls {

    private final ShopMaster plugin = ShopMaster.getInstance();
    private final String path;
    private final ShopItem shopItem;


    @Override
    public ItemStack getButtonItem(Player player) {
        Section itemConfig = plugin.getLayouts().getSection(path);
        ItemStack itemStack = shopItem.createItemStack();

        // Obtener el lore actual del item (si tiene)
        ItemMeta meta = itemStack.getItemMeta();
        List<String> lore = (meta != null && meta.getLore() != null) ? meta.getLore() : new ArrayList<>();

        // Obtener los precios de compra y venta
        int buyPrice = shopItem.getBuyPrice();
        int sellPrice = shopItem.getSellPrice();

        // Verificar si se puede comprar o vender y cargar el lore correspondiente
        List<String> additionalLore = new ArrayList<>();

        if (buyPrice > 0 && sellPrice > 0) {
            // Si ambos precios son mayores que 0, usamos el formato 'both'
            additionalLore.addAll(itemConfig.getStringList("items.loreFormat.both"));
            additionalLore = additionalLore.stream()
                    .map(line -> line.replace("{buy}", String.valueOf(buyPrice))
                            .replace("{sell}", String.valueOf(sellPrice)))
                    .collect(Collectors.toList());
        } else if (buyPrice > 0) {
            // Si solo se puede comprar, usamos el formato 'only-buy'
            additionalLore.addAll(itemConfig.getStringList("items.loreFormat.only-buy"));
            additionalLore = additionalLore.stream()
                    .map(line -> line.replace("{buy}", String.valueOf(buyPrice)))
                    .collect(Collectors.toList());
        } else if (sellPrice > 0) {
            // Si solo se puede vender, usamos el formato 'only-sell'
            additionalLore.addAll(itemConfig.getStringList("items.loreFormat.only-sell"));
            additionalLore = additionalLore.stream()
                    .map(line -> line.replace("{sell}", String.valueOf(sellPrice)))
                    .collect(Collectors.toList());
        }

        // Añadir el nuevo lore al lore actual
        lore.addAll(Utils.translate(additionalLore));

        // Asignar el lore actualizado al item
        if (meta != null) {
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }




}
