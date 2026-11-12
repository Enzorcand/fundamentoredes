package model;

import core.Logger;
import java.util.*;

/**
 * Tabela de Roteamento do roteador Link State.
 */
public class RoutingTable {

    private final Map<String, RoutingEntry> routes = new TreeMap<>();

    public synchronized RoutingEntry get(String destination) {
        return routes.get(destination);
    }

    public synchronized Map<String, RoutingEntry> getAll() {
        return Collections.unmodifiableMap(new TreeMap<>(routes));
    }

    public synchronized boolean update(Map<String, RoutingEntry> newRoutes) {
        List<RoutingEntry> added = new ArrayList<>();
        List<RoutingEntry> removed = new ArrayList<>();
        List<String> modified = new ArrayList<>();

        for (Map.Entry<String, RoutingEntry> entry : newRoutes.entrySet()) {
            String dest = entry.getKey();
            RoutingEntry newEntry = entry.getValue();
            RoutingEntry oldEntry = routes.get(dest);

            if (oldEntry == null) {
                added.add(newEntry);
            } else if (!oldEntry.equals(newEntry)) {
                modified.add(String.format("%s (custo: %d -> %d, via: %s -> %s)",
                        dest, oldEntry.getTotalCost(), newEntry.getTotalCost(),
                        oldEntry.getNextHop(), newEntry.getNextHop()));
            }
        }

        for (Map.Entry<String, RoutingEntry> entry : routes.entrySet()) {
            String dest = entry.getKey();
            if (!newRoutes.containsKey(dest)) {
                removed.add(entry.getValue());
            }
        }

        boolean changed = !added.isEmpty() || !removed.isEmpty() || !modified.isEmpty();

        routes.clear();
        routes.putAll(newRoutes);

        for (RoutingEntry e : added) {
            Logger.log(Logger.ROTA_ADICIONADA, e.getDestination(),
                    String.format("CUSTO=%d | PROXIMO_SALTO=%s", e.getTotalCost(), e.getNextHop()));
        }
        for (RoutingEntry e : removed) {
            Logger.log(Logger.ROTA_REMOVIDA, e.getDestination(),
                    String.format("CUSTO_ANTIGO=%d | PROXIMO_SALTO_ANTIGO=%s", e.getTotalCost(), e.getNextHop()));
        }
        for (String m : modified) {
            Logger.log(Logger.ROTA_MODIFICADA, m);
        }

        print(added, removed, modified);

        return changed;
    }

    public synchronized void print(List<RoutingEntry> added, List<RoutingEntry> removed, List<String> modified) {
        Set<String> addedSet = new HashSet<>();
        if (added != null) {
            for (RoutingEntry e : added) addedSet.add(e.getDestination());
        }
        Set<String> modDestSet = new HashSet<>();
        if (modified != null) {
            for (String m : modified) {
                String dest = m.split(" ")[0].trim();
                modDestSet.add(dest);
            }
        }

        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│                  TABELA DE ROTEAMENTO                  │");
        System.out.println("├─────────────────┬─────────────┬────────────────────────┤");
        System.out.printf("│ %-15s │ %-11s │ %-22s │%n", "Destino", "Custo total", "Próximo salto");
        System.out.println("├─────────────────┼─────────────┼────────────────────────┤");

        if (routes.isEmpty()) {
            System.out.println("│              (sem rotas disponíveis)                   │");
        } else {
            for (RoutingEntry e : routes.values()) {
                String tag = "";
                if (addedSet.contains(e.getDestination())) {
                    tag = " [NOVA]";
                } else if (modDestSet.contains(e.getDestination())) {
                    tag = " [MODIF]";
                }
                String nextHopWithTag = e.getNextHop() + tag;
                System.out.printf("│ %-15s │ %-11d │ %-22s │%n",
                        e.getDestination(), e.getTotalCost(), nextHopWithTag);
            }
        }
        System.out.println("└─────────────────┴─────────────┴────────────────────────┘");

        if (removed != null && !removed.isEmpty()) {
            System.out.print("  [ROTAS REMOVIDAS]: ");
            for (RoutingEntry r : removed) {
                System.out.print(r.getDestination() + " ");
            }
            System.out.println();
        }
    }

    public synchronized void print() {
        print(null, null, null);
    }
}
