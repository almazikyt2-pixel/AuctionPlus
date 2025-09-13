package me.kryik.auctionplus.lot;

import me.kryik.auctionplus.AuctionPlus;
import me.kryik.auctionplus.db.LotDAO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class AuctionActions {

    public static void buy(Player buyer, int lotId) {
        Optional<LotDAO.LotRecord> opt = AuctionPlus.get().lotDAO().findLot(lotId);
        if (opt.isEmpty()) {
            buyer.sendMessage("§cЛот не найден");
            return;
        }

        LotDAO.LotRecord lot = opt.get();
        UUID sellerId = lot.seller;

        if (sellerId.equals(buyer.getUniqueId())) {
            buyer.sendMessage("§cНельзя купить свой лот");
            return;
        }

        int price = lot.price;
        var pp = AuctionPlus.get().points();
        int balance = pp.look(buyer.getUniqueId());
        if (balance < price) {
            buyer.sendMessage("§cНедостаточно рублей. Нужно: " + price);
            return;
        }

        // снимаем деньги у покупателя
        pp.take(buyer.getUniqueId(), price);

        // выдаём предмет
        ItemStack item = lot.item.clone();
        var leftover = buyer.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            pp.give(buyer.getUniqueId(), price);
            buyer.sendMessage("§cНет места в инвентаре");
            return;
        }

        AuctionPlus.get().lotDAO().deleteLot(lotId);

        // начисляем продавцу
        pp.give(sellerId, price);

        buyer.sendMessage("§aВы купили лот §e#" + lotId + " §aза §e" + price + " руб.");
        Player sellerOnline = Bukkit.getPlayer(sellerId);
        if (sellerOnline != null) {
            sellerOnline.sendMessage("§aВаш лот §e#" + lotId + " §aкуплен за §e" + price + " руб.");
        }
    }

    public static void removeLot(Player seller, int lotId) {
        Optional<LotDAO.LotRecord> opt = AuctionPlus.get().lotDAO().findLot(lotId);
        if (opt.isEmpty()) {
            seller.sendMessage("§cЛот не найден");
            return;
        }
        LotDAO.LotRecord lot = opt.get();

        if (!lot.seller.equals(seller.getUniqueId())) {
            seller.sendMessage("§cЭто не ваш лот!");
            return;
        }

        ItemStack item = lot.item.clone();
        var leftover = seller.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            seller.sendMessage("§cНет места в инвентаре! Лот перемещён в /ah expired.");
            AuctionPlus.get().lotDAO().saveExpiredLot(seller.getUniqueId(), item);
        }

        AuctionPlus.get().lotDAO().deleteLot(lotId);
        seller.sendMessage("§aВы сняли лот §e#" + lotId);
    }

    public static void removeExpiredLots() {
        var expired = AuctionPlus.get().lotDAO().getExpiredLots();
        for (var lot : expired) {
            AuctionPlus.get().lotDAO().deleteLot(lot.id);
            AuctionPlus.get().lotDAO().saveExpiredLot(lot.seller, lot.item);

            Player seller = Bukkit.getPlayer(lot.seller);
            if (seller != null && seller.isOnline()) {
                seller.sendMessage("§eВаш лот §6#" + lot.id + " §eистёк и предмет перемещён в /ah expired.");
            }
        }
    }
}
