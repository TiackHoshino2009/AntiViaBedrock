package me.tiack2k9.antiviabedrock.config;

import me.tiack2k9.antiviabedrock.AntiViaBedrockPlugin;
import me.tiack2k9.antiviabedrock.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý cấu hình plugin từ config.yml.
 * 
 * @author tiack2k9
 */
public class ConfigManager {

    private final AntiViaBedrockPlugin plugin;

    // Detection
    private boolean checkDeviceModel;
    private boolean checkDeviceOsWin32;
    private boolean checkFakeFnv1Xuid;
    private boolean checkMaxViewDistance;
    private boolean checkGuiScale;
    private boolean checkViabedrockChannels;

    // Java Detection
    private boolean checkClientBrand;
    private List<String> blockedBrands = new ArrayList<>();

    // Punishment
    private boolean banOnSecondOffense;
    private String kickMessage;
    private String banMessage;
    private String banMethod;
    private String banCommand;
    private String banDuration;

    // Notifications
    private boolean notifyAdmins;
    private String adminAlertFormat;
    private boolean logToConsole;
    private String discordWebhookUrl;

    public ConfigManager(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Detection
        this.checkDeviceModel = config.getBoolean("detection.check-device-model", true);
        this.checkDeviceOsWin32 = config.getBoolean("detection.check-device-os-win32", true);
        this.checkFakeFnv1Xuid = config.getBoolean("detection.check-fake-fnv1-xuid", true);
        this.checkMaxViewDistance = config.getBoolean("detection.check-max-view-distance", true);
        this.checkGuiScale = config.getBoolean("detection.check-gui-scale", true);
        this.checkViabedrockChannels = config.getBoolean("detection.check-viabedrock-channels", true);

        // Java detection
        this.checkClientBrand = config.getBoolean("java-detection.check-client-brand", true);
        this.blockedBrands = config.getStringList("java-detection.blocked-brands");
        if (this.blockedBrands.isEmpty()) {
            this.blockedBrands.add("viafabricplus");
            this.blockedBrands.add("fabric");
        }

        // Punishment
        this.banOnSecondOffense = config.getBoolean("punishment.ban-on-second-offense", true);
        this.kickMessage = ColorUtil.color(config.getString("punishment.kick-message", "&c[Anti-ViaBedrock] &fVui lòng dùng bản Bedrock chính thức!"));
        this.banMessage = ColorUtil.color(config.getString("punishment.ban-message", "&c[Anti-ViaBedrock] &fBạn đã bị cấm khỏi máy chủ!"));
        this.banMethod = config.getString("punishment.ban-method", "BUILTIN").toUpperCase();
        this.banCommand = config.getString("punishment.ban-command", "ban %player% [Anti-ViaBedrock] Fake Bedrock Bypass");
        this.banDuration = config.getString("punishment.ban-duration", "permanent");

        // Notifications
        this.notifyAdmins = config.getBoolean("notifications.notify-admins", true);
        this.adminAlertFormat = ColorUtil.color(config.getString("notifications.admin-alert-format", "&8[&cAntiViaBedrock&8] &ePhát hiện: &c%player%"));
        this.logToConsole = config.getBoolean("notifications.log-to-console", true);
        this.discordWebhookUrl = config.getString("notifications.discord-webhook-url", "");
    }

    public boolean isCheckDeviceModel() {
        return checkDeviceModel;
    }

    public boolean isCheckDeviceOsWin32() {
        return checkDeviceOsWin32;
    }

    public boolean isCheckFakeFnv1Xuid() {
        return checkFakeFnv1Xuid;
    }

    public boolean isCheckMaxViewDistance() {
        return checkMaxViewDistance;
    }

    public boolean isCheckGuiScale() {
        return checkGuiScale;
    }

    public boolean isCheckViabedrockChannels() {
        return checkViabedrockChannels;
    }

    public boolean isCheckClientBrand() {
        return checkClientBrand;
    }

    public List<String> getBlockedBrands() {
        return blockedBrands;
    }

    public boolean isBanOnSecondOffense() {
        return banOnSecondOffense;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getBanMessage() {
        return banMessage;
    }

    public String getBanMethod() {
        return banMethod;
    }

    public String getBanCommand() {
        return banCommand;
    }

    public String getBanDuration() {
        return banDuration;
    }

    public boolean isNotifyAdmins() {
        return notifyAdmins;
    }

    public String getAdminAlertFormat() {
        return adminAlertFormat;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
}
