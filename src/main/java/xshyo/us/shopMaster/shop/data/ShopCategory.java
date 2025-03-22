package xshyo.us.shopMaster.shop.data;

import lombok.Getter;

import java.util.List;

@Getter
public class ShopCategory {
    private final String material;
    private final List<Integer> slots;
    private final int amount;
    private final int modelData;
    private final String displayName;
    private final boolean glowing;
    private final List<String> itemFlags;
    private final List<String> lore;

    public ShopCategory(
            String material,
            List<Integer> slots,
            int amount,
            int modelData,
            String displayName,
            boolean glowing,
            List<String> itemFlags,
            List<String> lore
    ) {
        this.material = material;
        this.slots = slots;
        this.amount = amount;
        this.modelData = modelData;
        this.displayName = displayName;
        this.glowing = glowing;
        this.itemFlags = itemFlags;
        this.lore = lore;
    }
}
