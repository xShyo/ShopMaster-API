package xshyo.us.shopMaster.commands.args;

import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ArgReload implements CommandArg {
    private static final String PERMISSION_RELOAD = "shopmaster.reload";

    private final ShopMaster shopMaster = ShopMaster.getInstance();

    @Override
    public List<String> getNames() {
        return Collections.singletonList("reload");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return List.of(PERMISSION_RELOAD);
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args) {
        if (!PluginUtils.hasPermission(s, PERMISSION_RELOAD)) return true;
        shopMaster.reload();
        PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.RELOAD");
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
