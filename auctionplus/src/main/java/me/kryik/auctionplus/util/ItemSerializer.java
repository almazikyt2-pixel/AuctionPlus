package me.kryik.auctionplus.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;

public class ItemSerializer {

    public static byte[] toBytes(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(item);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Item serialize error", e);
        }
    }

    public static ItemStack fromBytes(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Item deserialize error", e);
        }
    }
}
