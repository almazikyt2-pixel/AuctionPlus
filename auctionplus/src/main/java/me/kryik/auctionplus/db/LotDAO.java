package me.kryik.auctionplus.db;

import me.kryik.auctionplus.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class LotDAO {
    private final Database db;

    public LotDAO(Database db) { this.db = db; }

    public int createLot(UUID seller, ItemStack item, int price, int expireDays) {
        String sql = "INSERT INTO lots (seller, item, price, createdAt, expiresAt) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.toString());
            ps.setBytes(2, ItemSerializer.toBytes(item));
            ps.setInt(3, price);
            long now = Instant.now().getEpochSecond();
            long exp = Instant.now().plus(expireDays, ChronoUnit.DAYS).getEpochSecond();
            ps.setLong(4, now);
            ps.setLong(5, exp);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteLot(int id) {
        String sql = "DELETE FROM lots WHERE id = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Map<Integer, ItemStack> getLotsPaged(int page, int pageSize) {
        String sql = "SELECT id, item FROM lots ORDER BY id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, ItemStack> map = new LinkedHashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    ItemStack item = ItemSerializer.fromBytes(rs.getBytes("item"));
                    map.put(id, item);
                }
                return map;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<LotRecord> findLot(int id) {
        String sql = "SELECT id, seller, item, price, createdAt, expiresAt FROM lots WHERE id = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new LotRecord(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("seller")),
                        ItemSerializer.fromBytes(rs.getBytes("item")),
                        rs.getInt("price"),
                        rs.getLong("createdAt"),
                        rs.getLong("expiresAt")
                ));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public LotRecord getById(int id) {
        return findLot(id).orElse(null);
    }

    public boolean removeLot(LotRecord lot) {
        return deleteLot(lot.id);
    }

    // 🔹 активные лоты игрока
    public List<LotRecord> getPlayerLots(UUID player) {
        String sql = "SELECT id, seller, item, price, createdAt, expiresAt FROM lots WHERE seller = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<LotRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new LotRecord(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("seller")),
                            ItemSerializer.fromBytes(rs.getBytes("item")),
                            rs.getInt("price"),
                            rs.getLong("createdAt"),
                            rs.getLong("expiresAt")
                    ));
                }
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // 🔹 просроченные лоты
    public List<LotRecord> getExpiredLots() {
        String sql = "SELECT id, seller, item, price, createdAt, expiresAt FROM lots WHERE expiresAt < ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setLong(1, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                List<LotRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new LotRecord(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("seller")),
                            ItemSerializer.fromBytes(rs.getBytes("item")),
                            rs.getInt("price"),
                            rs.getLong("createdAt"),
                            rs.getLong("expiresAt")
                    ));
                }
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // 🔹 переносим в expired_lots
    public void saveExpiredLot(UUID seller, ItemStack item) {
        String sql = "INSERT INTO expired_lots (seller, item, expiredAt) VALUES (?, ?, ?)";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, seller.toString());
            ps.setBytes(2, ItemSerializer.toBytes(item));
            ps.setLong(3, Instant.now().getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<ItemStack> getExpiredLots(UUID seller) {
        String sql = "SELECT id, item FROM expired_lots WHERE seller = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, seller.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<ItemStack> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(ItemSerializer.fromBytes(rs.getBytes("item")));
                }
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void deleteExpiredLots(UUID seller) {
        String sql = "DELETE FROM expired_lots WHERE seller = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, seller.toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public static class LotRecord {
        public final int id;
        public final UUID seller;
        public final ItemStack item;
        public final int price;
        public final long createdAt;
        public final long expiresAt;

        public LotRecord(int id, UUID seller, ItemStack item, int price, long createdAt, long expiresAt) {
            this.id = id; this.seller = seller; this.item = item;
            this.price = price; this.createdAt = createdAt; this.expiresAt = expiresAt;
        }
    }
}
