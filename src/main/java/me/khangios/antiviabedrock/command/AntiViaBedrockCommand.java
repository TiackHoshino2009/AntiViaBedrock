package me.tiack2k9.antiviabedrock.command;

import me.tiack2k9.antiviabedrock.AntiViaBedrockPlugin;
import me.tiack2k9.antiviabedrock.detector.ViaBedrockDetector;
import me.tiack2k9.antiviabedrock.storage.PlayerViolation;
import me.tiack2k9.antiviabedrock.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Lệnh quản trị /antiviabedrock (alias: /avb).
 * 
 * @author tiack2k9
 */
public class AntiViaBedrockCommand implements CommandExecutor, TabCompleter {

    private final AntiViaBedrockPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public AntiViaBedrockCommand(AntiViaBedrockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antiviabedrock.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission to use this command. (&eantiviabedrock.admin&c)."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                plugin.getConfigManager().load();
                plugin.getDatabaseManager().load();
                sender.sendMessage(ColorUtil.color("&a[AntiViaBedrock] Successfully reloaded config.yml and data.yml.!"));
                return true;
            }

            case "check" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cPlease enter a player name: &e/" + label + " check <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cPlayer &e" + args[1] + " &cNot Online."));
                    return true;
                }
                checkPlayerInfo(sender, target);
                return true;
            }

            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cPlease enter a player name: &e/" + label + " reset <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                UUID targetUuid = target != null ? target.getUniqueId() : null;

                if (targetUuid == null) {
                    for (Map.Entry<UUID, PlayerViolation> entry : plugin.getDatabaseManager().getAllViolations().entrySet()) {
                        if (entry.getValue().getName().equalsIgnoreCase(args[1])) {
                            targetUuid = entry.getKey();
                            break;
                        }
                    }
                }

                if (targetUuid != null && plugin.getDatabaseManager().resetViolation(targetUuid)) {
                    sender.sendMessage(ColorUtil.color("&a[AntiViaBedrock] Violation history cleared for &e" + args[1]));
                } else {
                    sender.sendMessage(ColorUtil.color("&cNo violation data found for &e" + args[1]));
                }
                return true;
            }

            case "list" -> {
                Map<UUID, PlayerViolation> all = plugin.getDatabaseManager().getAllViolations();
                if (all.isEmpty()) {
                    sender.sendMessage(ColorUtil.color("&a[AntiViaBedrock] No players currently have recorded violations."));
                    return true;
                }

                sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.color("&6&lANTIVIABEDROCK VIOLATION LIST &7(" + all.size() + " mục):"));
                for (PlayerViolation pv : all.values()) {
                    String status = pv.isBanned() ? "&c[BANNED]" : "&e[WARNING]";
                    sender.sendMessage(ColorUtil.color(String.format(
                        "&7- %s &f%s &7| violations: &c%d &7| Last seen: &e%s &7| Reason: &f%s",
                        status, pv.getName(), pv.getViolations(),
                        dateFormat.format(new Date(pv.getLastViolation())),
                        pv.getLastReason()
                    )));
                }
                sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
                return true;
            }

            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private void checkPlayerInfo(CommandSender sender, Player target) {
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&6&lCLIENT INFORMATION FOR: &e" + target.getName()));
        sender.sendMessage(ColorUtil.color("&7UUID: &f" + target.getUniqueId()));
        sender.sendMessage(ColorUtil.color("&7IP: &f" + (target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : "N/A")));
        sender.sendMessage(ColorUtil.color("&7Client Brand: &f" + target.getClientBrandName()));

        boolean isBedrock = false;

        // Floodgate Info
        if (plugin.isFloodgateAvailable() && FloodgateApi.getInstance().isFloodgatePlayer(target.getUniqueId())) {
            isBedrock = true;
            FloodgatePlayer fp = FloodgateApi.getInstance().getPlayer(target.getUniqueId());
            if (fp != null) {
                sender.sendMessage(ColorUtil.color("&b[Floodgate]&r"));
                sender.sendMessage(ColorUtil.color("&7  Device OS: &f" + (fp.getDeviceOs() != null ? fp.getDeviceOs().name() : "N/A")));
                sender.sendMessage(ColorUtil.color("&7  XUID: &f" + fp.getXuid()));
                sender.sendMessage(ColorUtil.color("&7  Language: &f" + fp.getLanguageCode()));
                sender.sendMessage(ColorUtil.color("&7  Input Mode: &f" + (fp.getInputMode() != null ? fp.getInputMode().name() : "N/A")));
                sender.sendMessage(ColorUtil.color("&7  UI Profile: &f" + (fp.getUiProfile() != null ? fp.getUiProfile().name() : "N/A")));
            }
        }

        // Geyser Info
        if (plugin.isGeyserAvailable() && GeyserApi.api().isBedrockPlayer(target.getUniqueId())) {
            isBedrock = true;
            GeyserConnection conn = GeyserApi.api().connectionByUuid(target.getUniqueId());
            if (conn != null) {
                sender.sendMessage(ColorUtil.color("&b[Geyser]&r"));
                sender.sendMessage(ColorUtil.color("&7  Platform: &f" + (conn.platform() != null ? conn.platform().name() : "N/A")));
                sender.sendMessage(ColorUtil.color("&7  XUID: &f" + conn.xuid()));
                sender.sendMessage(ColorUtil.color("&7  Bedrock Username: &f" + conn.bedrockUsername()));
                sender.sendMessage(ColorUtil.color("&7  Bedrock Version: &f" + conn.version()));
            }
        }

        if (!isBedrock) {
            sender.sendMessage(ColorUtil.color("&7Connection type: &aJava Client"));
        }

        PlayerViolation pv = plugin.getDatabaseManager().getViolation(target.getUniqueId());
        if (pv != null) {
            sender.sendMessage(ColorUtil.color("&7**Number of violations**: &c" + pv.getViolations() + (pv.isBanned() ? " &4(BANNED)" : "")));
            sender.sendMessage(ColorUtil.color("&7Most recent violation reason: &f" + pv.getLastReason()));
        } else {
            sender.sendMessage(ColorUtil.color("&7Violation status: &aClean (0)"));
        }

        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&6&lAntiViaBedrock &7v" + plugin.getDescription().getVersion() + " &eby Tiack"));
        sender.sendMessage(ColorUtil.color("&e/" + label + " reload &7- Reload config.yml and data.yml"));
        sender.sendMessage(ColorUtil.color("&e/" + label + " check <player> &7- Check players clients and hardware"));
        sender.sendMessage(ColorUtil.color("&e/" + label + " reset <player> &7- Clear a player’s violations / pardon them"));
        sender.sendMessage(ColorUtil.color("&e/" + label + " list &7- View the list of players with recorded violations"));
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("antiviabedrock.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("reload", "check", "reset", "list", "help");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("reset"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    names.add(p.getName());
                }
            }
            return names;
        }

        return Collections.emptyList();
    }
}
