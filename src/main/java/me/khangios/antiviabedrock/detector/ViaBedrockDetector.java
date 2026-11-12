package me.tiack2k9.antiviabedrock.detector;

import me.tiack2k9.antiviabedrock.AntiViaBedrockPlugin;
import me.tiack2k9.antiviabedrock.config.ConfigManager;
import me.tiack2k9.antiviabedrock.util.Fnv1;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Trình phân tích và phát hiện các dấu hiệu giả mạo Bedrock của ViaBedrock / ViaFabricPlus.
 * 
 * @author tiack2k9
 */
public class ViaBedrockDetector {

    // Chuỗi DeviceModel đặc trưng được hardcode trong ViaBedrock SkinProvider
    public static final String VIABEDROCK_DEVICE_MODEL_SIGNATURE = "MS-7E51 Micro-Star International Co., Ltd. (Unknown)";
    public static final String VIABEDROCK_DEVICE_MODEL_KEYWORD = "MS-7E51";

    private final AntiViaBedrockPlugin plugin;
    private final boolean floodgateAvailable;
    private final boolean geyserAvailable;

    public ViaBedrockDetector(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
        this.floodgateAvailable = plugin.isFloodgateAvailable();
        this.geyserAvailable = plugin.isGeyserAvailable();
    }

    /**
     * Phân tích thông tin người chơi khi kết nối để tìm dấu hiệu của ViaBedrock.
     */
    public DetectionResult analyzePlayer(UUID uuid, String username) {
        ConfigManager cfg = plugin.getConfigManager();

        // 1. Kiểm tra qua Floodgate API
        if (floodgateAvailable) {
            DetectionResult result = checkFloodgate(uuid, username, cfg);
            if (result.detected()) {
                return result;
            }
        }

        // 2. Kiểm tra qua Geyser API
        if (geyserAvailable) {
            DetectionResult result = checkGeyser(uuid, username, cfg);
            if (result.detected()) {
                return result;
            }
        }

        return DetectionResult.clean();
    }

    private DetectionResult checkFloodgate(UUID uuid, String username, ConfigManager cfg) {
        try {
            if (!FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
                return DetectionResult.clean();
            }

            FloodgatePlayer fp = FloodgateApi.getInstance().getPlayer(uuid);
            if (fp == null) {
                return DetectionResult.clean();
            }

            // A. Kiểm tra Device OS Win32 (ID 8)
            // Bedrock Windows chính hãng luôn chạy UWP (ID 7). ViaBedrock luôn gửi Win32 (ID 8).
            if (cfg.isCheckDeviceOsWin32()) {
                DeviceOs os = fp.getDeviceOs();
                if (os != null && (os == DeviceOs.WIN32 || os.name().equalsIgnoreCase("WIN32") || os.ordinal() == 8)) {
                    return DetectionResult.flagged(
                        DetectionType.DEVICE_OS_WIN32,
                        "DeviceOS is WIN32 (ID 8), instead of the official UWP/Windows 10 value (ID 7)."
                    );
                }
            }

            // B. Kiểm tra XUID giả mạo bằng thuật toán FNV1-64 của ViaBedrock
            if (cfg.isCheckFakeFnv1Xuid()) {
                String xuid = fp.getXuid();
                if (xuid != null && !xuid.isEmpty()) {
                    if (Fnv1.matchesViaBedrockFakeXuid(username, xuid)) {
                        return DetectionResult.flagged(
                            DetectionType.FAKE_FNV1_XUID,
                            "XUID (" + xuid + ") Exact match with ViaBedrock’s offline FNV1-64 hash."
                        );
                    }
                }
            }

            // C. Kiểm tra Device Model và thông tin mở rộng qua thuộc tính hoặc session nội bộ
            if (cfg.isCheckDeviceModel()) {
                String model = extractDeviceModel(fp);
                if (model != null && (model.equalsIgnoreCase(VIABEDROCK_DEVICE_MODEL_SIGNATURE) || model.contains(VIABEDROCK_DEVICE_MODEL_KEYWORD))) {
                    return DetectionResult.flagged(
                        DetectionType.DEVICE_MODEL,
                        "DeviceModel matches a ViaBedrock-specific string.: \"" + model + "\""
                    );
                }
            }

        } catch (Throwable t) {
            plugin.getLogger().warning("Error checking Floodgate for this player." + username + ": " + t.getMessage());
        }

        return DetectionResult.clean();
    }

