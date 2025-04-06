package xshyo.us.shopMaster.gui;

import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;

import java.util.*;

public class SellMenu {
    private final SellService sellService;
    private final StorageGui gui;

    public SellMenu(SellService sellService) {
        this.sellService = sellService;

        // Create the storage GUI with 6 rows
        this.gui = Gui.storage()
                .title(Component.text("Vender Items"))
                .rows(6)
                .create();

        // Set up close handler to sell remaining items
        gui.setCloseGuiAction(createCloseHandler());
    }
    private GuiAction<InventoryCloseEvent> createCloseHandler() {
        return event -> {
            if (!(event.getPlayer() instanceof Player player)) return;

            // Process all items in the first 5 rows (excluding control row)
            Map<Material, Integer> itemCounts = new HashMap<>();
            Map<String, Map<Material, Integer>> itemsByCurrency = new HashMap<>();
            Map<String, Double> earningsByCurrency = new HashMap<>();
            double totalPrice = 0.0;
            int totalItems = 0;
            List<ItemStack> skippedItems = new ArrayList<>(); // Add this for skipped items
            Map<Material, Double> earningsByMaterial = new HashMap<>();

            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    int slot = row * 9 + col;
                    ItemStack item = gui.getInventory().getItem(slot);

                    if (item != null && item.getType() != Material.AIR) {
                        // Sell the item
                        SellResult result = sellService.sellGuiItem(player, item, item.getAmount());

                        if (result.status() == SellStatus.SUCCESS) {
                            Material type = item.getType();
                            int amount = item.getAmount();
                            double price = result.price();
                            String currency = result.currency();
                            earningsByMaterial.merge(type, price, Double::sum);

                            // Update summary information
                            itemCounts.put(type, itemCounts.getOrDefault(type, 0) + amount);

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
                        totalItems
                );

                // Generate and display the summary using the existing method
                sellResult.generateSummaryMessages(player);
            }
        };
    }



    public void open(Player player) {
        gui.open(player);
    }
}