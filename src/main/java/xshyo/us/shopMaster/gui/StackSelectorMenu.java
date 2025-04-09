package xshyo.us.shopMaster.gui;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.services.records.PurchaseResult;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.purchase.StackPurchaseControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.BackControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.CloseControls;
import xshyo.us.theAPI.utilities.Utils;

import java.util.*;

public class StackSelectorMenu {

    private final Player viewer;
    private final ShopItem item;
    private final Gui selectorMenu;
    private final Shop shop;
    private final ShopMaster plugin;
    private static final String MENU_PATH = "inventories.stack-selector";
    private final Set<Integer> reservedSlots = new HashSet<>();
    // Añadir campo para almacenar la página de retorno
    private final int returnPage;
    // Añadir referencia al menú de confirmación original
    private final PurchaseConfirmationMenu purchaseMenu;

    // Modificar el constructor para aceptar la página de retorno
    // Constructor para cuando venimos de un menú de confirmación existente
    public StackSelectorMenu(Player viewer, ShopItem item, Shop shop, PurchaseConfirmationMenu purchaseMenu) {
        this.viewer = viewer;
        this.item = item;
        this.shop = shop;
        this.plugin = ShopMaster.getInstance();
        this.selectorMenu = initializeGui();
        this.returnPage = purchaseMenu.getReturnPage();
        this.purchaseMenu = purchaseMenu;
    }

    // Constructor anterior para compatibilidad
    public StackSelectorMenu(Player viewer, ShopItem item, Shop shop) {
        this.viewer = viewer;
        this.item = item;
        this.shop = shop;
        this.plugin = ShopMaster.getInstance();
        this.selectorMenu = initializeGui();
        this.returnPage = 1;
        this.purchaseMenu = null;
    }

    // Constructor con página de retorno
    public StackSelectorMenu(Player viewer, ShopItem item, Shop shop, int returnPage) {
        this.viewer = viewer;
        this.item = item;
        this.shop = shop;
        this.plugin = ShopMaster.getInstance();
        this.selectorMenu = initializeGui();
        this.returnPage = returnPage;
        this.purchaseMenu = null;
    }

    public void openMenu() {
        // Limpiar el menú y resetear los slots reservados
        reservedSlots.clear();

        for (int i = 0; i < selectorMenu.getRows() * 9; i++) {
            selectorMenu.removeItem(i);
        }

        // Configurar botones de cierre
        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.close", plugin.getLayouts(),
                path -> new CloseControls(MENU_PATH + ".buttons.close"),
                selectorMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                selectorMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> viewer.closeInventory()));
                reservedSlots.add(slot);
            }
        });

        // Configurar botones de regresar - Modificado para volver al PurchaseConfirmationMenu
        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.back", plugin.getLayouts(),
                path -> new BackControls(MENU_PATH + ".buttons.back"),
                selectorMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                selectorMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> {
                    // Si tenemos una referencia al menú original, usarla
                    if (purchaseMenu != null) {
                        // Reabrir el menú original que ya tiene la cantidad configurada
                        purchaseMenu.openMenu();
                    } else {
                        // Fallback al comportamiento anterior
                        new PurchaseConfirmationMenu(viewer, item, shop, returnPage).openMenu();
                    }
                }));
                reservedSlots.add(slot);
            }
        });

        // Resto del código igual...
        setupCustomItems();
        setupStackControls();
        fillEmptySlots();

        String title = plugin.getLayouts().getString(MENU_PATH + ".title", "&8☀ Seleccionar Cantidad de Stacks");
        selectorMenu.updateTitle(Utils.translate(title));

        selectorMenu.setDefaultClickAction(event -> event.setCancelled(true));
        selectorMenu.open(viewer);
    }


    private void setupStackControls() {
        Section stackControlsSection = plugin.getLayouts().getSection(MENU_PATH + ".items.stack-controls");

        if (stackControlsSection == null) {
            return;
        }

        // Iterar a través de todas las keys de la sección stack-controls
        // Esto permitirá tener stack_1, stack_2, etc.
        for (String stackKey : stackControlsSection.getRoutesAsStrings(false)) {
            String fullPath = MENU_PATH + ".items.stack-controls." + stackKey;
            Section stackSection = plugin.getLayouts().getSection(fullPath);
            if (stackSection == null) continue;

            int slot = stackSection.getInt("slot", -1);
            if (slot == -1) continue;
            int stackAmount = stackSection.getInt("stack_amount", 1);

            int maxStackSize = item.createItemStack().getMaxStackSize();

            final int finalTotalItems = stackAmount * maxStackSize;

            selectorMenu.setItem(slot, new GuiItem(new StackPurchaseControls(stackSection, item, finalTotalItems).getButtonItem(viewer), event -> {
                event.setCancelled(true);
                viewer.closeInventory();

                PurchaseResult result =  ShopMaster.getInstance().getPurchaseService().processPurchase(viewer, item, finalTotalItems);

                if (result.success()) {
                    if (!item.getBuyCommands().isEmpty()) {
                        PluginUtils.executeActions(item.getBuyCommands(), viewer, item, finalTotalItems);
                    }
                }
            }));

            reservedSlots.add(slot);
        }
    }

    private Gui initializeGui() {
        int configSize = plugin.getLayouts().getInt(MENU_PATH + ".size", 54);
        int rows = (configSize % 9 == 0 && configSize >= 9 && configSize <= 54) ? configSize / 9 : 6;
        return Gui.gui()
                .title(Component.empty())
                .rows(rows)
                .create();
    }

    private void setupCustomItems() {
        Map<Integer, Controls> customButtons = PluginUtils.loadCustomButtons(MENU_PATH + ".custom-items"
                , plugin.getLayouts(), selectorMenu.getRows());

        customButtons.forEach((slot, button) -> {
            if (!reservedSlots.contains(slot)) {
                selectorMenu.setItem(slot, new GuiItem(
                        button.getButtonItem(viewer),
                        event -> button.clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton())
                ));
                reservedSlots.add(slot);
            }
        });
    }

    private void fillEmptySlots() {
        for (int slot = 0; slot < selectorMenu.getInventory().getSize(); slot++) {
            if (!reservedSlots.contains(slot)) {
                selectorMenu.setItem(slot, new GuiItem(
                        new ItemStack(Material.AIR),
                        event -> event.setCancelled(true)
                ));
            }
        }
    }
}