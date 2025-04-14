package xshyo.us.shopMaster.utilities;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.shopMaster.utilities.menu.controls.CustomControls;
import xshyo.us.theAPI.utilities.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;


@UtilityClass
public class PluginUtils {

    public String replacePlaceholders(String message, Player player, int amount, double earnings) {
        if (message == null) return "";

        return Utils.translate(message)
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount))
                .replace("{earnings}", String.format("%.2f", earnings));
    }

    public void sendSellAll(Player player, int amount, double earnings) {
        sendSellTitle(player, amount, earnings);
        sendSellActionbar(player, amount, earnings);
    }

    public void sendSellTitle(Player player, int amount, double earnings) {
        String path = "config.command.sell.messages.title";
        if (!ShopMaster.getInstance().getConf().getBoolean(path + ".enabled", false)) return;

        String title = ShopMaster.getInstance().getConf().getString(path + ".title");
        String subtitle = ShopMaster.getInstance().getConf().getString(path + ".subtitle");
        int fadeIn = ShopMaster.getInstance().getConf().getInt(path + ".fade-in", 10);
        int stay = ShopMaster.getInstance().getConf().getInt(path + ".stay", 40);
        int fadeOut = ShopMaster.getInstance().getConf().getInt(path + ".fade-out", 10);

        player.sendTitle(
                replacePlaceholders(title, player, amount, earnings),
                replacePlaceholders(subtitle, player, amount, earnings),
                fadeIn, stay, fadeOut
        );
    }

    public void sendSellActionbar(Player player, int amount, double earnings) {
        String path = "config.command.sell.messages.actionbar";
        if (!ShopMaster.getInstance().getConf().getBoolean(path + ".enabled", false)) return;

        String message = ShopMaster.getInstance().getConf().getString(path + ".message");
        if (message == null || message.isEmpty()) return;

        Utils.sendActionbar(player, replacePlaceholders(message, player, amount, earnings));
    }



    public void sellLog(String player, TypeService typeService, int amount, String item, String money, String ShopName) {
        boolean enabled = ShopMaster.getInstance().getConf().getBoolean("config.log.enabled", true);
        if (enabled) {
            String message = ShopMaster.getInstance().getConf().getString("config.log.message");
            message = message.replace("{player}", player);
            message = message.replace("{action}", typeService.toString());
            message = message.replace("{amount}", "" + amount);
            message = message.replace("{item}", item);
            message = message.replace("{money}", money);
            message = message.replace("{shopName}", ShopName);
            ShopMaster.getInstance().getLogger().log(Level.INFO, message);
        }
    }

    public boolean hasPermissionToCategory(CommandSender sender, String category) {
        String permission = "shopmaster.category." + category.toLowerCase(); // Asegúrate de que `shop.getId()` te dé un ID único
        return sender.hasPermission(permission);
    }

    public boolean hasPermissionToCategory(Player player, String category) {
        String permission = "shopmaster.category." + category.toLowerCase(); // Asegúrate de que `shop.getId()` te dé un ID único
        return player.hasPermission(permission);
    }

    public void executeActions(List<String> actions, Player player, ShopItem shopItem, int amount) {
        for (String action : actions) {
            action = action.trim();
            action = PluginUtils.replacePlaceholders(action, shopItem, amount);
            String[] parts = action.split("\\s+", 2);  // Dividir en dos partes, el tipo de acción y el resto
            String actionType = parts[0].toLowerCase();
            String actionData = (parts.length > 1) ? parts[1] : "";

            if (actionType.startsWith("[chance=")) {  // Verificar si la acción tiene probabilidad definida
                if (Utils.shouldExecuteAction(actionType)) {
                    ShopMaster.getInstance().getActionExecutor().executeAction(player, actionData);
                }
            } else {
                ShopMaster.getInstance().getActionExecutor().executeAction(player, action);
            }
        }

    }

    public String replacePlaceholders(String text, ShopItem shopItem, int amount) {
        // Ajuste el precio por unidad
        double pricePerUnit = (double) shopItem.getBuyPrice() / shopItem.getAmount();
        double totalPrice = pricePerUnit * amount;

        // Obtener el displayName, y si es nulo, usar el nombre del material
        ItemStack itemStack = shopItem.createItemStack();
        String item = itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName()
                ? itemStack.getItemMeta().getDisplayName()
                : itemStack.getType().toString();

        // Asegurarse de que display no sea null
        String displayName = shopItem.getDisplayName();
        String display = displayName != null ? displayName : item;
        String material = itemStack.getType().toString();

        // Asegurarse de que ningún valor de reemplazo sea null
        String amountStr = String.valueOf(amount);
        String priceStr = CurrencyFormatter.formatCurrency(pricePerUnit, shopItem.getEconomy());
        String totalPriceStr = CurrencyFormatter.formatCurrency(totalPrice, shopItem.getEconomy());

        // Garantizar que item no sea null
        if (item == null) item = "Unknown Item";

        // Garantizar que display no sea null
        if (display == null) display = "Unknown Item";

        return text.replace("{amount}", amountStr)
                .replace("{price}", priceStr)
                .replace("{totalPrice}", totalPriceStr)
                .replace("{currency}", shopItem.getEconomy())
                .replace("{item}", item)
                .replace("{displayName}", display)
                .replace("{material}", material);
    }

    public String formatItemName(Material material) {
        String name = material.toString();
        name = name.replace("_", " ").toLowerCase();

        // Capitalizar las palabras
        StringBuilder formattedName = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                formattedName.append(c);
            } else if (capitalizeNext) {
                formattedName.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formattedName.append(c);
            }
        }

        return formattedName.toString();
    }


    public String formatTitle(String title) {
        // Get the truncation settings from config
        boolean enableTruncation = ShopMaster.getInstance().getConf().getBoolean("config.gui.truncate-titles", false);
        int maxTitleLength = ShopMaster.getInstance().getConf().getInt("config.gui.max-title-length", 32);

        // If truncation is disabled or title is already short enough, return as is
        if (!enableTruncation || title.length() <= maxTitleLength) {
            return title;
        }

        // Truncate and add ellipsis
        return title.substring(0, maxTitleLength - 3) + "...";
    }


    public boolean hasPermission(CommandSender sender, String permission) {
        return Utils.hasPermission(sender,
                Utils.translate(ShopMaster.getInstance().getLang().getString("MESSAGES.COMMANDS.NOPERMS")),
                Utils.translate(ShopMaster.getInstance().getLang().getString("MESSAGES.COMMANDS.NOPERMS_LOGGER")),
                permission,
                "shopmaster.*");
    }


    public static void sendRawMessage(Player sender, String message, Object... args) {
        String formattedMessage = ShopMaster.getInstance().getLang().getString(message);
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    String placeholder = "{" + (i + 1) + "}";
                    formattedMessage = formattedMessage.replace(placeholder, args[i].toString());
                }
            }
            String cmdShop = ShopMaster.getInstance().getConf().getString("config.command.shop.name");
            String cmdAdmin = ShopMaster.getInstance().getConf().getString("config.command.admin.name");
            String cmdSell = ShopMaster.getInstance().getConf().getString("config.command.sell.name");

            formattedMessage = formattedMessage.replace("{shop}", cmdShop);
            formattedMessage = formattedMessage.replace("{admin}", cmdAdmin);
            formattedMessage = formattedMessage.replace("{sell}", cmdSell);

            sender.sendRawMessage(Utils.translate(formattedMessage));
        }
    }


    public static void sendMessage(CommandSender sender, String message, Object... args) {
        String formattedMessage = ShopMaster.getInstance().getLang().getString(message);
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    String placeholder = "{" + (i + 1) + "}";
                    formattedMessage = formattedMessage.replace(placeholder, args[i].toString());
                }
            }
            String cmdShop = ShopMaster.getInstance().getConf().getString("config.command.shop.name");
            String cmdAdmin = ShopMaster.getInstance().getConf().getString("config.command.admin.name");
            String cmdSell = ShopMaster.getInstance().getConf().getString("config.command.sell.name");

            formattedMessage = formattedMessage.replace("{shop}", cmdShop);
            formattedMessage = formattedMessage.replace("{admin}", cmdAdmin);
            formattedMessage = formattedMessage.replace("{sell}", cmdSell);

            sender.sendMessage(Utils.translate(formattedMessage));
        }
    }

    public static void sendMessageWhitPath(CommandSender sender, String message, Object... args) {
        if (message != null && !message.isEmpty()) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    String placeholder = "{" + (i + 1) + "}";
                    message = message.replace(placeholder, args[i].toString());
                }
            }
            String cmdShop = ShopMaster.getInstance().getConf().getString("config.command.shop.name");
            String cmdAdmin = ShopMaster.getInstance().getConf().getString("config.command.admin.name");
            String cmdSell = ShopMaster.getInstance().getConf().getString("config.command.sell.name");

            message = message.replace("{shop}", cmdShop);
            message = message.replace("{admin}", cmdAdmin);
            message = message.replace("{sell}", cmdSell);

            sender.sendMessage(Utils.translate(message));
        }
    }


    public <T extends Controls> Map<Integer, T> loadSingleButton(
            String path,
            YamlDocument plugin,
            Function<String, T> controlsFactory,
            int guiRows) {

        Map<Integer, T> buttonLoad = new HashMap<>();
        Section itemConfig = plugin.getSection(path);

        if (itemConfig == null) {
            return buttonLoad;
        }

        try {
            // Check for single slot first
            String singleSlot = itemConfig.getString("slot");
            if (singleSlot != null) {
                processSlotValue(singleSlot, path, buttonLoad, controlsFactory, guiRows);
                return buttonLoad;
            }

            // Check for slots
            Object slotsValue = itemConfig.get("slots");
            if (slotsValue == null) {
                return buttonLoad;
            }

            // Handle different formats
            if (slotsValue instanceof String) {
                processSlotValue(slotsValue.toString(), path, buttonLoad, controlsFactory, guiRows);
            } else if (slotsValue instanceof List) {
                List<?> slotsList = (List<?>) slotsValue;
                for (Object slot : slotsList) {
                    if (slot != null) {
                        String slotStr = slot.toString().trim();
                        processSlotValue(slotStr, path, buttonLoad, controlsFactory, guiRows);
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("Error processing item at path %s: %s%n", path, e.getMessage());
        }

        return buttonLoad;
    }

    private <T extends Controls> void processSlotValue(
            String slotValue,
            String path,
            Map<Integer, T> buttonLoad,
            Function<String, T> controlsFactory,
            int guiRows) {

        slotValue = slotValue.replaceAll("\\s+", ""); // Remove all whitespace

        if (slotValue.contains("-")) {
            // Handle range format (e.g., "10-20" or "10 - 20")
            String[] range = slotValue.split("-");
            try {
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                for (int slot = start; slot <= end; slot++) {
                    if (isValidSlot(slot, guiRows)) {
                        buttonLoad.put(slot, controlsFactory.apply(path));
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Invalid range format: " + slotValue);
            }
        } else {
            // Handle single slot
            try {
                int slot = Integer.parseInt(slotValue);
                if (isValidSlot(slot, guiRows)) {
                    buttonLoad.put(slot, controlsFactory.apply(path));
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid slot number: " + slotValue);
            }
        }
    }


    private boolean isValidSlot(int slot, int guiRows) {
        return slot >= 0 && slot < guiRows * 9;
    }


    public Map<Integer, Controls> loadCustomButtons(String path, YamlDocument yamlDocument, int guiRows) {
        Map<Integer, Controls> buttonsLoad = new HashMap<>();
        Section itemsConfig = yamlDocument.getSection(path);

        if (itemsConfig == null) {
            return buttonsLoad;
        }

        for (Object itemKey : itemsConfig.getKeys()) {
            String itemPath = path + "." + itemKey;
            try {
                processSlots(itemsConfig, itemKey.toString(), itemPath, buttonsLoad, guiRows, yamlDocument);
            } catch (NumberFormatException e) {
                System.out.printf("Error processing slots for item %s: %s%n", itemKey, e.getMessage());
            }
        }

        return buttonsLoad;
    }

    private void processSlots(Section itemsConfig, String itemKey, String itemPath,
                              Map<Integer, Controls> buttonsLoad, int guiRows, YamlDocument yamlDocument) {
        // Check for single slot first
        String singleSlot = itemsConfig.getString(itemKey + ".slot");
        if (singleSlot != null) {
            try {
                addButton(buttonsLoad, Integer.parseInt(singleSlot.trim()), itemPath, guiRows, yamlDocument);
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid single slot format: " + singleSlot);
            }
        }

        // Check for slots list or range
        Object slotsValue = itemsConfig.get(itemKey + ".slots");
        if (slotsValue == null) return;

        // Handle different formats
        if (slotsValue instanceof String) {
            processSlotString((String) slotsValue, buttonsLoad, itemPath, guiRows, yamlDocument);
        } else if (slotsValue instanceof List) {
            List<?> slotsList = (List<?>) slotsValue;
            for (Object slot : slotsList) {
                processSlotEntry(slot, buttonsLoad, itemPath, guiRows, yamlDocument);
            }
        }
    }

    private void processSlotString(String slotValue, Map<Integer, Controls> buttonsLoad, String itemPath, int guiRows, YamlDocument yamlDocument) {
        slotValue = slotValue.replaceAll("\\s+", "");
        if (slotValue.contains("-")) {
            String[] range = slotValue.split("-");
            try {
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                for (int slot = start; slot <= end; slot++) {
                    addButton(buttonsLoad, slot, itemPath, guiRows, yamlDocument);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Invalid range format: " + slotValue);
            }
        } else {
            try {
                addButton(buttonsLoad, Integer.parseInt(slotValue), itemPath, guiRows, yamlDocument);
            } catch (NumberFormatException e) {
                System.out.println("Invalid slot number: " + slotValue);
            }
        }
    }

    private void processSlotEntry(Object slotEntry, Map<Integer, Controls> buttonsLoad, String itemPath, int guiRows, YamlDocument yamlDocument) {
        if (slotEntry instanceof Integer) {
            addButton(buttonsLoad, (Integer) slotEntry, itemPath, guiRows, yamlDocument);
        } else if (slotEntry instanceof String) {
            String slotString = ((String) slotEntry).trim();
            processSlotString(slotString, buttonsLoad, itemPath, guiRows, yamlDocument);
        }
    }

    private void addButton(Map<Integer, Controls> buttonsLoad, int slot, String itemPath, int guiRows, YamlDocument yamlDocument) {
        if (slot < 0 || slot >= guiRows * 9) { // Validar si el slot está dentro de los límites
            System.out.printf("Invalid slot detected: %d (max: %d)%n", slot, guiRows * 9);
            return;
        }
        buttonsLoad.put(slot, new CustomControls(itemPath));
    }


}

