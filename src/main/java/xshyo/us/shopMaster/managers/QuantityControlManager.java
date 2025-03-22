package xshyo.us.shopMaster.managers;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xshyo.us.theAPI.utilities.Utils;
import xshyo.us.theAPI.utilities.item.ItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor
public class QuantityControlManager {

    private final Gui menu;
    private int currentQuantity;
    private final Function<String, String> placeholderReplacer;
    private final Consumer<Integer> onQuantityChange;
    private final int minQuantity;
    private final int maxQuantity;

    public void setupQuantityControls(Section quantityControlsSection, Set<Integer> reservedSlots) {
        if (quantityControlsSection == null) return;

        for (Object key : quantityControlsSection.getKeys()) {
            String controlKey = key.toString();
            Section controlSection = quantityControlsSection.getSection(controlKey);
            if (controlSection == null) continue;

            int slot = controlSection.getInt("slot", 0);
            String setAmountStr = controlSection.getString("set_amount", "0");

            // Determine if this is an absolute value (set) or a relative change
            boolean isAbsolute = !(setAmountStr.startsWith("+") || setAmountStr.startsWith("-"));
            int changeValue = Integer.parseInt(setAmountStr.startsWith("+") ? setAmountStr.substring(1) : setAmountStr);

            // Check if this control should be displayed based on current quantity limits
            boolean shouldDisplay = true;
            if (isAbsolute) {
                // For absolute values, they should be within min and max range
                shouldDisplay = changeValue >= minQuantity && changeValue <= maxQuantity;
            } else {
                // For relative changes, check if the resulting value would be valid
                int resultingQuantity = currentQuantity + changeValue;
                shouldDisplay = resultingQuantity >= minQuantity && resultingQuantity <= maxQuantity;
            }

            // Only add the control if it should be displayed
            if (shouldDisplay) {
                Material material = Material.valueOf(controlSection.getString("material", "STONE"));
                int amount = controlSection.getInt("amount", 1);
                int modelData = controlSection.getInt("model_data", 0);
                String displayName = controlSection.getString("display_name", "");
                boolean glowing = controlSection.getBoolean("glowing", false);

                List<String> lore = new ArrayList<>();
                for (String line : controlSection.getStringList("lore")) {
                    lore.add(Utils.translate(placeholderReplacer.apply(line)));
                }

                ItemBuilder builder = new ItemBuilder(material)
                        .setAmount(amount)
                        .setName(Utils.translate(placeholderReplacer.apply(displayName)))
                        .setLore(lore);

                if (modelData > 0) {
                    builder.setCustomModelData(modelData);
                }

                if (glowing) {
                    builder.setEnchanted(true);
                }

                List<String> flags = controlSection.getStringList("item_flags");
                builder.addFlagsFromConfig(new HashSet<>(flags));

                // Create the final control button
                ItemStack controlItem = builder.build();
                final int finalChangeValue = changeValue;
                final boolean finalIsAbsolute = isAbsolute;

                reservedSlots.add(slot);
                menu.setItem(slot, new GuiItem(controlItem, event -> {
                    event.setCancelled(true);
                    updateQuantity(finalChangeValue, finalIsAbsolute);
                }));
            } else {
                // Si no se debe mostrar, agregar el slot a reservedSlots para que no sea usado por otro ítem
                reservedSlots.add(slot);
            }
        }
    }

    /**
     * Updates the current quantity based on the specified change value and operation type.
     *
     * @param changeValue The value to add/subtract or set directly
     * @param isAbsolute If true, sets the quantity to the change value directly.
     *                   If false, adds the change value to the current quantity.
     */
    private void updateQuantity(int changeValue, boolean isAbsolute) {
        int newQuantity;

        if (isAbsolute) {
            // Set the quantity directly to the specified value
            newQuantity = changeValue;
        } else {
            // Add (or subtract if negative) the change value to the current quantity
            newQuantity = currentQuantity + changeValue;
        }

        // Ensure the new quantity is within valid range
        newQuantity = Math.max(minQuantity, Math.min(maxQuantity, newQuantity));

        currentQuantity = newQuantity;
        if (onQuantityChange != null) {
            onQuantityChange.accept(newQuantity);
        }
    }
}