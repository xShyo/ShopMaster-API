package xshyo.us.shopMaster.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.BackControls;
import xshyo.us.shopMaster.utilities.menu.controls.stacks.CloseControls;
import xshyo.us.theAPI.utilities.Utils;

import java.util.*;
import java.util.function.Consumer;

public class StackSelectorMenu {

    private final Player viewer;
    private final ShopItem item;
    private final Gui selectorMenu;
    private final Consumer<Integer> onQuantitySelected;
    private final Shop shop;
    private final int returnPage;
    private final ShopMaster plugin;
    private static final String MENU_PATH = "inventories.stack-selector";
    private static final int STACK_SIZE = 64;

    public StackSelectorMenu(Player viewer, ShopItem item, Shop shop, int returnPage, Consumer<Integer> onQuantitySelected) {
        this.viewer = viewer;
        this.item = item;
        this.shop = shop;
        this.returnPage = returnPage;
        this.onQuantitySelected = onQuantitySelected;
        this.plugin = ShopMaster.getInstance();
        this.selectorMenu = initializeGui();
    }

    private Gui initializeGui() {
        int size = plugin.getLayouts().getInt(MENU_PATH + ".size", 27);
        int rows = size / 9;

        return Gui.gui()
                .title(Component.empty())
                .rows(rows)
                .create();
    }

    public void openMenu() {
        // Limpiar el menú
        Set<Integer> reservedSlots = new HashSet<>();

        for (int i = 0; i < selectorMenu.getRows() * 9; i++) {
            selectorMenu.removeItem(i);
        }

        // Actualizar el título del menú
        String title = plugin.getLayouts().getString(MENU_PATH + ".title", "&8Seleccionar Cantidad de Stacks");
        title = PluginUtils.formatTitle(title);
        selectorMenu.updateTitle(Utils.translate(title));



//        setupStackButtons(reservedSlots);


        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.close", plugin.getLayouts(),
                path -> new CloseControls(MENU_PATH + ".buttons.close"),
                selectorMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                selectorMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> viewer.closeInventory()));
                reservedSlots.add(slot);
            }
        });

        PluginUtils.loadSingleButton(MENU_PATH + ".buttons.back", plugin.getLayouts(),
                path -> new BackControls(MENU_PATH + ".buttons.back"),
                selectorMenu.getRows()
        ).forEach((slot, controls) -> {
            if (controls.getButtonItem(viewer).getType() != Material.AIR) {
                selectorMenu.setItem(slot, new GuiItem(controls.getButtonItem(viewer), event -> new PurchaseConfirmationMenu(viewer, item, shop, returnPage).openMenu()));
                reservedSlots.add(slot);
            }
        });


        // Configurar ítems personalizados
        setupCustomItems(reservedSlots);

        // Rellenar espacios vacíos
        fillEmptySlots(reservedSlots);

        selectorMenu.update();
        selectorMenu.setDefaultClickAction(event -> event.setCancelled(true));
        selectorMenu.open(viewer);
    }

//    private void setupStackButtons(Set<Integer> reservedSlots) {
//        // Definir las posiciones de los stacks (1-12)
//        Map<Integer, Integer> stackPositions = new HashMap<>();
//        stackPositions.put(1, 10);
//        stackPositions.put(2, 11);
//        stackPositions.put(3, 12);
//        stackPositions.put(4, 13);
//        stackPositions.put(5, 14);
//        stackPositions.put(6, 15);
//        stackPositions.put(7, 16);
//        stackPositions.put(8, 19);
//        stackPositions.put(9, 20);
//        stackPositions.put(10, 21);
//        stackPositions.put(11, 22);
//        stackPositions.put(12, 23);
//
//        // Para cada stack, crear un ítem en el menú
//        for (Map.Entry<Integer, Integer> entry : stackPositions.entrySet()) {
//            int stackNumber = entry.getKey();
//            int slot = entry.getValue();
//
//            // Buscar en la configuración si existe una configuración específica para este stack
//            String configPath = MENU_PATH + ".items.stack_" + stackNumber;
//            Section stackSection = plugin.getLayouts().getSection(configPath);
//
//            // Si no existe configuración específica, usar valores predeterminados
//            if (stackSection == null) {
//                createDefaultStackButton(stackNumber, slot, reservedSlots);
//            } else {
//                createConfiguredStackButton(stackSection, stackNumber, slot, reservedSlots);
//            }
//        }
//    }

