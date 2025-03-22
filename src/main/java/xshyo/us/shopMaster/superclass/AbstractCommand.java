package xshyo.us.shopMaster.superclass;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import xshyo.us.shopMaster.utilities.NMSUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractCommand implements CommandExecutor, TabCompleter{

    @Getter
    protected final String			command;
    protected final String			description;
    protected final List<String>	alias;
    protected final String			usage;
    protected final String			permMessage;
    protected final Plugin			plugin;

    protected String 				permission;
    protected static CommandMap		cmap;

    public AbstractCommand(String command){
        this(command, null, null, null, null, null);
    }

    public AbstractCommand(String command, String usage){
        this(command, usage, null, null, null, null);
    }

    public AbstractCommand(String command, String usage, String description){
        this(command, usage, description, null, null, null);
    }

    public AbstractCommand(String command, String usage, String description, String permissionMessage){
        this(command, usage, description, permissionMessage, null, null);
    }

    public AbstractCommand(String command, String usage, String description, List<String> aliases){
        this(command, usage, description, null, aliases, null);
    }

    public AbstractCommand(String command, String usage, String description, String permissionMessage, List<String> aliases){
        this(command, usage, description, permissionMessage, aliases, null);
    }

    public AbstractCommand(String command, String usage, String description, String permissionMessage, List<String> aliases, Plugin plugin){
        this.command = command.toLowerCase();
        this.usage = usage;
        this.description = description;
        this.permMessage = permissionMessage;
        this.alias = aliases;
        this.plugin = plugin;
    }

    private static final CommandMap				commandMap		= getCommandMap();
    private static final Field					knownCommands	= getKnownCommands();

    private static boolean					loaded			= false;

    private static final List<AbstractCommand>	list			= new ArrayList<AbstractCommand>();

    public static void enable(){
        loaded = true;
        for(AbstractCommand ac : list){
            ac.register();
        }
        list.clear();
    }

    public static void removePluginCommands(Plugin plugin){
        try{
            Map<String, Command> commands = (Map<String, Command>)knownCommands.get(commandMap);
            List<String> list = new ArrayList<String>();
            for(Map.Entry<String, Command> e : commands.entrySet()){
                Command c = e.getValue();
                if(c != null && c instanceof PluginIdentifiableCommand){
                    if(((PluginIdentifiableCommand)c).getPlugin() == plugin){
                        list.add(e.getKey());
                    }
                }
            }
            for(String s : list){
                commands.remove(s);
            }
            knownCommands.set(commandMap, commands);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void register(){
        if(!loaded){
            list.add(this);
        }else{
            reg();
        }
    }

    @SuppressWarnings("unchecked")
    protected void reg(){
        ReflectCommand cmd = plugin == null ? new ReflectCommand(this.command) : new PluginReflectCommand(this.command, this.plugin);
        if(this.alias != null)
            cmd.setAliases(this.alias);
        if(this.description != null)
            cmd.setDescription(this.description);
        if(this.usage != null)
            cmd.setUsage(this.usage);
        if(this.permMessage != null)
            cmd.setPermissionMessage(this.permMessage);
        if(this.permission != null)
            cmd.setPermission(permission);
        try{
            Map<String, Command> commands = (Map<String, Command>)knownCommands.get(commandMap);
            commands.remove(this.command);
            knownCommands.set(commandMap, commands);
        }catch(Exception e){
            e.printStackTrace();
        }
        commandMap.register(this.command, plugin == null ? "" : plugin.getName(), cmd);
        cmd.setExecutor(this);
    }

    protected static Field getKnownCommands(){
        try{
            Class<?> clazz = commandMap.getClass();
            switch(clazz.getSimpleName()){
                case "MockCommandMap":
                case "CraftCommandMap":
                case "FakeSimpleCommandMap":{
                    clazz = clazz.getSuperclass();
                    break;
                }
            }
            return NMSUtils.getField(clazz, "knownCommands");
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }


    protected static CommandMap getCommandMap(){
        if(cmap == null){
            try{
                final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                cmap = (CommandMap)f.get(Bukkit.getServer());
            }catch(Exception e){
                e.printStackTrace();
            }
            return cmap;
        }else{
            return cmap;
        }
    }

    public static void removeCommandOfClass(Class<? extends Command> clazz){
        try{
            @SuppressWarnings("unchecked")
            Map<String, Command> commands = (Map<String, Command>)knownCommands.get(commandMap);
            Iterator<Map.Entry<String, Command>> i = commands.entrySet().iterator();
            while(i.hasNext()){
                Map.Entry<String, Command> e = i.next();
                if(clazz.isAssignableFrom(e.getValue().getClass())){
                    i.remove();
                }
            }
            knownCommands.set(commandMap, commands);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void removeCommand(String command){
        try{
            @SuppressWarnings("unchecked")
            Map<String, Command> commands = (Map<String, Command>)knownCommands.get(commandMap);
            Iterator<Map.Entry<String, Command>> i = commands.entrySet().iterator();
            while(i.hasNext()){
                Map.Entry<String, Command> e = i.next();
                if(e.getKey().equals(command)){
                    i.remove();
                }
            }
            knownCommands.set(commandMap, commands);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private class ReflectCommand extends Command{
        private AbstractCommand exe = null;

        protected ReflectCommand(String command){
            super(command);
        }

        public void setExecutor(AbstractCommand exe){
            this.exe = exe;
        }

        public boolean execute(CommandSender sender, String commandLabel, String[] args){
            if(exe != null){
                exe.onCommand(sender, this, commandLabel, args);
            }
            return false;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args){
            List<String> list = exe.onTabComplete(sender, this, alias, args);
            if(list != null)
                return list;
            return super.tabComplete(sender, alias, args);
        }

    }

    private class PluginReflectCommand extends ReflectCommand implements PluginIdentifiableCommand{

        protected Plugin plugin;

        protected PluginReflectCommand(String command, Plugin plugin){
            super(command);
            this.plugin = plugin;
        }

        @Override
        public Plugin getPlugin(){
            return plugin;
        }
    }

    public boolean isPlayer(CommandSender sender){
        return (sender instanceof Player);
    }

    public boolean isAuthorized(CommandSender sender, String permission){
        return sender.hasPermission(permission);
    }

    public boolean isAuthorized(Player player, String permission){
        return player.hasPermission(permission);
    }

    public boolean isAuthorized(CommandSender sender, Permission perm){
        return sender.hasPermission(perm);
    }

    public boolean isAuthorized(Player player, Permission perm){
        return player.hasPermission(perm);
    }

    public abstract boolean onCommand(CommandSender s, Command cmd, String label, String[] args);

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        return null;
    }
}
