package xshyo.us.shopMaster.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.managers.ShopManager;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.CategoryControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.CloseControls;
import xshyo.us.theAPI.utilities.Utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShopMainMenu {

    private final ShopMaster plugin = ShopMaster.getInstance();
    private final Player viewer;
    private final Gui mainMenu;
    private static final String MENU_PATH = "inventories.main";
    private final ShopManager shopManager;

    public ShopMainMenu(Player viewer) {
        this.viewer = viewer;
        this.shopManager = plugin.getShopManager();
        this.mainMenu = initializeGui();
    }

    private Gui initializeGui() {
        String title = Utils.translate(plugin.getLayouts().getString(MENU_PATH + ".title"));
        int configSize = plugin.getLayouts().getInt(MENU_PATH + ".size");
        int rows = (configSize % 9 == 0 && configSize >= 9 && configSize <= 54) ? configSize / 9 : 6;
        return Gui.gui().title(Component.text(Utils.translate(Utils.setPAPI(viewer, PluginUtils.formatTitle(title))))).rows(rows).create();
    }

    public void openMenu() {
        Set<Integer> reservedSlots = new HashSet<>();

        Map<String, Shop> shops = shopManager.getShopMap();

        for (Shop shop : shops.values()) {
            mainMenu.setItem(shop.getCategory().getSlots(),
                    new GuiItem(new CategoryControls(shop).getButtonItem(viewer), event -> {
                new CategoryControls(shop).clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton());
            }));
            reservedSlots.addAll(shop.getCategory().getSlots());
        }

        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.close", plugin.getLayouts(),
                path -> new CloseControls(MENU_PATH + ".buttons.close"),
                mainMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                mainMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> viewer.closeInventory()));
                reservedSlots.add(slot);
            }
        });


        setupCustomItems(reservedSlots);
        fillEmptySlots(reservedSlots);

        mainMenu.update();
        mainMenu.setDefaultClickAction(event -> event.setCancelled(true));
        mainMenu.open(viewer);

    }


    private void setupCustomItems(Set<Integer> reservedSlots) {
        Map<Integer, Controls> customButtons = PluginUtils.loadCustomButtons(MENU_PATH + ".custom-items"
                , plugin.getLayouts(), mainMenu.getRows());

        customButtons.forEach((slot, button) -> {
            if (!reservedSlots.contains(slot)) {
                mainMenu.setItem(slot, new GuiItem(
                        button.getButtonItem(viewer),
                        event -> button.clicked(viewer, event.getSlot(), event.getClick(), event.getHotbarButton())
                ));
                reservedSlots.add(slot);
            }
        });
    }

    private void fillEmptySlots(Set<Integer> reservedSlots) {
        for (int slot = 0; slot < mainMenu.getInventory().getSize(); slot++) {
            if (!reservedSlots.contains(slot)) {
                mainMenu.setItem(slot, new GuiItem(
                        new ItemStack(Material.AIR),
                        event -> event.setCancelled(true)
                ));
            }
        }
    }



}
