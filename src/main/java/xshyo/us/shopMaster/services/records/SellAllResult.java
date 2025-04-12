package xshyo.us.shopMaster.services.records;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.utilities.CurrencyFormatter;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.utilities.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado detallado de la operación de venta masiva
 */
public record SellAllResult(
        SellStatus status,
        double totalEarnings,
        Map<String, Double> earningsByCurrency,
        Map<Material, Integer> soldItems,
        Map<String, Map<Material, Integer>> itemsByCurrency,
        Map<Material, Double> earningsByMaterial,
        List<ItemStack> skippedItems,
        Map<Material, ShopItem> soldShopItems,
        int totalItemsSold) {


    /**
     * Genera un mensaje de resumen de venta formateado
     */
    public void generateSummaryMessages(Player player) {
        if (status != SellStatus.SUCCESS) {
            switch (status) {
                case WORLD_BLACKLISTED:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.WORLD_BLACKLISTED");
                    break;
                case GAMEMODE_BLACKLISTED:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.GAMEMODE_BLACKLISTED");
                    break;
                case NO_SELLABLE_ITEMS:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.ALL.NO_SELLABLE_ITEMS");
                    break;
                case INVALID_ECONOMY:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.ALL.INVALID_ECONOMY");
                    break;
                case ERROR:
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.ALL.ERROR");
                    break;
            }
            return;
        }

        // Obtener la configuración
        YamlDocument config = ShopMaster.getInstance().getConf();
        boolean summaryEnabled = config.getBoolean("config.command.sell.messages.summary.enabled", true);
        boolean showItemized = config.getBoolean("config.command.sell.messages.summary.show-itemized", true);

        // Formatear lista de monedas usando el nuevo utilitario

        // Mensaje base
        String message = config.getString("config.command.sell.messages.summary.message")
                .replace("{total}", String.valueOf(totalItemsSold))
                .replace("{earnings}", String.format("%.2f", totalEarnings))
                .replace("{currencies}", "" + earningsByCurrency.size());

        if (!summaryEnabled) {
            // Si el resumen está desactivado, solo enviar mensaje básico
            player.sendMessage(Utils.translate(message.replace("{summary}", "")));
            return;
        }

        // Usar MiniMessage para el formato avanzado
        MiniMessage miniMessage = MiniMessage.miniMessage();

        if (showItemized) {
            // Construir el resumen detallado para hover
            StringBuilder hoverText = new StringBuilder();
            List<String> hoverLines = config.getStringList("config.command.sell.messages.summary.hover-lines");
            String formatItems = config.getString("config.command.sell.messages.summary.format-items");

            // Procesar líneas de hover
            for (int i = 0; i < hoverLines.size(); i++) {
                String line = hoverLines.get(i);

                if (!line.contains("{format-items}")) {
                    // Línea normal, añadirla directamente
                    hoverText.append(line);
                    // Añadir salto de línea solo si no es la última línea
                    if (i < hoverLines.size() - 1) {
                        hoverText.append("<newline>");
                    }
                } else {
                    // Línea de formato de items, iterar sobre todos los items vendidos
                    Map<Material, String> itemCurrencyMap = createItemCurrencyMap();
                    boolean isFirstItem = true;

                    for (Map.Entry<Material, Integer> entry : soldItems.entrySet()) {
                        if (!isFirstItem) {
                            hoverText.append("<newline>");
                        }
                        isFirstItem = false;

                        Material material = entry.getKey();
                        int amount = entry.getValue();

                        // Obtener la moneda para este ítem
                        String currency = itemCurrencyMap.getOrDefault(material, "");
                        ShopItem shopItem = soldShopItems.getOrDefault(material, null);

                        // Obtener las ganancias reales por este material
                        double itemEarnings = earningsByMaterial.getOrDefault(material, 0.0);

                        // Calcular el porcentaje que representan estos ítems del total
                        double itemPercentage = (double) amount / totalItemsSold * 100;

                        // Formatear la línea del ítem usando el formato personalizado
                        String itemLine = formatItems
                                .replace("{amount}", String.valueOf(amount))
                                .replace("{item}", PluginUtils.formatItemName(material))
                                .replace("{earnings}", CurrencyFormatter.formatCurrency(itemEarnings, currency))
                                .replace("{currency}", currency)
                                .replace("{percent}", String.format("%.1f", itemPercentage));

                        PluginUtils.sellLog(player.getName(), TypeService.SELL, amount, PluginUtils.formatItemName(material),
                                CurrencyFormatter.formatCurrency(itemEarnings, currency), shopItem.getShopName());

                        hoverText.append(itemLine);
                    }

                    // Añadir salto de línea solo si no es la última línea
                    if (i < hoverLines.size() - 1) {
                        hoverText.append("<newline>");
                    }
                }
            }

            // Crear componente de texto con hover usando MiniMessage
            String summaryText = config.getString("config.command.sell.messages.summary.summary-text");

            String miniMessageText = message.replace("{summary}",
                    "<hover:show_text:\"" + hoverText.toString() + "\">" + summaryText + "</hover>");

            // Enviar mensaje con componentes de MiniMessage
            Component component = miniMessage.deserialize(miniMessageText);
            ShopMaster.getInstance().getAdventure().player(player).sendMessage(component);
        } else {
            // Si no se muestra detallado, enviar solo el mensaje básico
            Component component = miniMessage.deserialize(message.replace("{summary}", ""));
            ShopMaster.getInstance().getAdventure().player(player).sendMessage(component);
        }
    }

    /**
     * Crea un mapa que relaciona cada material con su tipo de economía
     */
    private Map<Material, String> createItemCurrencyMap() {
        Map<Material, String> result = new HashMap<>();

        // Para cada moneda, recorrer los ítems vendidos con esa moneda
        for (Map.Entry<String, Map<Material, Integer>> entry : itemsByCurrency.entrySet()) {
            String currency = entry.getKey();
            Map<Material, Integer> items = entry.getValue();

            for (Material material : items.keySet()) {
                result.put(material, currency);
            }
        }

        return result;
    }


}