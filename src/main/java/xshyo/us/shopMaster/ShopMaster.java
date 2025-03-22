package xshyo.us.shopMaster;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import xshyo.us.shopMaster.commands.SellCommand;
import xshyo.us.shopMaster.managers.SellManager;
import xshyo.us.shopMaster.superclass.AbstractCommand;
import xshyo.us.shopMaster.commands.ShopCommand;
import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.placeholders.JPlaceholderAPI;
import xshyo.us.shopMaster.managers.ShopManager;
import xshyo.us.shopMaster.superclass.CurrencyManager;
import xshyo.us.theAPI.TheAPI;
import xshyo.us.theAPI.utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

@Getter
public final class ShopMaster extends TheAPI {

    @Getter
    private static ShopMaster instance;
    private YamlDocument conf, lang, layouts;
    private ShopManager shopManager;
    private SellManager sellManager;

    private final HashMap<CurrencyType, CurrencyManager> currencyMap;
    private Economy economy;

    public ShopMaster() {
        instance = this;
        this.currencyMap = new HashMap<>();
    }


    @Override
    public void load() {

    }

    @Override
    public void start() {
        long startTime = System.nanoTime();
        Hooks();

        createFolder();

        AbstractCommand.enable(); // Habilitar el sistema de comandos

        try {
            getLogger().log(Level.INFO, "Checking for existing files...");

            File dataFolder = getDataFolder();
            File shopFolder = new File(dataFolder, "shops");

            File[] filesInDataFolder = shopFolder.listFiles();
            boolean hasFilesInDataFolder = filesInDataFolder != null && filesInDataFolder.length > 0;

            if (hasFilesInDataFolder) {
                getLogger().log(Level.INFO, "There are existing files, no need to create tools.yml.");
            } else {
                createDefaultShopFile(shopFolder);
                getLogger().log(Level.INFO, "tools successfully created!");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error checking or creating tools: " + e.getMessage());
        }


        this.shopManager = new ShopManager();
        this.shopManager.load();
        this.sellManager = new SellManager(this, shopManager);
        new SellCommand(this, sellManager).register(); // Registrar el comando


        logPluginEnabled(startTime);

    }

    public boolean setupVaultEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Skipping..");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Failed to get VAULT economy provider.");
            return false;
        }
        economy = rsp.getProvider();
        if (economy == null) {
            getLogger().warning("Failed to retrieve VAULT economy instance.");
            return false;
        }
        return true;
    }


    public void reload() {
        try {
            lang.reload();
            conf.reload();
            layouts.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        shopManager.load();
        sellManager.reload();
        reloadConfig();
    }


    private void createFolder() {
        File pluginFolder = getDataFolder();
        File folder = new File(pluginFolder, "shops");
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                getLogger().info("Folder '" + "shops" + "' successfully created.");
            } else {
                getLogger().warning("The folder could not be created '" + "shops" + "'.");
            }
        }
    }

    private void logPluginEnabled(long startTime) {
        Bukkit.getConsoleSender().sendMessage(Utils.translate("&2[ShopMaster] Server version: " + Bukkit.getServer().getVersion() + " " + Bukkit.getServer().getBukkitVersion()));
        Bukkit.getConsoleSender().sendMessage(Utils.translate("&2[ShopMaster] Done and enabled in %time%ms".replace("%time%", nanosToMillis(System.nanoTime() - startTime))));
    }

    private static final DecimalFormat NUMBER_FORMAT_NANO = new DecimalFormat("0.00");

    public static String nanosToMillis(long paramLong) {
        return NUMBER_FORMAT_NANO.format(paramLong / 1000000.0D);
    }


    public void Hooks() {
        getLogger().log(Level.INFO, "Registering Hooks...");
        PluginManager pm = Bukkit.getServer().getPluginManager();
        Plugin placeholderAPIPlugin = pm.getPlugin("PlaceholderAPI");
        if (placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled()) {
            (new JPlaceholderAPI()).register();
            getLogger().log(Level.INFO, "Hooked onto PlaceholderAPI");
        } else {
            getLogger().log(Level.WARNING, "PlaceholderAPI not found! Not enabling placeholders! Download and install it from https://www.spigotmc.org/resources/6245/");
        }


        getLogger().log(Level.INFO, "Registering currency hooks...");

        CurrencyManager currencyManager3 = CurrencyManager.initializeManager("PLAYER_POINTS", null);
        if (currencyManager3 != null) {
            this.currencyMap.put(CurrencyType.PLAYER_POINTS, currencyManager3);
            Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency PLAYER_POINTS successfully loaded!"));
        }

        CurrencyManager currencyManager2 = CurrencyManager.initializeManager("VAULT", null);
        if (currencyManager2 != null) {
            this.currencyMap.put(CurrencyType.VAULT, currencyManager2);
            Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency VAULT successfully loaded!"));
        }

        CurrencyManager currencyManager4 = CurrencyManager.initializeManager("TOKEN_MANAGER", null);
        if (currencyManager4 != null) {
            this.currencyMap.put(CurrencyType.TOKEN_MANAGER, currencyManager4);
            Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency TOKEN_MANAGER successfully loaded!"));

        }

        CurrencyManager currencyManager5 = CurrencyManager.initializeManager("BEAST_TOKENS", null);
        if (currencyManager5 != null) {
            this.currencyMap.put(CurrencyType.BEAST_TOKENS, currencyManager5);
            Bukkit.getConsoleSender().sendMessage(Utils.translate("&a[ShopMaster] Currency BEAST_TOKENS successfully loaded!"));
        }
    }

    @Override
    public void stop() {
        this.getScheduler().cancelTasks(this);
        AbstractCommand.removePluginCommands(this); // Eliminar comandos al deshabilitar el plugin

    }

    @Override
    public void setupListener() {

    }

    @Override
    public void setupCommands() {
        new ShopCommand().register(); // Registrar el comando


        if (conf.getBoolean("config.command.admin.enabled")) {
            JSCommand.addDefaultArguments();
            String basecommand = conf.getString("config.command.admin.name");
            List<String> aliases = conf.getStringList("config.command.admin.aliases");
            JSCommand psc = new JSCommand(basecommand);
            for (String command : aliases) {
                psc.getAliases().add(command);
            }
            registerCommand(basecommand, psc);
        }

    }

    private void registerCommand(String nombre, Command comando) {
        try {
            Field campo = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            campo.setAccessible(true);
            CommandMap commandMap = (CommandMap) campo.get(Bukkit.getServer());

            commandMap.register(nombre, comando);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            getLogger().log(Level.SEVERE, "Error when registering the command " + nombre, e);
        }
    }



    @Override
    public void setupFiles() {
        getLogger().log(Level.INFO, "Registering files...");
        try {
            conf = YamlDocument.create(new File(getDataFolder(), "config.yml"), getResource("config.yml"),
                    GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version")).build());
            lang = YamlDocument.create(new File(getDataFolder(), "lang.yml"), getResource("lang.yml"),
                    GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version")).build());
            layouts = YamlDocument.create(new File(getDataFolder(), "layouts.yml"), getResource("layouts.yml"),
                    GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version")).build());

       } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void createDefaultShopFile(File shopsFolder) {
        try {
            File defaultShopFile = new File(shopsFolder, "tools.yml");

            // Create the default shop file using BoostedYaml
            YamlDocument defaultShop = YamlDocument.create(
                    defaultShopFile,
                    this.getResource("shops/tools.yml"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version")).build()
            );

            // Save the file to ensure it's created
            defaultShop.save();

            this.getLogger().info("Tienda predeterminada creada correctamente: default_shop.yml");
        } catch (IOException e) {
            this.getLogger().severe("Error al crear la tienda predeterminada: " + e.getMessage());
        }
    }


    @Override
    public void setupActions() {

    }



}
