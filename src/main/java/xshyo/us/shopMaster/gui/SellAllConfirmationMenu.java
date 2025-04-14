package xshyo.us.shopMaster.gui;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.categories.ConfirmControls;
import xshyo.us.shopMaster.utilities.menu.controls.categories.InformationControls;
import xshyo.us.shopMaster.utilities.menu.controls.sell.SellInventoryControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.BackControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.CloseControls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.*;

public class SellAllConfirmationMenu {

    private final Player viewer;
    private final ShopItem item;

    private final Shop shop;
    private final Gui sellAllMenu;
    private final int returnPage;
    private int quantity = 1;
    private final double pricePerUnit;

    private final ShopMaster plugin;
    private static final String MENU_PATH = "inventories.sell-all-confirmation";
    private final Set<Integer> reservedSlots = new HashSet<>();
    private final SellService sellService;

    public SellAllConfirmationMenu(Player viewer, ShopItem item, Shop shop, int returnPage) {
        this.viewer = viewer;
        this.item = item;
        this.shop = shop;
        this.returnPage = returnPage;
        this.pricePerUnit = item.getSellPrice();
        this.plugin = ShopMaster.getInstance();
        this.sellService = plugin.getSellService();
        this.sellAllMenu = initializeGui();
        this.quantity = Math.max(1, item.getAmount());

    }


    private void setupDirectQuantityControls() {
        Section quantityControlsSection = plugin.getLayouts().getSection(MENU_PATH + ".items.quantity-controls");

        if (quantityControlsSection == null) return;

        int playerInventoryItemCount = getTotalItemCountInPlayerInventory();

        for (Object key : quantityControlsSection.getKeys()) {
            String controlKey = key.toString();
            Section controlSection = quantityControlsSection.getSection(controlKey);
            if (controlSection == null) continue;

            int slot = controlSection.getInt("slot", 0);
            String setAmountStr = controlSection.getString("set_amount", "0");

            // Determinar si es un cambio absoluto o relativo
            boolean isAbsolute = !(setAmountStr.startsWith("+") || setAmountStr.startsWith("-"));
            int changeValue = Integer.parseInt(setAmountStr.startsWith("+") ? setAmountStr.substring(1) : setAmountStr);

            // Verificar que el cambio de cantidad sea válido
            boolean shouldDisplay = isValidQuantityChange(changeValue, isAbsolute, playerInventoryItemCount);

            if (shouldDisplay) {
                Material material = Material.valueOf(controlSection.getString("material", "STONE"));
                int amount = controlSection.getInt("amount", 1);
                String displayName = controlSection.getString("display_name", "");

                ItemStack controlItem = new ItemBuilder(material)
                        .setAmount(amount)
                        .setName(Utils.translate(displayName))
                        .build();

                final int finalChangeValue = changeValue;
                final boolean finalIsAbsolute = isAbsolute;

                reservedSlots.add(slot);
                sellAllMenu.setItem(slot, new GuiItem(controlItem, event -> {
                    event.setCancelled(true);
                    updateQuantityDirectly(finalChangeValue, finalIsAbsolute);
                }));
            } else {
                // Si no se debe mostrar, eliminar el ítem del slot
                sellAllMenu.removeItem(slot);
                reservedSlots.remove(slot);
            }
        }
    }

