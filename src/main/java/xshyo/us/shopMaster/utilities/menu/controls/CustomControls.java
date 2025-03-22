package xshyo.us.shopMaster.utilities.menu.controls;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.utilities.menu.Controls;
import xshyo.us.theAPI.utilities.Utils;
import java.util.List;

@AllArgsConstructor
public class CustomControls extends Controls {
    private final ShopMaster plugin = ShopMaster.getInstance();
    private final String path;


    @Override
    public ItemStack getButtonItem(Player player) {
        Section itemConfig = plugin.getConf().getSection(path);
        return buildItem(itemConfig, player);
    }


    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        Section actionsConfig = plugin.getConf().getSection(path);
        if (actionsConfig != null) {
            List<String> actions = actionsConfig.getStringList(".actions");
            executeActions(actions, player);
        }
    }


    private void executeActions(List<String> actions, Player player) {
        for (String action : actions) {
            action = action.trim();
            String[] parts = action.split("\\s+", 2);  // Dividir en dos partes, el tipo de acción y el resto
            String actionType = parts[0].toLowerCase();
            String actionData = (parts.length > 1) ? parts[1] : "";

            if (actionType.startsWith("[chance=")) {  // Verificar si la acción tiene probabilidad definida
                if (Utils.shouldExecuteAction(actionType)) {
                    plugin.getActionExecutor().executeAction(player, actionData);
                }
            } else {
                plugin.getActionExecutor().executeAction(player, action);
            }
        }

    }


}
