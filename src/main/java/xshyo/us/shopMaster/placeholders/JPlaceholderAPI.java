package xshyo.us.shopMaster.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import xshyo.us.shopMaster.ShopMaster;

public class JPlaceholderAPI extends PlaceholderExpansion {

    private final ShopMaster plugin = ShopMaster.getInstance();

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    public @NotNull String getIdentifier() {
        return "theitemskin";
    }

    public @NotNull String getAuthor() {
        return "xShyo_";
    }

    public @NotNull String getVersion() {
        PluginDescriptionFile pluginDescription = plugin.getDescription();
        return pluginDescription.getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null || identifier.isEmpty()) {
            return "";
        }


        return "0";
    }

}