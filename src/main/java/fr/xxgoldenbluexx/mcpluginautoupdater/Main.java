package fr.xxgoldenbluexx.mcpluginautoupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;

public class Main extends JavaPlugin {
	
	private static Plugin mainPlugin;

	@Override
	public void onEnable() {
		super.onEnable();
		mainPlugin = this;
		CommandAPI.onEnable(this);
		new CommandAPICommand("update").withAliases("updt").withArguments(new StringArgument("pluginName")).executes(Main::UpdateCommand).register();
	}
	
	public static void UpdateCommand(CommandSender sender, Object[] args) {
		if (args.length>0) {
			if (args[0] instanceof String) {
				String pluginName = (String)args[0];
				JavaPlugin plugin = seekPlugin(pluginName);
				if (plugin!=null) {
					tryUpdate(plugin,sender);
				}else {
					sender.sendMessage(Component.text("Le plugin \""+pluginName+"\" est introuvable."));
				}
			}else {
				sender.sendMessage(Component.text("Le nom du plugin à mettre à jour doit être de type String."));
			}
		}else{
			sender.sendMessage(Component.text("usage: update <pluginName>"));
		};
		Plugin plugin = seekPlugin((String)args[0]);
		if (plugin==null) return;
	}
	
	private static JavaPlugin seekPlugin(String name) {
		JavaPlugin plugin = null;
		PluginManager pmanager = Bukkit.getServer().getPluginManager();
		if (pmanager.isPluginEnabled(name)) {
		     Plugin p = pmanager.getPlugin(name);
		     if (p!=null && p instanceof JavaPlugin) {
		    	 plugin = (JavaPlugin) p;
		     }
		}
		return plugin;
	}
	
	private static void tryUpdate(JavaPlugin plugin, CommandSender sender) {
		String pluginName = plugin.getName();
		String url = plugin.getConfig().getString("update_url");
		if (url!=null) {
			try {
				Method getFile = JavaPlugin.class.getDeclaredMethod("getFile");
				getFile.setAccessible(true);
				File pluginFile = (File) getFile.invoke(plugin);
				//mainPlugin.getLogger().severe("pluginFile="+pluginFile.getAbsolutePath());
				File downloadFolder = new File(mainPlugin.getDataFolder(),"build/");
				File downloadedFile = new File(downloadFolder,pluginName+".jar");
				URL updateUrl = new URL(url);
				if (!downloadFolder.exists()) {
					downloadFolder.mkdirs();
				}
				if (!downloadedFile.exists()) {
					downloadedFile.createNewFile();
				}
				//DOWNLOAD
				ReadableByteChannel inputChannel = Channels.newChannel(updateUrl.openStream());
				FileOutputStream fos = new FileOutputStream(downloadedFile);
				FileChannel outputChannel = fos.getChannel();
				
				outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);

				inputChannel.close();
				outputChannel.close();
				fos.close();
				
				//REPLACE EXISTING PLUGIN
				FileInputStream fin = new FileInputStream(downloadedFile);
				ReadableByteChannel finalInputChannel = Channels.newChannel(fin);
				FileOutputStream ffos = new FileOutputStream(pluginFile);
				FileChannel finalOutputChannel = fos.getChannel();
				
				finalOutputChannel.transferFrom(finalInputChannel, 0, Long.MAX_VALUE);
				
				finalInputChannel.close();
				fin.close();
				finalOutputChannel.close();
				ffos.close();
				sender.sendMessage(Component.text("Le plugin "+pluginName+" est desormet à jour!"));
			}catch(Exception e) {
				sender.sendMessage(Component.text("Une erreur est survenue lors de la mise à jour de "+pluginName+"."));
				mainPlugin.getLogger().severe(e.getMessage());
				e.printStackTrace();
			}
		}else {
			sender.sendMessage(Component.text("Le plugin "+pluginName+" ne spécifie pas de lien pour se mettre à jour."));
		}
	}
}
