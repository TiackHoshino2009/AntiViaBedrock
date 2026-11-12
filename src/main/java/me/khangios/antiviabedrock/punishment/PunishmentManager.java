package me.tiack2k9.antiviabedrock.punishment;

import me.tiack2k9.antiviabedrock.AntiViaBedrockPlugin;
import me.tiack2k9.antiviabedrock.config.ConfigManager;
import me.tiack2k9.antiviabedrock.detector.DetectionResult;
import me.tiack2k9.antiviabedrock.storage.DatabaseManager;
import me.tiack2k9.antiviabedrock.storage.PlayerViolation;
import me.tiack2k9.antiviabedrock.util.ColorUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Quản lý xử phạt người chơi vi phạm:
 * - Lần 1: Kick với thông báo yêu cầu dùng bản Bedrock chính thức.
 * - Lần 2: Ban khỏi máy chủ.
 * 
 * @author tiack2k9
 */
public class PunishmentManager {

    private final AntiViaBedrockPlugin plugin;

    public PunishmentManager(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Xử lý khi phát hiện vi phạm.
     */
    public void handleViolation(UUID uuid, String name, String ip, DetectionResult result, Player player, AsyncPlayerPreLoginEvent preLoginEvent) {
        ConfigManager cfg = plugin.getConfigManager();
        DatabaseManager db = plugin.getDatabaseManager();

        PlayerViolation pv = db.recordViolation(uuid, name, ip, result.details());
        int violations = pv.getViolations();

        boolean isBan = cfg.isBanOnSecondOffense() && violations >= 2;
        String actionName = isBan ? "BAN" : "KICK";

        // Gửi log console
        if (cfg.isLogToConsole()) {
            plugin.getLogger().warning(String.format(
                "[DETECTED] Player %s (%s) triggered violation #%d: %s [%s] → Action: %s",
                name, uuid, violations, result.type().name(), result.details(), actionName
            ));
        }

        // Gửi thông báo cho Staff/Admin
        if (cfg.isNotifyAdmins()) {
            notifyStaff(name, result, violations, actionName);
        }

        // Gửi webhook Discord nếu có cấu hình
        sendDiscordWebhook(name, uuid, ip, result, violations, actionName);

        if (isBan) {
            // Xử lý BAN (Lần 2)
            db.setBanned(uuid, true);
            applyBan(uuid, name, ip, result);

            String banMsg = cfg.getBanMessage();
            if (preLoginEvent != null) {
                preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMsg);
            } else if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(banMsg));
            }
        } else {
            // Xử lý KICK (Lần 1)
            String kickMsg = cfg.getKickMessage();
            if (preLoginEvent != null) {
                preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
            } else if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(kickMsg));
            }
        }
    }

    private void applyBan(UUID uuid, String name, String ip, DetectionResult result) {
        ConfigManager cfg = plugin.getConfigManager();
        String reason = "[Anti-ViaBedrock] Fake Bedrock Bypass: " + result.type().name();

        if ("COMMAND".equalsIgnoreCase(cfg.getBanMethod())) {
            String cmd = cfg.getBanCommand()
                .replace("%player%", name)
                .replace("%reason%", reason)
                .replace("%ip%", ip != null ? ip : "0.0.0.0");

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
        } else {
            // Builtin Bukkit/Paper Ban
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.getBanList(BanList.Type.NAME).addBan(name, reason, null, "AntiViaBedrock");
                    if (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0")) {
                        Bukkit.getBanList(BanList.Type.IP).addBan(ip, reason, null, "AntiViaBedrock");
                    }
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to ban the player." + name, t);
                }
            });
        }
    }

    private void notifyStaff(String name, DetectionResult result, int violations, String action) {
        ConfigManager cfg = plugin.getConfigManager();
        String format = cfg.getAdminAlertFormat()
            .replace("%player%", name)
            .replace("%type%", result.type().getDescription())
            .replace("%details%", result.details())
            .replace("%violation%", String.valueOf(violations))
            .replace("%action%", action);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("antiviabedrock.alerts") || staff.hasPermission("antiviabedrock.admin")) {
                staff.sendMessage(format);
            }
        }
    }

    private void sendDiscordWebhook(String name, UUID uuid, String ip, DetectionResult result, int violations, String action) {
        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(webhookUrl.trim()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "AntiViaBedrock-Plugin");
                conn.setDoOutput(true);

                String json = String.format(
                    "{\"embeds\":[{\"title\":\"🚨 ViaBedRock Detected\",\"color\":%d,\"fields\":[" +
                    "{\"name\":\"Player\",\"value\":\"%s (%s)\",\"inline\":true}," +
                    "{\"name\":\"Action\",\"value\":\"%s (Lần %d)\",\"inline\":true}," +
                    "{\"name\":\"Methood\",\"value\":\"%s\",\"inline\":false}," +
                    "{\"name\":\"Details\",\"value\":\"%s\",\"inline\":false}" +
                    "]}]}",
                    action.equals("BAN") ? 16711680 : 16776960,
                    escapeJson(name),
                    uuid.toString(),
                    action,
                    violations,
                    escapeJson(result.type().getDescription()),
                    escapeJson(result.details())
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Throwable ignored) {
            }
        });
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
