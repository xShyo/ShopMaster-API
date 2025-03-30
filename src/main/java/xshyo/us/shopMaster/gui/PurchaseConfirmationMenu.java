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
import xshyo.us.shopMaster.services.PurchaseService;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.CategoryControls;
import xshyo.us.shopMaster.utilities.menu.controls.categories.ConfirmControls;
import xshyo.us.shopMaster.utilities.menu.controls.categories.InformationControls;
import xshyo.us.shopMaster.utilities.menu.controls.purchase.StackSelectorControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.BackControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.CloseControls;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.*;

public class PurchaseConfirmationMenu {

    private final Player viewer;
    private final ShopItem item;
    private final Gui confirmationMenu;
    private int quantity = 1;
    private final double pricePerUnit;
    private final Shop shop;
    private final int returnPage;
    private final ShopMaster plugin;
    private static final String MENU_PATH = "inventories.purchase-confirmation";
    private final Set<Integer> reservedSlots = new HashSet<>();


    public PurchaseConfirmationMenu(Player viewer, ShopItem item, Shop shop, int returnPage) {
        this.viewer = viewer;
        this.item = item;
        this.shop = shop;
        this.returnPage = returnPage;
        this.pricePerUnit = item.getBuyPrice();
        this.plugin = ShopMaster.getInstance();
        this.confirmationMenu = initializeGui();
        this.quantity = Math.max(1, item.getAmount());

    }


