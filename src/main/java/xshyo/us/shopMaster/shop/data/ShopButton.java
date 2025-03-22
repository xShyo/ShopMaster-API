package xshyo.us.shopMaster.shop.data;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * @param slots Cambiado de int slot a List<Integer> slots
 */
public record ShopButton(String id, boolean enabled, String material, List<Integer> slots, int amount, int modelData,
                         String displayName, boolean glowing, List<String> itemFlags, List<String> lore) {
    // Cambiado de int slot a List<Integer> slots

    public static ShopButton fromConfig(Section buttonSection) {
        if (buttonSection == null) {
            return null;
        }

        boolean enabled = buttonSection.getBoolean("enabled", true);
        if (!enabled) {
            return null;
        }

        String materialName = buttonSection.getString("material", "BARRIER");

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
            slots.addAll(parseSlots(slotConfig));
        }
        // Caso 3: slots como lista
        else if (buttonSection.contains("slots")) {
            // Si es una lista en el config
            if (buttonSection.isList("slots")) {
                List<String> slotStrings = buttonSection.getStringList("slots");
                for (String slotString : slotStrings) {
                    slots.addAll(parseSlots(slotString));
                }
            }
            // Si es un string con formato especial
            else if (buttonSection.isString("slots")) {
                String slotsConfig = buttonSection.getString("slots");
                slots.addAll(parseSlots(slotsConfig));
            }
        } else {
            // Valor por defecto si no se especifica ningún slot
            slots.add(0);
        }

        int amount = buttonSection.getInt("amount", 1);
        int modelData = buttonSection.getInt("model_data", 0);
        String displayName = buttonSection.getString("display_name", "");
        boolean glowing = buttonSection.getBoolean("glowing", false);

        List<String> itemFlagsStr = buttonSection.getStringList("item_flags");
        List<String> lore = buttonSection.getStringList("lore");

        return new ShopButton(
                buttonSection.getRouteAsString(),
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
    }

    // Método para parsear los slots (puedes moverlo a una clase utilitaria si lo prefieres)
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
                    System.err.println("Error al parsear el rango de slots: " + slotConfig);
                }
            }
        } else {
            // Es un solo número
            try {
                slots.add(Integer.parseInt(slotConfig.trim()));
            } catch (NumberFormatException e) {
                // Manejar error de formato
                System.err.println("Error al parsear el slot: " + slotConfig);
            }
        }

        return slots;
    }
}