package xshyo.us.shopMaster.gui;

import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;

import java.util.*;

public class SellMenu {

    private final SellService sellService;
    private final StorageGui gui;
    private static final String MENU_PATH = "inventories.sell-gui";
    private final ShopMaster plugin = ShopMaster.getInstance();
    private final Player viewer;

    public SellMenu(SellService sellService, Player viewer) {
        this.sellService = sellService;
        this.viewer = viewer;
        this.gui = initializeGui();
    }

    private StorageGui initializeGui() {
        String title = Utils.translate(plugin.getLayouts().getString(MENU_PATH + ".title"));
        int configSize = plugin.getLayouts().getInt(MENU_PATH + ".size");
        int rows = (configSize % 9 == 0 && configSize >= 9 && configSize <= 54) ? configSize / 9 : 6;
        return Gui.storage().title(Component.text(Utils.translate(Utils.setPAPI(viewer, PluginUtils.formatTitle(title))))).rows(rows).create();
    }


    private GuiAction<InventoryCloseEvent> createCloseHandler() {
        return event -> {
            if (!(event.getPlayer() instanceof Player player)) return;
            int totalRows = gui.getRows();

            // Process all items in the first 5 rows (excluding control row)
            Map<Material, Integer> itemCounts = new HashMap<>();
            Map<Material, ShopItem> itemShops = new HashMap<>();

            Map<String, Map<Material, Integer>> itemsByCurrency = new HashMap<>();
            Map<String, Double> earningsByCurrency = new HashMap<>();
            double totalPrice = 0.0;
            int totalItems = 0;
            List<ItemStack> skippedItems = new ArrayList<>(); // Add this for skipped items
            Map<Material, Double> earningsByMaterial = new HashMap<>();

            for (int row = 0; row < totalRows; row++) {
                for (int col = 0; col < 9; col++) {
                    int slot = row * 9 + col;
                    ItemStack item = gui.getInventory().getItem(slot);

                    if (item != null && item.getType() != Material.AIR) {

                        SellResult result = sellService.sellGuiItem(player, item, item.getAmount());

                        if (result.status() == SellStatus.SUCCESS) {
                            Material type = item.getType();
                            int amount = item.getAmount();
                            double price = result.price();
                            String currency = result.currency();
                            earningsByMaterial.merge(type, price, Double::sum);

                            // Update summary information
                            itemCounts.put(type, itemCounts.getOrDefault(type, 0) + amount);
                            itemShops.put(type, result.shopItem());
                            // Update data by currency
                            itemsByCurrency.computeIfAbsent(currency, k -> new HashMap<>())
                                    .merge(type, amount, Integer::sum);
                            earningsByCurrency.merge(currency, price, Double::sum);

                            totalItems += amount;
                            totalPrice += price;

                            // Clear the item from inventory
                            gui.getInventory().setItem(slot, null);
                        } else {
                            // Add to skipped items
                            skippedItems.add(item);
                        }
                    }
                }
            }

            // Send summary message to player if any items were sold
            if (totalPrice > 0) {
                // Create SellAllResult with the collected data
                SellAllResult sellResult = new SellAllResult(
                        SellStatus.SUCCESS,
                        totalPrice,
                        earningsByCurrency,
                        itemCounts,
                        itemsByCurrency,
                        earningsByMaterial,// Add this parameter
                        skippedItems,
                        itemShops,
                        totalItems
                );

                // Generate and display the summary using the existing method
                sellResult.generateSummaryMessages(player);
            }
        };
    }


    private void setupCustomItems() {
        Map<Integer, Controls> customButtons = PluginUtils.loadCustomButtons(MENU_PATH + ".custom-items"
                , plugin.getLayouts(), gui.getRows());

        customButtons.forEach((slot, button) -> {
            gui.setItem(slot, new GuiItem(
                    button.getButtonItem(viewer),
                    event -> {
                        button.clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton());
                        event.setCancelled(true);
                    }

            ));

        });
    }


    public void open() {
        setupCustomItems();

        gui.setCloseGuiAction(createCloseHandler());

        gui.open(viewer);
    }
}