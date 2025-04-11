package xshyo.us.shopMaster.services;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xshyo.us.shopMaster.ItemComparator;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.managers.ShopManager;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.shopMaster.managers.CurrencyManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class SellService {
    private static final int MAX_CACHE_SIZE = 100;
    private static final int PARALLEL_THRESHOLD = 100;

    private final ShopMaster plugin;
    private final ShopManager shopManager;
    private final Map<Material, List<SellableItemInfo>> sellableItems;
    private final List<CustomItemEntry> customSellableItems;
    private final Map<CacheKey, SellableItemInfo> resultCache;
    private final ItemComparator itemComparator;

    public SellService(ShopMaster plugin, ShopManager shopManager) {
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
        this.itemComparator = plugin.getItemComparator();

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
    private record CustomItemEntry(Shop shop, ShopItem shopItem) {
    }

    /**
     * Clase para información de ítem vendible
     */
    public record SellableItemInfo(Shop shop, ShopItem shopItem, ItemStack itemStack) {
    }


    /**
     * Verifica rápidamente si un ItemStack es vendible
     *
     * @param player El jugador que intenta vender el ítem
     * @param item   El ItemStack a verificar
     * @return true si el ítem puede ser vendido, false en caso contrario
     */
    public boolean isSellable(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // Verificar restricciones de mundo y modo de juego
        if (isWorldBlacklisted(player.getWorld().getName()) ||
                isGameModeBlacklisted(player.getGameMode().toString())) {
            return false;
        }

        // Usar el cache para evitar búsquedas repetidas
        CacheKey key = new CacheKey(item);
        if (resultCache.containsKey(key)) {
            SellableItemInfo info = resultCache.get(key);
            return info != null && info.shopItem().getSellPrice() > 0;
        }


        // Verificar en ítems estándar primero (más rápido)
        SellableItemInfo standardInfo = findStandardItem(player, item);
        if (standardInfo != null) {
            if (standardInfo.shopItem().getSellPrice() > 0) {
                return true;
            }
        }


        // Si no, verificar en ítems personalizados
        SellableItemInfo customInfo = findCustomItem(player, item);
        return customInfo != null && customInfo.shopItem().getSellPrice() > 0;

    }

    private SellableItemInfo findStandardItem(Player player, ItemStack item) {
        Material material = item.getType();

        if (!sellableItems.containsKey(material)) {
            return null;
        }

        return sellableItems.get(material).stream()
                .filter(info -> itemComparator.areItemsSimilar(item, info.itemStack()))
                .findFirst()
                .orElse(null);
    }


    /**
     * Optimized item indexing with better parallel processing
     */
    public void buildSellableItemsIndex() {
        sellableItems.clear();
        customSellableItems.clear();
        resultCache.clear();

        // Iteramos sobre las tiendas de manera secuencial
        for (Shop shop : shopManager.getShopMap().values()) {
            if (shop.isEnabled()) {
                try {
                    for (ShopItem item : shop.getItems().values()) {
                        if (item.getSellPrice() <= 0) continue;

                        try {
                            try {
                                String matName = item.getMaterial();
                                Material material = Material.valueOf(matName.toUpperCase());
                                ItemStack shopItemStack = item.createItemStack();
                                SellableItemInfo info = new SellableItemInfo(shop, item, shopItemStack);
                                sellableItems.computeIfAbsent(material, k -> new CopyOnWriteArrayList<>()).add(info);
                            } catch (IllegalArgumentException e) {
                                try {
                                    customSellableItems.add(new CustomItemEntry(shop, item));
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning(
                                    "Error processing item " + item.getMaterial() + ": " + e.getMessage()
                            );
                        }
                    }

//                    processShopItems(shop);  // Procesamos los items de la tienda
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error processing shop items for shop: " + shop.getName(), e);
                }
            }
        }

        // Ordenamos las listas de manera más eficiente
        sellableItems.values().forEach(list -> list.sort(
                Comparator.comparing(info -> -info.shopItem().getSellPrice())
        ));

        plugin.getLogger().info(String.format(
                "Shop Item Index Built: Standard Materials: %d, Custom Items: %d",
                sellableItems.size(), customSellableItems.size()
        ));
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
            return customSellableItems.stream()
                    .parallel()
                    // Usar itemComparator en lugar de isSimilar
                    .filter(entry -> itemComparator.areItemsSimilar(item, entry.shopItem.createItemStack()))
                    .findFirst()
                    .map(entry -> new SellableItemInfo(entry.shop, entry.shopItem, entry.shopItem.createItemStack()))
                    .orElse(null);
        }

        return customSellableItems.stream()
                // Usar itemComparator en lugar de isSimilar
                .filter(entry -> itemComparator.areItemsSimilar(item, entry.shopItem.createItemStack()))
                .findFirst()
                .map(entry -> new SellableItemInfo(entry.shop, entry.shopItem, entry.shopItem.createItemStack()))
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


    // En SellService, añade un nuevo método
    public SellResult sellGuiItem(Player player, ItemStack item, int amount) {
        if (isWorldBlacklisted(player.getWorld().getName())) {
            return new SellResult(SellStatus.WORLD_BLACKLISTED, 0, "");
        }

        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return new SellResult(SellStatus.GAMEMODE_BLACKLISTED, 0, "");
        }

        SellableItemInfo info = getSellableShopItem(player, item);
        if (info == null || info.shopItem().getSellPrice() <= 0) {
            return new SellResult(SellStatus.NOT_SELLABLE, 0, "");
        }

        // No necesitamos verificar el inventario ni remover ítems, ya que los ítems están en la GUI
        // y serán eliminados por separado

        double unitPrice = info.shopItem().getSellPrice();
        double baseAmount = info.shopItem().getAmount(); // Cantidad mínima del shop
        if (baseAmount > 1) {
            unitPrice = unitPrice / baseAmount; // Ajustar precio por unidad
        }

        double totalPrice = unitPrice * amount;

        // Verificaciones de economía
        CurrencyManager currencyManager = getCurrencyManager(info);
        if (currencyManager == null) {
            return new SellResult(SellStatus.INVALID_ECONOMY, 0, "");
        }
        String currency = info.shopItem().getEconomy();

        // Dar dinero
        boolean success = currencyManager.add(player, totalPrice);
        if (!success) {
            return new SellResult(SellStatus.ERROR, 0, "");
        }

        return new SellResult(SellStatus.SUCCESS, totalPrice, currency);
    }


    /**
     * Vende un ítem y retorna el resultado
     */
    public SellResult sellItem(Player player, ItemStack item, int amount, boolean searchEntireInventory) {

        if (isWorldBlacklisted(player.getWorld().getName())) {
            return new SellResult(SellStatus.WORLD_BLACKLISTED, 0, "");
        }

        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            return new SellResult(SellStatus.GAMEMODE_BLACKLISTED, 0, "");
        }

        SellableItemInfo info = getSellableShopItem(player, item);
        if (info == null || info.shopItem().getSellPrice() <= 0) {  // Verifica que el precio sea mayor que 0
            return new SellResult(SellStatus.NOT_SELLABLE, 0, "");
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack handItem = inventory.getItemInMainHand();

        // Determinar la estrategia de búsqueda de ítems
        int availableAmount = searchEntireInventory
                ? countItemsInEntireInventory(inventory, item)
                : handItem != null && handItem.isSimilar(item) ? handItem.getAmount() : 0;

        if (availableAmount == 0) {
            return new SellResult(SellStatus.INSUFFICIENT_ITEMS, 0, "");
        }

        // Limitar la cantidad a vender según lo solicitado
        int amountToSell = Math.min(availableAmount, amount);

        double unitPrice = info.shopItem().getSellPrice();
        double baseAmount = info.shopItem().getAmount(); // Cantidad mínima del shop
        if (baseAmount > 1) {
            unitPrice = unitPrice / baseAmount; // Ajustar precio por unidad
        }

        double totalPrice = unitPrice * amountToSell;

        String currency = info.shopItem().getEconomy();

        // Verificaciones de economía
        CurrencyManager currencyManager = getCurrencyManager(info);
        if (currencyManager == null) {
            return new SellResult(SellStatus.INVALID_ECONOMY, 0, "");
        }

        // Remover ítems del inventario
        removeItems(inventory, item, amountToSell, searchEntireInventory);

        // Dar dinero
        boolean success = currencyManager.add(player, totalPrice);
        if (!success) {
            return new SellResult(SellStatus.ERROR, 0, "");
        }

        return new SellResult(SellStatus.SUCCESS, totalPrice, currency);
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
        private final Map<Material, Double> earningsByMaterial = new HashMap<>();

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
            earningsByMaterial.merge(material, itemTotal, Double::sum); // Añadir esto
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
                    itemsByCurrency,
                    earningsByMaterial, // Añadir esto
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
                Collections.emptyMap(),
                Collections.emptyMap(), // Añadir esto
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