//    private void createDefaultStackButton(int stackNumber, int slot, Set<Integer> reservedSlots) {
//        Material material = Material.GRAY_STAINED_GLASS_PANE;
//        String displayName = "#FBD008" + stackNumber + (stackNumber == 1 ? " Stack" : " Stacks");
//        int totalItems = stackNumber * STACK_SIZE;
//
//        ItemBuilder builder = new ItemBuilder(material)
//                .setAmount(Math.min(stackNumber, 64))
//                .setName(Utils.translate(displayName))
//                .setLore(Utils.translate(Arrays.asList(
//                        "&8Selector de Stacks",
//                        "",
//                        "&8 ● &fSeleccionar #FBD008" + stackNumber + (stackNumber == 1 ? " stack" : " stacks"),
//                        "   &f(" + totalItems + " items)",
//                        "",
//                        "#FBD008► Click para seleccionar."
//                )));
//
//        reservedSlots.add(slot);
//        selectorMenu.setItem(slot, new GuiItem(builder.build(), event -> {
//            event.setCancelled(true);
//            selectStack(stackNumber);
//        }));
//    }

//    private void createConfiguredStackButton(Section stackSection, int stackNumber, int slot, Set<Integer> reservedSlots) {
//        int configSlot = stackSection.getInt("slot", slot);
//        Material material = Material.valueOf(stackSection.getString("material", "GRAY_STAINED_GLASS_PANE"));
//        int amount = stackSection.getInt("amount", Math.min(stackNumber, 64));
//        int modelData = stackSection.getInt("model_data", 0);
//        String displayName = stackSection.getString("display_name", "#FBD008" + stackNumber + (stackNumber == 1 ? " Stack" : " Stacks"));
//        boolean glowing = stackSection.getBoolean("glowing", false);
//
//        // Procesar lore con placeholders
//        List<String> lore = new ArrayList<>();
//        for (String line : stackSection.getStringList("lore")) {
//            int totalItems = stackNumber * STACK_SIZE;
//            line = line.replace("{stacks}", String.valueOf(stackNumber))
//                    .replace("{items}", String.valueOf(totalItems));
//            lore.add(Utils.translate(line));
//        }
//
//        ItemBuilder builder = new ItemBuilder(material)
//                .setAmount(amount)
//                .setName(Utils.translate(displayName))
//                .setLore(lore);
//
//        if (modelData > 0) {
//            builder.setCustomModelData(modelData);
//        }
//
//        if (glowing) {
//            builder.setEnchanted(true);
//        }
//
//        List<String> flags = stackSection.getStringList("item_flags");
//        builder.addFlagsFromConfig(new HashSet<>(flags));
//
//        reservedSlots.add(configSlot);
//        selectorMenu.setItem(configSlot, new GuiItem(builder.build(), event -> {
//            event.setCancelled(true);
//            selectStack(stackNumber);
//        }));
//    }


    private void setupCustomItems(Set<Integer> reservedSlots) {
        Map<Integer, Controls> customButtons = PluginUtils.loadCustomButtons(MENU_PATH + ".custom-items",
                plugin.getLayouts(), selectorMenu.getRows());

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

    private void fillEmptySlots(Set<Integer> reservedSlots) {
        for (int slot = 0; slot < selectorMenu.getInventory().getSize(); slot++) {
            if (!reservedSlots.contains(slot)) {
                selectorMenu.setItem(slot, new GuiItem(
                        new ItemStack(Material.AIR),
                        event -> event.setCancelled(true)
                ));
            }
        }
    }

    private void selectStack(int stackNumber) {
        int totalItems = stackNumber * STACK_SIZE;
        onQuantitySelected.accept(totalItems);

        // No cerrar el inventario aquí, dejar que el callback lo haga
    }
}