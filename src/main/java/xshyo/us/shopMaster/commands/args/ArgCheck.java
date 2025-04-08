package xshyo.us.shopMaster.commands.args;

import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ArgCheck implements CommandArg {
    private static final String PERMISSION_CHECK = "shopmaster.check";

    private final ShopMaster shopMaster = ShopMaster.getInstance();

    @Override
    public List<String> getNames() {
        return Collections.singletonList("check");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return List.of(PERMISSION_CHECK);
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args) {
        if (!PluginUtils.hasPermission(s, PERMISSION_CHECK)) return true;



        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
