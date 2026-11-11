package me.khangios.antiviabedrock.util;

import org.bukkit.ChatColor;

/**
 * Tiện ích chuyển đổi mã màu chat cho thông báo.
 * 
 * @author khangios
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static String color(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
