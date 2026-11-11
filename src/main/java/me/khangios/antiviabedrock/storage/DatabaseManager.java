package me.khangios.antiviabedrock.storage;

import me.khangios.antiviabedrock.AntiViaBedrockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Quản lý cơ sở dữ liệu vi phạm của người chơi (lưu trữ trong data.yml).
 * 
 * @author khangios
 */
public class DatabaseManager {

    private final AntiViaBedrockPlugin plugin;
    private final File dataFile;
    private final Map<UUID, PlayerViolation> violations = new ConcurrentHashMap<>();

    public DatabaseManager(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public synchronized void load() {
        violations.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String name = section.getString(key + ".name", "Unknown");
                String ip = section.getString(key + ".ip", "0.0.0.0");
                int count = section.getInt(key + ".violations", 0);
                long first = section.getLong(key + ".first-violation", System.currentTimeMillis());
                long last = section.getLong(key + ".last-violation", System.currentTimeMillis());
                String reason = section.getString(key + ".last-reason", "");
                boolean banned = section.getBoolean(key + ".banned", false);

                violations.put(uuid, new PlayerViolation(uuid, name, ip, count, first, last, reason, banned));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Không thể nạp dữ liệu người chơi " + key, e);
            }
        }
        plugin.getLogger().info("Đã nạp " + violations.size() + " hồ sơ vi phạm từ data.yml.");
    }

    public void save(boolean async) {
        Runnable saveTask = () -> {
            synchronized (this) {
                YamlConfiguration config = new YamlConfiguration();
                for (Map.Entry<UUID, PlayerViolation> entry : violations.entrySet()) {
                    String path = "players." + entry.getKey().toString();
                    PlayerViolation pv = entry.getValue();
                    config.set(path + ".name", pv.getName());
                    config.set(path + ".ip", pv.getIp());
                    config.set(path + ".violations", pv.getViolations());
                    config.set(path + ".first-violation", pv.getFirstViolation());
                    config.set(path + ".last-violation", pv.getLastViolation());
                    config.set(path + ".last-reason", pv.getLastReason());
                    config.set(path + ".banned", pv.isBanned());
                }

                try {
                    config.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Lỗi khi lưu data.yml!", e);
                }
            }
        };

        if (async && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }

    public PlayerViolation getViolation(UUID uuid) {
        return violations.get(uuid);
    }

    public synchronized PlayerViolation recordViolation(UUID uuid, String name, String ip, String reason) {
        PlayerViolation pv = violations.computeIfAbsent(uuid, k -> new PlayerViolation(uuid, name, ip));
        pv.setName(name);
        if (ip != null && !ip.isEmpty()) {
            pv.setIp(ip);
        }
        pv.incrementViolations(reason);
        save(true);
        return pv;
    }

    public synchronized void setBanned(UUID uuid, boolean banned) {
        PlayerViolation pv = violations.get(uuid);
        if (pv != null) {
            pv.setBanned(banned);
            save(true);
        }
    }

    public boolean isBanned(UUID uuid) {
        PlayerViolation pv = violations.get(uuid);
        return pv != null && pv.isBanned();
    }

    public synchronized boolean resetViolation(UUID uuid) {
        PlayerViolation removed = violations.remove(uuid);
        if (removed != null) {
            save(true);
            return true;
        }
        return false;
    }

    public Map<UUID, PlayerViolation> getAllViolations() {
        return violations;
    }
}
