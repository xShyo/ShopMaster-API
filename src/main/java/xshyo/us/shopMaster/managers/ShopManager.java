package xshyo.us.shopMaster.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.Shop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ShopManager {

    @Getter
    private final ShopMaster plugin = ShopMaster.getInstance();
    private final Map<String, Shop> shopMap = new HashMap<>();
    private int loadedShopsCount = 0;
    private int loadedItemsCount = 0; // New counter for items

    public void load() {
        shopMap.clear();
        loadedShopsCount = 0;
        loadedItemsCount = 0; // Reset item counter

        File shopsFolder = new File(plugin.getDataFolder(), "shops");

        if (!shopsFolder.exists()) {
            shopsFolder.mkdirs();
        }

        loadFilesRecursively(shopsFolder);
        plugin.getLogger().info("Total tiendas cargadas: " + loadedShopsCount + ", Total items cargados: " + loadedItemsCount);
    }

    private void loadFilesRecursively(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadFilesRecursively(file);
            } else if (file.getName().endsWith(".yml")) {
                processFile(file);
            }
        }
    }

    private void processFile(File file) {
        try {
            YamlDocument configFile = YamlDocument.create(
                    file,
                    plugin.getResource(file.getName()),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version")).build()
            );

            String shopName = file.getName().replace(".yml", "");
            String relativePath = getRelativePath(file);

            if (shopMap.containsKey(shopName)) {
                plugin.getLogger().warning("Tienda duplicada encontrada y omitida: " + relativePath);
                return;
            }

            Shop shop = new Shop(shopName, configFile);
            shopMap.put(shopName, shop);
            loadedShopsCount++;

            // Count the items in this shop
            int shopItemsCount = shop.getItems().size();
            loadedItemsCount += shopItemsCount;

            plugin.getLogger().info("Tienda cargada: " + shopName + " desde " + relativePath +
                    " (" + shopItemsCount + " items)");

        } catch (IOException e) {
            plugin.getLogger().severe("Error cargando la tienda " + file.getName() + ": " + e.getMessage());
        }
    }

    private String getRelativePath(File file) {
        File shopsFolder = new File(plugin.getDataFolder(), "shops");
        return shopsFolder.toPath().relativize(file.toPath()).toString();
    }

    public Shop getShop(String name) {
        return shopMap.get(name);
    }

}