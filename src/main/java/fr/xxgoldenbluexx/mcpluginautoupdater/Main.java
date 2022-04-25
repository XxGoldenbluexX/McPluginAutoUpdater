package fr.xxgoldenbluexx.mcpluginautoupdater;

import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.CommandAPI;

public class Main extends JavaPlugin {

	@Override
	public void onEnable() {
		super.onEnable();
		CommandAPI.onEnable(this);
	}
	
}
