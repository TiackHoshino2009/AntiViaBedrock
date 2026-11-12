package me.tiack2k9.antiviabedrock.util;

import java.nio.charset.StandardCharsets;

/**
 * Thuật toán FNV1-64 tương ứng chính xác với implementation trong ViaBedrock.
 * Dùng để kiểm tra XUID giả mạo được tạo tự động bởi ViaBedrock khi người chơi
 * kết nối ở chế độ tự ký (self-signed/offline).
 * 
 * @author tiack2k9
 */
public final class Fnv1 {

    private static final long FNV1_64_INIT = 0xCBF29CE484222325L;
    private static final long FNV1_PRIME_64 = 1099511628211L;

    public static long fnv1_64(final byte[] data) {
        long hash = FNV1_64_INIT;
        for (byte b : data) {
            hash *= FNV1_PRIME_64;
            hash ^= (b & 0xFF);
        }
        return hash;
    }

    /**
     * Kiểm tra xem một XUID có phải là XUID giả mạo được sinh bởi ViaBedrock hay không.
     * Trong ViaBedrock LoginPackets:
     *   final long rawXuid = Fnv1.fnv1_64(javaUsername.getBytes(StandardCharsets.UTF_8));
     *   final String xuid = String.valueOf(Math.abs(rawXuid));
     */
    public static boolean matchesViaBedrockFakeXuid(String username, String xuid) {
        if (username == null || xuid == null || xuid.isEmpty()) {
            return false;
        }
        long rawXuid = fnv1_64(username.getBytes(StandardCharsets.UTF_8));
        String expectedFakeXuid = String.valueOf(Math.abs(rawXuid));
        return xuid.equals(expectedFakeXuid);
    }

    private Fnv1() {
    }
}
