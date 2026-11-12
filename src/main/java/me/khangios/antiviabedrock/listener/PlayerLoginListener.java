package me.tiack2k9.antiviabedrock.listener;

import me.tiack2k9.antiviabedrock.AntiViaBedrockPlugin;
import me.tiack2k9.antiviabedrock.detector.DetectionResult;
import me.tiack2k9.antiviabedrock.detector.ViaBedrockDetector;
import me.tiack2k9.antiviabedrock.detector.ViaFabricPlusJavaDetector;
import me.tiack2k9.antiviabedrock.punishment.PunishmentManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Lắng nghe các sự kiện kết nối của người chơi để phát hiện và xử phạt.
 * 
 * @author tiack2k9
 */
public class PlayerLoginListener implements Listener, PluginMessageListener {

    private final AntiViaBedrockPlugin plugin;
    private final ViaBedrockDetector bedrockDetector;
    private final ViaFabricPlusJavaDetector javaDetector;
    private final PunishmentManager punishmentManager;

    public PlayerLoginListener(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
        this.bedrockDetector = plugin.getBedrockDetector();
        this.javaDetector = plugin.getJavaDetector();
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        InetAddress address = event.getAddress();
        String ip = address != null ? address.getHostAddress() : "0.0.0.0";

        // 1. Kiểm tra nếu người chơi đã bị cấm trước đó trong database
        if (plugin.getDatabaseManager().isBanned(uuid)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, plugin.getConfigManager().getBanMessage());
            return;
        }

        // 2. Kiểm tra các dấu hiệu của ViaBedrock
        DetectionResult result = bedrockDetector.analyzePlayer(uuid, name);
        if (result.detected()) {
            punishmentManager.handleViolation(uuid, name, ip, result, null, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiviabedrock.bypass")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "0.0.0.0";

        // Kiểm tra sau khi join phòng trường hợp một số dữ liệu Geyser chỉ nạp sau khi hoàn tất handshake
        DetectionResult bedrockResult = bedrockDetector.analyzePlayer(uuid, name);
        if (bedrockResult.detected()) {
            punishmentManager.handleViolation(uuid, name, ip, bedrockResult, player, null);
            return;
        }

        // Kiểm tra client brand (Java kết nối trực tiếp)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.hasPermission("antiviabedrock.bypass")) {
                return;
            }
            DetectionResult brandResult = javaDetector.checkClientBrand(player);
            if (brandResult.detected()) {
                punishmentManager.handleViolation(uuid, name, ip, brandResult, player, null);
            }
        }, 5L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (player.hasPermission("antiviabedrock.bypass")) {
            return;
        }

        DetectionResult result = javaDetector.checkPluginChannel(channel);
        if (result.detected()) {
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "0.0.0.0";
            punishmentManager.handleViolation(player.getUniqueId(), player.getName(), ip, result, player, null);
        }
    }
}
