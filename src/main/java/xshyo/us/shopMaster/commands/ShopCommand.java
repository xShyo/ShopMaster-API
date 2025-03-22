package xshyo.us.shopMaster.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.gui.ShopCategoryMenu;
import xshyo.us.shopMaster.gui.ShopMainMenu;
import xshyo.us.shopMaster.managers.ShopManager;
import xshyo.us.shopMaster.shop.Shop;
import xshyo.us.shopMaster.superclass.AbstractCommand;
import xshyo.us.shopMaster.utilities.PluginUtils;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand extends AbstractCommand {

    private final ShopManager shopManager;

    public ShopCommand() {
        super("shop", "/shop [shopName] [player]", "Abre el menú de la tienda");
        this.shopManager = ShopMaster.getInstance().getShopManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Verificación del permiso básico del comando
        boolean needPermissions = ShopMaster.getInstance().getConf().getBoolean("config.command.shop.need-permissions");
        String basePermission = ShopMaster.getInstance().getConf().getString("config.command.shop.permission");

        if (needPermissions && !PluginUtils.hasPermission(sender, basePermission)) {
            return true;
        }

        // Si no es un jugador y no especifica un jugador objetivo
        if (!(sender instanceof Player) && (args.length < 2)) {
            PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.PLAYER_ONLY");
            return true;
        }

        // Caso 1: /shop - Abre el menú principal para el jugador que ejecuta el comando
        if (args.length == 0) {
            Player player = (Player) sender;
            new ShopMainMenu(player).openMenu();
            return true;
        }

        // Caso 2: /shop nameShop - Abre una tienda específica para el jugador que ejecuta
        if (args.length == 1) {
            Player player = (Player) sender;
            String shopName = args[0];

            // Verificar permiso específico para esta tienda
            if (needPermissions &&
                    !PluginUtils.hasPermission(player, basePermission + ".all") &&
                    !PluginUtils.hasPermission(player, basePermission + "." + shopName.toLowerCase())) {
                PluginUtils.sendMessage(player, "COMMANDS.NOPERMS", player.getName(), basePermission + "." + shopName.toLowerCase());
                return true;
            }

            openSpecificShop(player, shopName);
            return true;
        }

        // Caso 3: /shop nameShop player - Abre una tienda específica para el jugador especificado
        if (args.length == 2) {
            // Verificar permiso para abrir tiendas para otros jugadores
            if (needPermissions && !PluginUtils.hasPermission(sender, basePermission + ".others")) {
                PluginUtils.sendMessage(sender, "COMMANDS.NOPERMS", sender.getName(), basePermission + ".others");
                return true;
            }

            String shopName = args[0];
            String targetPlayerName = args[1];
            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.PLAYER_OFFLINE", targetPlayerName);
                return true;
            }

            openSpecificShop(targetPlayer, shopName);
            PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.OTHER_SUCCESS", shopName, targetPlayerName);
            return true;
        }

        PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.USAGE");
        return true;
    }

    private void openSpecificShop(Player player, String shopName) {
        Shop shop = shopManager.getShop(shopName);

        if (shop == null) {
            PluginUtils.sendMessage(player, "COMMANDS.SHOP.OPEN.NOT_FOUND", shopName);
            return;
        }

        PluginUtils.sendMessage(player, "COMMANDS.SHOP.OPEN.SUCCESS", shopName);
        // Asumiendo que tienes una clase ShopMenu que puede mostrar una tienda específica
        new ShopCategoryMenu(player, shop).openMenu(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        // Obtener configuración de permisos
        boolean needPermissions = ShopMaster.getInstance().getConf().getBoolean("config.command.shop.need-permissions");
        String basePermission = ShopMaster.getInstance().getConf().getString("config.command.shop.permission");

        // Verificar permiso base para el autocompletado
        if (needPermissions && !PluginUtils.hasPermission(sender, basePermission)) {
            return completions;
        }

        if (args.length == 1) {
            // Sugerencias basadas en tiendas reales del ShopManager
            String partialName = args[0].toLowerCase();
            boolean hasAllShopsPermission = PluginUtils.hasPermission(sender, basePermission + ".all");

            for (String shopName : shopManager.getShopMap().keySet()) {
                // Solo mostrar tiendas para las que el jugador tiene permiso
                if (!needPermissions ||
                        hasAllShopsPermission ||
                        PluginUtils.hasPermission(sender, basePermission + "." + shopName.toLowerCase())) {
                    if (shopName.toLowerCase().startsWith(partialName)) {
                        completions.add(shopName);
                    }
                }
            }
        } else if (args.length == 2) {
            // Verificar permiso para abrir tiendas para otros jugadores
            if (!needPermissions || PluginUtils.hasPermission(sender, basePermission + ".others")) {
                // Sugerencias para nombres de jugadores en línea
                String partialName = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}