package xshyo.us.shopMaster.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.superclass.CurrencyManager;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.utilities.Utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SellManager {

    private final ShopMaster plugin;
    private final ShopManager shopManager;

    // Estructura de datos optimizada: Material -> Lista de información vendible
    private final Map<Material, List<SellableItemInfo>> sellableItems;
    // Items personalizados con ItemStacks pre-creados
    private final List<CustomItemEntry> customSellableItems;
    // Caché de búsquedas recientes (LRU cache limitado)
    private final Map<CacheKey, SellableItemInfo> resultCache;
    private static final int MAX_CACHE_SIZE = 100;

    public SellManager(ShopMaster plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.sellableItems = new HashMap<>();
        this.customSellableItems = new ArrayList<>();
        this.resultCache = new LinkedHashMap<CacheKey, SellableItemInfo>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, SellableItemInfo> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        buildSellableItemsIndex();
    }

    /**
     * Clave para el caché de resultados
     */
    private static class CacheKey {
        private final Material material;
        private final String displayName;
        private final int hashCode;

        public CacheKey(ItemStack item) {
            this.material = item.getType();
            this.displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName() : "";
            this.hashCode = calculateHashCode();
        }

        private int calculateHashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((material == null) ? 0 : material.hashCode());
            result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CacheKey other = (CacheKey) obj;
            return Objects.equals(material, other.material) &&
                    Objects.equals(displayName, other.displayName);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    /**
     * Clase auxiliar para almacenar items personalizados con el ItemStack pre-creado
     */
    private static class CustomItemEntry {
        private final Shop shop;
        private final ShopItem shopItem;
        private final ItemStack itemStack;

        public CustomItemEntry(Shop shop, ShopItem shopItem, ItemStack itemStack) {
            this.shop = shop;
            this.shopItem = shopItem;
            this.itemStack = itemStack;
        }
    }

    /**
     * Construye un índice de todos los ítems vendibles en paralelo para mejor rendimiento
     */
    public void buildSellableItemsIndex() {
        sellableItems.clear();
        customSellableItems.clear();
        resultCache.clear();

        // Usar CompletableFuture para paralelizar la indexación
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Shop shop : shopManager.getShopMap().values()) {
            if (!shop.isEnabled()) {
                continue;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (ShopItem item : shop.getItems().values()) {
                    if (item.getSellPrice() <= 0) {
                        continue;
                    }

                    try {
                        // Intentar convertir a Material estándar
                        Material material = Material.valueOf(item.getMaterial().toUpperCase());

                        // Pre-crear el ItemStack para comparación futura
                        ItemStack shopItemStack = item.createItemStack();
                        SellableItemInfo info = new SellableItemInfo(shop, item, shopItemStack);

                        // Sincronizar acceso a la estructura de datos compartida
                        synchronized (sellableItems) {
                            if (!sellableItems.containsKey(material)) {
                                sellableItems.put(material, new ArrayList<>());
                            }
                            sellableItems.get(material).add(info);
                        }
                    } catch (IllegalArgumentException e) {
                        // Si no es un Material estándar, crear el ItemStack y guardarlo
                        try {
                            ItemStack customItem = item.createItemStack();
                            synchronized (customSellableItems) {
                                customSellableItems.add(new CustomItemEntry(shop, item, customItem));
                            }
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Error al crear ítem personalizado: " + item.getMaterial() + " - " + ex.getMessage());
                        }
                    }
                }
            });

            futures.add(future);
        }

        // Esperar a que todas las tareas terminen
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Ordenar las listas por precio de venta (de mayor a menor)
        for (List<SellableItemInfo> items : sellableItems.values()) {
            items.sort((a, b) -> Double.compare(b.shopItem().getSellPrice(), a.shopItem().getSellPrice()));
        }

        plugin.getLogger().info("Índice de ítems vendibles construido. Materiales estándar: " + sellableItems.size()
                + ", Items personalizados: " + customSellableItems.size());
    }

    /**
     * Verifica si un ítem puede ser vendido
     */
    public boolean isSellable(Player player, ItemStack item) {
        return getSellableShopItem(player, item) != null;
    }

    /**
     * Encuentra el ShopItem correspondiente para vender un ítem con caché
     */
    public SellableItemInfo getSellableShopItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        // Verificar mundo en lista negra
        String worldName = player.getWorld().getName();
        if (isWorldBlacklisted(worldName)) {
            return null;
        }

        // Verificar modo de juego en lista negra
        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return null;
        }

        // Verificar cache
        CacheKey key = new CacheKey(item);
        if (resultCache.containsKey(key)) {
            SellableItemInfo cachedInfo = resultCache.get(key);
            // Verificar permisos si es necesario
            // String permission = "shopmaster.shop." + cachedInfo.shop().getName();
            // if (!player.hasPermission(permission)) {
            //     return null;
            // }
            return cachedInfo;
        }

        // Primero buscar en materiales estándar
        SellableItemInfo standardItemInfo = findStandardItem(player, item);
        if (standardItemInfo != null) {
            resultCache.put(key, standardItemInfo);
            return standardItemInfo;
        }

        // Si no se encuentra, buscar en items personalizados
        SellableItemInfo customItemInfo = findCustomItem(player, item);
        if (customItemInfo != null) {
            resultCache.put(key, customItemInfo);
        }
        return customItemInfo;
    }

    /**
     * Busca en materiales estándar
     */
    private SellableItemInfo findStandardItem(Player player, ItemStack item) {
        Material material = item.getType();

        if (!sellableItems.containsKey(material)) {
            return null;
        }

        // La lista ya está ordenada por precio, así que el primero que coincida será el mejor
        for (SellableItemInfo info : sellableItems.get(material)) {
            // Verificar permisos si es necesario
            // String permission = "shopmaster.shop." + info.shop().getName();
            // if (!player.hasPermission(permission)) {
            //     continue;
            // }

            // Usar el ItemStack pre-creado en vez de crearlo cada vez
            if (item.isSimilar(info.itemStack())) {
                return info;
            }
        }

        return null;
    }

    /**
     * Busca en items personalizados usando un algoritmo de partición
     */
    private SellableItemInfo findCustomItem(Player player, ItemStack item) {
        if (customSellableItems.isEmpty()) {
            return null;
        }

        // Usar multi-threading para búsquedas en colecciones grandes
        if (customSellableItems.size() > 100) {
            return findCustomItemParallel(player, item);
        }

        // Para colecciones pequeñas, búsqueda secuencial
        for (CustomItemEntry entry : customSellableItems) {
            // Verificar permisos si es necesario
            // String permission = "shopmaster.shop." + entry.shop.getName();
            // if (!player.hasPermission(permission)) {
            //     continue;
            // }

            if (item.isSimilar(entry.itemStack)) {
                return new SellableItemInfo(entry.shop, entry.shopItem, entry.itemStack);
            }
        }

        return null;
    }

    /**
     * Versión paralela de búsqueda de items personalizados
     */
    private SellableItemInfo findCustomItemParallel(Player player, ItemStack item) {
        // Dividir la lista en segmentos para procesamiento paralelo
        int processors = Runtime.getRuntime().availableProcessors();
        int segmentSize = Math.max(10, customSellableItems.size() / processors);

        List<CompletableFuture<SellableItemInfo>> futures = new ArrayList<>();

        for (int i = 0; i < customSellableItems.size(); i += segmentSize) {
            final int start = i;
            final int end = Math.min(i + segmentSize, customSellableItems.size());

            CompletableFuture<SellableItemInfo> future = CompletableFuture.supplyAsync(() -> {
                for (int j = start; j < end; j++) {
                    CustomItemEntry entry = customSellableItems.get(j);

                    // Verificar permisos si es necesario
                    // String permission = "shopmaster.shop." + entry.shop.getName();
                    // if (!player.hasPermission(permission)) {
                    //     continue;
                    // }

                    if (item.isSimilar(entry.itemStack)) {
                        return new SellableItemInfo(entry.shop, entry.shopItem, entry.itemStack);
                    }
                }
                return null;
            });

            futures.add(future);
        }

        // Encontrar el primer resultado no nulo
        for (CompletableFuture<SellableItemInfo> future : futures) {
            try {
                SellableItemInfo result = future.get();
                if (result != null) {
                    // Cancelar las demás búsquedas
                    for (CompletableFuture<SellableItemInfo> otherFuture : futures) {
                        if (otherFuture != future && !otherFuture.isDone()) {
                            otherFuture.cancel(true);
                        }
                    }
                    return result;
                }
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().warning("Error en búsqueda paralela: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Vende un ítem y retorna el resultado
     */
    public SellResult sellItem(Player player, ItemStack item, int amount) {
        // Obtener la información del ítem vendible (incluye verificaciones de mundo y gamemode)

        String worldName = player.getWorld().getName();
        if (isWorldBlacklisted(worldName)) {
            return new SellResult(SellStatus.WORLD_BLACKLISTED, 0);
        }

        // Check gamemode blacklist
        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return new SellResult(SellStatus.GAMEMODE_BLACKLISTED, 0);
        }

        SellableItemInfo info = getSellableShopItem(player, item);

        if (info == null) {
            return new SellResult(SellStatus.NOT_SELLABLE, 0);
        }

        double unitPrice = info.shopItem().getSellPrice();
        double totalPrice = unitPrice * amount;

        // Verificar que la moneda sea válida
        String economyType = info.shopItem().getEconomy();
        CurrencyManager currencyManager = ShopMaster.getInstance().getCurrencyMap()
                .get(CurrencyType.getType(economyType, CurrencyType.VAULT));

        if (currencyManager == null) {
            return new SellResult(SellStatus.INVALID_ECONOMY, 0);
        }

        // Dar dinero al jugador usando la moneda específica del ítem
        boolean success = currencyManager.add(player, totalPrice);
        if (!success) {
            return new SellResult(SellStatus.ERROR, 0);
        }

        return new SellResult(SellStatus.SUCCESS, totalPrice);
    }


    /**
     * Clase para información de ítem vendible
     */
    public record SellableItemInfo(Shop shop, ShopItem shopItem, ItemStack itemStack) {
        // Record para almacenar la información de un ítem vendible junto con su ItemStack pre-creado
    }


    /**
     * Vende todos los ítems vendibles del inventario del jugador
     *
     * @param player Jugador que vende los ítems
     * @return Resultado de la venta con toda la información relevante
     */
    public SellAllResult sellAllItems(
            Player player) {

        // Check world blacklist
        String worldName = player.getWorld().getName();
        if (isWorldBlacklisted(worldName)) {
            return new SellAllResult(SellStatus.WORLD_BLACKLISTED, 0, Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyList(), 0);
        }

        // Check gamemode blacklist
        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return new SellAllResult(SellStatus.GAMEMODE_BLACKLISTED, 0, Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyList(), 0);
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        // Cache para evitar búsquedas repetidas del mismo material
        Map<Material, SellableItemInfo> materialInfoCache = new HashMap<>();

        // Mapas para seguimiento de ventas
        Map<String, Map<Material, Integer>> itemsByCurrency = new HashMap<>();
        Map<Material, Integer> soldItems = new HashMap<>();
        Map<String, Double> earningsByCurrency = new HashMap<>();
        double totalEarnings = 0.0;
        int totalItemsSold = 0;
        List<ItemStack> skippedItems = new ArrayList<>();

        // Una sola pasada por el inventario
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Material material = item.getType();

            // Usar cache para evitar búsquedas repetidas
            SellableItemInfo info = getSellableShopItem(player, item);


            if (info != null) {
                String currency = info.shopItem().getEconomy();
                CurrencyManager currencyManager = ShopMaster.getInstance().getCurrencyMap()
                        .get(CurrencyType.getType(currency, CurrencyType.VAULT));

                if (currencyManager == null) {
                    // Omitir ítem por economía inválida
                    skippedItems.add(item);
                    continue;
                }

                // Acumular ítems por moneda y material
                if (!itemsByCurrency.containsKey(currency)) {
                    itemsByCurrency.put(currency, new HashMap<>());
                    earningsByCurrency.put(currency, 0.0);
                }

                int amount = item.getAmount();
                itemsByCurrency.get(currency).put(material,
                        itemsByCurrency.get(currency).getOrDefault(material, 0) + amount);

                // Actualizar conteo total
                soldItems.put(material, soldItems.getOrDefault(material, 0) + amount);
                totalItemsSold += amount;

                // Calcular ganancias
                double unitPrice = info.shopItem().getSellPrice();
                double itemTotal = unitPrice * amount;
                totalEarnings += itemTotal;
                earningsByCurrency.put(currency, earningsByCurrency.get(currency) + itemTotal);

                // Eliminar el ítem del inventario
                inventory.setItem(i, null);
                materialInfoCache.put(material, info);

            }
        }

        if (totalItemsSold == 0) {
            SellStatus status = skippedItems.isEmpty() ?
                    SellStatus.NO_SELLABLE_ITEMS : SellStatus.INVALID_ECONOMY;
            return new SellAllResult(status, 0, Collections.emptyMap(),
                    Collections.emptyMap(), skippedItems, skippedItems.size());
        }

        // Dar dinero al jugador agrupado por moneda
        for (Map.Entry<String, Map<Material, Integer>> currencyEntry : itemsByCurrency.entrySet()) {
            String currency = currencyEntry.getKey();
            double amount = earningsByCurrency.get(currency);

            if (amount > 0) {
                // Usar cualquier ítem de esta moneda para el pago
                Material firstMaterial = currencyEntry.getValue().keySet().iterator().next();

                CurrencyManager currencyManager = ShopMaster.getInstance().getCurrencyMap()
                        .get(CurrencyType.getType(materialInfoCache.get(firstMaterial).shopItem.getEconomy(), CurrencyType.VAULT));

                if (currencyManager == null) {
                    return new SellAllResult(SellStatus.INVALID_ECONOMY, 0, Collections.emptyMap(), Collections.emptyMap(),
                            skippedItems, skippedItems.size());
                }

                boolean success = currencyManager.add(player, totalItemsSold);
                if (!success) {
                    return new SellAllResult(SellStatus.ERROR, 0, Collections.emptyMap(), Collections.emptyMap(),
                            skippedItems, skippedItems.size());
                }

            }
        }

        return new SellAllResult(
                SellStatus.SUCCESS,
                totalEarnings,
                earningsByCurrency,
                soldItems,
                skippedItems,
                totalItemsSold
        );
    }

    /**
     * Recarga el índice de ítems vendibles
     */
    public void reload() {
        buildSellableItemsIndex();
    }

    /**
     * Resultado detallado de la operación de venta masiva
     */
    public record SellAllResult(
            SellStatus status,
            double totalEarnings,
            Map<String, Double> earningsByCurrency,
            Map<Material, Integer> soldItems,
            List<ItemStack> skippedItems,
            int totalItemsSold
    ) {
        /**
         * Genera un mensaje de resumen de venta formateado
         */
        public void generateSummaryMessages(Player player) {

            if (status != SellStatus.SUCCESS) {
                switch (status) {
                    case WORLD_BLACKLISTED:
                        PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.ALL.WORLD_BLACKLISTED");
                        break;
                    case GAMEMODE_BLACKLISTED:
                        PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.ALL.GAMEMODE_BLACKLISTED");
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
            if (ShopMaster.getInstance().getConf().getBoolean("config.command.sell.enable-summary")) {
                List<String> messages = new ArrayList<>();

                // Recuperar configuraciones de mensajes
                String headerMessage = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.HEADER",
                        "&a---------- Resumen de Venta ----------"
                );
                String totalItemsMessage = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.TOTAL_ITEMS",
                        "&aHas vendido &e{total_items} &aítems"
                );
                String totalEarningsMessage = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.TOTAL_EARNINGS",
                        "&aPor un total de &e${total_earnings}"
                );
                String currencyBreakdownHeader = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.CURRENCY_BREAKDOWN_HEADER",
                        "&aDesglose por moneda:"
                );
                String currencyBreakdownFormat = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.CURRENCY_BREAKDOWN_FORMAT",
                        "&7- &e{currency}: ${amount}"
                );
                String itemBreakdownFormat = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.ITEM_BREAKDOWN_FORMAT",
                        "&7- &e{amount}x {item_name}"
                );
                String skippedItemsMessage = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.SKIPPED_ITEMS",
                        "&e{skipped_count} &6ítems no se vendieron porque no tienen una economía válida configurada."
                );
                String footerMessage = ShopMaster.getInstance().getLang().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SUMMARY_MESSAGES.FOOTER",
                        "&a---------------------------------------"
                );

                // Configuración de elementos del summary
                boolean showTotalItems = ShopMaster.getInstance().getConf().getBoolean("config.command.sell.summary.show-total-items", true);
                boolean showTotalEarnings = ShopMaster.getInstance().getConf().getBoolean("config.command.sell.summary.show-total-earnings", true);
                boolean showCurrencyBreakdown = ShopMaster.getInstance().getConf().getBoolean("config.command.sell.summary.show-currency-breakdown", true);
                boolean showItemBreakdown = ShopMaster.getInstance().getConf().getBoolean("config.command.sell.summary.show-item-breakdown", true);
                int maxItemsToShow = ShopMaster.getInstance().getConf().getInt("config.command.sell.summary.max-items-to-show", 5);
                boolean showSkippedItems = ShopMaster.getInstance().getConf().getBoolean("config.command.sell.summary.show-skipped-items", true);

                // Añadir mensajes al resumen
                messages.add(headerMessage);

                // Mostrar total de ítems vendidos
                if (showTotalItems) {
                    messages.add(totalItemsMessage
                            .replace("{total_items}", String.valueOf(totalItemsSold))
                    );
                }

                // Mostrar ganancias totales
                if (showTotalEarnings) {
                    messages.add(totalEarningsMessage
                            .replace("{total_earnings}", String.format("%.2f", totalEarnings))
                    );
                }

                // Mostrar desglose por moneda si hay más de una y está habilitado
                if (showCurrencyBreakdown && earningsByCurrency.size() > 1) {
                    messages.add(currencyBreakdownHeader);
                    for (Map.Entry<String, Double> entry : earningsByCurrency.entrySet()) {
                        messages.add(currencyBreakdownFormat
                                .replace("{currency}", entry.getKey())
                                .replace("{amount}", String.format("%.2f", entry.getValue()))
                        );
                    }
                }

                // Mostrar desglose de ítems vendidos
                if (showItemBreakdown && soldItems.size() <= maxItemsToShow) {
                    for (Map.Entry<Material, Integer> entry : soldItems.entrySet()) {
                        messages.add(itemBreakdownFormat
                                .replace("{amount}", String.valueOf(entry.getValue()))
                                .replace("{item_name}", PluginUtils.formatItemName(entry.getKey()))
                        );
                    }
                }

                // Informar sobre ítems omitidos
                if (showSkippedItems && !skippedItems.isEmpty()) {
                    int skippedCount = skippedItems.size();
                    messages.add(skippedItemsMessage
                            .replace("{skipped_count}", String.valueOf(skippedCount))
                    );
                }

                messages.add(footerMessage);

                // Enviar los mensajes del resumen
                for (String message : messages) {
                    player.sendMessage(Utils.translate(message));
                }
            } else {
                // Si el summary está desactivado, solo enviar mensaje de venta básico
                String defaultSimpleMessage = ShopMaster.getInstance().getConf().getString(
                        "MESSAGES.COMMANDS.SHOP.SELL.ALL.SIMPLE_MESSAGE",
                        "&aHas vendido &e{total_items} &aítems por un total de &e${total_earnings}"
                );

                player.sendMessage(Utils.translate(
                        defaultSimpleMessage
                                .replace("{total_items}", String.valueOf(totalItemsSold))
                                .replace("{total_earnings}", String.format("%.2f", totalEarnings))
                ));
            }

        }
    }

    private boolean isWorldBlacklisted(String worldName) {
        YamlDocument config = ShopMaster.getInstance().getConf();
        List<String> blacklistedWorlds = config.getStringList("config.command.sell.black-list.world");
        return blacklistedWorlds.contains(worldName);
    }

    private boolean isGameModeBlacklisted(String gameMode) {
        YamlDocument config = ShopMaster.getInstance().getConf();
        List<String> blacklistedGameModes = config.getStringList("config.command.sell.black-list.gameModes");
        return blacklistedGameModes.contains(gameMode);
    }

    public record SellResult(SellStatus status, double price) {
    }
}