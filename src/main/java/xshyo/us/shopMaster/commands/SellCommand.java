package xshyo.us.shopMaster.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import dev.dejvokep.boostedyaml.YamlDocument;
import xshyo.us.shopMaster.enums.TypeService;
import xshyo.us.shopMaster.gui.SellMenu;
import xshyo.us.shopMaster.services.SellService;
import xshyo.us.shopMaster.services.records.SellAllResult;
import xshyo.us.shopMaster.services.records.SellResult;
import xshyo.us.shopMaster.superclass.AbstractCommand;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.utilities.Utils;

public class SellCommand extends AbstractCommand {

    private final ShopMaster plugin;
    private final SellService sellService;

    public SellCommand(ShopMaster plugin, SellService sellService) {
        super("sell", "/sell [all/hand/gui]", "Vende automaticamente");
        this.plugin = plugin;
        this.sellService = sellService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        YamlDocument config = ShopMaster.getInstance().getConf();

        // Check if the main command is enabled
        if (!config.getBoolean("config.command.sell.enabled", true)) {
            PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.DISABLED");
            return true;
        }

        // Check main command permission
        boolean needPermission = config.getBoolean("config.command.sell.need-permissions", true);
        String mainPermission = config.getString("config.command.sell.permission", "shopmaster.sell");

        if (needPermission && !PluginUtils.hasPermission(player, mainPermission)) {
            return true;
        }

        if (args.length == 0) {
            // Default to GUI if no args provided
            if (config.getBoolean("config.command.sell.subcommands.gui.default", true)) {
                openSellGUI(player);
            } else {
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.USAGE");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hand":
                // Check if hand subcommand is enabled
                if (!config.getBoolean("config.command.sell.subcommands.hand.enabled", true)) {
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.HAND.DISABLED");
                    return true;
                }

                // Check hand subcommand permission
                boolean needHandPermission = config.getBoolean("config.command.sell.subcommands.hand.need-permissions", true);
                String handPermission = config.getString("config.command.sell.subcommands.hand.permission", "shopmaster.sell.hand");

                if (needHandPermission && !PluginUtils.hasPermission(player, handPermission)) {
                    return true;
                }

                sellItemInHand(player);
                break;

            case "all":
                // Check if all subcommand is enabled
                if (!config.getBoolean("config.command.sell.subcommands.all.enabled", true)) {
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.ALL.DISABLED");
                    return true;
                }

                // Check all subcommand permission
                boolean needAllPermission = config.getBoolean("config.command.sell.subcommands.all.need-permissions", true);
                String allPermission = config.getString("config.command.sell.subcommands.all.permission", "shopmaster.sell.all");

                if (needAllPermission && !PluginUtils.hasPermission(player, allPermission)) {
                    return true;
                }

                sellAllItems(player);
                break;

            case "gui":
                // Check if GUI subcommand is enabled
                if (!config.getBoolean("config.command.sell.subcommands.gui.enabled", true)) {
                    PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.GUI.DISABLED");
                    return true;
                }

                // Check GUI subcommand permission
                boolean needGUIPermission = config.getBoolean("config.command.sell.subcommands.gui.need-permissions", true);
                String guiPermission = config.getString("config.command.sell.subcommands.gui.permission", "shopmaster.sell.gui");

                if (needGUIPermission && !PluginUtils.hasPermission(player, guiPermission)) {
                    return true;
                }

                openSellGUI(player);
                break;

            default:
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.USAGE");
                break;
        }

        return true;
    }

    private void openSellGUI(Player player) {
        new SellMenu(sellService, player).open();
    }

    private void sellItemInHand(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.HAND.EMPTY");
            return;
        }

        SellResult result = sellService.sellItem(player, itemInHand, itemInHand.getAmount(), false);

        switch (result.status()) {
            case SUCCESS:
                String message = plugin.getConf().getString("config.command.sell.messages.simple");
                message = message.replace("{amount}", "" + itemInHand.getAmount());
                message = message.replace("{item}", PluginUtils.formatItemName(itemInHand.getType()));
                message = message.replace("{earnings}", "" + result.price());
                message = Utils.translate(message);
                player.sendMessage(message);
                player.getInventory().setItemInMainHand(null);
                PluginUtils.sellLog(player.getName(), TypeService.SELL, itemInHand.getAmount(), PluginUtils.formatItemName(itemInHand.getType()), "" + result.price(), result.shopItem().getShopName());

                break;
            case WORLD_BLACKLISTED:
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.WORLD_BLACKLISTED");
                break;
            case GAMEMODE_BLACKLISTED:
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.GAMEMODE_BLACKLISTED");
                break;
            case NOT_SELLABLE:
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.HAND.NOT_SELLABLE");
                break;
            case INVALID_ECONOMY:
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.HAND.INVALID_ECONOMY");
                break;
            case ERROR:
                PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.SELL.HAND.ERROR");
                break;
        }
    }

    private void sellAllItems(Player player) {
        // Llamar al método optimizado en SellManager
        SellAllResult result = sellService.sellAllItems(player);
        result.generateSummaryMessages(player);
    }
}