package xshyo.us.shopMaster.shop.data;


import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.MusicInstrument;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShopItem {


    public ShopItem(String shopName, String material, int amount, int slot, int page, String economy, double buyPrice, double sellPrice) {

    }


    public ShopItem(String shopName, String material, int amount, List<Integer> slots, int page, String economy, double buyPrice, double sellPrice) {

    }

    public ShopItem(String shopName, String material, int amount, int startSlot, int endSlot, int page, String economy, double buyPrice, double sellPrice) {

    }


    public ItemStack createItemStack() {
        return null;
    }


    public String getShopName() {
        return null;
    }

    public void setShopName(String shopName) {
    }

    public String getMaterial() {
        return null;
    }

    public void setMaterial(String material) {
    }

    public int getAmount() {
        return 0;
    }

    public int getModelData() {
        return 0;
    }

    public List<Integer> getSlots() {
        return null;
    }

    public int getPage() {
        return 0;
    }

    public String getEconomy() {
        return null;
    }

    public double getBuyPrice() {
        return -1.0;
    }

    public double getSellPrice() {
        return -1.0;
    }

    public String getDisplayName() {
        return null;
    }

    public void setDisplayName(String displayName) {
    }

    public List<String> getLore() {
        return null;
    }

    public void setLore(List<String> lore) {

    }

    public Map<Enchantment, Integer> getEnchantments() {
        return null;
    }

    public PotionType getPotionType() {
        return null;
    }

    public void setPotionType(PotionType potionType) {
    }

    public boolean isExtended() {
        return false;
    }

    public void setExtended(boolean extended) {
    }

    public boolean isUpgraded() {
        return false;
    }

    public void setUpgraded(boolean upgraded) {
    }

    public List<PotionEffect> getCustomEffects() {
        return null;
    }

    public void setCustomEffects(List<PotionEffect> customEffects) {
    }

    public List<Pattern> getBannerPatterns() {
        return null;
    }

    public void setBannerPatterns(List<Pattern> bannerPatterns) {
    }

    public int getFireworkPower() {
        return 0;
    }

    public void setFireworkPower(int fireworkPower) {
    }

    public List<FireworkEffect> getFireworkEffects() {
        return null;
    }

    public void setFireworkEffects(List<FireworkEffect> fireworkEffects) {
    }

    public Axolotl.Variant getAxolotlVariant() {
        return null;
    }

    public void setAxolotlVariant(Axolotl.Variant axolotlVariant) {
    }

    public PotionType getArrowPotionType() {
        return null;
    }

    public void setArrowPotionType(PotionType arrowPotionType) {
    }

    public boolean isArrowExtended() {
        return false;
    }

    public void setArrowExtended(boolean arrowExtended) {
    }

    public boolean isArrowUpgraded() {
        return false;
    }

    public void setArrowUpgraded(boolean arrowUpgraded) {
    }

    public List<PotionEffect> getArrowCustomEffects() {
        return null;
    }

    public void setArrowCustomEffects(List<PotionEffect> arrowCustomEffects) {
    }

    public List<ItemStack> getLoadedProjectiles() {
        return null;
    }

    public void setLoadedProjectiles(List<ItemStack> loadedProjectiles) {
    }

    public String getSpawnerMobType() {
        return null;
    }

    public void setSpawnerMobType(String spawnerMobType) {
    }

    public MusicInstrument getMusicInstrument() {
        return null;
    }

    public void setMusicInstrument(MusicInstrument musicInstrument) {
    }

    public DyeColor getShieldBaseColor() {
        return null;
    }

    public void setShieldBaseColor(DyeColor shieldBaseColor) {
    }

    public List<Pattern> getShieldPatterns() {
        return null;
    }

    public void setShieldPatterns(List<Pattern> shieldPatterns) {
    }

    public String getArmorColor() {
        return null;
    }

    public void setArmorColor(String armorColor) {
    }

    public TrimPattern getArmorTrimPattern() {
        return null;
    }

    public void setArmorTrimPattern(TrimPattern armorTrimPattern) {
    }

    public TrimMaterial getArmorTrimMaterial() {
        return null;
    }

    public void setArmorTrimMaterial(TrimMaterial armorTrimMaterial) {
    }

    public boolean isHidden() {
        return false;
    }

    public void setHidden(boolean hidden) {
    }

    public String getNbtData() {
        return null;
    }

    public void setNbtData(String nbtData) {
    }

    public List<String> getBuyCommands() {
        return null;
    }

    public void setBuyCommands(List<String> buyCommands) {
    }

    public List<String> getSellCommands() {
        return null;
    }

    public void setSellCommands(List<String> sellCommands) {
    }

    public Set<String> getItemFlags() {
        return null;
    }

    public void setItemFlags(Set<String> itemFlags) {
    }


}