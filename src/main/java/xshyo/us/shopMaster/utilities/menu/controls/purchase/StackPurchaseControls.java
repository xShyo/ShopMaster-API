package xshyo.us.shopMaster.utilities.menu.controls.purchase;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.utilities.menu.Controls;

@AllArgsConstructor
public class StackPurchaseControls extends Controls {

    private final ShopMaster plugin = ShopMaster.getInstance();
    private final Section section;


    @Override
    public ItemStack getButtonItem(Player player) {
        return buildItem(section, player);
    }

}
