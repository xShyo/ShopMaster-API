package xshyo.us.shopMaster.utilities.menu.controls.categories;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.PurchaseResult;
import xshyo.us.shopMaster.services.records.SellResult;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class ConfirmControls extends Controls {

    private final ShopMaster plugin = ShopMaster.getInstance();
    private final String path;
    private final int amount;
    private final ShopItem shopItem;
    private final SellService sellService;
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


    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {

        if (typeService == TypeService.BUY) {
            PurchaseResult result = plugin.getPurchaseService().processPurchase(player, shopItem, amount);
            if (result.success()) {
                if (!shopItem.getBuyCommands().isEmpty()) {
                    PluginUtils.executeActions(shopItem.getBuyCommands(), player, shopItem, amount);
                }

                PluginUtils.sellLog(player.getName(), TypeService.BUY, amount,
                        PluginUtils.formatItemName(shopItem.createItemStack().getType()), "" + result.price(), shopItem.getShopName());

            }
            player.closeInventory();

        } else {

            SellResult result = sellService.sellItem(player, shopItem.createItemStack(), amount, true);

            switch (result.status()) {
                case SUCCESS:
                    PluginUtils.sendMessage(player, "MESSAGES.GUI.SELL.SUCCESS", amount, PluginUtils.formatItemName(shopItem.createItemStack().getType()), result.price());
                    if (!shopItem.getSellCommands().isEmpty()) {
                        PluginUtils.executeActions(shopItem.getSellCommands(), player, shopItem, amount);
                    }
                    PluginUtils.sendSellAll(player, amount,  result.price());
                    PluginUtils.sellLog(player.getName(), typeService, amount, PluginUtils.formatItemName(shopItem.createItemStack().getType()), "" + result.price(), shopItem.getShopName());
                    break;
                case WORLD_BLACKLISTED:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.WORLD_BLACKLISTED");
                    break;
                case GAMEMODE_BLACKLISTED:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.GAMEMODE_BLACKLISTED");
                    break;
                case NOT_SELLABLE:
                    PluginUtils.sendMessage(player, "MESSAGES.GUI.SELL.NOT_SELLABLE");
                    break;
                case INVALID_ECONOMY:
                    PluginUtils.sendMessage(player, "MESSAGES.GUI.SELL.INVALID_ECONOMY");
                    break;
                case ERROR:
                    PluginUtils.sendMessage(player, "MESSAGES.GUI.SELL.ERROR");
                    break;

            }

            player.closeInventory();

        }


    }


}
