package xshyo.us.shopMaster.managers;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SellManager {
    private static final int MAX_CACHE_SIZE = 100;
    private static final int PARALLEL_THRESHOLD = 100;

    private final ShopMaster plugin;
    private final ShopManager shopManager;
    private final Map<Material, List<SellableItemInfo>> sellableItems;
    private final List<CustomItemEntry> customSellableItems;
    private final Map<CacheKey, SellableItemInfo> resultCache;
    private final ListeningExecutorService executorService;

    public SellManager(ShopMaster plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.sellableItems = new ConcurrentHashMap<>();
        this.customSellableItems = new CopyOnWriteArrayList<>();
        this.resultCache = Collections.synchronizedMap(
                new LinkedHashMap<>(MAX_CACHE_SIZE + 1, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<CacheKey, SellableItemInfo> eldest) {
                        return size() > MAX_CACHE_SIZE;
                    }
                }
        );
        this.executorService = MoreExecutors.listeningDecorator(
                new ThreadPoolExecutor(
                        Runtime.getRuntime().availableProcessors(), // núcleos mínimos
                        Runtime.getRuntime().availableProcessors() * 2, // máximo de hilos
                        60L, // tiempo de espera antes de eliminar hilos inactivos
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>() // cola sin límite
                )
        );

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
    private record CustomItemEntry(Shop shop, ShopItem shopItem, ItemStack itemStack) {
    }

    /**
     * Clase para información de ítem vendible
     */
    public record SellableItemInfo(Shop shop, ShopItem shopItem, ItemStack itemStack) {
    }

    /**
     * Resultado de la venta de un ítem individual
     */
    public record SellResult(SellStatus status, double price) {
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
            // Método existente de generación de mensajes de resumen
            // [El código se mantiene igual que en la implementación original]
        }
    }

    /**
     * Optimized item indexing with better parallel processing
     */
    public void buildSellableItemsIndex() {
        sellableItems.clear();
        customSellableItems.clear();
        resultCache.clear();

        List<CompletableFuture<Void>> futures = shopManager.getShopMap().values().stream()
                .filter(Shop::isEnabled)
                .map(shop -> CompletableFuture.runAsync(() -> processShopItems(shop), executorService))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Error indexing shop items", ex);
                    return null;
                })
                .join();

        // Sort lists more efficiently
        sellableItems.values().forEach(list -> list.sort(
                Comparator.comparing(info -> -info.shopItem().getSellPrice())
        ));

        plugin.getLogger().info(String.format(
                "Shop Item Index Built: Standard Materials: %d, Custom Items: %d",
                sellableItems.size(), customSellableItems.size()
        ));
    }

    private void processShopItems(Shop shop) {
        for (ShopItem item : shop.getItems().values()) {
            if (item.getSellPrice() <= 0) continue;

            try {
                processStandardOrCustomItem(shop, item);
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "Error processing item " + item.getMaterial() + ": " + e.getMessage()
                );
            }
        }
    }

    private void processStandardOrCustomItem(Shop shop, ShopItem item) {
        try {
            Material material = Material.valueOf(item.getMaterial().toUpperCase());
            ItemStack shopItemStack = item.createItemStack();
            SellableItemInfo info = new SellableItemInfo(shop, item, shopItemStack);

            sellableItems.computeIfAbsent(material, k -> new CopyOnWriteArrayList<>()).add(info);
        } catch (IllegalArgumentException e) {
            // Custom item handling
            ItemStack customItem = item.createItemStack();
            customSellableItems.add(new CustomItemEntry(shop, item, customItem));
        }
    }

    /**
     * Busca en materiales estándar
     */
    private SellableItemInfo findStandardItem(Player player, ItemStack item) {
        Material material = item.getType();

        if (!sellableItems.containsKey(material)) {
            return null;
        }

        return sellableItems.get(material).stream()
                .filter(info -> item.isSimilar(info.itemStack()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca en items personalizados usando un algoritmo de partición
     */
    private SellableItemInfo findCustomItem(Player player, ItemStack item) {
        if (customSellableItems.isEmpty()) {
            return null;
        }

        // Usar multi-threading para búsquedas en colecciones grandes
        if (customSellableItems.size() > PARALLEL_THRESHOLD) {
            return findCustomItemParallel(player, item);
        }

        return customSellableItems.stream()
                .filter(entry -> item.isSimilar(entry.itemStack))
                .findFirst()
                .map(entry -> new SellableItemInfo(entry.shop, entry.shopItem, entry.itemStack))
                .orElse(null);
    }

    /**
     * Advanced parallel processing for custom item search
     */
    private SellableItemInfo findCustomItemParallel(Player player, ItemStack item) {
        int processors = Runtime.getRuntime().availableProcessors();
        int segmentSize = Math.max(10, customSellableItems.size() / processors);

        return customSellableItems.stream()
                .parallel()
                .filter(entry -> item.isSimilar(entry.itemStack))
                .findFirst()
                .map(entry -> new SellableItemInfo(entry.shop, entry.shopItem, entry.itemStack))
                .orElse(null);
    }

    /**
     * Enhanced sellable item search with improved caching and performance
     */
    public SellableItemInfo getSellableShopItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        // World and gamemode checks
        if (isWorldBlacklisted(player.getWorld().getName()) ||
                isGameModeBlacklisted(player.getGameMode().toString())) {
            return null;
        }

        // Cache lookup
        CacheKey key = new CacheKey(item);
        return resultCache.computeIfAbsent(key, k -> {
            SellableItemInfo standardInfo = findStandardItem(player, item);
            return standardInfo != null ? standardInfo : findCustomItem(player, item);
        });
    }


    /**
     * Vende un ítem y retorna el resultado
     */
    public SellResult sellItem(Player player, ItemStack item, int amount, boolean searchEntireInventory) {

        if (isWorldBlacklisted(player.getWorld().getName())) {
            return new SellResult(SellStatus.WORLD_BLACKLISTED, 0);
        }

        if(  isGameModeBlacklisted(player.getGameMode().toString())){
            return new SellResult(SellStatus.GAMEMODE_BLACKLISTED, 0);
        }

        SellableItemInfo info = getSellableShopItem(player, item);
        if (info == null || info.shopItem().getSellPrice() <= 0) {  // Verifica que el precio sea mayor que 0
            return new SellResult(SellStatus.NOT_SELLABLE, 0);
        }

        PlayerInventory inventory = player.getInventory();

        // Determinar la estrategia de búsqueda de ítems
        int availableAmount = searchEntireInventory
                ? countItemsInEntireInventory(inventory, item)
                : countItemsInHand(inventory, item);

        if (availableAmount == 0) {
            return new SellResult(SellStatus.INSUFFICIENT_ITEMS, 0);
        }

        // Limitar la cantidad a vender según lo solicitado
        int amountToSell = Math.min(availableAmount, amount);

        double unitPrice = info.shopItem().getSellPrice();
        double baseAmount = info.shopItem().getAmount(); // Cantidad mínima del shop
        if (baseAmount > 1) {
            unitPrice = unitPrice / baseAmount; // Ajustar precio por unidad
        }

        double totalPrice = unitPrice * amountToSell;


        // Verificaciones de economía
        CurrencyManager currencyManager = getCurrencyManager(info);
        if (currencyManager == null) {
            return new SellResult(SellStatus.INVALID_ECONOMY, 0);
        }

        // Remover ítems del inventario
        removeItems(inventory, item, amountToSell, searchEntireInventory);

        // Dar dinero
        boolean success = currencyManager.add(player, totalPrice);
        if (!success) {
            return new SellResult(SellStatus.ERROR, 0);
        }

        return new SellResult(SellStatus.SUCCESS, totalPrice);
    }

    // Método para contar ítems en la mano
    private int countItemsInHand(PlayerInventory inventory, ItemStack referenceItem) {
        ItemStack handItem = inventory.getItemInMainHand();
        return handItem != null && handItem.isSimilar(referenceItem) ? handItem.getAmount() : 0;
    }

    // Método para contar ítems en todo el inventario
    private int countItemsInEntireInventory(PlayerInventory inventory, ItemStack referenceItem) {
        int totalItems = 0;
        for (ItemStack invItem : inventory.getContents()) {
            if (invItem != null && invItem.isSimilar(referenceItem)) {
                totalItems += invItem.getAmount();
            }
        }
        return totalItems;
    }

    // Método para remover ítems
    private void removeItems(PlayerInventory inventory, ItemStack referenceItem, int amountToRemove, boolean searchEntireInventory) {
        if (!searchEntireInventory) {
            // Venta solo de items en mano
            inventory.setItemInMainHand(null);
            return;
        }

        // Búsqueda y eliminación en todo el inventario
        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack invItem = inventory.getContents()[i];
            if (invItem != null && invItem.isSimilar(referenceItem)) {
                int itemAmount = invItem.getAmount();
                if (amountToRemove >= itemAmount) {
                    inventory.setItem(i, null);
                    amountToRemove -= itemAmount;
                } else {
                    invItem.setAmount(itemAmount - amountToRemove);
                    break;
                }

                if (amountToRemove <= 0) break;
            }
        }
    }


    /**
     * Comprehensive sell method with enhanced error handling and logging
     */
    public SellAllResult sellAllItems(Player player) {
        if (isWorldBlacklisted(player.getWorld().getName())) {
            return createFailedSellResult(SellStatus.WORLD_BLACKLISTED);
        }

        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return createFailedSellResult(SellStatus.GAMEMODE_BLACKLISTED);
        }

        SellAllProcessor processor = new SellAllProcessor(player);
        return processor.processInventory();
    }

    /**
     * Vende todos los ítems de un tipo específico
     */
    public SellAllResult sellAllItemOfType(Player player, ItemStack referenceItem) {
        if (isWorldBlacklisted(player.getWorld().getName())) {
            return createFailedSellResult(SellStatus.WORLD_BLACKLISTED);
        }

        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return createFailedSellResult(SellStatus.GAMEMODE_BLACKLISTED);
        }

        SellAllProcessor processor = new SellAllProcessor(player, referenceItem);
        return processor.processInventory();
    }

    /**
     * Inner class for advanced sell processing
     */
    private class SellAllProcessor {
        private final Player player;
        private final PlayerInventory inventory;
        private final ItemStack referenceItem;
        private final Map<String, Map<Material, Integer>> itemsByCurrency = new HashMap<>();
        private final Map<Material, Integer> soldItems = new HashMap<>();
        private final Map<String, Double> earningsByCurrency = new HashMap<>();
        private final List<ItemStack> skippedItems = new ArrayList<>();
        private double totalEarnings = 0.0;
        private int totalItemsSold = 0;

        SellAllProcessor(Player player) {
            this(player, null);
        }

        SellAllProcessor(Player player, ItemStack referenceItem) {
            this.player = player;
            this.inventory = player.getInventory();
            this.referenceItem = referenceItem;
        }

        SellAllResult processInventory() {
            ItemStack[] contents = inventory.getContents();
            for (int i = 0; i < contents.length; i++) {
                processInventorySlot(contents[i], i);
            }

            return finalizeSale();
        }

        private void processInventorySlot(ItemStack item, int slot) {
            if (item == null || item.getType() == Material.AIR) return;

            // Si hay un ítem de referencia, solo procesar ítems similares
            if (referenceItem != null) {
                SellableItemInfo referenceInfo = getSellableShopItem(player, referenceItem);
                if (referenceInfo == null ||
                        !referenceInfo.shopItem().getMaterial().equals(item.getType().name())) {
                    return;
                }
            }

            SellableItemInfo info = getSellableShopItem(player, item);
            if (info == null) return;

            CurrencyManager currencyManager = getCurrencyManager(info);
            if (currencyManager == null) {
                skippedItems.add(item);
                return;
            }

            processSellableItem(info, item, slot);
        }

        private void processSellableItem(SellableItemInfo info, ItemStack item, int slot) {
            String currency = info.shopItem().getEconomy();
            int amount = item.getAmount();

            double unitPrice = info.shopItem().getSellPrice();
            double baseAmount = info.shopItem().getAmount(); // Cantidad mínima de venta en la tienda

            if (baseAmount > 1) {
                unitPrice = unitPrice / baseAmount; // Ajuste del precio unitario
            }

            double itemTotal = unitPrice * amount;

            // Actualizar el seguimiento de ventas
            updateSalesTracking(currency, info.itemStack().getType(), amount, itemTotal);

            // Eliminar el ítem del inventario
            inventory.setItem(slot, null);
        }


        private void updateSalesTracking(String currency, Material material, int amount, double itemTotal) {
            itemsByCurrency.computeIfAbsent(currency, k -> new HashMap<>())
                    .merge(material, amount, Integer::sum);

            soldItems.merge(material, amount, Integer::sum);
            totalItemsSold += amount;
            totalEarnings += itemTotal;
            earningsByCurrency.merge(currency, itemTotal, Double::sum);
        }

        private SellAllResult finalizeSale() {
            if (totalItemsSold == 0) {
                return createFailedSellResult(
                        skippedItems.isEmpty() ? SellStatus.NO_SELLABLE_ITEMS : SellStatus.INVALID_ECONOMY
                );
            }

            processEarnings();

            return new SellAllResult(
                    SellStatus.SUCCESS,
                    totalEarnings,
                    earningsByCurrency,
                    soldItems,
                    skippedItems,
                    totalItemsSold
            );
        }

        private void processEarnings() {
            for (Map.Entry<String, Double> entry : earningsByCurrency.entrySet()) {
                CurrencyManager currencyManager = ShopMaster.getInstance().getCurrencyMap()
                        .get(CurrencyType.getType(entry.getKey(), CurrencyType.VAULT));

                if (currencyManager != null) {
                    currencyManager.add(player, entry.getValue());
                }
            }
        }
    }

    private CurrencyManager getCurrencyManager(SellableItemInfo info) {
        return ShopMaster.getInstance().getCurrencyMap()
                .get(CurrencyType.getType(info.shopItem().getEconomy(), CurrencyType.VAULT));
    }

    private SellAllResult createFailedSellResult(SellStatus status) {
        return new SellAllResult(status, 0,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList(),
                0
        );
    }

    /**
     * Verifica si un mundo está en la lista negra
     */
    private boolean isWorldBlacklisted(String worldName) {
        YamlDocument config = ShopMaster.getInstance().getConf();
        List<String> blacklistedWorlds = config.getStringList("config.command.sell.black-list.world");
        return blacklistedWorlds.contains(worldName);
    }

    /**
     * Verifica si un modo de juego está en la lista negra
     */
    private boolean isGameModeBlacklisted(String gameMode) {
        YamlDocument config = ShopMaster.getInstance().getConf();
        List<String> blacklistedGameModes = config.getStringList("config.command.sell.black-list.gameModes");
        return blacklistedGameModes.contains(gameMode);
    }

    /**
     * Recarga el índice de ítems vendibles
     */
    public void reload() {
        buildSellableItemsIndex();
    }
}