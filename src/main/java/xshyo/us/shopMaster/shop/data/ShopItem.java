package xshyo.us.shopMaster.shop.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.FireworkEffect;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.block.banner.Pattern;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ShopItem {

    private String material;
    private final int amount;
    private final List<Integer> slots;
    private final int page;
    private final String economy;
    private final int buyPrice;
    private final int sellPrice;
    private String displayName;
    private List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private PotionType potionType;
    private boolean extended;
    private boolean upgraded;
    private List<PotionEffect> customEffects;
    private List<Pattern> bannerPatterns;
    private int fireworkPower;
    private List<FireworkEffect> fireworkEffects;
    private Axolotl.Variant axolotlVariant;
    private PotionType arrowPotionType;
    private boolean arrowExtended;
    private boolean arrowUpgraded;
    private List<PotionEffect> arrowCustomEffects;
    private List<ItemStack> loadedProjectiles;

    /**
     * Constructor para un único slot
     */
    public ShopItem(String material, int amount, int slot, int page, String economy, int buyPrice, int sellPrice) {
        this.material = material;
        this.amount = amount;
        this.economy = economy;
        this.slots = new ArrayList<>();
        this.slots.add(slot);
        this.page = page;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;

        // Inicializar colecciones vacías
        this.enchantments = new HashMap<>();
        this.lore = new ArrayList<>();
        this.customEffects = new ArrayList<>();
        this.bannerPatterns = new ArrayList<>();
        this.fireworkEffects = new ArrayList<>();
        this.arrowCustomEffects = new ArrayList<>();
        this.loadedProjectiles = new ArrayList<>();
    }

    /**
     * Constructor para múltiples slots específicos
     */
    public ShopItem(String material, int amount, List<Integer> slots, int page, String economy, int buyPrice, int sellPrice) {
        this.material = material;
        this.amount = amount;
        this.slots = slots;
        this.page = page;
        this.economy = economy;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;

        // Inicializar colecciones vacías
        this.enchantments = new HashMap<>();
        this.lore = new ArrayList<>();
        this.customEffects = new ArrayList<>();
        this.bannerPatterns = new ArrayList<>();
        this.fireworkEffects = new ArrayList<>();
        this.arrowCustomEffects = new ArrayList<>();
        this.loadedProjectiles = new ArrayList<>();
    }

    /**
     * Constructor para un rango de slots
     */
    public ShopItem(String material, int amount, int startSlot, int endSlot, int page, String economy, int buyPrice, int sellPrice) {
        this.material = material;
        this.amount = amount;
        this.economy = economy;
        this.slots = new ArrayList<>();
        for (int i = startSlot; i <= endSlot; i++) {
            this.slots.add(i);
        }
        this.page = page;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;

        // Inicializar colecciones vacías
        this.enchantments = new HashMap<>();
        this.lore = new ArrayList<>();
        this.customEffects = new ArrayList<>();
        this.bannerPatterns = new ArrayList<>();
        this.fireworkEffects = new ArrayList<>();
        this.arrowCustomEffects = new ArrayList<>();
        this.loadedProjectiles = new ArrayList<>();
    }


    /**
     * Método para crear un ItemStack con todos los metadatos configurados
     * utilizando la clase ItemBuilder mejorada
     */
    public ItemStack createItemStack() {
        // Crear un ItemBuilder con el material y cantidad base
        ItemBuilder builder = new ItemBuilder(material);

        if (amount < 0) {
            builder.setAmount(1);
        } else {
            builder.setAmount(amount);
        }

        // Aplicar nombre y lore
        if (displayName != null) {
            builder.setName(displayName);
        }

        if (!lore.isEmpty()) {
            builder.setLore(lore);
        }

        // Aplicar metadatos específicos según el tipo de ítem
        if (material.equalsIgnoreCase("POTION") ||
                material.equalsIgnoreCase("SPLASH_POTION") ||
                material.equalsIgnoreCase("LINGERING_POTION")) {
            applyPotionMetadata(builder);
        } else if (material.endsWith("_BANNER")) {
            applyBannerMetadata(builder);
        } else if (material.equalsIgnoreCase("FIREWORK_ROCKET")) {
            applyFireworkMetadata(builder);
        } else if (material.equalsIgnoreCase("AXOLOTL_BUCKET") && Utils.getCurrentVersion() >= 1170) {
            applyAxolotlMetadata(builder);
        } else if (material.equalsIgnoreCase("TIPPED_ARROW")) {
            applyArrowMetadata(builder);
        } else if (material.equalsIgnoreCase("CROSSBOW")) {
            applyCrossbowMetadata(builder);
        }

        // Aplicar encantamientos al final
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            builder.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * Aplicar metadatos de poción al ItemBuilder
     */
    private void applyPotionMetadata(ItemBuilder builder) {
        if (potionType != null) {
            builder.setBasePotionData(new PotionData(potionType, extended, upgraded));
        }

        for (PotionEffect effect : customEffects) {
            builder.addCustomEffect(effect, true);
        }
    }

    /**
     * Aplicar metadatos de banner al ItemBuilder
     */
    private void applyBannerMetadata(ItemBuilder builder) {
        for (Pattern pattern : bannerPatterns) {
            builder.addBannerPattern(pattern);
        }
    }

    /**
     * Aplicar metadatos de fuego artificial al ItemBuilder
     */
    private void applyFireworkMetadata(ItemBuilder builder) {
        builder.setFireworkPower(fireworkPower);

        for (FireworkEffect effect : fireworkEffects) {
            builder.addFireworkEffect(effect);
        }
    }

    /**
     * Aplicar metadatos de ajolote al ItemBuilder
     */
    private void applyAxolotlMetadata(ItemBuilder builder) {
        if (axolotlVariant != null) {
            builder.setAxolotlVariant(axolotlVariant);
        }
    }

    /**
     * Aplicar metadatos de flecha al ItemBuilder
     */
    private void applyArrowMetadata(ItemBuilder builder) {
        if (arrowPotionType != null) {
            builder.setBasePotionData(new PotionData(arrowPotionType, arrowExtended, arrowUpgraded));
        }

        for (PotionEffect effect : arrowCustomEffects) {
            builder.addCustomEffect(effect, true);
        }
    }

    /**
     * Aplicar metadatos de ballesta al ItemBuilder
     */
    private void applyCrossbowMetadata(ItemBuilder builder) {
        if (!loadedProjectiles.isEmpty()) {
            builder.setChargedProjectiles(loadedProjectiles);
        }
    }

    /**
     * Método estático para procesar una configuración de slots desde un string
     */
    public static List<Integer> parseSlots(String slotConfig) {
        List<Integer> slots = new ArrayList<>();

        // Eliminar caracteres de viñetas si existen
        slotConfig = slotConfig.replaceAll("^\\s*\\*\\s*", "").trim();

        // Verificar si es un rango (contiene un guión)
        if (slotConfig.contains("-")) {
            String[] range = slotConfig.split("-");
            if (range.length == 2) {
                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());

                    // Agregar todos los slots en el rango
                    for (int i = start; i <= end; i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException e) {
                    // Manejar error de formato
                }
            }
        } else {
            // Es un solo número
            try {
                slots.add(Integer.parseInt(slotConfig.trim()));
            } catch (NumberFormatException e) {
                // Manejar error de formato
            }
        }

        return slots;
    }

    /**
     * Método para verificar si un slot específico está asociado con este item
     */
    public boolean containsSlot(int slot) {
        return slots.contains(slot);
    }
}