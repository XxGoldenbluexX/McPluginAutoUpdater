package fr.xxgoldenbluexx.mcpluginautoupdater;

import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;

public class Main extends JavaPlugin {

	@Override
	public void onEnable() {
		super.onEnable();
		CommandAPI.onEnable(this);
		new CommandAPICommand("updater").withAliases("updt").withArguments(new StringArgument("pluginName")).executes((sender,args)->{
			
		}).register();
	}
	
	public static void UpdateCommand() {
		
	}
}
