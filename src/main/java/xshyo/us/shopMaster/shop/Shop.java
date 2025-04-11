package xshyo.us.shopMaster.shop;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.shop.data.ShopButton;
import xshyo.us.shopMaster.shop.data.ShopCategory;
import xshyo.us.shopMaster.shop.data.ShopItem;
import xshyo.us.theAPI.utilities.Utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

@Getter
public class Shop {

    private final String name;
    private final boolean enabled;
    private final String title;
    private final int size;
    private final ShopCategory category;
    private final Map<String, ShopButton> buttons;

    private final Map<Integer, ShopItem> items;

    // Mapa auxiliar para buscar ítems por slot rápidamente
    private final Map<Integer, ShopItem> slotToItemMap;

    private final YamlDocument config;


    public Shop(String name, YamlDocument config) {
        this.name = name;
        this.config = config;

        this.enabled = config.getBoolean(name + ".enable");
        this.title = config.getString(name + ".title");
        this.size = config.getInt(name + ".size");

        // Inicializar mapas
        this.items = new HashMap<>();
        this.slotToItemMap = new HashMap<>();

        // Cargar la categoría
        if (config.contains(name + ".category")) {
            this.category = loadCategory(config.getSection(name + ".category"));
        } else {
            this.category = null;
        }

        // Cargar botones
        if (config.contains(name + ".buttons")) {
            this.buttons = loadButtons(config.getSection(name + ".buttons"));
        } else {
            this.buttons = null;
        }


        if (config.contains(name + ".items")) {
            for (String key : config.getSection(name + ".items").getRoutesAsStrings(false)) {
                String path = name + ".items." + key;
                int itemId = Integer.parseInt(key);

                String material = config.getString(path + ".item.material", "STONE");
                String mob = config.getString(path + ".item.mob", "ZOMBIE");

                int quantity = config.getInt(path + ".item.quantity", 1);
                int page = config.getInt(path + ".page", 1);
                String economy = config.getString(path + ".economy", "VAULT");

                int buyPrice = config.getInt(path + ".buyPrice", -1);
                int sellPrice = config.getInt(path + ".sellPrice", -1);

                // Procesar configuración de slots
                ShopItem item;

                if (config.contains(path + ".slot") && config.isInt(path + ".slot")) {
                    int slot = config.getInt(path + ".slot");
                    item = new ShopItem(material, quantity, Collections.singletonList(slot), page, economy, buyPrice, sellPrice);
                } else if (config.contains(path + ".slot") && config.isString(path + ".slot")) {
                    String slotConfig = config.getString(path + ".slot");
                    List<Integer> slots = ShopItem.parseSlots(slotConfig);
                    item = new ShopItem(material, quantity, slots, page, economy, buyPrice, sellPrice);
                } else {
                    List<Integer> slots = new ArrayList<>();

                    // Si es una lista en el config
                    if (config.isList(path + ".slots")) {
                        List<String> slotStrings = config.getStringList(path + ".slots");
                        for (String slotString : slotStrings) {
                            // Procesar cada entrada que podría ser un número o un rango "10 - 15"
                            slots.addAll(ShopItem.parseSlots(slotString));
                        }
                    } else if (config.isString(path + ".slots")) {
                        String slotsConfig = config.getString(path + ".slots");
                        slots.addAll(ShopItem.parseSlots(slotsConfig));
                    }

                    item = new ShopItem(material, quantity, slots, page, economy, buyPrice, sellPrice);
                }

                if (config.contains(path + ".item.name")) {
                    String displayName = config.getString(path + ".item.name");
                    item.setDisplayName(displayName);
                }

                if (config.contains(path + ".item.lore")) {
                    item.setLore(config.getStringList(path + ".item.lore"));
                }

                // Cargar encantamientos si existen
                if (config.contains(path + ".item.enchantments")) {
                    for (String enchantKey : config.getSection(path + ".item.enchantments").getRoutesAsStrings(false)) {
                        try {
                            Enchantment enchant = Enchantment.getByName(enchantKey.toUpperCase());
                            if (enchant != null) {
                                int level = config.getInt(path + ".item.enchantments." + enchantKey);
                                item.getEnchantments().put(enchant, level);
                            } else {
                                ShopMaster.getInstance().getLogger().warning("Invalid enchantment in shop " + name + ", item " + key + ": " + enchantKey);
                            }
                        } catch (Exception e) {
                            ShopMaster.getInstance().getLogger().warning("Error loading enchantment " + enchantKey + " for item " + key + " in shop " + name);
                        }
                    }
                }

                // Procesar configuración específica para pociones
                if (material.equalsIgnoreCase("POTION") || material.equalsIgnoreCase("SPLASH_POTION") || material.equalsIgnoreCase("LINGERING_POTION")) {
                    if (config.contains(path + ".item.potion")) {
                        String potionTypeStr = config.getString(path + ".item.potion.type", "WATER");
                        boolean extended = config.getBoolean(path + ".item.potion.extended", false);
                        boolean upgraded = config.getBoolean(path + ".item.potion.upgraded", false);

                        try {
                            PotionType potionType = PotionType.valueOf(potionTypeStr.toUpperCase());
                            item.setPotionType(potionType);
                            item.setExtended(extended);
                            item.setUpgraded(upgraded);
                        } catch (IllegalArgumentException e) {
                            ShopMaster.getInstance().getLogger().warning("Invalid potion type in shop " + name + ", item " + key + ": " + potionTypeStr);
                        }

                        // Cargar efectos personalizados si existen
                        if (config.contains(path + ".item.potion.custom_effects")) {
                            for (String effectKey : config.getSection(path + ".item.potion.custom_effects").getRoutesAsStrings(false)) {
                                try {
                                    String effectTypeStr = config.getString(path + ".item.potion.custom_effects." + effectKey + ".type");
                                    int duration = config.getInt(path + ".item.potion.custom_effects." + effectKey + ".duration", 200);
                                    int amplifier = config.getInt(path + ".item.potion.custom_effects." + effectKey + ".amplifier", 0);

                                    PotionEffectType effectType = PotionEffectType.getByName(effectTypeStr.toUpperCase());
                                    if (effectType != null) {
                                        PotionEffect effect = new PotionEffect(effectType, duration, amplifier);
                                        item.getCustomEffects().add(effect);
                                    }
                                } catch (Exception e) {
                                    ShopMaster.getInstance().getLogger().warning("Error loading custom potion effect for item " + key + " in shop " + name);
                                }
                            }
                        }
                    }
                }

                if (material.contains("SPAWNER")) {
                    item.setSpawnerMobType(mob);
                }

                // Procesar configuración específica para banners
                if (material.endsWith("_BANNER")) {
                    if (config.contains(path + ".item.banner")) {

                        if (config.contains(path + ".item.banner.patterns")) {
                            for (String patternKey : config.getSection(path + ".item.banner.patterns").getRoutesAsStrings(false)) {
                                try {
                                    String patternTypeStr = config.getString(path + ".item.banner.patterns." + patternKey + ".pattern");
                                    String colorStr = config.getString(path + ".item.banner.patterns." + patternKey + ".color", "WHITE");

                                    PatternType patternType = PatternType.valueOf(patternTypeStr.toUpperCase());
                                    DyeColor color = DyeColor.valueOf(colorStr.toUpperCase());

                                    Pattern pattern = new Pattern(color, patternType);
                                    item.getBannerPatterns().add(pattern);
                                } catch (Exception e) {
                                    ShopMaster.getInstance().getLogger().warning("Error loading banner pattern for item " + key + " in shop " + name);
                                }
                            }
                        }
                    }
                }

                if (material.equals("SHIELD")) {
                    if (config.contains(path + ".item.shield_pattern")) {
                        // Procesar color base del escudo
                        if (config.contains(path + ".item.shield_pattern.base_color")) {
                            try {
                                String baseColorStr = config.getString(path + ".item.shield_pattern.base_color", "WHITE");
                                DyeColor baseColor = DyeColor.valueOf(baseColorStr.toUpperCase());
                                item.setShieldBaseColor(baseColor);
                            } catch (Exception e) {
                                ShopMaster.getInstance().getLogger().warning("Error loading shield base color for item " + key + " in shop " + name);
                            }
                        }

                        // Procesar patrones del escudo
                        if (config.contains(path + ".item.shield_pattern.patterns")) {
                            for (String patternKey : config.getSection(path + ".item.shield_pattern.patterns").getRoutesAsStrings(false)) {
//                                try {
                                    String patternTypeStr = config.getString(path + ".item.shield_pattern.patterns." + patternKey + ".pattern");
                                    String colorStr = config.getString(path + ".item.shield_pattern.patterns." + patternKey + ".color", "WHITE");

                                    PatternType patternType = PatternType.valueOf(patternTypeStr.toUpperCase());
                                    DyeColor color = DyeColor.valueOf(colorStr.toUpperCase());

                                    Pattern pattern = new Pattern(color, patternType);
                                    item.getShieldPatterns().add(pattern);
//                                } catch (Exception e) {
//                                    ShopMaster.getInstance().getLogger().warning("Error loading shield pattern for item " + key + " in shop " + name);
//                                }
                            }
                        }
                    }
                }



                // Procesar configuración específica para fuegos artificiales
                if (material.equalsIgnoreCase("FIREWORK_ROCKET")) {
                    if (config.contains(path + ".item.firework")) {
                        int power = config.getInt(path + ".item.firework.power", 1);
                        item.setFireworkPower(power);

                        // Cargar efectos si existen
                        if (config.contains(path + ".item.firework.effects")) {
                            for (String effectKey : config.getSection(path + ".item.firework.effects").getRoutesAsStrings(false)) {
                                try {
                                    String typeStr = config.getString(path + ".item.firework.effects." + effectKey + ".type", "BALL");
                                    boolean flicker = config.getBoolean(path + ".item.firework.effects." + effectKey + ".flicker", false);
                                    boolean trail = config.getBoolean(path + ".item.firework.effects." + effectKey + ".trail", false);

                                    FireworkEffect.Type type = FireworkEffect.Type.valueOf(typeStr.toUpperCase());
                                    xshyo.us.shopMaster.utilities.FireworkEffect effect = new xshyo.us.shopMaster.utilities.FireworkEffect(type, flicker, trail);

                                    // Cargar colores
                                    if (config.contains(path + ".item.firework.effects." + effectKey + ".colors")) {
                                        for (String colorStr : config.getStringList(path + ".item.firework.effects." + effectKey + ".colors")) {
                                            // Formato esperado: "R,G,B"
                                            String[] rgb = colorStr.split(",");
                                            if (rgb.length == 3) {
                                                int r = Integer.parseInt(rgb[0].trim());
                                                int g = Integer.parseInt(rgb[1].trim());
                                                int b = Integer.parseInt(rgb[2].trim());
                                                effect.addColor(Color.fromRGB(r, g, b));
                                            }
                                        }
                                    }

                                    // Cargar colores de desvanecimiento
                                    if (config.contains(path + ".item.firework.effects." + effectKey + ".fade_colors")) {
                                        for (String colorStr : config.getStringList(path + ".item.firework.effects." + effectKey + ".fade_colors")) {
                                            // Formato esperado: "R,G,B"
                                            String[] rgb = colorStr.split(",");
                                            if (rgb.length == 3) {
                                                int r = Integer.parseInt(rgb[0].trim());
                                                int g = Integer.parseInt(rgb[1].trim());
                                                int b = Integer.parseInt(rgb[2].trim());
                                                effect.addFadeColor(Color.fromRGB(r, g, b));
                                            }
                                        }
                                    }

                                    item.getFireworkEffects().add(effect.build());
                                } catch (Exception e) {
                                    ShopMaster.getInstance().getLogger().warning("Error loading firework effect for item " + key + " in shop " + name);
                                }
                            }
                        }
                    }
                }

                if (Utils.getCurrentVersion() >= 1170 && material.equalsIgnoreCase("AXOLOTL_BUCKET")) {
                    if (config.contains(path + ".item.axolotl")) {
                        String variantStr = config.getString(path + ".item.axolotl.variant", "LUCY");
                        try {
                            Class<?> axolotlClass = Class.forName("org.bukkit.entity.Axolotl$Variant");
                            Object variant = Enum.valueOf((Class<Enum>) axolotlClass, variantStr.toUpperCase());

                            Method setVariant = item.getClass().getMethod("setAxolotlVariant", axolotlClass);
                            setVariant.invoke(item, variant);
                        } catch (Exception e) {
                            ShopMaster.getInstance().getLogger().warning("Could not assign axolotl type in shop " + name + ", item " + key + ": " + e.getMessage());
                        }
                    }
                }

// Procesar configuración específica para flechas con efectos
                if (material.equalsIgnoreCase("TIPPED_ARROW")) {
                    if (config.contains(path + ".item.arrow_potion")) {
                        String potionTypeStr = config.getString(path + ".item.arrow_potion.type", "WATER");
                        boolean extended = config.getBoolean(path + ".item.arrow_potion.extended", false);
                        boolean upgraded = config.getBoolean(path + ".item.arrow_potion.upgraded", false);

                        try {
                            PotionType potionType = PotionType.valueOf(potionTypeStr.toUpperCase());
                            item.setArrowPotionType(potionType);
                            item.setArrowUpgraded(upgraded);
                            item.setArrowExtended(extended);

                        } catch (IllegalArgumentException e) {
                            ShopMaster.getInstance().getLogger().warning("Invalid potion type for arrow in shop " + name + ", item " + key + ": " + potionTypeStr);
                        }

                        // Cargar efectos personalizados para flechas si existen
                        if (config.contains(path + ".item.arrow_potion.custom_effects")) {
                            for (String effectKey : config.getSection(path + ".item.arrow_potion.custom_effects").getRoutesAsStrings(false)) {
                                try {
                                    String effectTypeStr = config.getString(path + ".item.arrow_potion.custom_effects." + effectKey + ".type");
                                    int duration = config.getInt(path + ".item.arrow_potion.custom_effects." + effectKey + ".duration", 200);
                                    int amplifier = config.getInt(path + ".item.arrow_potion.custom_effects." + effectKey + ".amplifier", 0);

                                    PotionEffectType effectType = PotionEffectType.getByName(effectTypeStr.toUpperCase());
                                    if (effectType != null) {
                                        PotionEffect effect = new PotionEffect(effectType, duration, amplifier);
                                        item.getArrowCustomEffects().add(effect);
                                    }
                                } catch (Exception e) {
                                    ShopMaster.getInstance().getLogger().warning("Error loading custom effect for arrow in item " + key + " in shop " + name);
                                }
                            }
                        }
                    }
                }

                if (material.equalsIgnoreCase("CROSSBOW")) {
                    if (config.contains(path + ".item.loaded_projectiles")) {
                        for (String projectileKey : config.getSection(path + ".item.loaded_projectiles").getRoutesAsStrings(false)) {
                            try {
                                String projectileMaterialStr = config.getString(path + ".item.loaded_projectiles." + projectileKey + ".material", "ARROW");
                                Material projectileMaterial = Material.valueOf(projectileMaterialStr.toUpperCase());
                                int integer = config.getInt(path + ".item.loaded_projectiles." + projectileKey + ".quantity", 1);

                                ItemStack projectile = new ItemStack(projectileMaterial, integer);

                                // Si es una flecha con efecto, procesar su configuración
                                if (projectileMaterial == Material.TIPPED_ARROW && config.contains(path + ".item.loaded_projectiles." + projectileKey + ".potion")) {
                                    PotionMeta meta = (PotionMeta) projectile.getItemMeta();

                                    String potionTypeStr = config.getString(path + ".item.loaded_projectiles." + projectileKey + ".potion.type", "INSTANT_DAMAGE");
                                    boolean extended = config.getBoolean(path + ".item.loaded_projectiles." + projectileKey + ".potion.extended", false);
                                    boolean upgraded = config.getBoolean(path + ".item.loaded_projectiles." + projectileKey + ".potion.upgraded", true);

                                    try {
                                        PotionType potionType = PotionType.valueOf(potionTypeStr.toUpperCase());
                                        meta.setBasePotionData(new PotionData(potionType, extended, upgraded));
                                        projectile.setItemMeta(meta);
                                    } catch (IllegalArgumentException e) {
                                        ShopMaster.getInstance().getLogger().warning("Invalid potion type for arrow in crossbow in shop " + name + ", item " + key);
                                    }
                                }

                                item.getLoadedProjectiles().add(projectile);
                            } catch (Exception e) {
                                ShopMaster.getInstance().getLogger().warning("Error loading projectile for crossbow in item " + key + " in shop " + name + ": " + e.getMessage());
                            }
                        }
                    }
                }

                if (material.startsWith("LEATHER_") && config.contains(path + ".item.armor_color")) {
                    String colorStr = config.getString(path + ".item.armor_color");
                    item.setArmorColor(colorStr);
                }

                if (Utils.getCurrentVersion() >= 1190) {
                    if (material.contains("GOAT_HORN") &&
                            config.contains(path + ".item.musicInstrument")) {
                        String musicInstrumentStr = config.getString(path + ".item.musicInstrument");
                        try {
                            MusicInstrument trimMaterial = Registry.INSTRUMENT.get(NamespacedKey.minecraft(musicInstrumentStr.toLowerCase()));
                            item.setMusicInstrument(trimMaterial);
                        } catch (IllegalArgumentException e) {
                            ShopMaster.getInstance().getLogger().warning("Invalid music instrument configuration in shop " + name + ", item " + key);
                        }
                    }
                }


                if (Utils.getCurrentVersion() >= 1200) {

                    if ((material.contains("_HELMET") || material.contains("_CHESTPLATE") ||
                            material.contains("_LEGGINGS") || material.contains("_BOOTS")) &&
                            config.contains(path + ".item.trim")) {

                        String trimPatternStr = config.getString(path + ".item.trim.pattern");
                        String trimMaterialStr = config.getString(path + ".item.trim.material");

                        try {
                            TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(trimMaterialStr.toLowerCase()));
                            TrimPattern pattern = Registry.TRIM_PATTERN.match("minecraft:" + trimPatternStr.toLowerCase());

                            item.setArmorTrimPattern(pattern);
                            item.setArmorTrimMaterial(trimMaterial);
                        } catch (IllegalArgumentException e) {
                            ShopMaster.getInstance().getLogger().warning("Invalid armor trim configuration in shop " + name + ", item " + key);
                        }
                    }


                }
// Process item flags
                if (config.contains(path + ".item.flags")) {
                    if (config.isList(path + ".item.flags")) {
                        List<String> flagStrings = config.getStringList(path + ".item.flags");
                        item.setItemFlags(new HashSet<>(flagStrings));
                    }
                }

// Process hidden property
                item.setHidden(config.getBoolean(path + ".hidden", false));

// Process NBT data
                if (config.contains(path + ".item.nbt")) {
                    String nbtData = config.getString(path + ".item.nbt");
                    item.setNbtData(nbtData);
                }

// Process buy commands
                if (config.contains(path + ".buy_commands")) {
                    if (config.isList(path + ".buy_commands")) {
                        List<String> commands = config.getStringList(path + ".buy_commands");
                        item.setBuyCommands(commands);
                    }
                }

// Process sell commands
                if (config.contains(path + ".sell_commands")) {
                    if (config.isList(path + ".sell_commands")) {
                        List<String> commands = config.getStringList(path + ".sell_commands");
                        item.setSellCommands(commands);
                    }
                }

                // Guardar el ítem en el mapa principal
                items.put(itemId, item);

                // Registrar cada slot en el mapa auxiliar para búsqueda rápida
                for (int slot : item.getSlots()) {
                    slotToItemMap.put(slot, item);
                }
            }
        }

    }

    /**
     * Carga la configuración de la categoría
     */
    private ShopCategory loadCategory(Section section) {

        String materialName = section.getString("material", "BARRIER");
        List<Integer> slots = new ArrayList<>();

        // Caso 1: slot único como número
        if (section.contains("slot") && section.isInt("slot")) {
            int slot = section.getInt("slot", 0);
            slots.add(slot);
        }
        // Caso 2: slot como string (podría ser un rango)
        else if (section.contains("slot") && section.isString("slot")) {
            String slotConfig = section.getString("slot");
            slots.addAll(ShopItem.parseSlots(slotConfig));
        }
        // Caso 3: slots como lista
        else if (section.contains("slots")) {
            // Si es una lista en el config
            if (section.isList("slots")) {
                List<String> slotStrings = section.getStringList("slots");
                for (String slotString : slotStrings) {
                    slots.addAll(ShopItem.parseSlots(slotString));
                }
            }
            // Si es un string con formato especial
            else if (section.isString("slots")) {
                String slotsConfig = section.getString("slots");
                slots.addAll(ShopItem.parseSlots(slotsConfig));
            }
        } else {
            // Valor por defecto si no se especifica ningún slot
            slots.add(0);
        }

        int amount = section.getInt("amount", 1);
        int modelData = section.getInt("model_data", 0);
        String displayName = section.getString("display_name", "");
        boolean glowing = section.getBoolean("glowing", false);

        List<String> itemFlagsStr = section.getStringList("item_flags");

        List<String> lore = section.getStringList("lore");

        // Necesitarías modificar el constructor de ShopCategory para aceptar una lista de slots
        return new ShopCategory(
                materialName,
                slots,  // Ahora pasamos una lista de slots en lugar de un único slot
                amount,
                modelData,
                displayName,
                glowing,
                itemFlagsStr,
                lore
        );
    }


    /**
     * Carga los botones de navegación y devuelve un mapa con ellos
     */
    private Map<String, ShopButton> loadButtons(Section section) {
        Map<String, ShopButton> buttonMap = new HashMap<>();

        if (section == null) {
            return buttonMap; // Devolver un mapa vacío si la sección es nula
        }

        for (String buttonKey : section.getRoutesAsStrings(false)) {
            Section buttonSection = section.getSection(buttonKey);
            if (buttonSection == null || !buttonSection.getBoolean("enabled", true)) {
                continue;
            }

            try {
                String materialName = buttonSection.getString("material", "BARRIER");
                boolean enabled = buttonSection.getBoolean("enabled", true);
                int amount = buttonSection.getInt("amount", 1);
                int modelData = buttonSection.getInt("model_data", 0);
                String displayName = buttonSection.getString("display_name", "");
                boolean glowing = buttonSection.getBoolean("glowing", false);

                List<String> itemFlagsStr = buttonSection.getStringList("item_flags");
                List<String> lore = buttonSection.getStringList("lore");

                // Procesamiento de slots con los nuevos formatos
                List<Integer> slots = new ArrayList<>();

                // Caso 1: slot único como número
                if (buttonSection.contains("slot") && buttonSection.isInt("slot")) {
                    int slot = buttonSection.getInt("slot", 0);
                    slots.add(slot);
                }
                // Caso 2: slot como string (podría ser un rango)
                else if (buttonSection.contains("slot") && buttonSection.isString("slot")) {
                    String slotConfig = buttonSection.getString("slot");
                    slots.addAll(ShopButton.parseSlots(slotConfig));
                }
                // Caso 3: slots como lista
                else if (buttonSection.contains("slots")) {
                    // Si es una lista en el config
                    if (buttonSection.isList("slots")) {
                        List<String> slotStrings = buttonSection.getStringList("slots");
                        for (String slotString : slotStrings) {
                            slots.addAll(ShopButton.parseSlots(slotString));
                        }
                    }
                    // Si es un string con formato especial
                    else if (buttonSection.isString("slots")) {
                        String slotsConfig = buttonSection.getString("slots");
                        slots.addAll(ShopButton.parseSlots(slotsConfig));
                    }
                } else {
                    // Valor por defecto si no se especifica ningún slot
                    slots.add(0);
                }

                ShopButton button = new ShopButton(
                        buttonKey,
                        enabled,
                        materialName,
                        slots,  // Ahora pasamos la lista de slots
                        amount,
                        modelData,
                        displayName,
                        glowing,
                        itemFlagsStr,
                        lore
                );

                buttonMap.put(buttonKey, button);
            } catch (Exception e) {
                System.err.println("Error loading button " + buttonKey + ": " + e.getMessage());
            }
        }

        return buttonMap;
    }

    /**
     * Busca un ShopItem por su slot
     *
     * @param slot El slot a buscar
     * @return El ShopItem encontrado o null si no existe
     */
    public ShopItem getItemBySlot(int slot) {
        return slotToItemMap.get(slot);
    }

    /**
     * Verifica si hay un ítem en el slot especificado
     *
     * @param slot El slot a verificar
     * @return true si hay un ítem, false si no
     */
    public boolean hasItemInSlot(int slot) {
        return slotToItemMap.containsKey(slot);
    }

    public boolean saveChanges() {
        try {
            config.save();
            return true;
        } catch (IOException e) {
            ShopMaster.getInstance().getLogger().severe("Error saving the shop " + name + ": " + e.getMessage());
            return false;
        }
    }


    // Método para actualizar un ítem en el YAML
    public void updateItem(int itemId, ShopItem item) {
        String path = name + ".items." + itemId;

        // Actualizar en el YAML
        config.set(path + ".item.material", item.getMaterial().toString());
        config.set(path + ".item.quantity", item.getAmount());
        config.set(path + ".page", item.getPage());
        config.set(path + ".buyPrice", item.getBuyPrice());
        config.set(path + ".sellPrice", item.getSellPrice());

//        // Si hay un solo slot, guardarlo como un entero
//        if (item.getSlots().size() == 1) {
//            config.set(path + ".slot", item.getSlots().get(0));
//        } else {
//            // Si hay múltiples slots, guardarlos como un string formateado
//            config.set(path + ".slot", ShopItem.formatSlots(item.getSlots()));
//        }

        // Actualizar el mapa de ítems
        items.put(itemId, item);

        // Actualizar el mapa de slots
        for (int slot : item.getSlots()) {
            slotToItemMap.put(slot, item);
        }
    }


}