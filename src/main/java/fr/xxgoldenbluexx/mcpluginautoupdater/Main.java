package fr.xxgoldenbluexx.mcpluginautoupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;

public class Main extends JavaPlugin {
	
	private static Plugin mainPlugin;

	@Override
	public void onLoad() {
		super.onLoad();
	}
	
	@Override
	public void onEnable() {
		super.onEnable();
		mainPlugin = this;
		CommandAPI.onEnable(this);
		new CommandAPICommand("update")
		.withAliases("upd")
		.withArguments(
				new StringArgument("pluginName")
				.replaceSuggestions(ArgumentSuggestions.strings(Main::GetAllPluginNames))
				)
		.executes(Main::UpdateCommand)
		.register();
		new CommandAPICommand("update")
		.withAliases("upd")
		.withArguments(
				new StringArgument("pluginName")
				.replaceSuggestions(ArgumentSuggestions.strings(Main::GetAllPluginNames)),
				new TextArgument("downloadUrl")
				)
		.executes(Main::UpdateCommand)
		.register();
		new CommandAPICommand("update")
		.withAliases("upd")
		.executes(Main::ShowHelp)
		.register();
	}
	
	private static String[] GetAllPluginNames(SuggestionInfo info) {
		List<String> list = new ArrayList<>();
		PluginManager pmanager = Bukkit.getServer().getPluginManager();
		for (Plugin plugin : pmanager.getPlugins()) {
			if (plugin.isEnabled()) {
			     list.add(plugin.getName());
			}
		}
		return list.toArray(new String[0]);
	}
	
	public static void UpdateCommand(CommandSender sender, Object[] args) {
		if (args.length>0) {
			if (args[0] instanceof String) {
				String pluginName = (String)args[0];
				JavaPlugin plugin = seekPlugin(pluginName);
				if (plugin!=null) {
					if (args.length>1) {
						tryUpdate(plugin,sender,(String)args[1]);
					}else {
						tryUpdate(plugin,sender);
					}
				}else {
					sender.sendMessage(Component.text("Le plugin \""+pluginName+"\" est introuvable."));
				}
			}else {
				sender.sendMessage(Component.text("Le nom du plugin à mettre à jour doit être de type String."));
			}
		}else{
			sender.sendMessage(Component.text("usage: update <pluginName> [<downloadUrl>]"));
		};
		Plugin plugin = seekPlugin((String)args[0]);
		if (plugin==null) return;
	}
	
	public static void ShowHelp(CommandSender sender, Object[] args) {
		sender.sendMessage(Component.text("usage: update <pluginName> [<downloadUrl>]"));
		return;
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
		try {
			String url = plugin.getConfig().getString("update_url");
			tryUpdate(plugin, sender, url);
		}catch(Exception e) {
			sender.sendMessage(Component.text("Impossible de trouver l'url de mise à jours du plugin."));
		}
	}
	
	private static void tryUpdate(JavaPlugin plugin, CommandSender sender, String url) {
		String pluginName = plugin.getName();
		File pluginFile = null;
		if (url!=null) {
			String error = "";
			try {
				Method getFile = JavaPlugin.class.getDeclaredMethod("getFile");
				getFile.setAccessible(true);
				pluginFile = (File) getFile.invoke(plugin);
			}catch (Exception e) {
				error = "Impossible de récuperer le fichier du plugin avec de la reflexion: "+e.getMessage();
				sender.sendMessage(Component.text(error));
				return;
			}
			if (pluginFile == null) {
				error = "Impossible de récuperer le fichier du plugin avec de la reflexion, la valeur retournée est null";
				sender.sendMessage(Component.text(error));
				return;
			}
			final File fpluginFile = pluginFile;
			new BukkitRunnable() {
				@Override
				public void run() {
					final String error1 = download(url,pluginName);
					if (error1 != null) {
						new BukkitRunnable() {
							@Override
							public void run() {
								sender.sendMessage(Component.text(error1));
							}
						}.runTask(mainPlugin);
						return;
					}
					final String error2 = replace(fpluginFile,pluginName);
					if (error2 != null) {
						new BukkitRunnable() {
							@Override
							public void run() {
								sender.sendMessage(Component.text(error2));
							}
						}.runTask(mainPlugin);
						return;
					}
				}
			}.runTaskAsynchronously(mainPlugin);
			sender.sendMessage(Component.text("Le plugin "+pluginName+" à été téléchargé et remplacé, les changements seront effectif au prochain redémarage."));
		}else {
			sender.sendMessage(Component.text("Le plugin "+pluginName+" ne spécifie pas de lien pour se mettre à jour."));
		}
	}
	
	private static String download(String url, String pluginName) {
		
		String error = null;
		File downloadedFile = null;
		URL downloadUrl = null;
		
		// PARSE URL
		try {
			downloadUrl = new URL(url);
		}catch(MalformedURLException e) {
			error = "Erreur dans l'URL ("+url+"): "+e.getMessage();
		}
		if (error!=null) return error;
		
		// PREPARE LOCAL FILES
		try {
			File downloadFolder = new File(mainPlugin.getDataFolder(),"downloads/");
			downloadedFile = new File(downloadFolder,pluginName+".jar");
			if (!downloadFolder.exists()) {
				downloadFolder.mkdirs();
			}
			if (!downloadedFile.exists()) {
				downloadedFile.createNewFile();
			}
		}catch(Exception e) {
			error = "Erreur lors de la création du fichier à télécharger: "+e.getMessage();
		}
		if (error!=null) return error;
		
		// DOWNLOAD
		try (ReadableByteChannel inputChannel = Channels.newChannel(downloadUrl.openStream());
				FileOutputStream fos = new FileOutputStream(downloadedFile);
				FileChannel outputChannel = fos.getChannel();){
			outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
		}catch(Exception e) {
			error = "Erreur lors du téléchargment à l'adresse "+url+" : "+e.getMessage();
		}
		return error;
	}
	
	private static String replace(File output, String pluginName) {
		String error = null;
		try {
			File downloadFolder = new File(mainPlugin.getDataFolder(),"downloads/");
			File downloadedFile = new File(downloadFolder,pluginName+".jar");
			try(ReadableByteChannel inputChannel = Channels.newChannel(new FileInputStream(downloadedFile));
					FileOutputStream fos = new FileOutputStream(output);
					FileChannel outputChannel = fos.getChannel();){
				outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
			}
		}catch(Exception e) {
			error = "Impossible de remplacer le plugin avec sa mise à jour: "+e.getMessage();
		}
		return error;
	}
}
