package me.khangios.antiviabedrock.detector;

/**
 * Kết quả phân tích và kiểm tra người chơi.
 * 
 * @author khangios
 */
public record DetectionResult(
    boolean detected,
    DetectionType type,
    String details
) {
    public static DetectionResult clean() {
        return new DetectionResult(false, null, "Hợp lệ (Clean Bedrock Client)");
    }

    public static DetectionResult flagged(DetectionType type, String details) {
        return new DetectionResult(true, type, details);
    }
}
