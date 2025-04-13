package xshyo.us.shopMaster.gui;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.services.records.PurchaseResult;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.shop.data.ShopButton;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.categories.DisplayControls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ShopCategoryMenu {
    /**
     * Registro del log erroneo al comprar muchos stacks
     * reiniciar la cantidad configurada cada vez que se realize una venta desde el menu
     *
     * */
    private final ShopMaster plugin = ShopMaster.getInstance();

    private final Player viewer;
    private final Shop shop;
    private final Gui categoryMenu;
    private int currentPage = 1;
    private final int maxPage;
    private final int totalItems; // Total de items en la tienda

    /**
     * Factory method to create a ShopCategoryMenu instance after checking permissions
     *
     * @param viewer The player viewing the menu
     * @param shop   The shop to display
     * @return A ShopCategoryMenu instance or null if player doesn't have permission
     */
    public static ShopCategoryMenu create(Player viewer, Shop shop) {
        // Verify permissions first
        if (!PluginUtils.hasPermissionToCategory(viewer, shop.getName().toLowerCase())) {
            PluginUtils.sendMessage(viewer, "MESSAGES.GUI.NO_ACCESS_TO_CATEGORY", "shopmaster.category." + shop.getName().toLowerCase());
            return null;
        }

        // If player has permissions, create and initialize the menu
        return new ShopCategoryMenu(viewer, shop);
    }

    /**
     * Private constructor - use ShopCategoryMenu.create() instead
     */
    private ShopCategoryMenu(Player viewer, Shop shop) {
        this.viewer = viewer;
        this.shop = shop;
        this.categoryMenu = initializeGui();

        // Calculate total number of items
        this.totalItems = calculateTotalItems();

        // Calculate maximum number of pages
        this.maxPage = calculateMaxPages();
    }

    private int calculateTotalItems() {
        return shop.getItems().size();
    }

    private int calculateMaxPages() {
        return shop.getItems().values().stream()
                .mapToInt(ShopItem::getPage)
                .max()
                .orElse(1); // Default to at least 1 page
    }

    private Gui initializeGui() {
        int size = 9; // Default size (1 row)

        // Check if shop size is valid
        if (shop.getSize() > 0 && shop.getSize() % 9 == 0) {
            size = shop.getSize();
        } else {
            // Use default config value if available
            int configSize = plugin.getLayouts().getInt("inventories.categories.size", 27); // 27 = 3 rows default

            // Make sure config size is valid (multiple of 9)
            if (configSize > 0 && configSize % 9 == 0) {
                size = configSize;
            }
        }

        return Gui.gui()
                .title(Component.empty())
                .rows(size / 9)
                .create();
    }

    public void openMenu(int page) {
        if (page < 1 || page > maxPage) {
            PluginUtils.sendMessage(viewer, "MESSAGES.GUI.PAGE_NOT_FOUND");
            if (maxPage > 0) {
                // Redirect to first page if shops are available
                openMenu(1);
            } else {
                // No pages available, show message and close
                PluginUtils.sendMessage(viewer, "MESSAGES.GUI.NO_ITEMS_AVAILABLE");
                viewer.closeInventory();
            }
            return;
        }

        this.currentPage = page;

        // Clear inventory
        for (int i = 0; i < categoryMenu.getRows() * 9; i++) {
            categoryMenu.removeItem(i);
        }

        Set<Integer> usedSlots = new HashSet<>(); // To track used slots

        // Filter items for current page
        Map<Integer, ShopItem> pageItems = shop.getItems().entrySet().stream()
                .filter(entry -> entry.getValue().getPage() == currentPage)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (pageItems.isEmpty()) {
            PluginUtils.sendMessage(viewer, "MESSAGES.GUI.EMPTY_PAGE");
            // Try to find a non-empty page
            int nonEmptyPage = findNonEmptyPage();
            if (nonEmptyPage > 0) {
                openMenu(nonEmptyPage);
            } else {
                PluginUtils.sendMessage(viewer, "MESSAGES.GUI.NO_ITEMS_AVAILABLE");
                viewer.closeInventory();
            }
            return;
        }

        // Display items for current page
        pageItems.forEach((itemId, shopItem) -> {
            for (int slot : shopItem.getSlots()) {
                if (slot < categoryMenu.getRows() * 9) {
                    if (shopItem.isHidden()) {
                        continue;// Check that slot is within menu
                    }
                    ItemStack itemStack = new DisplayControls("inventories.categories", shopItem).getButtonItem(viewer);
                    categoryMenu.setItem(slot, new GuiItem(itemStack, event -> {


                        // Get current click type
                        String clickType = getClickTypeString(event);

                        // Get configured button types
                        String buyButton = plugin.getConf().getString("config.gui.buttons.buy");
                        String sellButton = plugin.getConf().getString("config.gui.buttons.sell");

                        if (clickType.equals(buyButton)) {
                            // Revisa si el item es un placeholder de plugin faltante
                            if (isMissingPluginItem(itemStack)) {
                                PluginUtils.sendMessageWhitPath(viewer, ChatColor.RED + "This item requires a plugin that is not installed.");
                                return;
                            }

                            if (plugin.getConf().getBoolean("config.gui.purchase-confirmation.enabled")) {
                                new PurchaseConfirmationMenu(viewer, shopItem, shop, currentPage).openMenu();
                            } else {
                                PurchaseResult result = plugin.getPurchaseService().processPurchase(viewer, shopItem, shopItem.getAmount());
                                if (result.success()) {
                                    if (!shopItem.getBuyCommands().isEmpty()) {
                                        PluginUtils.executeActions(shopItem.getBuyCommands(), viewer, shopItem, shopItem.getAmount());
                                    }
                                    PluginUtils.sellLog(viewer.getName(), TypeService.BUY, shopItem.getAmount(),
                                            PluginUtils.formatItemName(shopItem.createItemStack().getType()), "" + shopItem.getBuyPrice(), shopItem.getShopName());
                                }
                            }
                        } else if (clickType.equals(sellButton)) {
                            // Revisa si el item es un placeholder de plugin faltante
                            if (isMissingPluginItem(itemStack)) {
                                PluginUtils.sendMessageWhitPath(viewer, ChatColor.RED + "This item requires a plugin that is not installed.");
                                return;
                            }
                            if (shopItem.getSellPrice() > 0) {
                                new SellAllConfirmationMenu(viewer, shopItem, shop, currentPage).openMenu();
                            } else {
                                PluginUtils.sendMessage(viewer, "MESSAGES.GUI.SELL.NOT_SELLABLE");
                            }
                        }
                    }));

                    usedSlots.add(slot);
                }
            }
        });

        // Add navigation buttons using shop buttons
        setupNavigationButtons(usedSlots);

        // Add custom elements
        setupCustomItems(usedSlots);

        // Fill empty slots
        fillEmptySlots(usedSlots);

        String title = shop.getTitle();
        if (title == null || title.isEmpty()) {
            title = plugin.getLayouts().getString("inventories.categories.title", "Title Shop");
        }
        title = PluginUtils.formatTitle(title);

        categoryMenu.update();
        categoryMenu.setDefaultClickAction(event -> event.setCancelled(true));
        categoryMenu.updateTitle(Utils.translate(Utils.setPAPI(viewer, title
                .replace("{totalpages}", String.valueOf(maxPage))
                .replace("{page}", String.valueOf(page)))));
        categoryMenu.open(viewer);
    }


    private boolean isMissingPluginItem(ItemStack item) {
        if (item.getType() != Material.BARRIER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey key = new NamespacedKey(plugin, "missing_plugin");
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }


    private int findNonEmptyPage() {
        for (int i = 1; i <= maxPage; i++) {
            int finalI = i;
            boolean hasItems = shop.getItems().values().stream()
                    .anyMatch(item -> item.getPage() == finalI);
            if (hasItems) {
                return i;
            }
        }
        return 0;
    }

    private String getClickTypeString(InventoryClickEvent event) {
        if (event.isLeftClick() && !event.isShiftClick()) return "LEFT";
        if (event.isRightClick() && !event.isShiftClick()) return "RIGHT";
        if (event.isLeftClick() && event.isShiftClick()) return "SHIFT_LEFT";
        if (event.isRightClick() && event.isShiftClick()) return "SHIFT_RIGHT";
        if (event.getClick() == ClickType.MIDDLE) return "MIDDLE";
        return "UNKNOWN";
    }

    private void setupNavigationButtons(Set<Integer> usedSlots) {
        // Get buttons for current shop
        Map<String, ShopButton> shopButtons = shop.getButtons();

        // Get default global buttons
        Map<String, ShopButton> defaultButtons = getDefaultButtons();

        // Process each button type
        String[] buttonTypes = {"next", "previous", "close", "back", "indicator"};

        for (String buttonType : buttonTypes) {
            // Use category-specific button if it exists, otherwise use default button
            ShopButton button = (shopButtons != null && shopButtons.containsKey(buttonType))
                    ? shopButtons.get(buttonType)
                    : defaultButtons.get(buttonType);

            if (button == null) continue; // If no button exists (neither specific nor default), continue

            // Check if button is enabled
            if (!button.enabled()) continue;

            // Specific logic for each button type
            switch (buttonType) {
                case "next":
                    if (currentPage < maxPage) {
                        ItemStack itemStack = createButtonItemStack(button);
                        categoryMenu.setItem(button.slots(), new GuiItem(itemStack, event -> {
                            event.setCancelled(true);
                            openMenu(currentPage + 1);
                        }));
                        usedSlots.addAll(button.slots());
                    }
                    break;
                case "previous":
                    if (currentPage > 1) {
                        ItemStack itemStack = createButtonItemStack(button);
                        categoryMenu.setItem(button.slots(), new GuiItem(itemStack, event -> {
                            event.setCancelled(true);
                            openMenu(currentPage - 1);
                        }));
                        usedSlots.addAll(button.slots());
                    }
                    break;
                case "close":
                    ItemStack closeItemStack = createButtonItemStack(button);
                    categoryMenu.setItem(button.slots(), new GuiItem(closeItemStack, event -> {
                        event.setCancelled(true);
                        viewer.closeInventory();
                    }));
                    usedSlots.addAll(button.slots());
                    break;
                case "back":
                    ItemStack backItemStack = createButtonItemStack(button);
                    categoryMenu.setItem(button.slots(), new GuiItem(backItemStack, event -> {
                        new ShopMainMenu(viewer).openMenu();
                    }));
                    usedSlots.addAll(button.slots());
                    break;
                case "indicator":
                    ItemStack indicatorItemStack = createButtonItemStack(button);
                    categoryMenu.setItem(button.slots(), new GuiItem(indicatorItemStack));
                    usedSlots.addAll(button.slots());
                    break;
            }
        }
    }

    public static Map<String, ShopButton> getDefaultButtons() {
        Map<String, ShopButton> defaultButtons = new HashMap<>();

        // Get plugin instance
        ShopMaster plugin = ShopMaster.getInstance();

        // Try to get default buttons section
        YamlDocument config = plugin.getLayouts();
        if (config == null) {
            plugin.getLogger().warning("Could not load configuration for default buttons");
            return defaultButtons;
        }

        Section buttonsSection = config.getSection("inventories.categories.buttons");
        if (buttonsSection == null) {
            plugin.getLogger().warning("Default buttons section not found in configuration");
            return defaultButtons;
        }

        // Process each button in section
        for (String buttonKey : buttonsSection.getRoutesAsStrings(false)) {
            Section buttonSection = buttonsSection.getSection(buttonKey);
            if (buttonSection != null) {
                ShopButton button = ShopButton.fromConfig(buttonSection);
                if (button != null) {
                    defaultButtons.put(buttonKey, button);
                }
            }
        }

        return defaultButtons;
    }

    private ItemStack createButtonItemStack(ShopButton button) {
        return new ItemBuilder(button.material())
                .setName(button.displayName())
                .setLore(button.lore())
                .setCustomModelData(button.modelData())
                .setEnchanted(button.glowing())
                .setAmount(button.amount())
                .addFlagsFromConfig(new HashSet<>(button.itemFlags()))
                .build();
    }

    private void setupCustomItems(Set<Integer> reservedSlots) {
        Map<Integer, Controls> customButtons = PluginUtils.loadCustomButtons("inventories.categories.custom-items"
                , plugin.getLayouts(), categoryMenu.getRows());

        customButtons.forEach((slot, button) -> {
            if (!reservedSlots.contains(slot)) {
                categoryMenu.setItem(slot, new GuiItem(
                        button.getButtonItem(viewer),
                        event -> button.clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton())
                ));
                reservedSlots.add(slot);
            }
        });
    }

    private void fillEmptySlots(Set<Integer> reservedSlots) {
        for (int slot = 0; slot < categoryMenu.getInventory().getSize(); slot++) {
            if (!reservedSlots.contains(slot)) {
                categoryMenu.setItem(slot, new GuiItem(
                        new ItemStack(Material.AIR),
                        event -> event.setCancelled(true)
                ));
            }
        }
    }
}