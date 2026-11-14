package routing;

import model.LSAEntry;
import model.LSDB;
import model.RoutingEntry;
import java.util.*;

/**
 * Motor de roteamento: constrói o grafo a partir da LSDB e calcula os caminhos
 * mínimos utilizando o algoritmo de Dijkstra implementado manualmente.
 *
 * Conforme enunciado e adendos:
 * - "O próximo salto será sempre um vizinho ativo."
 * - Desempate determinístico: menor IP de próximo salto (Adendo 159).
 */
public class DijkstraRouter {

    private final String myIp;
    private final LSDB lsdb;

    public DijkstraRouter(String myIp, LSDB lsdb) {
        this.myIp = myIp;
        this.lsdb = lsdb;
    }

    /**
     * Compara dois endereços IPv4 numericamente por octetos.
     */
    public static int compareIps(String ip1, String ip2) {
        if (ip1 == null && ip2 == null) return 0;
        if (ip1 == null) return 1;
        if (ip2 == null) return -1;
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");
        if (parts1.length == 4 && parts2.length == 4) {
            try {
                for (int i = 0; i < 4; i++) {
                    int octet1 = Integer.parseInt(parts1[i]);
                    int octet2 = Integer.parseInt(parts2[i]);
                    if (octet1 != octet2) {
                        return Integer.compare(octet1, octet2);
                    }
                }
                return 0;
            } catch (NumberFormatException ignored) {
            }
        }
        return ip1.compareTo(ip2);
    }

    /**
     * Calcula as rotas de menor custo a partir de myIp.
     *
     * @param activeNeighborIps conjunto de IPs de vizinhos atualmente ativos;
     *                          apenas rotas cujo próximo salto esteja neste conjunto são incluídas.
     */
    public Map<String, RoutingEntry> computeRoutes(Set<String> activeNeighborIps) {
        Map<String, Map<String, Integer>> graph = buildValidatedGraph();

        if (!graph.containsKey(myIp)) {
            return Collections.emptyMap();
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> nextHop = new HashMap<>();
        Set<String> unvisited = new HashSet<>(graph.keySet());

        for (String node : graph.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
            nextHop.put(node, null);
        }
        dist.put(myIp, 0);

        while (!unvisited.isEmpty()) {
            int minDist = Integer.MAX_VALUE;
            String u = null;

            for (String node : unvisited) {
                int d = dist.get(node);
                if (d < minDist) {
                    minDist = d;
                    u = node;
                } else if (d == minDist && d < Integer.MAX_VALUE) {
                    // Desempate determinístico pelo menor IP de nó
                    if (u == null || compareIps(node, u) < 0) {
                        u = node;
                    }
                }
            }

            if (minDist == Integer.MAX_VALUE || u == null) {
                break;
            }

            unvisited.remove(u);

            Map<String, Integer> neighbors = graph.get(u);
            if (neighbors == null) continue;

            for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                String v = edge.getKey();
                if (!unvisited.contains(v)) continue;

                int edgeCost = edge.getValue();
                int alt = dist.get(u) + edgeCost;
                int currentDist = dist.get(v);

                String candidateNextHop = u.equals(myIp) ? v : nextHop.get(u);

                if (alt < currentDist) {
                    dist.put(v, alt);
                    nextHop.put(v, candidateNextHop);
                } else if (alt == currentDist) {
                    // Adendo 159: Desempate pelo menor IP de próximo salto
                    String existingNextHop = nextHop.get(v);
                    if (existingNextHop == null || compareIps(candidateNextHop, existingNextHop) < 0) {
                        nextHop.put(v, candidateNextHop);
                    }
                }
            }
        }

        Map<String, RoutingEntry> routes = new TreeMap<>();
        for (String dest : graph.keySet()) {
            if (dest.equals(myIp)) continue;

            int totalCost = dist.get(dest);
            String nh = nextHop.get(dest);

            if (totalCost < Integer.MAX_VALUE && nh != null) {
                // Enunciado: "O próximo salto será sempre um vizinho ativo."
                if (!activeNeighborIps.contains(nh)) continue;
                routes.put(dest, new RoutingEntry(dest, totalCost, nh));
            }
        }

        return routes;
    }

    private Map<String, Map<String, Integer>> buildValidatedGraph() {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        Collection<LSAEntry> entries = lsdb.getAll();

        for (LSAEntry entry : entries) {
            graph.put(entry.getOriginIp(), new HashMap<>());
        }

        for (LSAEntry lsaU : entries) {
            String u = lsaU.getOriginIp();

            for (Map.Entry<String, Integer> linkU : lsaU.getLinks().entrySet()) {
                String v = linkU.getKey();
                int costUV = linkU.getValue();

                LSAEntry lsaV = lsdb.get(v);
                if (lsaV == null) continue;

                if (lsaV.getLinks().containsKey(u)) {
                    graph.get(u).put(v, costUV);
                }
            }
        }

        return graph;
    }
}