    public void openMenu() {
        // Limpiar el menú y resetear los slots reservados
        reservedSlots.clear();

        for (int i = 0; i < sellAllMenu.getRows() * 9; i++) {
            sellAllMenu.removeItem(i);
        }

        // Configurar botones de cierre
        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.close", plugin.getLayouts(),
                path -> new CloseControls(MENU_PATH + ".buttons.close"),
                sellAllMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                sellAllMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> viewer.closeInventory()));
                reservedSlots.add(slot);
            }
        });

        // Configurar botones de retorno
        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.back", plugin.getLayouts(),
                path -> new BackControls(MENU_PATH + ".buttons.back"),
                sellAllMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                sellAllMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event ->{
                    ShopCategoryMenu menu = ShopCategoryMenu.create(viewer, shop);
                    if (menu != null) {
                        menu.openMenu(returnPage);
                    }
                }
                    ));
                reservedSlots.add(slot);
            }
        });


        // Configurar controles de cantidad
        setupDirectQuantityControls();

        // Configurar ítems personalizados
        setupCustomItems();

        updateItemsWithQuantity();

        // Rellenar slots vacíos
        fillEmptySlots();

        String displayName = item.getDisplayName() != null ? item.getDisplayName() : item.createItemStack().getType().toString();

        String title = plugin.getLayouts().getString(MENU_PATH + ".title", "&8Confirmar Venta: &f{item}");
        title = title.replace("{item}", displayName);
        title = PluginUtils.formatTitle(title);
        sellAllMenu.updateTitle(Utils.translate(title));

        sellAllMenu.setDefaultClickAction(event -> event.setCancelled(true));
        sellAllMenu.open(viewer);
    }


    private int getTotalItemCountInPlayerInventory() {
        int totalCount = 0;

        for (ItemStack inventoryItem : viewer.getInventory().getContents()) {
            if (inventoryItem != null) {
                boolean isSellable = ShopMaster.getInstance().getSellService().isSellable(viewer, inventoryItem);

                if (isSellable) {
                    totalCount += inventoryItem.getAmount();
                }
            }
        }
        return totalCount;
    }


    private boolean isValidQuantityChange(int changeValue, boolean isAbsolute, int maxInventoryCount) {
        if (maxInventoryCount <= 1) {
            return false;
        }

        if (isAbsolute) {
            // Para valores absolutos, verificar que estén dentro del rango del inventario
            return changeValue >= 1 && changeValue <= maxInventoryCount;
        } else {
            // Para cambios relativos, verificar que el resultado esté dentro del rango del inventario
            int resultingQuantity = quantity + changeValue;
            return resultingQuantity >= 1 && resultingQuantity <= maxInventoryCount;
        }
    }

    private void updateQuantityDirectly(int changeValue, boolean isAbsolute) {
        int maxInventoryCount = getTotalItemCountInPlayerInventory();
        int itemMaxStackSize = item.createItemStack().getMaxStackSize();

        int newQuantity;

        if (isAbsolute) {
            newQuantity = changeValue;
        } else {
            newQuantity = quantity + changeValue;
        }

        // Asegurar que la nueva cantidad esté dentro del rango válido
        newQuantity = Math.max(1, Math.min(Math.min(maxInventoryCount, itemMaxStackSize), newQuantity));

        // Actualizar la cantidad
        quantity = newQuantity;

        updateItemsWithQuantity();

    }

    private void updateItemsWithQuantity() {
        setupDirectQuantityControls();

        int displaySlot = plugin.getLayouts().getInt(MENU_PATH + ".items.display.slot");
        ItemStack itemdisplay = new ItemStack(item.createItemStack());
        itemdisplay.setAmount(quantity);

        GuiItem guiItem = new GuiItem(itemdisplay, event -> event.setCancelled(true));
        sellAllMenu.updateItem(displaySlot, guiItem);
        reservedSlots.add(displaySlot);

        // Actualizar confirm item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.confirm", plugin.getLayouts(),
                path -> new ConfirmControls(MENU_PATH + ".items.confirm", quantity, item, sellService, TypeService.SELL),
                sellAllMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                sellAllMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer),
                        event -> {
                            new ConfirmControls(MENU_PATH + ".items.confirm", quantity, item, sellService, TypeService.SELL).clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton());
                            updateItemsWithQuantity();
                        }

                ));
                reservedSlots.add(slot);
            }
        });

        PluginUtils.loadSingleButton(MENU_PATH + ".items.information", plugin.getLayouts(),
                path -> new InformationControls(MENU_PATH + ".items.information", quantity, item, TypeService.SELL),
                sellAllMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                sellAllMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer)));
                reservedSlots.add(slot);
            }
        });


        // Actualizar stack_selector item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.sell-inventory", plugin.getLayouts(),
                path -> new SellInventoryControls(MENU_PATH + ".items.sell-inventory", quantity, item),
                sellAllMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                sellAllMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer),
                        event -> {

                            SellAllResult result =  sellService.sellAllItemOfType(viewer, itemdisplay);
                            result.generateSummaryMessages(viewer);
                            viewer.closeInventory();


                        }

                ));
                reservedSlots.add(slot);
            }
        });

        sellAllMenu.update();  // Esta línea es clave

    }


    private Gui initializeGui() {
        int configSize = plugin.getLayouts().getInt(MENU_PATH + ".size");
        int rows = (configSize % 9 == 0 && configSize >= 9 && configSize <= 54) ? configSize / 9 : 6;
        return Gui.gui()
                .title(Component.empty())
                .rows(rows)
                .create();
    }

    private void setupCustomItems() {
        Map<Integer, Controls> customButtons = PluginUtils.loadCustomButtons(MENU_PATH + ".custom-items"
                , plugin.getLayouts(), sellAllMenu.getRows());

        customButtons.forEach((slot, button) -> {
            if (!reservedSlots.contains(slot)) {
                sellAllMenu.setItem(slot, new GuiItem(
                        button.getButtonItem(viewer),
                        event -> button.clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton())
                ));
                reservedSlots.add(slot);
            }
        });
    }

    private void fillEmptySlots() {
        for (int slot = 0; slot < sellAllMenu.getInventory().getSize(); slot++) {
            if (!reservedSlots.contains(slot)) {
                sellAllMenu.setItem(slot, new GuiItem(
                        new ItemStack(Material.AIR),
                        event -> event.setCancelled(true)
                ));
            }
        }
    }

}