package me.kryik.auctionplus.db;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final Plugin plugin;
    private Connection connection;

    public Database(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File folder = new File(plugin.getDataFolder(), "");
            if (!folder.exists()) folder.mkdirs();
            File db = new File(folder, "auction.db");
            String url = "jdbc:sqlite:" + db.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS lots (" +
                                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                " seller TEXT NOT NULL," +
                                " item BLOB NOT NULL," +
                                " price INTEGER NOT NULL," +
                                " createdAt INTEGER NOT NULL," +
                                " expiresAt INTEGER NOT NULL" +
                                ")"
                );

                // 🔹 таблица для просроченных лотов
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS expired_lots (" +
                                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                " seller TEXT NOT NULL," +
                                " item BLOB NOT NULL," +
                                " expiredAt INTEGER NOT NULL" +
                                ")"
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQLite init error", e);
        }
    }

    public Connection conn() { return connection; }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}
