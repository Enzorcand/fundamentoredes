package model;

/**
 * Representa um vizinho configurado no roteadores.txt.
 * Armazena IP, custo, estado (ativo/inativo) e timestamp da última receção de HELLO válido.
 */
public class Neighbor {

    private final String ip;
    private int cost;
    private boolean enabled;
    private boolean active;
    private long lastHelloTimestamp;

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
        if (lastHelloTimestamp == 0L) return true;
        return (System.currentTimeMillis() - lastHelloTimestamp) > 15_000L;
    }

    @Override
    public String toString() {
        return String.format("Neighbor{ip='%s', cost=%d, enabled=%b, active=%b, lastHello=%d}",
                ip, cost, enabled, active, lastHelloTimestamp);
    }
}
