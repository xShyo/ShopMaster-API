package xshyo.us.shopMaster.utilities.menu.controls;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.utilities.menu.Controls;

import java.util.List;

public class CustomControls extends Controls {


    @Override
    public ItemStack getButtonItem(Player player) {
        return null;
    }


    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {

    }


    private void executeActions(List<String> actions, Player player) {

    }


}
