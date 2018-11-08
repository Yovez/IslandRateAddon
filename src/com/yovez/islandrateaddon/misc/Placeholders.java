package com.yovez.islandrateaddon.misc;

import com.yovez.islandrateaddon.IslandRateAddon;

import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

public class Placeholders {

	IslandRateAddon plugin;

	public Placeholders(IslandRateAddon plugin) {
		this.plugin = plugin;
		plugin.getPlugin().getPlaceholdersManager().registerPlaceholder(plugin, "rate_top_player",
				new PlaceholderReplacer() {

					@Override
					public String onReplace(User arg0) {
						if (arg0 != null) {
							if (plugin.getAPI().getTopRated() == null)
								return "No top rated player found";
							return plugin.getAPI().getTopRated().getName();
						}
						return null;
					}

				});
		plugin.getPlugin().getPlaceholdersManager().registerPlaceholder(plugin, "rate_top_amount",
				new PlaceholderReplacer() {

					@Override
					public String onReplace(User arg0) {
						if (arg0 != null) {
							return String.valueOf(plugin.getAPI().getTotalRatings(plugin.getAPI().getTopRated()));
						}
						return null;
					}

				});
		plugin.getPlugin().getPlaceholdersManager().registerPlaceholder(plugin, "rate_total_voters",
				new PlaceholderReplacer() {

					@Override
					public String onReplace(User arg0) {
						if (arg0 != null) {
							return String.valueOf(plugin.getAPI().getTotalNumOfRaters(arg0.getPlayer()));
						}
						return null;
					}

				});
		plugin.getPlugin().getPlaceholdersManager().registerPlaceholder(plugin, "rate_average",
				new PlaceholderReplacer() {

					@Override
					public String onReplace(User arg0) {
						if (arg0 != null) {
							return String.valueOf(plugin.getAPI().getAverageRating(arg0.getPlayer()));
						}
						return null;
					}

				});
		plugin.getPlugin().getPlaceholdersManager().registerPlaceholder(plugin, "rate_total_ratings",
				new PlaceholderReplacer() {

					@Override
					public String onReplace(User arg0) {
						if (arg0 != null) {
							return String.valueOf(plugin.getAPI().getTotalRatings());
						}
						return null;
					}

				});
		plugin.getPlugin().getPlaceholdersManager().registerPlaceholder(plugin, "rate_total_ratings_player",
				new PlaceholderReplacer() {

					@Override
					public String onReplace(User arg0) {
						if (arg0 != null) {
							return String.valueOf(plugin.getAPI().getTotalRatings(arg0.getPlayer()));
						}
						return null;
					}

				});
	}

}
