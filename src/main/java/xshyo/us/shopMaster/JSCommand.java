package xshyo.us.shopMaster;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import xshyo.us.shopMaster.commands.args.ArgHelp;
import xshyo.us.shopMaster.commands.args.ArgInfo;
import xshyo.us.shopMaster.commands.args.ArgReload;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSCommand extends Command {

    JSCommand(String name) {
        super(name);
    }

    static void addDefaultArguments() {
        ShopMaster.getInstance().getCommandArgs().add(new ArgHelp());
        ShopMaster.getInstance().getCommandArgs().add(new ArgInfo());
        ShopMaster.getInstance().getCommandArgs().add(new ArgReload());


    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> l = new ArrayList<>();
            for (CommandArg ps : ShopMaster.getInstance().getCommandArgs()) {
                boolean hasPerm = false;
                if (ps.getPermissionsToExecute() == null) {
                    hasPerm = true;
                } else {
                    for (String perm : ps.getPermissionsToExecute()) {
                        if (sender.hasPermission(perm)) {
                            hasPerm = true;
                            break;
                        }
                    }
                }
                if (hasPerm) l.addAll(ps.getNames());
            }
            return StringUtil.copyPartialMatches(args[0], l, new ArrayList<>());
        } else if (args.length >= 2) {
            for (CommandArg ps : ShopMaster.getInstance().getCommandArgs()) {
                for (String arg : ps.getNames()) {
                    if (arg.equalsIgnoreCase(args[0])) {
                        return ps.tabComplete(sender, alias, args);
                    }
                }
            }
        }
        return null;
    }


    @Override
    public boolean execute(@NotNull CommandSender s, @NotNull String label, String[] args) {
        String cmd = ShopMaster.getInstance().getConfig().getString("config.command.admin.name");

        if (args.length == 0) { // no arguments
            if (s instanceof ConsoleCommandSender) {
                s.sendMessage(ChatColor.RED + "You can only use /" + cmd + " reload, /" + cmd + " help from console.");
            } else {
                new ArgHelp().executeArgument(s, args);
            }
            return true;
        }
        for (CommandArg command : ShopMaster.getInstance().getCommandArgs()) {
            if (command.getNames().contains(args[0])) {
                if (command.allowNonPlayersToExecute() || s instanceof Player) {
                    List<String> nArgs = new ArrayList<>(Arrays.asList(args));
                    return command.executeArgument(s, nArgs.toArray(new String[0]));

                } else if (!command.allowNonPlayersToExecute()) {
                    s.sendMessage(ChatColor.RED + "You can only use /" + cmd + " reload");
                    return true;
                }
            }
        }

        PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.NO_SUCH_COMMAND", ShopMaster.getInstance().getLang());
        return true;
    }
}
