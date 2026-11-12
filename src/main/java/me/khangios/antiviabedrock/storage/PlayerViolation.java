package me.tiack2k9.antiviabedrock.storage;

import java.util.UUID;

/**
 * Lưu trữ thông tin vi phạm của người chơi.
 * 
 * @author tiack2k9
 */
public class PlayerViolation {

    private final UUID uuid;
    private String name;
    private String ip;
    private int violations;
    private long firstViolation;
    private long lastViolation;
    private String lastReason;
    private boolean banned;

    public PlayerViolation(UUID uuid, String name, String ip, int violations, long firstViolation, long lastViolation, String lastReason, boolean banned) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
        this.violations = violations;
        this.firstViolation = firstViolation;
        this.lastViolation = lastViolation;
        this.lastReason = lastReason;
        this.banned = banned;
    }

    public PlayerViolation(UUID uuid, String name, String ip) {
        this(uuid, name, ip, 0, System.currentTimeMillis(), System.currentTimeMillis(), "", false);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getViolations() {
        return violations;
    }

    public void setViolations(int violations) {
        this.violations = violations;
    }

    public int incrementViolations(String reason) {
        this.violations++;
        this.lastViolation = System.currentTimeMillis();
        this.lastReason = reason;
        return this.violations;
    }

    public long getFirstViolation() {
        return firstViolation;
    }

    public long getLastViolation() {
        return lastViolation;
    }

    public String getLastReason() {
        return lastReason;
    }

    public void setLastReason(String lastReason) {
        this.lastReason = lastReason;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}
