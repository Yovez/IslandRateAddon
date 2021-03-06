package com.yovez.islandrateaddon.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import com.yovez.islandrateaddon.IslandRateAddon;

public class MySQL {

	final IslandRateAddon plugin;
	private Connection connection;
	private String host, port, database, username, password, storageType;

	private static MySQL instance;

	public static MySQL getInstance() {
		if (instance == null)
			new MySQL(IslandRateAddon.getAddon());
		return instance;
	}

	protected MySQL(IslandRateAddon plugin) {
		this.plugin = plugin;
		instance = this;
		storageType = getConfig().getString("storage.type", "SQLITE");
		host = getConfig().getString("storage.mysql.host", "localhost");
		port = getConfig().getString("storage.mysql.port", "3306");
		database = getConfig().getString("storage.mysql.database", "minecraft");
		username = getConfig().getString("storage.mysql.username", "root");
		password = getConfig().getString("storage.mysql.password", "password");
		try {
			openConnection(storageType);
			if (!doesTableExist("island_owners") || !doesTableExist("island_ratings"))
				setupDatabase();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			Bukkit.getConsoleSender().sendMessage(
					"�2[IslandRate] �4WARNING: �cAn error occured while trying to connect to the MySQL/SQLite server/database.");
			Bukkit.getConsoleSender().sendMessage("�2[IslandRate] �4WARNING: �c" + e.getMessage());
		} finally {
			try {
				if (connection != null && !connection.isClosed()) {
					Bukkit.getConsoleSender()
							.sendMessage("�2[IslandRate] �a" + storageType + " successfully connected!");
					DbUtils.close(connection);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public FileConfiguration getConfig() {
		return plugin.getConfig();
	}

	public void setupDatabase() throws SQLException {
		if (connection == null || connection.isClosed()) {
			Bukkit.getConsoleSender().sendMessage(
					"�2[IslandRate] �4WARNING: �cAn error occured while trying to generate database tables. "
							+ "Please notify the Developer!");
			return;
		}
		PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS island_owners "
				+ "(player_uuid VARCHAR(36) NOT NULL, total_ratings INT(11) DEFAULT 0, PRIMARY KEY (player_uuid));");
		ps.executeUpdate();
		DbUtils.close(ps);
		PreparedStatement ps2 = null, ps3 = null, ps4 = null, ps5 = null;
		if (doesTableExist("island_ratings")) {
			if (storageType.equalsIgnoreCase("sqlite")) {
				ps2 = connection.prepareStatement(
						"CREATE TABLE island_ratings_copy (id INTEGER PRIMARY KEY, rater_uuid TEXT NOT NULL, "
								+ "player_uuid TEXT NOT NULL, rating INTEGER NOT NULL);");
				ps2.executeUpdate();
				DbUtils.close(ps2);
				ps3 = connection.prepareStatement("INSERT INTO island_ratings_copy (rater_uuid, player_uuid, rating) "
						+ "SELECT rater_uuid, player_uuid, rating FROM island_ratings;");
				ps3.executeUpdate();
				DbUtils.close(ps3);
				ps4 = connection.prepareStatement("DROP TABLE island_ratings;");
				ps4.executeUpdate();
				DbUtils.close(ps4);
				ps5 = connection.prepareStatement("ALTER TABLE island_ratings_copy RENAME TO island_ratings;");
				ps5.executeUpdate();
				DbUtils.close(ps5);
			} else if (storageType.equalsIgnoreCase("mysql")) {
				ps3 = connection.prepareStatement("ALTER TABLE island_ratings DROP PRIMARY KEY;");
				ps3.executeUpdate();
				DbUtils.close(ps3);
				ps5 = connection.prepareStatement(
						"ALTER TABLE island_ratings ADD COLUMN id INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT FIRST;");
				ps5.executeUpdate();
				DbUtils.close(ps5);
			}
		} else {
			if (storageType.equalsIgnoreCase("sqlite")) {
				ps2 = connection.prepareStatement(
						"CREATE TABLE IF NOT EXISTS island_ratings (id INTEGER PRIMARY KEY, rater_uuid TEXT NOT NULL, "
								+ "player_uuid TEXT NOT NULL, rating INTEGER NOT NULL);");
				ps2.executeUpdate();
				DbUtils.close(ps2);
			} else if (storageType.equalsIgnoreCase("mysql")) {
				ps2 = connection.prepareStatement(
						"CREATE TABLE IF NOT EXISTS island_ratings (id int(11) NOT NULL AUTO_INCREMENT, rater_uuid VARCHAR(36) NOT NULL, "
								+ "player_uuid VARCHAR(36) NOT NULL, rating INT(11) NOT NULL, PRIMARY KEY(id));");
				ps2.executeUpdate();
				DbUtils.close(ps2);
			}
		}
	}

	public void openConnection(String type) throws SQLException, ClassNotFoundException {
		synchronized (this) {
			if (type.equalsIgnoreCase("sqlite")) {
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/islandrate.db");
				if (connection == null || connection.isClosed()) {
					Bukkit.getConsoleSender().sendMessage("�2[IslandRate] �4WARNING: �cAn error occured while trying"
							+ " to connect to the MySQL/SQLite server/database.");
					return;
				}
			} else if (type.equalsIgnoreCase("mysql")) {
				Class.forName("com.mysql.jdbc.Driver");
				connection = DriverManager.getConnection(
						"jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, this.username,
						this.password);
				if (connection == null || connection.isClosed()) {
					Bukkit.getConsoleSender().sendMessage("�2[IslandRate] �4WARNING: �cAn error occured while trying"
							+ " to connect to the MySQL/SQLite server/database.");
					return;
				}
			} else {
				Bukkit.getConsoleSender()
						.sendMessage("�2[IslandRate] �4WARNING: �cUnable to establish a storage solution.");
				Bukkit.getConsoleSender()
						.sendMessage("�2[IslandRate] �4WARNING: �cUnknown storage type: " + type + "!");
				Bukkit.getConsoleSender().sendMessage(
						"�2[IslandRate] �4WARNING: �cPlease choose either 'SQLITE' or 'MYSQL' for the storage type!");
			}
		}
	}

	public Connection getConnection() {
		try {
			openConnection(storageType);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	private boolean doesTableExist(String tableName) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet rs = dbm.getTables(null, null, tableName, null);
		if (rs.next())
			return true;
		else
			return false;
	}

	@SuppressWarnings("unused")
	private boolean doesColumnExist(String tableName, String columnName) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet rs = dbm.getColumns(null, null, tableName, columnName);
		if (rs.next())
			return true;
		else
			return false;
	}

}
