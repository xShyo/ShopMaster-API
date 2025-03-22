package xshyo.us.shopMaster.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.managers.SellManager;
import dev.dejvokep.boostedyaml.YamlDocument;
import xshyo.us.shopMaster.superclass.AbstractCommand;

import java.util.*;

public class SellCommand extends AbstractCommand {

    private final ShopMaster plugin;
    private final SellManager sellManager;

    public SellCommand(ShopMaster plugin, SellManager sellManager) {
        super("sell", "/sell [all/hand]", "Vende automaticamente");
        this.plugin = plugin;
        this.sellManager = sellManager;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        YamlDocument config = ShopMaster.getInstance().getConf();

        // Check if the main command is enabled
        if (!config.getBoolean("config.command.sell.enabled", true)) {
            player.sendMessage("&cEste comando está desactivado.");
            return true;
        }

        // Check main command permission
        boolean needPermission = config.getBoolean("config.command.sell.need-permissions", true);
        String mainPermission = config.getString("config.command.sell.permission", "shopmaster.sell");

        if (needPermission && !player.hasPermission(mainPermission)) {
            player.sendMessage("&cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("&cUso: /sell <hand|all>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hand":
                // Check if hand subcommand is enabled
                if (!config.getBoolean("config.command.sell.subcommands.hand.enabled", true)) {
                    player.sendMessage("&cEste subcomando está desactivado.");
                    return true;
                }

                // Check hand subcommand permission
                boolean needHandPermission = config.getBoolean("config.command.sell.subcommands.hand.need-permissions", true);
                String handPermission = config.getString("config.command.sell.subcommands.hand.permission", "shopmaster.sell.hand");

                if (needHandPermission && !player.hasPermission(handPermission)) {
                    player.sendMessage("&cNo tienes permiso para vender el ítem en tu mano.");
                    return true;
                }

                sellItemInHand(player);
                break;

            case "all":
                // Check if all subcommand is enabled
                if (!config.getBoolean("config.command.sell.subcommands.all.enabled", true)) {
                    player.sendMessage("&cEste subcomando está desactivado.");
                    return true;
                }

                // Check all subcommand permission
                boolean needAllPermission = config.getBoolean("config.command.sell.subcommands.all.need-permissions", true);
                String allPermission = config.getString("config.command.sell.subcommands.all.permission", "shopmaster.sell.all");

                if (needAllPermission && !player.hasPermission(allPermission)) {
                    player.sendMessage("&cNo tienes permiso para vender todos los ítems.");
                    return true;
                }

                sellAllItems(player);
                break;

            default:
                player.sendMessage("&cUso: /sell <hand|all>");
                break;
        }

        return true;
    }

    private void sellItemInHand(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage("&cNo tienes ningún ítem en tu mano.");
            return;
        }

        SellManager.SellResult result = sellManager.sellItem(player, itemInHand, itemInHand.getAmount());

        switch (result.status()) {
            case SUCCESS:
                player.sendMessage("&aHas vendido &e" + itemInHand.getAmount() + "x " +
                        formatItemName(itemInHand.getType()) + " &apor &e$" + result.price());
                player.getInventory().setItemInMainHand(null);
                break;
            case NOT_SELLABLE:
                player.sendMessage("&cEste ítem no se puede vender.");
                break;
            case ERROR:
                player.sendMessage("&cHa ocurrido un error al vender el ítem.");
                break;
        }
    }


    private void sellAllItems(Player player) {
        // Llamar al método optimizado en SellManager
        SellManager.SellAllResult result = sellManager.sellAllItems(
                player,
                this::formatItemName
        );

        // Mostrar el resumen de venta
        List<String> messages = result.generateSummaryMessages(this::formatItemName);
        for (String message : messages) {
            player.sendMessage(message);
        }
    }



    private String formatItemName(Material material) {
        String name = material.toString();
        name = name.replace("_", " ").toLowerCase();

        // Capitalizar las palabras
        StringBuilder formattedName = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                formattedName.append(c);
            } else if (capitalizeNext) {
                formattedName.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formattedName.append(c);
            }
        }

        return formattedName.toString();
    }


}