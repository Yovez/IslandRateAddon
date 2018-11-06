package com.yovez.islandrateaddon;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class RateCommand extends CompositeCommand {

	IslandRateAddon addon;
	String prefix;
	boolean menu;
	boolean topMenu;
	boolean disableCommand;

	public RateCommand(IslandRateAddon addon) {
		super(addon, "rate");
		this.addon = addon;
	}

	private void setupPrefix() {
		prefix = ChatColor.translateAlternateColorCodes('&', addon.getMessages().getConfig().getString("prefix"));
		menu = addon.getConfig().getBoolean("menu.enabled", false);
		topMenu = addon.getConfig().getBoolean("top_menu.enabled", false);
		disableCommand = addon.getConfig().getBoolean("disable-command-rating", false);
	}

	private String getMessage(String path, Player p, OfflinePlayer t, int rating, int topPlace) {
		if (addon.getMessage(path, p, t, rating, topPlace) == null)
			return "";
		else
			return addon.getMessage(path, p, t, rating, topPlace);
	}

	@Override
	public boolean execute(User sender, String cmd, List<String> args) {
		if (sender == null) {
			return false;
		}
		if (cmd.equalsIgnoreCase("rate")) {
			if (!(sender.isPlayer())) {
				if (args.size() == 0) {
					sender.sendMessage("IslandRate console commands:");
					sender.sendMessage("/rate reset <player|all>");
					return true;
				}
				if (args.size() == 1) {
					if (args.get(0).equalsIgnoreCase("reset")) {
						sender.sendMessage("Try /rate reset <player|all>");
						return true;
					}
					sender.sendMessage("IslandRate console commands:");
					sender.sendMessage("/rate reset <player|all>");
					return true;
				}
				if (args.size() == 2) {
					if (args.get(0).equalsIgnoreCase("reset")) {
						if (args.get(1).equalsIgnoreCase("all")) {
							return true;
						}
						@SuppressWarnings("deprecation")
						OfflinePlayer t = Bukkit.getOfflinePlayer(args.get(1));
						if (t == null) {
							sender.sendMessage(args.get(1) + " is not a valid player. Try /rate reset <player|all>");
							return true;
						}

					}
				}
				return true;
			}
			Player p = null;
			if (sender.isPlayer())
				p = sender.getPlayer();
			if (p == null)
				return false;
			String commandUsage = getMessage("command-usage", p, null, 0, 0);
			String noPermission = getMessage("no-permission", p, null, 0, 0);
			String noIsland = getMessage("no-island", p, null, 0, 0);
			String ownedIsland = getMessage("owned-island", p, null, 0, 0);
			String teamIsland = getMessage("team-island", p, null, 0, 0);
			String numberNotFound = getMessage("number-not-found", p, null, 0, 0);
			// String resetUsage = getMessage("reset-usage", p, null, 0, 0);
			// String resetAll = getMessage("reset-all", p, null, 0, 0);
			// String resetPlayerNotFound = getMessage("reset-player-not-found", p, null, 0,
			// 0);
			String topHeader = getMessage("top.header", p, null, 0, 0);
			String topFooter = getMessage("top.footer", p, null, 0, 0);
			String topNoTop = getMessage("top.no-top", p, null, 0, 0);
			String commandDisabled = getMessage("command-disabled", p, null, 0, 0);
			if (!p.hasPermission("islandrate.use")) {
				p.sendMessage(noPermission);
				return true;
			}
			if (args.size() == 0) {
				if (menu) {
					if (!p.getLocation().getWorld().getName().equals(getWorld().getName())) {
						p.sendMessage(noIsland);
						return true;
					}
					if (!addon.getIslands().getIslandAt(p.getLocation()).isPresent()) {
						p.sendMessage(noIsland);
						return true;
					}
					if (addon.getIslands().getIslandAt(p.getLocation()).get().getOwner().equals(p.getUniqueId())) {
						if (addon.getConfig().getBoolean("island_menu.enabled", false) == true) {
							IslandMenu im = new IslandMenu(addon, p);
							im.openInv();
						} else {
							p.sendMessage(ownedIsland);
						}
						return true;
					}
					RateMenu rm = new RateMenu(addon,
							Bukkit.getOfflinePlayer(addon.getIslands().getIslandAt(p.getLocation()).get().getOwner()));
					if (addon.getConfig().getBoolean("menu.custom", false) == false)
						rm.openInv(p);
					else
						rm.openCustomInv(p);
					return true;
				}
				p.sendMessage(commandUsage);
				return true;
			}
			if (args.size() == 1) {
				if (args.get(0).equalsIgnoreCase("invcheck")) {
					if (!p.hasPermission("islandrate.invcheck")) {
						p.sendMessage(noPermission);
						return true;
					}
					InventoryCheck ic = new InventoryCheck(addon);
					p.sendMessage("§aSuccessfully ran an inv check on all online players...");
					p.sendMessage("§bNumber of Players caught: §e" + ic.runCheck().keySet().size());
					p.sendMessage("§bNumber of Items removed: §e" + ic.runCheck().values().size());
					return true;
				}
				if (args.get(0).equalsIgnoreCase("average")) {
					if (!p.hasPermission("islandrate.average")) {
						p.sendMessage(noPermission);
						return true;
					}
					p.sendMessage(getMessage("average-rating", p, null, 0, 0));
					return true;
				}
				if (args.get(0).equalsIgnoreCase("total")) {
					if (!p.hasPermission("islandrate.total")) {
						p.sendMessage(noPermission);
						return true;
					}
					p.sendMessage(getMessage("total-ratings", p, null, 0, 0));
					return true;
				}
				if (args.get(0).equalsIgnoreCase("migrate")) {
					if (!p.hasPermission("islandrate.migrate")) {
						p.setDisplayName(noPermission);
						return true;
					}
					try {
						addon.getMySQL().convertFromFile();
						p.sendMessage("§aMigrated from file storage to MySQL/SQLite storage successfully!");
					} catch (SQLException | ClassNotFoundException e) {
						e.printStackTrace();
						p.sendMessage("§cMigrated from file storage to MySQL/SQLite storage unsuccessfully :("
								+ " Please contact the Developer!");
					}
					return true;
				}
				if (args.get(0).equalsIgnoreCase("reload")) {
					if (!p.hasPermission("islandrate.reload")) {
						p.sendMessage(noPermission);
						return true;
					}
					addon.getMessages().reloadConfig();
					addon.getOptOut().reloadConfig();
					setupPrefix();
					p.sendMessage("§aSuccessfully Reloaded IslandRate Configs!");
					addon.getMySQL();
					return true;
				}
				/*
				 * if (args[0].equalsIgnoreCase("reset")) { if
				 * (!p.hasPermission("islandrate.reset")) { p.sendMessage(noPermission); return
				 * true; } p.sendMessage(resetUsage); return true; } if
				 * (args[0].equalsIgnoreCase("add")) { if
				 * (!p.hasPermission("islandrate.admin.add")) { p.sendMessage(noPermission);
				 * return true; }
				 * p.sendMessage("§cCorrect usage is /rate add <player> <# of stars>"); return
				 * true; } if (args[0].equalsIgnoreCase("take")) { if
				 * (!p.hasPermission("islandrate.admin.take")) { p.sendMessage(noPermission);
				 * return true; }
				 * p.sendMessage("§cCorrect usage is /rate take <player> <# of stars>"); return
				 * true; } if (args[0].equalsIgnoreCase("set")) { if
				 * (!p.hasPermission("islandrate.admin.set")) { p.sendMessage(noPermission);
				 * return true; }
				 * p.sendMessage("§cCorrect usage is /rate set <player> <# of stars>"); return
				 * true; } if (args[0].equalsIgnoreCase("inftop")) { if
				 * (!p.hasPermission("islandrate.infinitetop")) { p.sendMessage(noPermission);
				 * return true; } InfiniteTopMenu itm = new InfiniteTopMenu(addon);
				 * itm.openInv(p); return true; }
				 */
				if (args.get(0).equalsIgnoreCase("top")) {
					if (!p.hasPermission("islandrate.top")) {
						p.sendMessage(noPermission);
						return true;
					}
					if (addon.getAPI().getTopRated() == null) {
						p.sendMessage(topNoTop);
						return true;
					}
					if (topMenu) {
						TopMenu tm = new TopMenu(addon);
						tm.openInv(p);
						return true;
					}
					p.sendMessage(topHeader);
					for (int i = 1; i < 11; i++) {
						if (addon.getAPI().getTotalRatings(addon.getAPI().getTopRated(i)) == 0)
							break;
						p.sendMessage(getMessage("top.entry", null, addon.getAPI().getTopRated(i),
								addon.getAPI().getTotalRatings(addon.getAPI().getTopRated(i)), i));
					}
					p.sendMessage(topFooter);
					return true;
				}
				if (disableCommand) {
					p.sendMessage(commandDisabled);
					return true;
				}
				if (addon.getIslands().userIsOnIsland(getWorld(), (User) p)) {
					p.sendMessage(noIsland);
					return true;
				}
				if (!addon.getIslands().hasIsland(getWorld(), p.getUniqueId())) {
					p.sendMessage(noIsland);
					return true;
				}
				if (addon.getIslands().getIslandAt(p.getLocation()).get().getOwner().equals(p.getUniqueId())) {
					p.sendMessage(ownedIsland);
					return true;
				}
				Island island = addon.getIslands().getIslandAt(p.getLocation()).get();
				if (island == null) {
					p.sendMessage(noIsland);
					return true;
				}
				if (island.getMembers().containsKey(p.getUniqueId())) {
					p.sendMessage(teamIsland);
					return true;
				}
				if (!addon.getAPI().isInt(args.get(0))) {
					p.sendMessage(numberNotFound);
					return true;
				}
				if (Integer.parseInt(args.get(0)) <= 0
						|| Integer.parseInt(args.get(0)) > addon.getConfig().getInt("max-command-rating", 5)) {
					p.sendMessage(commandUsage);
					return true;
				}
				addon.rateIsland(p, Bukkit.getOfflinePlayer(island.getOwner()), Integer.parseInt(args.get(0)));
			} else if (args.size() == 2) {
				if (args.get(1).equalsIgnoreCase("average")) {
					if (!p.hasPermission("islandrate.average")) {
						p.sendMessage(noPermission);
						return true;
					}
					@SuppressWarnings("deprecation")
					OfflinePlayer t = Bukkit.getServer().getOfflinePlayer(args.get(1));
					if (t == null) {
						p.sendMessage(getMessage("average-player-not-found", p, null, 0, 0));
						return true;
					}
					p.sendMessage(getMessage("average-rating-target", p, t, 0, 0));
					return true;
				}
				if (args.get(1).equalsIgnoreCase("total")) {
					if (!p.hasPermission("islandrate.total.other")) {
						p.sendMessage(noPermission);
						return true;
					}
					@SuppressWarnings("deprecation")
					OfflinePlayer t = Bukkit.getServer().getOfflinePlayer(args.get(1));
					if (t == null) {
						p.sendMessage(getMessage("total-player-not-found", p, null, 0, 0));
						return true;
					}
					p.sendMessage(getMessage("total-ratings-other", p, t, 0, 0));
					return true;
				}
				/*
				 * if (args[0].equalsIgnoreCase("add")) { if
				 * (!p.hasPermission("islandrate.admin.add")) { p.sendMessage(noPermission);
				 * return true; }
				 * p.sendMessage("§cCorrect usage is /rate add <player> <# of stars>"); return
				 * true; } if (args[0].equalsIgnoreCase("take")) { if
				 * (!p.hasPermission("islandrate.admin.take")) { p.sendMessage(noPermission);
				 * return true; }
				 * p.sendMessage("§cCorrect usage is /rate take <player> <# of stars>"); return
				 * true; } if (args[0].equalsIgnoreCase("set")) { if
				 * (!p.hasPermission("islandrate.admin.set")) { p.sendMessage(noPermission);
				 * return true; }
				 * p.sendMessage("§cCorrect usage is /rate set <player> <# of stars>"); return
				 * true; }
				 */
			} // else if (args.length == 3) {
			/*
			 * if (args[0].equalsIgnoreCase("add")) { if
			 * (!p.hasPermission("islandrate.admin.add")) { p.sendMessage(noPermission);
			 * return true; }
			 * p.sendMessage("§cCorrect usage is /rate add <player> <# of stars>"); return
			 * true; } if (args[0].equalsIgnoreCase("take")) { if
			 * (!p.hasPermission("islandrate.admin.take")) { p.sendMessage(noPermission);
			 * return true; }
			 * p.sendMessage("§cCorrect usage is /rate take <player> <# of stars>"); return
			 * true; } if (args[0].equalsIgnoreCase("set")) { if
			 * (!p.hasPermission("islandrate.admin.set")) { p.sendMessage(noPermission);
			 * return true; }
			 * p.sendMessage("§cCorrect usage is /rate set <player> <# of stars>"); return
			 * true; }
			 */
			/* } */ else {
				p.sendMessage(commandUsage);
				return true;
			}
		}
		return true;
	}

	@Override
	public void setup() {
		setWorld(addon.getServer().getWorld(
				addon.getAddonByName("BSkyBlock").get().getConfig().getString("world.world-name", "BSkyBlock_world")));
	}

}
