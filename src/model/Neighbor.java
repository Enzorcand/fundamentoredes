package model;

public class Neighbor {

    public static final long HELLO_TIMEOUT_MS = 15_000L;

    private final String ip;
    private volatile int cost;
    private volatile boolean enabled;
    private volatile boolean active;
    private volatile long lastHelloTimestamp;

    public Neighbor(String ip, int cost) {
        this.ip = ip;
        this.cost = cost;
        this.enabled = true;
        this.active = false;
        this.lastHelloTimestamp = 0L;
    }

    public String getIp() { return ip; }
    public int getCost() { return cost; }
    public boolean isEnabled() { return enabled; }
    public boolean isActive() { return active; }
    public long getLastHelloTimestamp() { return lastHelloTimestamp; }

    public void setCost(int cost) { this.cost = cost; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setActive(boolean active) { this.active = active; }
    public void updateLastHello() { this.lastHelloTimestamp = System.currentTimeMillis(); }

    public boolean isHelloTimedOut() {
        long last = lastHelloTimestamp;
        if (last == 0L) return true;
        return (System.currentTimeMillis() - last) > HELLO_TIMEOUT_MS;
    }

    @Override
    public String toString() {
        return String.format("Neighbor{ip='%s', cost=%d, enabled=%b, active=%b, lastHello=%d}",
                ip, cost, enabled, active, lastHelloTimestamp);
    }
}
