package me.kryik.auctionplus.lot;

import me.kryik.auctionplus.AuctionPlus;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class AhCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Команда только от игрока");
            return true;
        }
        if (args.length == 0) {
            AhMenu.open(p, 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) { p.sendMessage("§cИспользование: /ah sell <цена>"); return true; }
            int price;
            try { price = Integer.parseInt(args[1]); } catch (Exception e) { p.sendMessage("§cЦена — число"); return true; }

            int min = AuctionPlus.get().getConfig().getInt("min-price", 10);
            if (price < min) { p.sendMessage("§cМинимальная цена: " + min); return true; }

            // 🔹 проверка лимита
            int limit = getLotLimit(p);
            int currentLots = AuctionPlus.get().lotDAO().getPlayerLots(p.getUniqueId()).size();
            if (currentLots >= limit) {
                p.sendMessage("§cВы достигли лимита лотов (" + limit + ")");
                return true;
            }

            ItemStack inHand = p.getInventory().getItem(EquipmentSlot.HAND);
            if (inHand == null || inHand.getType().isAir()) { p.sendMessage("§cВозьми предмет в основную руку"); return true; }

            NamespacedKey key = inHand.getType().getKey();
            if (!"minecraft".equalsIgnoreCase(key.getNamespace())) {
                p.sendMessage("§cНельзя продавать модовые предметы.");
                return true;
            }

            ItemStack toSell = inHand.clone();
            inHand.setAmount(0);

            int id = AuctionPlus.get().lotDAO().createLot(p.getUniqueId(), toSell, price,
                    AuctionPlus.get().getConfig().getInt("lot-expire-days", 365));
            p.sendMessage("§aЛот выставлен! ID: §e" + id + "§a, цена: §e" + price + " руб.");
            return true;
        }

        if (args[0].equalsIgnoreCase("buy")) {
            if (args.length < 2) { p.sendMessage("§cИспользование: /ah buy <id>"); return true; }
            int id;
            try { id = Integer.parseInt(args[1]); } catch (Exception e) { p.sendMessage("§cID — число"); return true; }

            AuctionActions.buy(p, id);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) { p.sendMessage("§cИспользование: /ah remove <id>"); return true; }
            int id;
            try { id = Integer.parseInt(args[1]); } catch (Exception e) { p.sendMessage("§cID — число"); return true; }
            AuctionActions.removeLot(p, id);
            return true;
        }

        if (args[0].equalsIgnoreCase("expired")) {
            var items = AuctionPlus.get().lotDAO().getExpiredLots(p.getUniqueId());
            if (items.isEmpty()) {
                p.sendMessage("§eУ вас нет просроченных лотов.");
                return true;
            }
            for (ItemStack it : items) {
                var leftover = p.getInventory().addItem(it);
                if (!leftover.isEmpty()) {
                    p.sendMessage("§cНет места для всех предметов. Освободите инвентарь и повторите.");
                    return true;
                }
            }
            AuctionPlus.get().lotDAO().deleteExpiredLots(p.getUniqueId());
            p.sendMessage("§aВы забрали все просроченные предметы.");
            return true;
        }

        p.sendMessage("§e/ah — открыть аукцион\n§e/ah sell <цена>\n§e/ah buy <id>\n§e/ah remove <id>\n§e/ah expired");
        return true;
    }

    private int getLotLimit(Player p) {
        for (int i = 100; i > 0; i--) {
            if (p.hasPermission("auctionplus.limit." + i)) return i;
        }
        return AuctionPlus.get().getConfig().getInt("default-lot-limit", 5);
    }
}
