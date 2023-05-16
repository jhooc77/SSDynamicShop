package me.sat7.dynamicshop.events;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.sat7.dynamicshop.DynamicShop;

public class JoinQuit implements Listener {
	
	public JoinQuit(JavaPlugin plugin) {
	}
	
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        Player player = e.getPlayer();
        FileConfiguration config = DynamicShop.ccUser.get(player);
        config.set("tmpString","");
        config.set("interactItem","");
        config.set("lastJoin",System.currentTimeMillis());
        config.addDefault("cmdHelp",true);
        DynamicShop.ccUser.save(player);
    }
}
