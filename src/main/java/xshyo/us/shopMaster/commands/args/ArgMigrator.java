package xshyo.us.shopMaster.commands.args;

import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;
import xshyo.us.shopMaster.ShopMaster;
import xshyo.us.shopMaster.superclass.ShopMigrator;
import xshyo.us.shopMaster.utilities.PluginUtils;
import xshyo.us.theAPI.commands.CommandArg;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ArgMigrator implements CommandArg {
    private static final String PERMISSION_MIGRATE = "shopmaster.migrator";

    private final ShopMaster shopMaster = ShopMaster.getInstance();

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
        PluginUtils.sendMessage(s, "&aMigrando tiendas desde ShopGUIPlus...");

        ShopMigrator migrator = new ShopMigrator(shopMaster);
        boolean success = migrator.migrateShops();

        if (success) {
            PluginUtils.sendMessage(s, "&aMigración completada exitosamente.");
        } else {
            PluginUtils.sendMessage(s, "&cError durante la migración. Revisa la consola para más detalles.");
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
