package xshyo.us.shopMaster.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.Shop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class ShopManager {

    private static final String SHOPS_DIRECTORY = "shops";
    private static final String YML_EXTENSION = ".yml";

    private final ShopMaster plugin = ShopMaster.getInstance();

    private final Map<String, Shop> shopMap;
    private int loadedShopsCount;
    private int loadedItemsCount;

    public ShopManager() {
        this.shopMap = new HashMap<>();
        this.loadedShopsCount = 0;
        this.loadedItemsCount = 0;
    }

    /**
     * Loads all shops from configuration files
     */
    public void load() {
        shopMap.clear();
        loadedShopsCount = 0;
        loadedItemsCount = 0;

        File shopsFolder = getOrCreateShopsFolder();
        if (shopsFolder == null) {
            plugin.getLogger().severe("The store directory could not be created or accessed.");
            return;
        }

        loadFilesRecursively(shopsFolder);
        plugin.getLogger().info(String.format("Total shops loaded: %d, Total items loaded: %d",
                loadedShopsCount, loadedItemsCount));
    }

    /**
     * Gets the shop by name
     * @param name shop name
     * @return the shop or null if not found
     */
    @Nullable
    public Shop getShop(String name) {
        return shopMap.get(name);
    }

    /**
     * Gets an unmodifiable view of all shops
     * @return map of all shops
     */
    public Map<String, Shop> getAllShops() {
        return Collections.unmodifiableMap(shopMap);
    }

    /**
     * Creates or retrieves the shops folder
     * @return the shops folder or null if it couldn't be created
     */
    @Nullable
    private File getOrCreateShopsFolder() {
        File shopsFolder = new File(plugin.getDataFolder(), SHOPS_DIRECTORY);

        if (!shopsFolder.exists()) {
            boolean created = shopsFolder.mkdirs();
            if (!created) {
                return null;
            }
        } else if (!shopsFolder.isDirectory()) {
            return null;
        }

        return shopsFolder;
    }

    /**
     * Recursively loads all YML files from the given folder
     * @param folder folder to search in
     */
    private void loadFilesRecursively(@NotNull File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadFilesRecursively(file);
            } else if (file.getName().endsWith(YML_EXTENSION)) {
                loadShopFromFile(file);
            }
        }
    }

    /**
     * Loads a shop from a file
     * @param file file to load
     */
    private void loadShopFromFile(@NotNull File file) {
        try {
            YamlDocument configFile = createYamlDocument(file);
            if (configFile == null) return;

            String shopName = getShopNameFromFile(file);
            String relativePath = getRelativePath(file);

            if (shopMap.containsKey(shopName)) {
                plugin.getLogger().warning("Duplicate store found and omitted: " + relativePath);
                return;
            }

            Shop shop = new Shop(shopName, configFile);
            shopMap.put(shopName, shop);
            loadedShopsCount++;

            int shopItemsCount = shop.getItems().size();
            loadedItemsCount += shopItemsCount;

            plugin.getLogger().info(String.format("Store loaded: %s from %s (%d items)",
                    shopName, relativePath, shopItemsCount));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading store " + file.getName(), e);
        }
    }

    /**
     * Creates a YAML document from a file
     * @param file file to load
     * @return YAML document or null if there was an error
     */
    @Nullable
    private YamlDocument createYamlDocument(@NotNull File file) {
        try {
            return YamlDocument.create(
                    file,
                    plugin.getResource(file.getName()),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version")).build()
            );
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating Yaml for. " + file.getName(), e);
            return null;
        }
    }

    /**
     * Gets the shop name from a file
     * @param file file to get name from
     * @return shop name
     */
    @NotNull
    private String getShopNameFromFile(@NotNull File file) {
        return file.getName().replace(YML_EXTENSION, "");
    }

    /**
     * Gets the relative path of a file from the shops folder
     * @param file file to get path for
     * @return relative path
     */
    @NotNull
    private String getRelativePath(@NotNull File file) {
        File shopsFolder = new File(plugin.getDataFolder(), SHOPS_DIRECTORY);
        Path relativePath = shopsFolder.toPath().relativize(file.toPath());
        return relativePath.toString();
    }

    /**
     * Reload a specific shop by name
     * @param shopName name of the shop to reload
     * @return true if shop was reloaded successfully
     */
    public boolean reloadShop(String shopName) {
        Shop existingShop = shopMap.get(shopName);
        if (existingShop == null) {
            return false;
        }

        // Find the shop file
        File shopsFolder = new File(plugin.getDataFolder(), SHOPS_DIRECTORY);
        File shopFile = findShopFile(shopsFolder, shopName);

        if (shopFile != null && shopFile.exists()) {
            // Remove the existing shop and load the new one
            shopMap.remove(shopName);
            loadShopFromFile(shopFile);
            return shopMap.containsKey(shopName);
        }

        return false;
    }

    /**
     * Find a shop file by name
     * @param directory directory to search in
     * @param shopName name of the shop
     * @return shop file or null if not found
     */
    @Nullable
    private File findShopFile(File directory, String shopName) {
        File[] files = directory.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                File result = findShopFile(file, shopName);
                if (result != null) {
                    return result;
                }
            } else if (file.getName().equals(shopName + YML_EXTENSION)) {
                return file;
            }
        }

        return null;
    }
}