    private void setupDirectQuantityControls() {
        Section quantityControlsSection = plugin.getLayouts().getSection(MENU_PATH + ".items.quantity-controls");

        if (quantityControlsSection == null) return;

        for (Object key : quantityControlsSection.getKeys()) {
            String controlKey = key.toString();
            Section controlSection = quantityControlsSection.getSection(controlKey);
            if (controlSection == null) continue;

            int slot = controlSection.getInt("slot", 0);
            String setAmountStr = controlSection.getString("set_amount", "0");

            // Determinar si es un cambio absoluto o relativo
            boolean isAbsolute = !(setAmountStr.startsWith("+") || setAmountStr.startsWith("-"));
            int changeValue = Integer.parseInt(setAmountStr.startsWith("+") ? setAmountStr.substring(1) : setAmountStr);

            int maxStackSize = item.createItemStack().getMaxStackSize();

            boolean shouldDisplay = isValidQuantityChange(changeValue, isAbsolute, maxStackSize);
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
                confirmationMenu.setItem(slot, new GuiItem(controlItem, event -> {
                    event.setCancelled(true);
                    updateQuantityDirectly(finalChangeValue, finalIsAbsolute);
                }));
            } else {
                // Si no se debe mostrar, eliminar el ítem del slot
                confirmationMenu.removeItem(slot);
                reservedSlots.remove(slot);
            }
        }
    }


    private boolean isValidQuantityChange(int changeValue, boolean isAbsolute, int maxStackSize) {
        // Si el stack máximo es 1, no mostrar ningún botón de cambio de cantidad
        if (maxStackSize <= 1) {
            return false;
        }

        if (isAbsolute) {
            // Para valores absolutos, verificar que estén dentro del rango del stack máximo
            return changeValue >= 1 && changeValue <= maxStackSize;
        } else {
            // Para cambios relativos, verificar que el resultado esté dentro del rango del stack máximo
            int resultingQuantity = quantity + changeValue;
            return resultingQuantity >= 1 && resultingQuantity <= maxStackSize;
        }
    }


    private void updateQuantityDirectly(int changeValue, boolean isAbsolute) {
        int newQuantity;

        if (isAbsolute) {
            newQuantity = changeValue;
        } else {
            newQuantity = quantity + changeValue;
        }

        // Asegurar que la nueva cantidad esté dentro del rango válido
        newQuantity = Math.max(1, Math.min(64, newQuantity));

        // Actualizar la cantidad
        quantity = newQuantity;
        // Actualizar los ítems del menú
        updateItemsWithQuantity();
    }


    private void updateItemsWithQuantity() {

        setupDirectQuantityControls();


        int displaySlot = plugin.getLayouts().getInt(MENU_PATH + ".items.display.slot");
        ItemStack itemdisplay = new ItemStack(item.createItemStack());
        itemdisplay.setAmount(quantity);


        GuiItem guiItem = new GuiItem(itemdisplay, event -> event.setCancelled(true));
        confirmationMenu.updateItem(displaySlot, guiItem);

        reservedSlots.add(displaySlot);


        // Actualizar confirm item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.confirm", plugin.getLayouts(),
                path -> new ConfirmControls(MENU_PATH + ".items.confirm", quantity, item, null, TypeService.BUY),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer),
                        event -> new ConfirmControls(MENU_PATH + ".items.confirm", quantity, item, null, TypeService.BUY).clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton())));
                reservedSlots.add(slot);
            }
        });

        // Actualizar information item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.information", plugin.getLayouts(),
                path -> new InformationControls(MENU_PATH + ".items.information", quantity, item, TypeService.BUY),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer)));
                reservedSlots.add(slot);
            }
        });

        // Actualizar stack_selector item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.stack_selector", plugin.getLayouts(),
                path -> new StackSelectorControls(MENU_PATH + ".items.stack_selector"),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer),
                        event -> {
                    new StackSelectorMenu(viewer, item, shop).openMenu();
//                            openStackSelector();

                        }));
                reservedSlots.add(slot);
            }
        });


        // Actualizar la GUI
        confirmationMenu.update();
    }

    public void openMenu() {
        // Limpiar el menú y resetear los slots reservados
        reservedSlots.clear();

        for (int i = 0; i < confirmationMenu.getRows() * 9; i++) {
            confirmationMenu.removeItem(i);
        }

        setupDirectQuantityControls();


        // Configurar botones de cierre
        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.close", plugin.getLayouts(),
                path -> new CloseControls(MENU_PATH + ".buttons.close"),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> viewer.closeInventory()));
                reservedSlots.add(slot);
            }
        });

        // Configurar botones de cierre
        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.back", plugin.getLayouts(),
                path -> new BackControls(MENU_PATH + ".buttons.back"),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event ->
                        new ShopCategoryMenu(viewer, shop).openMenu(1)));
                reservedSlots.add(slot);
            }
        });

        // Configurar ítems personalizados
        setupCustomItems();

        // Ahora actualizar los ítems relacionados con la cantidad
        updateItemsWithQuantity();

        // Rellenar slots vacíos
        fillEmptySlots();

        String displayName = item.getDisplayName() != null ? item.getDisplayName() : item.createItemStack().getType().toString();
        String title = plugin.getLayouts().getString(MENU_PATH + ".title", "&8Confirmar Compra: &f{item}");
        title = title.replace("{item}", displayName);
        title = PluginUtils.formatTitle(title);
        confirmationMenu.updateTitle(Utils.translate(title));


        confirmationMenu.setDefaultClickAction(event -> event.setCancelled(true));
        confirmationMenu.open(viewer);
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
                , plugin.getLayouts(), confirmationMenu.getRows());

        customButtons.forEach((slot, button) -> {
            if (!reservedSlots.contains(slot)) {
                confirmationMenu.setItem(slot, new GuiItem(
                        button.getButtonItem(viewer),
                        event -> button.clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton())
                ));
                reservedSlots.add(slot);
            }
        });
    }

    private void fillEmptySlots() {
        for (int slot = 0; slot < confirmationMenu.getInventory().getSize(); slot++) {
            if (!reservedSlots.contains(slot)) {
                confirmationMenu.setItem(slot, new GuiItem(
                        new ItemStack(Material.AIR),
                        event -> event.setCancelled(true)
                ));
            }
        }
    }


}