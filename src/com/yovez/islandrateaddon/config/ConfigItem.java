package com.yovez.islandrateaddon.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import com.yovez.islandrateaddon.IslandRateAddon;

public class ConfigItem {

	final IslandRateAddon plugin;
	private Map<ItemStack, Integer> items;

	public ConfigItem(IslandRateAddon plugin, OfflinePlayer p) {
		this.plugin = plugin;
		if (items == null || items.isEmpty())
			setupItems(p);
	}

	public void setupItems(OfflinePlayer p) {
		items = new HashMap<ItemStack, Integer>();
		for (String s : plugin.getConfig().getConfigurationSection("menu.items").getKeys(false)) {
			s = "menu.items." + s;
			if (s.equalsIgnoreCase("menu.items.skull"))
				continue;
			if (plugin.getConfigItem(s, p) != null)
				if (!items.containsKey(plugin.getConfigItem(s, p)))
					items.put(plugin.getConfigItem(s, p), plugin.getConfig().getInt(s + ".rating", 0));
		}
	}

	public Map<ItemStack, Integer> getItems() {
		return items;
	}

	public void setItems(Map<ItemStack, Integer> items) {
		this.items = items;
	}

}
