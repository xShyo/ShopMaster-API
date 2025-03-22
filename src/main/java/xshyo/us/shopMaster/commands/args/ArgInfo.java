package xshyo.us.shopMaster.commands.args;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;
import xshyo.us.theAPI.utilities.Utils;
import java.util.*;

@AllArgsConstructor
public class ArgInfo implements CommandArg {

    private static final String PERMISSION_INFO = "theitemskin.info";
    private final ShopMaster shopMaster = ShopMaster.getInstance();

    @Override
    public List<String> getNames() {
        return Collections.singletonList("info");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList(PERMISSION_INFO);
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args) {

        if (args.length < 2) {
            PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.INFO_USAGE");
            return true;
        }

        if (!PluginUtils.hasPermission(s, PERMISSION_INFO)) return true;

        String playerName = args[1];


        return true;
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
    }
}