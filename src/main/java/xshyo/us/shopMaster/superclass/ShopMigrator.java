package xshyo.us.shopMaster.superclass;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ShopMigrator {

    private final JavaPlugin plugin;
    private final File shopGUIConfigFile;
    private final File shopGUIShopsDir;
    private final File shopMasterDir;

    public ShopMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopGUIConfigFile = new File("plugins/ShopGUIPlus/config.yml");
        this.shopGUIShopsDir = new File("plugins/ShopGUIPlus/shops");
        this.shopMasterDir = new File(plugin.getDataFolder(), "shops");

        if (!shopMasterDir.exists()) {
            shopMasterDir.mkdirs();
        }
    }

    public boolean migrateShops() {
        if (!shopGUIConfigFile.exists()) {
            plugin.getLogger().warning("No se encontró el archivo de configuración de ShopGUIPlus.");
            return false;
        }

        YamlConfiguration shopGUIConfig = YamlConfiguration.loadConfiguration(shopGUIConfigFile);
        ConfigurationSection shopMenuItems = shopGUIConfig.getConfigurationSection("shopMenuItems");

        if (shopMenuItems == null) {
            plugin.getLogger().warning("No se encontró la sección 'shopMenuItems' en la configuración de ShopGUIPlus.");
            return false;
        }

        Set<String> menuItemKeys = shopMenuItems.getKeys(false);
        if (menuItemKeys.isEmpty()) {
            plugin.getLogger().warning("No se encontraron items en 'shopMenuItems'.");
            return false;
        }

        int migratedShops = 0;

        for (String key : menuItemKeys) {
            ConfigurationSection menuItemSection = shopMenuItems.getConfigurationSection(key);
            if (menuItemSection == null) continue;

            String shopName = menuItemSection.getString("shop");
            if (shopName == null || shopName.isEmpty()) {
                plugin.getLogger().warning("Tienda sin nombre en el item " + key + ", saltándola.");
                continue;
            }

            boolean success = migrateShop(shopName, menuItemSection);
            if (success) {
                migratedShops++;
                plugin.getLogger().info("Tienda migrada exitosamente: " + shopName);
            } else {
                plugin.getLogger().warning("Error al migrar la tienda: " + shopName);
            }
        }

        plugin.getLogger().info("Migración completada: " + migratedShops + " tiendas migradas.");
        return migratedShops > 0;
    }
    private boolean migrateShop(String shopName, ConfigurationSection menuItemSection) {
        File shopGUIShopFile = new File(shopGUIShopsDir, shopName + ".yml");
        YamlConfiguration shopConfig = new YamlConfiguration();

        shopConfig.set(shopName + ".enable", true);
        shopConfig.set(shopName + ".title", "&8☀ Tienda | &l" + shopName.toUpperCase());
        shopConfig.set(shopName + ".size", 54);

        ConfigurationSection itemSection = menuItemSection.getConfigurationSection("item");
        String material = (itemSection != null) ? itemSection.getString("material", "STONE") : "STONE";
        String skin = (itemSection != null) ? itemSection.getString("skin") : "";

        String name = (itemSection != null) ? itemSection.getString("name", shopName) : shopName;
        int slot = menuItemSection.getInt("slot", 0);
        int model = menuItemSection.getInt("model", 0);
        List<String> lore = (itemSection != null) ? itemSection.getStringList("lore") : new ArrayList<>();

        if (skin != null && !skin.isEmpty()) {
            material = "basehead-" + skin;
        }
        shopConfig.set(shopName + ".category.material", material);
        shopConfig.set(shopName + ".category.slot", slot);
        shopConfig.set(shopName + ".category.amount", 1);
        shopConfig.set(shopName + ".category.model_data", model);
        shopConfig.set(shopName + ".category.display_name", name);
        shopConfig.set(shopName + ".category.glowing", false);
        shopConfig.set(shopName + ".category.item_flags", List.of("HIDE_ATTRIBUTES"));
        shopConfig.set(shopName + ".category.lore", lore);

        if (shopGUIShopFile.exists()) {
            YamlConfiguration shopGUIShopConfig = YamlConfiguration.loadConfiguration(shopGUIShopFile);
            ConfigurationSection itemsSection = shopGUIShopConfig.getConfigurationSection(shopName + ".items");

            if (itemsSection != null) {
                Set<String> itemKeys = itemsSection.getKeys(false);
                for (String key : itemKeys) {
                    ConfigurationSection itemData = itemsSection.getConfigurationSection(key);
                    if (itemData == null || !"item".equals(itemData.getString("type"))) continue;

                    ConfigurationSection itemConfig = itemData.getConfigurationSection("item");
                    if (itemConfig == null) continue;

                    String itemMaterial = itemConfig.getString("material", "STONE");
                    String itemSkin = itemConfig.getString("skin", "");

                    int quantity = itemConfig.getInt("quantity", 1);
                    String itemName = itemConfig.getString("name", "");
                    List<String> itemLore = itemConfig.getStringList("lore");

                    // Obtener los encantamientos del formato antiguo
                    List<String> oldEnchantments = itemConfig.getStringList("enchantments");

                    double buyPrice = itemData.getDouble("buyPrice", -1);
                    double sellPrice = itemData.getDouble("sellPrice", -1);
                    int itemSlot = itemData.getInt("slot", 0);
                    int page = itemData.getInt("page", 1);

                    String itemPath = shopName + ".items." + key;
                    shopConfig.set(itemPath + ".slot", itemSlot);
                    shopConfig.set(itemPath + ".page", page);
                    shopConfig.set(itemPath + ".buyPrice", buyPrice);
                    shopConfig.set(itemPath + ".sellPrice", sellPrice);

                    if (itemSkin != null && !itemSkin.isEmpty()) {
                        itemMaterial = "basehead-" + itemSkin;
                    }

                    // Migrar FIREWORK a FIREWORK_ROCKET
                    if ("FIREWORK".equals(itemMaterial)) {
                        itemMaterial = "FIREWORK_ROCKET";
                    }

                    shopConfig.set(itemPath + ".item.material", itemMaterial);
                    shopConfig.set(itemPath + ".item.quantity", quantity);

                    // Migrar pociones
                    if (isPotionMaterial(itemMaterial)) {
                        // 1. Verificar formato antiguo (1.7-1.8) usando damage value
                        int damage = itemConfig.getInt("damage", 0);
                        if (damage > 0) {
                            // Convertir damage value a tipo de poción, extendida y mejorada
                            PotionInfo potionInfo = decodePotionDamageValue(damage);
                            if (potionInfo != null) {
                                shopConfig.set(itemPath + ".item.potion.type", potionInfo.type);
                                shopConfig.set(itemPath + ".item.potion.extended", potionInfo.extended);
                                shopConfig.set(itemPath + ".item.potion.upgraded", potionInfo.upgraded);
                            }
                        } else {
                            // 2. Verificar formato 1.9+
                            ConfigurationSection potionSection = itemConfig.getConfigurationSection("potion");
                            if (potionSection != null) {
                                String potionType = potionSection.getString("type", "WATER");
                                int level = potionSection.getInt("level", 1);
                                boolean extended = potionSection.getBoolean("extended", false);

                                shopConfig.set(itemPath + ".item.potion.type", potionType);
                                shopConfig.set(itemPath + ".item.potion.extended", extended);
                                shopConfig.set(itemPath + ".item.potion.upgraded", level > 1);

                                // Si hay efectos personalizados, migrarlos también
                                ConfigurationSection customEffectsSection = potionSection.getConfigurationSection("custom_effects");
                                if (customEffectsSection != null) {
                                    for (String effectKey : customEffectsSection.getKeys(false)) {
                                        ConfigurationSection effectSection = customEffectsSection.getConfigurationSection(effectKey);
                                        if (effectSection != null) {
                                            String effectPath = itemPath + ".item.potion.custom_effects." + effectKey;
                                            shopConfig.set(effectPath + ".type", effectSection.getString("type"));
                                            shopConfig.set(effectPath + ".duration", effectSection.getInt("duration", 200));
                                            shopConfig.set(effectPath + ".amplifier", effectSection.getInt("amplifier", 0));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Migrar fireworks
                    if ("FIREWORK_ROCKET".equals(itemMaterial)) {
                        int fireworkPower = itemConfig.getInt("fireworkPower", 1);
                        shopConfig.set(itemPath + ".item.firework.power", fireworkPower);

                        ConfigurationSection fireworkEffectsSection = itemConfig.getConfigurationSection("fireworkEffects");
                        if (fireworkEffectsSection != null) {
                            for (String effectKey : fireworkEffectsSection.getKeys(false)) {
                                ConfigurationSection effectSection = fireworkEffectsSection.getConfigurationSection(effectKey);
                                if (effectSection == null) continue;

                                String effectType = effectSection.getString("type", "BALL");
                                List<String> colorNames = effectSection.getStringList("colors");

                                String effectPath = itemPath + ".item.firework.effects." + effectKey;
                                shopConfig.set(effectPath + ".type", effectType);
                                shopConfig.set(effectPath + ".flicker", false); // Valores por defecto
                                shopConfig.set(effectPath + ".trail", false);   // Valores por defecto

                                // Convertir colores nombrados a RGB
                                List<String> rgbColors = new ArrayList<>();
                                for (String colorName : colorNames) {
                                    String rgbValue = convertColorToRGB(colorName);
                                    if (rgbValue != null) {
                                        rgbColors.add(rgbValue);
                                    }
                                }

                                if (!rgbColors.isEmpty()) {
                                    shopConfig.set(effectPath + ".colors", rgbColors);
                                }
                            }
                        }
                    }

                    if (itemMaterial.endsWith("_BANNER")) {
                        ConfigurationSection patternsSection = itemConfig.getConfigurationSection("patterns");
                        if (patternsSection != null) {
                            ConfigurationSection newPatternsSection = shopConfig.createSection(itemPath + ".item.banner.patterns");
                            for (String patternKey : patternsSection.getKeys(false)) {
                                ConfigurationSection patternData = patternsSection.getConfigurationSection(patternKey);
                                if (patternData != null) {
                                    newPatternsSection.set(patternKey + ".pattern", patternData.getString("type"));
                                    newPatternsSection.set(patternKey + ".color", patternData.getString("color"));
                                }
                            }
                        }
                    }

                    // Migrar encantamientos al nuevo formato
                    if (!oldEnchantments.isEmpty()) {
                        for (String enchantmentStr : oldEnchantments) {
                            String[] parts = enchantmentStr.split(":");
                            if (parts.length == 2) {
                                String enchantName = parts[0];
                                try {
                                    int enchantLevel = Integer.parseInt(parts[1]);
                                    shopConfig.set(itemPath + ".item.enchantments." + enchantName, enchantLevel);
                                } catch (NumberFormatException e) {
                                    // Manejar formato inválido de nivel
                                    System.out.println("Error al convertir nivel de encantamiento: " + enchantmentStr);
                                }
                            }
                        }
                    }

                    if (!itemName.isEmpty()) shopConfig.set(itemPath + ".item.name", itemName);
                    if (!itemLore.isEmpty()) shopConfig.set(itemPath + ".item.lore", itemLore);
                }
            }
        }

        return saveShopConfig(shopName, shopConfig);
    }

    // Método para verificar si el material es una poción
    private boolean isPotionMaterial(String material) {
        return "POTION".equals(material) ||
                "SPLASH_POTION".equals(material) ||
                "LINGERING_POTION".equals(material);
    }

    // Clase para almacenar información de pociones
    private static class PotionInfo {
        String type;
        boolean extended;
        boolean upgraded;

        PotionInfo(String type, boolean extended, boolean upgraded) {
            this.type = type;
            this.extended = extended;
            this.upgraded = upgraded;
        }
    }

    // Método para decodificar valor de daño de pociones (1.7-1.8)
    private PotionInfo decodePotionDamageValue(int damageValue) {
        // Extraer bits para determinar tipo, splash, extendido y mejorado
        boolean isSplash = (damageValue & 0x4000) != 0; // Bit 14 indica si es splash potion
        int potionId = damageValue & 0x3F; // Bits 0-5 indican el tipo de poción
        boolean isExtended = (damageValue & 0x40) != 0; // Bit 6 indica duración extendida
        boolean isUpgraded = (damageValue & 0x80) != 0; // Bit 7 indica potencia mejorada

        String potionType = mapPotionIdToType(potionId);
        if (potionType == null) return null;

        return new PotionInfo(potionType, isExtended, isUpgraded);
    }

    // Mapa de ID de poción (1.7-1.8) a tipo de poción (1.9+)
    private String mapPotionIdToType(int potionId) {
        switch (potionId) {
            case 1: return "REGENERATION";
            case 2: return "SPEED";
            case 3: return "FIRE_RESISTANCE";
            case 4: return "POISON";
            case 5: return "INSTANT_HEAL";
            case 6: return "NIGHT_VISION";
            case 8: return "WEAKNESS";
            case 9: return "STRENGTH";
            case 10: return "SLOWNESS";
            case 11: return "JUMP";
            case 12: return "INSTANT_DAMAGE";
            case 13: return "WATER_BREATHING";
            case 14: return "INVISIBILITY";
            default: return "WATER"; // Poción de agua por defecto
        }
    }

    // Método para convertir colores nombrados a RGB
    private String convertColorToRGB(String colorName) {
        switch (colorName.toUpperCase()) {
            case "BLACK":
                return "0,0,0";
            case "BLUE":
                return "0,0,255";
            case "GREEN":
                return "0,128,0";
            case "CYAN":
                return "0,255,255";
            case "RED":
                return "255,0,0";
            case "PURPLE":
                return "128,0,128";
            case "ORANGE":
                return "255,165,0";
            case "SILVER":
            case "LIGHT_GRAY":
                return "192,192,192";
            case "GRAY":
                return "128,128,128";
            case "LIGHT_BLUE":
                return "173,216,230";
            case "LIME":
                return "0,255,0";
            case "AQUA":
            case "AQUAMARINE":
                return "127,255,212";
            case "YELLOW":
                return "255,255,0";
            case "MAGENTA":
            case "PINK":
                return "255,0,255";
            case "WHITE":
                return "255,255,255";
            case "BROWN":
                return "165,42,42";
            default:
                System.out.println("Color desconocido: " + colorName);
                return null;
        }
    }


    public YamlConfiguration getShopConfig(String shopName) {
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopName + ".yml");
        if (!shopFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(shopFile);
    }

    private boolean saveShopConfig(String shopName, YamlConfiguration config) {
        File shopFile = new File(shopMasterDir, shopName + ".yml");
        try {
            config.save(shopFile);
            plugin.getLogger().info("Tienda guardada: " + shopName);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar la tienda " + shopName, e);
            return false;
        }
    }
}
