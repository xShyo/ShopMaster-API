package xshyo.us.shopMaster.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.services.PurchaseService;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.confirm.ConfirmControls;
import xshyo.us.shopMaster.utilities.menu.controls.confirm.InformationControls;
import xshyo.us.shopMaster.utilities.menu.controls.confirm.StackSelectorControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.BackControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.CloseControls;
import xshyo.us.theAPI.utilities.Utils;

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

    }

    private void updateItemsWithQuantity() {

        ItemStack itemdisplay = item.createItemStack();
        itemdisplay.setAmount(quantity);
        confirmationMenu.updateItem(plugin.getLayouts().getInt(MENU_PATH + ".items.display.slot"),
                new GuiItem(itemdisplay));
        reservedSlots.add(plugin.getLayouts().getInt(MENU_PATH + ".items.display.slot"));


        // Actualizar confirm item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.confirm", plugin.getLayouts(),
                path -> new ConfirmControls(MENU_PATH + ".items.confirm", String.valueOf(quantity),
                        String.valueOf(pricePerUnit), String.valueOf(pricePerUnit * quantity), item.getDisplayName(),
                        item.getDisplayName(), item.getMaterial()),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer),
                        event -> PurchaseService.processPurchase(viewer, item, quantity)));
                reservedSlots.add(slot);
            }
        });

        // Actualizar information item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.information", plugin.getLayouts(),
                path -> new InformationControls(MENU_PATH + ".items.information", String.valueOf(quantity),
                        String.valueOf(pricePerUnit), String.valueOf(pricePerUnit * quantity), item.getDisplayName(),
                        item.getDisplayName(), item.getMaterial()),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer)));
                reservedSlots.add(slot);
            }
        });

        // Actualizar stack_selector item
        PluginUtils.loadSingleButton(MENU_PATH + ".items.stack_selector", plugin.getLayouts(),
                path -> new StackSelectorControls(MENU_PATH + ".items.stack_selector", String.valueOf(quantity),
                        String.valueOf(pricePerUnit), String.valueOf(pricePerUnit * quantity), item.getDisplayName(),
                        item.getDisplayName(), item.getMaterial()),
                confirmationMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                confirmationMenu.updateItem(slot, new GuiItem(controls.getButtonItem(viewer),
                        event -> {
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

        // Actualizar el título del menú
        String title = plugin.getLayouts().getString(MENU_PATH + ".title", "&8Confirmar Compra: &f{item}");
        title = title.replace("{item}", item.getDisplayName());
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