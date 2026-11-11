package me.khangios.antiviabedrock.detector;

import me.khangios.antiviabedrock.AntiViaBedrockPlugin;
import me.khangios.antiviabedrock.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Trình phát hiện người chơi sử dụng ViaFabricPlus khi kết nối qua cổng Java
 * hoặc thông qua các kênh tin nhắn plugin đặc trưng.
 * 
 * @author khangios
 */
public class ViaFabricPlusJavaDetector {

    public static final String VIABEDROCK_CONFIRM_CHANNEL = "viabedrockutility:confirm";
    public static final String VIABEDROCK_DATA_CHANNEL = "viabedrockutility:data";

    private final AntiViaBedrockPlugin plugin;

    public ViaFabricPlusJavaDetector(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Kiểm tra client brand của người chơi khi join vào server.
     */
    public DetectionResult checkClientBrand(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isCheckClientBrand()) {
            return DetectionResult.clean();
        }

        try {
            String brand = player.getClientBrandName();
            if (brand != null && !brand.isEmpty()) {
                String lowerBrand = brand.toLowerCase(Locale.ROOT);
                for (String blocked : cfg.getBlockedBrands()) {
                    if (lowerBrand.contains(blocked.toLowerCase(Locale.ROOT))) {
                        return DetectionResult.flagged(
                            DetectionType.JAVA_BLOCKED_BRAND,
                            "Client Brand bị chặn: \"" + brand + "\" (Khớp với: " + blocked + ")"
                        );
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return DetectionResult.clean();
    }

    /**
     * Kiểm tra khi nhận gói tin từ kênh plugin message.
     */
    public DetectionResult checkPluginChannel(String channel) {
        if (!plugin.getConfigManager().isCheckViabedrockChannels()) {
            return DetectionResult.clean();
        }

        if (channel.equalsIgnoreCase(VIABEDROCK_CONFIRM_CHANNEL) || channel.equalsIgnoreCase(VIABEDROCK_DATA_CHANNEL)) {
            return DetectionResult.flagged(
                DetectionType.VIABEDROCK_CHANNEL,
                "Phát hiện kênh giao tiếp độc quyền của ViaBedrock: " + channel
            );
        }

        return DetectionResult.clean();
    }
}
