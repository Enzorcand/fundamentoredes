package model;

import java.util.Objects;

/**
 * Uma entrada da tabela de roteamento: Destino, Custo Total e Próximo Salto.
 */
public class RoutingEntry {

    private final String destination;
    private final int totalCost;
    private final String nextHop;

    public RoutingEntry(String destination, int totalCost, String nextHop) {
        this.destination = destination;
        this.totalCost = totalCost;
        this.nextHop = nextHop;
    }

    public String getDestination() { return destination; }
    public int getTotalCost() { return totalCost; }
    public String getNextHop() { return nextHop; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutingEntry)) return false;
        RoutingEntry that = (RoutingEntry) o;
        return totalCost == that.totalCost
                && Objects.equals(destination, that.destination)
                && Objects.equals(nextHop, that.nextHop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, totalCost, nextHop);
    }

    @Override
    public String toString() {
        return String.format("%-15s  custo=%-6d  via=%s", destination, totalCost, nextHop);
    }
}
