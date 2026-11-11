package me.khangios.antiviabedrock;

import me.khangios.antiviabedrock.command.AntiViaBedrockCommand;
import me.khangios.antiviabedrock.config.ConfigManager;
import me.khangios.antiviabedrock.detector.ViaBedrockDetector;
import me.khangios.antiviabedrock.detector.ViaFabricPlusJavaDetector;
import me.khangios.antiviabedrock.listener.PlayerLoginListener;
import me.khangios.antiviabedrock.punishment.PunishmentManager;
import me.khangios.antiviabedrock.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AntiViaBedrock Plugin
 * Chặn người chơi sử dụng ViaFabricPlus / ViaBedrock để giả lập Bedrock vượt qua anti-cheat.
 * Tương thích Leaf / Paper 1.21.1+
 * 
 * @author khangios
 */
public class AntiViaBedrockPlugin extends JavaPlugin {

    private static AntiViaBedrockPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ViaBedrockDetector bedrockDetector;
    private ViaFabricPlusJavaDetector javaDetector;
    private PunishmentManager punishmentManager;

    private boolean floodgateAvailable;
    private boolean geyserAvailable;

    @Override
    public void onEnable() {
        instance = this;

        // In banner khởi động
        sendStartupBanner();

        // Kiểm tra Floodgate và Geyser
        checkDependencies();

        // Nạp cấu hình và cơ sở dữ liệu
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.load();

        // Khởi tạo các bộ phát hiện & xử phạt
        this.bedrockDetector = new ViaBedrockDetector(this);
        this.javaDetector = new ViaFabricPlusJavaDetector(this);
        this.punishmentManager = new PunishmentManager(this);

        // Đăng ký Listener
        PlayerLoginListener loginListener = new PlayerLoginListener(this);
        Bukkit.getPluginManager().registerEvents(loginListener, this);

        // Đăng ký Plugin Channels
        Bukkit.getMessenger().registerIncomingPluginChannel(this, ViaFabricPlusJavaDetector.VIABEDROCK_CONFIRM_CHANNEL, loginListener);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, ViaFabricPlusJavaDetector.VIABEDROCK_DATA_CHANNEL, loginListener);

        // Đăng ký Command
        AntiViaBedrockCommand cmd = new AntiViaBedrockCommand(this);
        if (getCommand("antiviabedrock") != null) {
            getCommand("antiviabedrock").setExecutor(cmd);
            getCommand("antiviabedrock").setTabCompleter(cmd);
        }

        getLogger().info("AntiViaBedrock v" + getDescription().getVersion() + " đã kích hoạt thành công!");
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.save(false);
        }
        getLogger().info("AntiViaBedrock đã tắt và lưu trữ dữ liệu an toàn.");
    }

    private void checkDependencies() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            this.floodgateAvailable = Bukkit.getPluginManager().isPluginEnabled("floodgate");
        } catch (ClassNotFoundException ignored) {
            this.floodgateAvailable = false;
        }

        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            this.geyserAvailable = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot");
        } catch (ClassNotFoundException ignored) {
            this.geyserAvailable = false;
        }

        if (this.floodgateAvailable) {
            getLogger().info("[Hook] Đã tìm thấy và kết nối với Floodgate API thành công!");
        } else {
            getLogger().warning("[Hook] Không tìm thấy Floodgate plugin trên server (Có thể đang chạy proxy hoặc Geyser độc lập).");
        }

        if (this.geyserAvailable) {
            getLogger().info("[Hook] Đã tìm thấy và kết nối với Geyser-Spigot API thành công!");
        }
    }

    private void sendStartupBanner() {
        getLogger().info("======================================================");
        getLogger().info("    _          _   _ __     ___       ____           _                 _    ");
        getLogger().info("   / \\   _ __ | |_(_) \\ \\   / (_) __ _| __ )  ___  __| |_ __ ___   ___| | __");
        getLogger().info("  / _ \\ | '_ \\| __| | |\\ \\ / /| |/ _` |  _ \\ / _ \\/ _` | '__/ _ \\ / __| |/ /");
        getLogger().info(" / ___ \\| | | | |_| | | \\ V / | | (_| | |_) |  __/ (_| | | | (_) | (__|   < ");
        getLogger().info("/_/   \\_\\_| |_|\\__|_|_|  \\_/  |_|\\__,_|____/ \\___|\\__,_|_|  \\___/ \\___|_|\\_\\");
        getLogger().info("   Anti-ViaBedrock & Anti-ViaFabricPlus Protection by khangios");
        getLogger().info("   Phiên bản: " + getDescription().getVersion() + " | Dành cho: Leaf 1.21.11");
        getLogger().info("======================================================");
    }

    public static AntiViaBedrockPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ViaBedrockDetector getBedrockDetector() {
        return bedrockDetector;
    }

    public ViaFabricPlusJavaDetector getJavaDetector() {
        return javaDetector;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }

    public boolean isGeyserAvailable() {
        return geyserAvailable;
    }
}
