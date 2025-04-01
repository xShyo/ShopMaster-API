package xshyo.us.shopMaster.gui;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
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
    private final ShopMaster plugin = ShopMaster.getInstance();

    private final Player viewer;
    private final Shop shop;
    private final Gui categoryMenu;
    private int currentPage = 1;
    private final int maxPage;
    private final int totalItems; // Total de items en la tienda

    public ShopCategoryMenu(Player viewer, Shop shop) {
        this.viewer = viewer;
        this.shop = shop;
        this.categoryMenu = initializeGui();

        // Calcular el número total de items
        this.totalItems = calculateTotalItems();

        // Calcular el número máximo de páginas
        this.maxPage = calculateMaxPages();
    }

    private int calculateTotalItems() {
        return shop.getItems().size();
    }

    private int calculateMaxPages() {
        return shop.getItems().values().stream()
                .mapToInt(ShopItem::getPage)
                .max()
                .orElse(1); // Por defecto, al menos 1 página
    }

    private Gui initializeGui() {
        int size = 9; // Tamaño predeterminado (1 fila)

        // Verificar si el tamaño del shop es válido
        if (shop.getSize() > 0 && shop.getSize() % 9 == 0) {
            size = shop.getSize();
        } else {
            // Usar un valor de configuración por defecto si está disponible
            int configSize = plugin.getLayouts().getInt("inventories.categories.size", 27); // 27 = 3 filas por defecto

            // Asegurarse de que el tamaño de configuración sea válido (múltiplo de 9)
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
        this.currentPage = page;

        // Limpiar el inventario
        for (int i = 0; i < categoryMenu.getRows() * 9; i++) {
            categoryMenu.removeItem(i);
        }

        Set<Integer> usedSlots = new HashSet<>(); // Para rastrear los slots utilizados

        // Filtrar los items para la página actual
        Map<Integer, ShopItem> pageItems = shop.getItems().entrySet().stream()
                .filter(entry -> entry.getValue().getPage() == currentPage)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Mostrar los items de la página actual
        pageItems.forEach((itemId, shopItem) -> {
            for (int slot : shopItem.getSlots()) {
                if (slot < categoryMenu.getRows() * 9) {
                    if(shopItem.isHidden()){
                        continue;// Verificar que el slot está dentro del menú
                    }
                    categoryMenu.setItem(slot, new GuiItem(new DisplayControls("inventories.categories", shopItem).getButtonItem(viewer), event -> {
                        // Obtener el tipo de click actual
                        String clickType = getClickTypeString(event);

                        // Obtener los tipos de botones configurados
                        String buyButton = plugin.getConf().getString("config.gui.buttons.buy");
                        String sellButton = plugin.getConf().getString("config.gui.buttons.sell");

                        if (clickType.equals(buyButton)) {
                            if (plugin.getConf().getBoolean("config.gui.purchase-confirmation.enabled")) {
                                new PurchaseConfirmationMenu(viewer, shopItem, shop, currentPage).openMenu();
                            } else {
                                plugin.getPurchaseService().processPurchase(viewer, shopItem, 1); // Comprar 1 unidad directamente
                            }
                        } else if (clickType.equals(sellButton)) {
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

        // Agregar los botones de navegación usando los botones del shop
        setupNavigationButtons(usedSlots);

        // Agregar elementos personalizados
        setupCustomItems(usedSlots);

        // Rellenar slots vacíos
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


    private String getClickTypeString(InventoryClickEvent event) {
        if (event.isLeftClick() && !event.isShiftClick()) return "LEFT";
        if (event.isRightClick() && !event.isShiftClick()) return "RIGHT";
        if (event.isLeftClick() && event.isShiftClick()) return "SHIFT_LEFT";
        if (event.isRightClick() && event.isShiftClick()) return "SHIFT_RIGHT";
        if (event.getClick() == ClickType.MIDDLE) return "MIDDLE";
        return "UNKNOWN";
    }

    private void setupNavigationButtons(Set<Integer> usedSlots) {
        // Obtener los botones de la tienda actual
        Map<String, ShopButton> shopButtons = shop.getButtons();

        // Obtener los botones globales por defecto
        Map<String, ShopButton> defaultButtons = getDefaultButtons();

        // Procesar cada tipo de botón
        String[] buttonTypes = {"next", "previous", "close", "back", "indicator"};

        for (String buttonType : buttonTypes) {
            // Usar el botón específico de la categoría si existe, de lo contrario usar el botón por defecto
            ShopButton button = (shopButtons != null && shopButtons.containsKey(buttonType))
                    ? shopButtons.get(buttonType)
                    : defaultButtons.get(buttonType);

            if (button == null) continue; // Si no hay botón ni específico ni por defecto, continuar

            // Verificar si el botón está habilitado
            if (!button.enabled()) continue;

            // Lógica específica para cada tipo de botón
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

        // Obtener la instancia del plugin
        ShopMaster plugin = ShopMaster.getInstance();

        // Intentar obtener la sección de botones predeterminados
        YamlDocument config = plugin.getLayouts();
        if (config == null) {
            plugin.getLogger().warning("No se pudo cargar la configuración para los botones predeterminados");
            return defaultButtons;
        }

        Section buttonsSection = config.getSection("inventories.categories.buttons");
        if (buttonsSection == null) {
            plugin.getLogger().warning("No se encontró la sección de botones predeterminados en la configuración");
            return defaultButtons;
        }

        // Procesar cada botón en la sección
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