    private DetectionResult checkGeyser(UUID uuid, String username, ConfigManager cfg) {
        try {
            if (!GeyserApi.api().isBedrockPlayer(uuid)) {
                return DetectionResult.clean();
            }

            GeyserConnection conn = GeyserApi.api().connectionByUuid(uuid);
            if (conn == null) {
                return DetectionResult.clean();
            }

            // A. Kiểm tra Platform Win32
            if (cfg.isCheckDeviceOsWin32()) {
                BedrockPlatform platform = conn.platform();
                if (platform != null && (platform == BedrockPlatform.WIN32 || platform.name().equalsIgnoreCase("WIN32"))) {
                    return DetectionResult.flagged(
                        DetectionType.DEVICE_OS_WIN32,
                        "Bedrock platform is WIN32 (emulated by ViaBedrock)."
                    );
                }
            }

            // B. Kiểm tra XUID FNV1
            if (cfg.isCheckFakeFnv1Xuid()) {
                String xuid = conn.xuid();
                if (xuid != null && !xuid.isEmpty()) {
                    if (Fnv1.matchesViaBedrockFakeXuid(username, xuid)) {
                        return DetectionResult.flagged(
                            DetectionType.FAKE_FNV1_XUID,
                            "XUID (" + xuid + ") Exact match with ViaBedrock’s offline FNV1 hash."
                        );
                    }
                }
            }

            // C. Trích xuất thông tin chi tiết (DeviceModel, MaxViewDistance, GuiScale)
            ClientDataInfo clientData = extractClientData(conn);
            if (clientData != null) {
                if (cfg.isCheckDeviceModel() && clientData.deviceModel != null) {
                    if (clientData.deviceModel.equalsIgnoreCase(VIABEDROCK_DEVICE_MODEL_SIGNATURE) || clientData.deviceModel.contains(VIABEDROCK_DEVICE_MODEL_KEYWORD)) {
                        return DetectionResult.flagged(
                            DetectionType.DEVICE_MODEL,
                            "DeviceModel matches ViaBedrock: \"" + clientData.deviceModel + "\""
                        );
                    }
                }

                if (cfg.isCheckMaxViewDistance() && clientData.maxViewDistance >= 96) {
                    return DetectionResult.flagged(
                        DetectionType.MAX_VIEW_DISTANCE_96,
                        "MaxViewDistance = " + clientData.maxViewDistance + " (ViaBedrock hardcoded 96 chunks)"
                    );
                }

                if (cfg.isCheckGuiScale() && clientData.guiScale == -1) {
                    return DetectionResult.flagged(
                        DetectionType.GUI_SCALE_MINUS_ONE,
                        "GuiScale = -1 (Unusual hardcoded ViaBedrock value.)"
                    );
                }
            }

        } catch (Throwable t) {
            plugin.getLogger().warning("Error checking Geyser for this player " + username + ": " + t.getMessage());
        }

        return DetectionResult.clean();
    }

    private String extractDeviceModel(FloodgatePlayer fp) {
        try {
            Object prop = fp.getProperty("device_model");
            if (prop != null) return prop.toString();

            prop = fp.getProperty("deviceModel");
            if (prop != null) return prop.toString();

            for (Method m : fp.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("devicemodel") && m.getParameterCount() == 0) {
                    Object val = m.invoke(fp);
                    if (val != null) return val.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private ClientDataInfo extractClientData(GeyserConnection conn) {
        try {
            Object rawClientData = null;
            for (Method m : conn.getClass().getMethods()) {
                if ((m.getName().equals("getClientData") || m.getName().equals("clientData")) && m.getParameterCount() == 0) {
                    rawClientData = m.invoke(conn);
                    break;
                }
            }

            if (rawClientData == null) {
                return null;
            }

            String deviceModel = null;
            int maxViewDistance = 0;
            int guiScale = 0;

            for (Method m : rawClientData.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("devicemodel")) {
                        Object val = m.invoke(rawClientData);
                        if (val != null) deviceModel = val.toString();
                    } else if (name.contains("viewdistance")) {
                        Object val = m.invoke(rawClientData);
                        if (val instanceof Number num) maxViewDistance = num.intValue();
                    } else if (name.contains("guiscale")) {
                        Object val = m.invoke(rawClientData);
                        if (val instanceof Number num) guiScale = num.intValue();
                    }
                }
            }

            return new ClientDataInfo(deviceModel, maxViewDistance, guiScale);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static class ClientDataInfo {
        public final String deviceModel;
        public final int maxViewDistance;
        public final int guiScale;

        public ClientDataInfo(String deviceModel, int maxViewDistance, int guiScale) {
            this.deviceModel = deviceModel;
            this.maxViewDistance = maxViewDistance;
            this.guiScale = guiScale;
        }
    }
}
