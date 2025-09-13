package me.kryik.auctionplus.util;

import me.kryik.auctionplus.AuctionPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageUtil {

    private static FileConfiguration messages;

    public static void load() {
        File file = new File(AuctionPlus.get().getDataFolder(), "messages.yml");
        if (!file.exists()) {
            AuctionPlus.get().saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public static void reload() {
        load();
    }

    public static String get(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                messages.getString(path, "&cСообщение не найдено: " + path));
    }
}
