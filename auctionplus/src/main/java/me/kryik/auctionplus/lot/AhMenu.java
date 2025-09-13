package me.kryik.auctionplus.lot;

import me.kryik.auctionplus.AuctionPlus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class AhMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final NamespacedKey LOT_KEY = new NamespacedKey(AuctionPlus.get(), "lot-id");

    public static void open(Player p, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "Аукцион | Стр. " + page);

        Map<Integer, ItemStack> lots = AuctionPlus.get().lotDAO().getLotsPaged(page, PAGE_SIZE);
        int slot = 0;
        for (Map.Entry<Integer, ItemStack> e : lots.entrySet()) {
            if (slot >= PAGE_SIZE) break;
            int id = e.getKey();
            ItemStack item = e.getValue().clone();

            ItemMeta meta = item.getItemMeta();
            var lotOpt = AuctionPlus.get().lotDAO().findLot(id);

            int price = lotOpt.map(l -> l.price).orElse(0);
            String seller = lotOpt.map(l -> Bukkit.getOfflinePlayer(l.seller).getName()).orElse("Неизвестно");

            var lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<String>();
            lore.add("§7Цена: §a" + price + " руб.");
            lore.add("§7Продавец: §f" + seller);
            lore.add("§eЛКМ — купить");
            lore.add("§eShift+ПКМ — снять (если ваш)");
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(LOT_KEY, PersistentDataType.INTEGER, id);

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        inv.setItem(53, navItem(Material.ARROW, "§eСледующая страница"));
        inv.setItem(45, navItem(Material.ARROW, "§eПредыдущая страница"));

        p.openInventory(inv);
    }

    private static ItemStack navItem(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        it.setItemMeta(im);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player p)) return;
        if (e.getView().getTitle() == null || !e.getView().getTitle().startsWith("Аукцион")) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == 53) {
            int page = getPage(e.getView().getTitle());
            open(p, page + 1);
            return;
        }
        if (slot == 45) {
            int page = getPage(e.getView().getTitle());
            open(p, Math.max(1, page - 1));
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            Integer id = meta.getPersistentDataContainer().get(LOT_KEY, PersistentDataType.INTEGER);

            if (id != null) {
                if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    var lotOpt = AuctionPlus.get().lotDAO().findLot(id);
                    if (lotOpt.isPresent() && lotOpt.get().seller.equals(p.getUniqueId())) {
                        AuctionActions.removeLot(p, id);
                        Bukkit.getScheduler().runTask(AuctionPlus.get(), () -> {
                            int page = getPage(e.getView().getTitle());
                            open(p, page);
                        });
                        return;
                    } else {
                        p.sendMessage("§cВы можете снять только свои лоты!");
                        return;
                    }
                }

                // Обычный клик — покупка
                AuctionActions.buy(p, id);
                Bukkit.getScheduler().runTask(AuctionPlus.get(), () -> {
                    int page = getPage(e.getView().getTitle());
                    open(p, page);
                });
            }
        }
    }

    private int getPage(String title) {
        try {
            return Integer.parseInt(title.replaceAll("[^0-9]", ""));
        } catch (Exception ex) {
            return 1;
        }
    }
}
