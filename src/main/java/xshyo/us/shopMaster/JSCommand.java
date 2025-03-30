package xshyo.us.shopMaster;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import xshyo.us.shopMaster.commands.args.ArgHelp;
import xshyo.us.shopMaster.commands.args.ArgMigrator;
import xshyo.us.shopMaster.commands.args.ArgReload;
import xshyo.us.shopMaster.superclass.AbstractCommand;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSCommand extends AbstractCommand {

    public JSCommand() {
        // Use the command name from config
        super(ShopMaster.getInstance().getConfig().getString("config.command.admin.name"));
        addDefaultArguments();

    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) { // no arguments
            if (s instanceof ConsoleCommandSender) {
                s.sendMessage(ChatColor.RED + "You can only use /" + command + " reload, /" + command + " help from console.");
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
                    s.sendMessage(ChatColor.RED + "You can only use /" + this.command + " reload");
                    return true;
                }
            }
        }

        PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.NO_SUCH_COMMAND", ShopMaster.getInstance().getLang());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
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
                        return ps.tabComplete(sender, label, args);
                    }
                }
            }
        }
        return null;
    }

    public static void addDefaultArguments() {
        ShopMaster.getInstance().getCommandArgs().add(new ArgHelp());
        ShopMaster.getInstance().getCommandArgs().add(new ArgReload());
        ShopMaster.getInstance().getCommandArgs().add(new ArgMigrator());

    }
}