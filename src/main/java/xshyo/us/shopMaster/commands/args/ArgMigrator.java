package xshyo.us.shopMaster.commands.args;

import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.superclass.ShopMigrator;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ArgMigrator implements CommandArg {
    private static final String PERMISSION_MIGRATE = "shopmaster.migrator";

    private final ShopMaster shopMaster = ShopMaster.getInstance();

    // Lista de migradores disponibles
    private final List<String> availableMigrators = Arrays.asList("shopguiplus", "economyShopGUIFree", "economyShopGUIPremium");

    @Override
    public List<String> getNames() {
        return Collections.singletonList("migrator");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return List.of(PERMISSION_MIGRATE);
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args) {
        if (!PluginUtils.hasPermission(s, PERMISSION_MIGRATE)) return true;

        // Si no hay argumentos adicionales o si el argumento no es un migrador válido
        if (args.length < 2 || !availableMigrators.contains(args[1].toLowerCase())) {
            PluginUtils.sendMessage(s, "&cCorrect use: /shopmaster migrator <type>");
            PluginUtils.sendMessage(s, "&cMigrators available: " + String.join(", ", availableMigrators));
            return true;
        }

        String migratorType = args[1].toLowerCase();
        PluginUtils.sendMessage(s, "&aMigrating stores from " + migratorType + "...");

        ShopMigrator migrator = createMigrator(migratorType);
        boolean success = migrator.migrateShops();

        if (success) {
            PluginUtils.sendMessage(s, "&aMigration successfully completed.");
        } else {
            PluginUtils.sendMessage(s, "&cError during migration. Check the console for more details.");
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 2) {
            String currentInput = args[1].toLowerCase();
            return availableMigrators.stream()
                    .filter(migrator -> migrator.startsWith(currentInput))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private ShopMigrator createMigrator(String type) {
        // Crear el migrador apropiado según el tipo
        switch (type) {
            case "shopguiplus":
                return new ShopMigrator(shopMaster); // Asumo que el actual es para ShopGUIPlus
            default:
                return new ShopMigrator(shopMaster);
        }
    }
}