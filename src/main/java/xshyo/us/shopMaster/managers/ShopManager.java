package xshyo.us.shopMaster.managers;

import xshyo.us.shopMaster.shop.Shop;

import java.io.File;
import java.util.Map;


public class ShopManager {


    public ShopManager() {
    }

    /**
     * Loads all shops from configuration files
     */
    public void load() {
    }

    /**
     * Gets the shop by name
     *
     * @param name shop name
     * @return the shop or null if not found
     */
    public Shop getShop(String name) {
        return null;
    }

    /**
     * Gets an unmodifiable view of all shops
     *
     * @return map of all shops
     */
    public Map<String, Shop> getAllShops() {
        return null;
    }

    /**
     * Creates or retrieves the shops folder
     *
     * @return the shops folder or null if it couldn't be created
     */
    private File getOrCreateShopsFolder() {
        return null;
    }

    /**
     * Recursively loads all YML files from the given folder
     *
     * @param folder folder to search in
     */
    private void loadFilesRecursively(File folder) {

    }

    /**
     * Loads a shop from a file
     *
     * @param file file to load
     */
    private void loadShopFromFile(File file) {

    }


    /**
     * Gets the shop name from a file
     *
     * @param file file to get name from
     * @return shop name
     */
    private String getShopNameFromFile(File file) {
        return null;
    }

    /**
     * Gets the relative path of a file from the shops folder
     *
     * @param file file to get path for
     * @return relative path
     */
    private String getRelativePath(File file) {
        return null;
    }

    /**
     * Reload a specific shop by name
     *
     * @param shopName name of the shop to reload
     * @return true if shop was reloaded successfully
     */
    public boolean reloadShop(String shopName) {
        return false;
    }

    /**
     * Find a shop file by name
     *
     * @param directory directory to search in
     * @param shopName  name of the shop
     * @return shop file or null if not found
     */
    private File findShopFile(File directory, String shopName) {
        return null;
    }
}