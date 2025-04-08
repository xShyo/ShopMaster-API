package xshyo.us.shopMaster.commands;

import dev.dejvokep.boostedyaml.YamlDocument;
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

    public ShopCommand() {
        super("shop", "/shop [shopName/player] [page/shopName] [player/page]", "Abre el menú de la tienda");
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
        if (!(sender instanceof Player) && !containsPlayerArg(args)) {
            PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.PLAYER_ONLY");
            return true;
        }

        // Caso 1: /shop - Abre el menú principal para el jugador que ejecuta el comando
        if (args.length == 0) {
            Player player = (Player) sender;
            new ShopMainMenu(player).openMenu();
            return true;
        }

        // Caso 2: /shop shopName [page] - Abre una tienda específica para el jugador que ejecuta
        if (args.length >= 1 && isShopName(args[0])) {
            if (!(sender instanceof Player)) {
                PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.PLAYER_ONLY");
                return true;
            }

            Player player = (Player) sender;
            String shopName = args[0];
            int page = 1; // Página predeterminada

            // Verificar permiso específico para esta tienda
            if (needPermissions &&
                    !PluginUtils.hasPermission(player, basePermission + ".all") &&
                    !PluginUtils.hasPermission(player, basePermission + "." + shopName.toLowerCase())) {
                PluginUtils.sendMessage(player, "COMMANDS.NOPERMS", player.getName(), basePermission + "." + shopName.toLowerCase());
                return true;
            }

            // Verificar si se especificó una página
            if (args.length >= 2 && isInteger(args[1])) {
                page = Integer.parseInt(args[1]);
            }

            openSpecificShop(player, shopName, page);
            return true;
        }

        // Caso 3: /shop playerName - Abre el menú principal para el jugador especificado
        if (args.length == 1 && isPlayerName(args[0])) {
            // Verificar permiso para abrir tiendas para otros jugadores
            if (needPermissions && !PluginUtils.hasPermission(sender, basePermission + ".others")) {
                PluginUtils.sendMessage(sender, "COMMANDS.NOPERMS", sender.getName(), basePermission + ".others");
                return true;
            }

            String targetPlayerName = args[0];
            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.PLAYER_OFFLINE", targetPlayerName);
                return true;
            }

            new ShopMainMenu(targetPlayer).openMenu();
            PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.OTHER_SUCCESS_MAIN", targetPlayerName);
            return true;
        }

        // Caso 4: /shop playerName shopName [page] - Abre una tienda específica para el jugador especificado
        if (args.length >= 2 && isPlayerName(args[0])) {
            // Verificar permiso para abrir tiendas para otros jugadores
            if (needPermissions && !PluginUtils.hasPermission(sender, basePermission + ".others")) {
                PluginUtils.sendMessage(sender, "COMMANDS.NOPERMS", sender.getName(), basePermission + ".others");
                return true;
            }

            String targetPlayerName = args[0];
            String shopName = args[1];
            int page = 1; // Página predeterminada

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.PLAYER_OFFLINE", targetPlayerName);
                return true;
            }

            // Verificar si se especificó una página
            if (args.length >= 3 && isInteger(args[2])) {
                page = Integer.parseInt(args[2]);
            }

            openSpecificShop(targetPlayer, shopName, page);
            PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.OTHER_SUCCESS", shopName, targetPlayerName);
            return true;
        }

        PluginUtils.sendMessage(sender, "COMMANDS.SHOP.OPEN.USAGE");
        return true;
    }

    private void openSpecificShop(Player player, String shopName, int page) {
        Shop shop = ShopMaster.getInstance().getShopManager().getShop(shopName);

        if (shop == null) {
            PluginUtils.sendMessage(player, "COMMANDS.SHOP.OPEN.NOT_FOUND", shopName);
            return;
        }

        if (isWorldBlacklisted(player.getWorld().getName())) {
            PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.WORLD_BLACKLISTED");
            return;
        }

        if (isGameModeBlacklisted(player.getGameMode().toString())) {
            PluginUtils.sendMessage(player, "MESSAGES.COMMANDS.SHOP.GAMEMODE_BLACKLISTED");
            return;
        }

        PluginUtils.sendMessage(player, "COMMANDS.SHOP.OPEN.SUCCESS", shopName);
        // Abrir la tienda específica en la página indicada
        new ShopCategoryMenu(player, shop).openMenu(page);
    }

    /**
     * Verifica si un mundo está en la lista negra
     */
    private boolean isWorldBlacklisted(String worldName) {
        YamlDocument config = ShopMaster.getInstance().getConf();
        List<String> blacklistedWorlds = config.getStringList("config.command.shop.black-list.world");
        return blacklistedWorlds.contains(worldName);
    }

    /**
     * Verifica si un modo de juego está en la lista negra
     */
    private boolean isGameModeBlacklisted(String gameMode) {
        YamlDocument config = ShopMaster.getInstance().getConf();
        List<String> blacklistedGameModes = config.getStringList("config.command.shop.black-list.gameModes");
        return blacklistedGameModes.contains(gameMode);
    }

    /**
     * Verifica si un argumento es el nombre de una tienda válida
     */
    private boolean isShopName(String arg) {
        return ShopMaster.getInstance().getShopManager().getShop(arg) != null;
    }

    /**
     * Verifica si un argumento es un nombre de jugador en línea
     */
    private boolean isPlayerName(String arg) {
        return Bukkit.getPlayer(arg) != null;
    }

    /**
     * Verifica si un string puede ser convertido a un entero
     */
    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Verifica si los argumentos contienen un nombre de jugador válido
     */
    private boolean containsPlayerArg(String[] args) {
        if (args.length == 0) return false;
        return isPlayerName(args[0]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ShopManager shopManager = ShopMaster.getInstance().getShopManager();
        if (shopManager == null) {
            return new ArrayList<>(); // Retorna lista vacía si shopManager aún no está disponible
        }

        List<String> completions = new ArrayList<>();

        // Obtener configuración de permisos
        boolean needPermissions = ShopMaster.getInstance().getConf().getBoolean("config.command.shop.need-permissions");
        String basePermission = ShopMaster.getInstance().getConf().getString("config.command.shop.permission");

        // Verificar permiso base para el autocompletado
        if (needPermissions && !PluginUtils.hasPermission(sender, basePermission)) {
            return completions;
        }

        if (args.length == 1) {
            // Sugerencias para primer argumento: nombres de tiendas o jugadores
            String partialName = args[0].toLowerCase();
            boolean hasAllShopsPermission = PluginUtils.hasPermission(sender, basePermission + ".all");

            // Añadir nombres de tiendas
            for (String shopName : ShopMaster.getInstance().getShopManager().getShopMap().keySet()) {
                if (!needPermissions ||
                        hasAllShopsPermission ||
                        PluginUtils.hasPermission(sender, basePermission + "." + shopName.toLowerCase())) {
                    if (shopName.toLowerCase().startsWith(partialName)) {
                        completions.add(shopName);
                    }
                }
            }

            // Añadir nombres de jugadores si tiene permiso para otros
            if (!needPermissions || PluginUtils.hasPermission(sender, basePermission + ".others")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 2) {
            // Segundo argumento: depende del primer argumento
            String firstArg = args[0];
            String partialName = args[1].toLowerCase();

            if (isPlayerName(firstArg)) {
                // Si el primer argumento es un jugador, sugerir tiendas
                for (String shopName : ShopMaster.getInstance().getShopManager().getShopMap().keySet()) {
                    if (shopName.toLowerCase().startsWith(partialName)) {
                        completions.add(shopName);
                    }
                }
            } else if (isShopName(firstArg)) {
                // Si el primer argumento es una tienda, sugerir páginas (1-5 como ejemplo)
                for (int i = 1; i <= 5; i++) {
                    if (String.valueOf(i).startsWith(partialName)) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 3) {
            // Tercer argumento: página (si los dos primeros son jugador y tienda)
            if (isPlayerName(args[0]) && isShopName(args[1])) {
                String partialPage = args[2];
                // Sugerir páginas 1-5 como ejemplo
                for (int i = 1; i <= 5; i++) {
                    if (String.valueOf(i).startsWith(partialPage)) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        }

        return completions;
    }
}