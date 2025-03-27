package xshyo.us.shopMaster.commands.args;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;
import xshyo.us.theAPI.utilities.Utils;
import java.util.Collections;
import java.util.List;

public class ArgHelp implements CommandArg {
    private static final String PERMISSION_HELP = "shopmaster.help";
    private final ShopMaster shopMaster = ShopMaster.getInstance();

    @Override
    public List<String> getNames() {
        return Collections.singletonList("help");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return List.of(PERMISSION_HELP);
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args) {
        if (isPlayer(s)) {
            if (!PluginUtils.hasPermission(s, PERMISSION_HELP)) return true;
            if (args.length < 2) {
                TextComponent textComponent = getHelpCommandPagination(1);
                if (textComponent != null)
                    s.spigot().sendMessage(textComponent);
                return true;
            }

            if (isNumber(args[1])) {
                int page = Integer.parseInt(args[1]);

                if (page <= 0) {
                    PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.HELP_USAGE");
                    return true;
                }

                TextComponent textComponent = getHelpCommandPagination(page);
                if(textComponent != null){
                    s.spigot().sendMessage(textComponent);
                }else{
                    System.out.println("TextComponent null");
                }
            } else {
                PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.HELP_USAGE");
            }

        } else {
            PluginUtils.sendMessage(s, "MESSAGES.COMMANDS.HELP_CONSOLE");
        }

        return true;
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }


    private TextComponent getHelpCommandPagination(int page) {
        List<String> helpMessages = shopMaster.getLang().getStringList("MESSAGES.COMMANDS.HELP");
        int totalPages = (int) Math.ceil((double) helpMessages.size() / 10.0);
        if (page > totalPages || page <= 0) {
            return null;
        }

        String cmdShop = ShopMaster.getInstance().getConf().getString("config.command.shop.name");
        String cmdAdmin = ShopMaster.getInstance().getConf().getString("config.command.admin.name");
        String cmdSell = ShopMaster.getInstance().getConf().getString("config.command.sell.name");


        int startIndex = (page - 1) * 10;
        int endIndex = Math.min(startIndex + 10, helpMessages.size());
        TextComponent helpMessage = new TextComponent("");
        PluginDescriptionFile pluginDescription = shopMaster.getDescription();
        String version = pluginDescription.getVersion();
        TextComponent title = new TextComponent("======= Help Page " + page + " / " + totalPages + " =======\n ShopMaster " + version + "\n \n");
        title.setColor(ChatColor.GOLD.asBungee());
        helpMessage.addExtra(title);
        for (int i = startIndex; i < endIndex; ++i) {
            String message = helpMessages.get(i);

            message = message.replace("{shop}", cmdShop);
            message = message.replace("{admin}", cmdAdmin);
            message = message.replace("{sell}", cmdSell);

            TextComponent line = new TextComponent(Utils.translate(message) + "\n");
            line.setColor(ChatColor.WHITE.asBungee());
            helpMessage.addExtra(line);
        }
        TextComponent navButtons = new TextComponent("");
        if (page > 1) {
            TextComponent backButton = new TextComponent("<< Previous ");
            backButton.setColor(ChatColor.RED.asBungee());
            backButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + cmdAdmin + " help " + (page - 1)));
            navButtons.addExtra(backButton);
        }
        if (page < totalPages) {
            TextComponent nextButton = new TextComponent("Next >>");
            nextButton.setColor(ChatColor.GREEN.asBungee());
            nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + cmdAdmin + " help " + (page + 1)));
            if (navButtons.getExtra() != null && !navButtons.getExtra().isEmpty()) {
                TextComponent space = new TextComponent(" ");
                navButtons.addExtra(space);
            }
            navButtons.addExtra(nextButton);
        }
        if (navButtons.getExtra() != null && !navButtons.getExtra().isEmpty()) {
            helpMessage.addExtra("\n");
            helpMessage.addExtra(navButtons);
        }
        return helpMessage;
    }

}
