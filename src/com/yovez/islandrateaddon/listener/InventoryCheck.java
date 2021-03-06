package com.yovez.islandrateaddon.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.yovez.islandrateaddon.IslandRateAddon;
import com.yovez.islandrateaddon.gui.RateMenu;

public class InventoryCheck implements Runnable {

	IslandRateAddon plugin;

	public InventoryCheck(IslandRateAddon plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			Inventory inv = p.getInventory();
			RateMenu menu = new RateMenu(plugin, p);
			if (inv.getContents().length > 0)
				for (ItemStack item : inv.getContents()) {
					if (item.equals(menu.getHelp())) {
						inv.remove(menu.getHelp());
					}
					if (item.equals(menu.getSkull())) {
						inv.remove(menu.getSkull());
					}
					for (int i = 0; i < 5; i++) {
						if (item.equals(menu.getStar(i))) {
							inv.remove(menu.getStar(i));
						}
					}
				}
		}
	}

	public Map<UUID, Integer> runCheck() {
		Map<UUID, Integer> list = new HashMap<UUID, Integer>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			Inventory inv = p.getInventory();
			RateMenu menu = new RateMenu(plugin, p);
			if (inv.contains(menu.getHelp())) {
				inv.remove(menu.getHelp());
				list.put(p.getUniqueId(), list.get(p.getUniqueId()) + 1);
			}
			if (inv.contains(menu.getSkull())) {
				inv.remove(menu.getSkull());
				list.put(p.getUniqueId(), list.get(p.getUniqueId()) + 1);
			}
			for (int i = 0; i < 5; i++) {
				if (inv.contains(menu.getStar(i))) {
					inv.remove(menu.getStar(i));
					list.put(p.getUniqueId(), list.get(p.getUniqueId()) + 1);
				}
			}
		}
		return list;
	}

}
