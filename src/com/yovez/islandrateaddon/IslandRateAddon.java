package com.yovez.islandrateaddon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.yovez.islandrateaddon.api.IslandRateAPI;
import com.yovez.islandrateaddon.command.RateCommand;
import com.yovez.islandrateaddon.config.CustomConfig;
import com.yovez.islandrateaddon.listener.EventListener;
import com.yovez.islandrateaddon.listener.InventoryCheck;
import com.yovez.islandrateaddon.misc.Placeholders;
import com.yovez.islandrateaddon.util.DbUtils;
import com.yovez.islandrateaddon.util.MySQL;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;

public class IslandRateAddon extends Addon {

	private MySQL mysql;
	private IslandRateAPI api;
	private Map<UUID, Long> cooldown;
	private CustomConfig messages, optOut;
	public static IslandRateAddon addon;
	private RateCommand rateCmd;

	@Override
	public void onLoad() {
		addon = this;
		saveDefaultConfig();
		messages = new CustomConfig(this, "messages");
		messages.saveDefaultConfig();
		optOut = new CustomConfig(this, "opt-out");
		optOut.saveDefaultConfig();
		api = IslandRateAPI.getInstance();
		mysql = MySQL.getInstance();
		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new Placeholders(this);
		}
		getServer().getPluginManager().registerEvents(new EventListener(addon),
				getServer().getPluginManager().getPlugin("BentoBox"));
		if (addon.getConfig().getInt("cooldown", 60) > 0)
			cooldown = new HashMap<UUID, Long>();
		if (getConfig().getBoolean("inv_check.enabled", false) == true)
			Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new InventoryCheck(this), 0L,
					getConfig().getLong("inv_check.timer") * 1000);
	}

	@Override
	public void onEnable() {
		rateCmd = new RateCommand(this);
	}

	public static IslandRateAddon getAddon() {
		return addon;
	}

	@Override
	public void onDisable() {
		messages.saveConfig();
		optOut.saveConfig();
		try {
			if (mysql != null)
				if (mysql.getConnection() != null && !mysql.getConnection().isClosed())
					DbUtils.close(mysql.getConnection());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getMessage(String msg, Player p, OfflinePlayer t, int rating, int topPlace) {
		if (getConfig().getString(msg) != null)
			msg = getConfig().getString(msg);
		else if (messages.getConfig().getString(msg) != null)
			msg = messages.getConfig().getString(msg);
		if (msg == null || msg.isEmpty())
			return "";
		msg = msg.replaceAll("%prefix%", messages.getConfig().getString("prefix"));
		if (p != null) {
			if (msg.contains("%player%"))
				msg = msg.replaceAll("%player%", p.getName());
			if (msg.contains("%player-stars%"))
				msg = msg.replaceAll("%player-stars%", String.valueOf(getAPI().getTotalRatings(p)));
			if (msg.contains("%player-average%"))
				msg = msg.replaceAll("%player-average%", String.valueOf(getAPI().getAverageRating(p)));
			if (msg.contains("%player-total-voters%"))
				msg = msg.replaceAll("%player-total-voters%", String.valueOf(getAPI().getTotalNumOfRaters(p)));
			if (msg.contains("%cooldown%"))
				msg = msg.replaceAll("%cooldown%",
						String.valueOf(cooldown.get(p.getUniqueId()) + getConfig().getInt("cooldown")
								- TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
			if (msg.contains("%opted-out-player%"))
				msg = msg.replaceAll("%opted-out-player%",
						addon.getOptOut().getConfig().getBoolean(p.getUniqueId().toString(), false) ? "True" : "False");
		}
		if (t != null) {
			if (msg.contains("%target%"))
				msg = msg.replaceAll("%target%", t.getName());
			if (msg.contains("%target-stars%"))
				msg = msg.replaceAll("%target-stars%", String.valueOf(getAPI().getTotalRatings(t)));
			if (msg.contains("%target-average%"))
				msg = msg.replaceAll("%target-average%", String.valueOf(getAPI().getAverageRating(t)));
			if (msg.contains("%target-total-voters%"))
				msg = msg.replaceAll("%target-total-voters%", String.valueOf(getAPI().getTotalNumOfRaters(t)));
			if (msg.contains("%cooldown%"))
				msg = msg.replaceAll("%cooldown%",
						String.valueOf(cooldown.get(t.getUniqueId()) + getConfig().getInt("cooldown")
								- TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
			if (msg.contains("%opted-out-target%"))
				msg = msg.replaceAll("%opted-out-target%",
						addon.getOptOut().getConfig().getBoolean(t.getUniqueId().toString(), false) ? "True" : "False");
		}
		if (rating > 0)
			msg = msg.replaceAll("%rating%", String.valueOf(rating));
		msg = msg.replaceAll("%top-place%", String.valueOf(topPlace));
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	public ItemStack getConfigItem(String path, OfflinePlayer op) {
		if (!getConfig().contains(path))
			return null;
		ItemStack item = new ItemStack(Material.matchMaterial(getConfig().getString(path + ".material").toUpperCase()),
				getConfig().getInt(path + ".amount", 1));
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(getMessage(path + ".display_name", null, op, 0, 0));
		meta.setLore(getConvertedLore(path, op));
		item.setItemMeta(meta);
		return item;
	}

	public List<String> getConvertedLore(String path, OfflinePlayer op) {
		List<String> oldList = getConfig().getStringList(path + ".lore");
		List<String> newList = new ArrayList<String>();
		for (String a : oldList) {
			newList.add(getMessage(a, null, op, 0, 0));
		}
		return newList;
	}

	public void rateIsland(Player p, OfflinePlayer op, int rating) {
		if (p == null) {
			Bukkit.getConsoleSender().sendMessage(
					"§2[IslandRate] §4WARNING: §cAn error occured! Please tell the developer about this error! (P391)");
			return;
		}
		if (op == null) {
			p.sendMessage(getMessage("no-island", p, null, 0, 0));
			p.playSound(p.getLocation(),
					Sound.valueOf(
							Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ? "ANVIL_BREAK"
									: "BLOCK_ANVIL_BREAK"),
					100, 100);
			return;
		}
		if (rating < 1) {
			Bukkit.getConsoleSender().sendMessage(
					"§2[IslandRate] §4WARNING: §cAn error occured! Please tell the developer about this error! (R393)");
			return;
		}
		/*
		 * if (getConfig().getLong("min-island-level", 0) > 0) { if
		 * (getIslands().hasIsland(skyblockWorld, p.getUniqueId())) {
		 * 
		 * if (getIslands().getIsland(skyblockWorld, p.getUniqueId()) <
		 * getConfig().getLong("min-island-level", 0)) {
		 * p.sendMessage(getMessage("incorrect-level", p, null, 0, 0));
		 * p.playSound(p.getLocation(),
		 * Sound.valueOf(Bukkit.getVersion().contains("1.7") ||
		 * Bukkit.getVersion().contains("1.8") ? "ANVIL_BREAK" : "BLOCK_ANVIL_BREAK"),
		 * 100, 100); return; } p.sendMessage(getMessage("incorrect-level", p, null, 0,
		 * 0)); p.playSound(p.getLocation(),
		 * Sound.valueOf(Bukkit.getVersion().contains("1.7") ||
		 * Bukkit.getVersion().contains("1.8") ? "ANVIL_BREAK" : "BLOCK_ANVIL_BREAK"),
		 * 100, 100); return;
		 * 
		 * } }
		 */
		Island island = getIslands().getIsland(rateCmd.getWorld(), p.getUniqueId());
		if (island == null) {
			p.sendMessage(getMessage("no-island", p, null, 0, 0));
			p.playSound(p.getLocation(),
					Sound.valueOf(
							Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ? "ANVIL_BREAK"
									: "BLOCK_ANVIL_BREAK"),
					100, 100);
			return;
		}
		if (optOut.getConfig().getBoolean(op.getUniqueId().toString(), false) == true) {
			p.sendMessage(getMessage("opted-out", p, op, 0, 0));
			return;
		}
		if (getConfig().getBoolean("change-rating", true) == false) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				conn = getMySQL().getConnection();
				ps = conn.prepareStatement(
						"SELECT rater_uuid, player_uuid FROM island_ratings WHERE (rater_uuid = ? AND player_uuid = ?);");
				ps.setString(1, p.getUniqueId().toString());
				ps.setString(2, op.getUniqueId().toString());
				rs = ps.executeQuery();
				if (rs.next()) {
					p.sendMessage(getMessage("already-rated-island", p,
							Bukkit.getServer().getOfflinePlayer(island.getOwner()), rating, 0));
					p.playSound(p.getLocation(),
							Sound.valueOf(Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8")
									? "ANVIL_BREAK"
									: "BLOCK_ANVIL_BREAK"),
							100, 100);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					DbUtils.close(rs);
					DbUtils.close(ps);
					DbUtils.close(conn);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		if (cooldown != null)
			if (cooldown.containsKey(p.getUniqueId())) {
				long timeLeft = cooldown.get(p.getUniqueId());
				if (timeLeft >= (System.currentTimeMillis() / 1000) - getConfig().getInt("cooldown")) {
					p.sendMessage(getMessage("cooldown-time", p, null, rating, 0));
					return;
				} else {
					cooldown.remove(p.getUniqueId());
				}
			}
		Connection conn = null;
		PreparedStatement psDuplicateCheck = null, ps = null, psr = null;
		ResultSet rs = null;
		int previousRating = 0;
		int totalRatings = getAPI().getTotalRatings(op);
		try {
			conn = getMySQL().getConnection();
			psDuplicateCheck = conn
					.prepareStatement("SELECT * FROM island_ratings WHERE (rater_uuid = ? AND player_uuid = ?);");
			psDuplicateCheck.setString(1, p.getUniqueId().toString());
			psDuplicateCheck.setString(2, op.getUniqueId().toString());
			rs = psDuplicateCheck.executeQuery();
			if (rs.next()) {
				previousRating = rs.getInt("rating");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				DbUtils.close(rs);
				DbUtils.close(psDuplicateCheck);
				DbUtils.close(conn);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			conn = getMySQL().getConnection();
			ps = conn.prepareStatement("REPLACE INTO island_owners(player_uuid, total_ratings) VALUES (?,?);");
			ps.setString(1, op.getUniqueId().toString());
			ps.setInt(2, (totalRatings + rating) - previousRating);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				DbUtils.close(ps);
				DbUtils.close(conn);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			conn = getMySQL().getConnection();
			// psr = conn
			// .prepareStatement("UPDATE island_ratings SET rating = ? WHERE rater_uuid = ?
			// AND player_uuid = ?");
			PreparedStatement del = conn
					.prepareStatement("DELETE FROM island_ratings WHERE rater_uuid = ? AND player_uuid = ?;");
			del.setString(1, p.getUniqueId().toString());
			del.setString(2, op.getUniqueId().toString());
			del.execute();
			DbUtils.close(del);
			psr = conn.prepareStatement("REPLACE INTO island_ratings(rater_uuid, player_uuid, rating) VALUES (?,?,?);");
			psr.setString(1, p.getUniqueId().toString());
			psr.setString(2, op.getUniqueId().toString());
			psr.setInt(3, rating);
			psr.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				DbUtils.close(psr);
				DbUtils.close(conn);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		p.playSound(p.getLocation(),
				Sound.valueOf(Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ? "LEVEL_UP"
						: "ENTITY_PLAYER_LEVELUP"),
				100, 100);
		if (rating != 1)
			p.sendMessage(getMessage("successfull-rate-plural", p,
					Bukkit.getServer().getOfflinePlayer(island.getOwner()), rating, 0));
		else
			p.sendMessage(getMessage("successfull-rate", p, Bukkit.getServer().getOfflinePlayer(island.getOwner()),
					rating, 0));
		if (Bukkit.getOfflinePlayer(island.getOwner()).isOnline()
				&& getConfig().getBoolean("send-owner-message", false) == true)
			Bukkit.getPlayer(island.getOwner()).sendMessage(getMessage("owner-message", p, p, rating, 0));
		if (cooldown != null)
			cooldown.put(p.getUniqueId(), TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
		if (getConfig().getBoolean("logging.enabled", false) == true)
			if (getConfig().getBoolean("logging.rate-island", false) == true)
				Bukkit.getConsoleSender().sendMessage("§2[IslandRate] §eLOG§f: " + p.getName() + " rated "
						+ op.getName() + "'s Island " + rating + ".");
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public IslandRateAPI getAPI() {
		return api;
	}

	public CustomConfig getMessages() {
		return messages;
	}

	public CustomConfig getOptOut() {
		return optOut;
	}

	public RateCommand getRateCommand() {
		return rateCmd;
	}
}
