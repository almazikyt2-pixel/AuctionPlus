package me.kryik.auctionplus;

import me.kryik.auctionplus.db.Database;
import me.kryik.auctionplus.db.LotDAO;
import me.kryik.auctionplus.lot.AhCommand;
import me.kryik.auctionplus.lot.AhMenu;
import me.kryik.auctionplus.util.MessageUtil;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionPlus extends JavaPlugin {

    private static AuctionPlus instance;
    private PlayerPointsAPI points;
    private Database database;
    private LotDAO lotDAO;

    public static AuctionPlus get() { return instance; }
    public PlayerPointsAPI points() { return points; }
    public LotDAO lotDAO() { return lotDAO; }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        MessageUtil.load(); // загрузка сообщений

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            getLogger().severe("PlayerPoints не найден! Отключаюсь...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        points = PlayerPoints.getInstance().getAPI();

        // БД
        database = new Database(this);
        database.init();
        lotDAO = new LotDAO(database);

        // Команды
        getCommand("ah").setExecutor(new AhCommand());

        // Команда перезагрузки сообщений
        getCommand("auctionplus").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                MessageUtil.reload();
                sender.sendMessage(MessageUtil.get("prefix") + "Сообщения перезагружены.");
                return true;
            }
            sender.sendMessage(MessageUtil.get("prefix") + "Используйте: /auctionplus reload");
            return true;
        });

        // GUI листенеры
        Bukkit.getPluginManager().registerEvents(new AhMenu(), this);
        getLogger().info("AuctionPlus включён");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    // чтобы не ругался на отсутствующую реализацию метода в Java 8
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return super.onCommand(sender, command, label, args);
    }
}
