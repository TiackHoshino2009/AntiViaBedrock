package me.tiack2k9.antiviabedrock.detector;

/**
 * Kết quả phân tích và kiểm tra người chơi.
 * 
 * @author tiack2k9
 */
public record DetectionResult(
    boolean detected,
    DetectionType type,
    String details
) {
    public static DetectionResult clean() {
        return new DetectionResult(false, null, "Valid (Clean Bedrock Client)");
    }

    public static DetectionResult flagged(DetectionType type, String details) {
        return new DetectionResult(true, type, details);
    }
}
