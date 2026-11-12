package me.tiack2k9.antiviabedrock.detector;

/**
 * Loại phát hiện vi phạm ViaBedrock hoặc ViaFabricPlus.
 * 
 * @author tiack2k9
 */
public enum DetectionType {
    DEVICE_MODEL("ViaBedrock Hardcoded Device Model (MS-7E51)"),
    DEVICE_OS_WIN32("Fake Bedrock Win32 Platform (DeviceOS 8)"),
    FAKE_FNV1_XUID("ViaBedrock Self-Signed FNV1 XUID"),
    MAX_VIEW_DISTANCE_96("ViaBedrock Hardcoded View Distance (96 Chunks)"),
    GUI_SCALE_MINUS_ONE("ViaBedrock Hardcoded GuiScale (-1)"),
    VIABEDROCK_CHANNEL("ViaBedrock Utility Plugin Channel"),
    JAVA_BLOCKED_BRAND("Blocked Java Client Brand (ViaFabricPlus/Fabric)");

    private final String description;

    DetectionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
