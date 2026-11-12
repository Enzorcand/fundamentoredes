package routing;

import model.LSAEntry;
import model.LSDB;
import model.RoutingEntry;
import java.util.*;

/**
 * Motor de roteamento: constrói o grafo a partir da LSDB e calcula os caminhos
 * mínimos utilizando o algoritmo de Dijkstra implementado manualmente.
 */
public class DijkstraRouter {

    private final String myIp;
    private final LSDB lsdb;
    private final Random random = new Random();

    public DijkstraRouter(String myIp, LSDB lsdb) {
        this.myIp = myIp;
        this.lsdb = lsdb;
    }

    public Map<String, RoutingEntry> computeRoutes() {
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
            List<String> candidates = new ArrayList<>();

            for (String node : unvisited) {
                int d = dist.get(node);
                if (d < minDist) {
                    minDist = d;
                    candidates.clear();
                    candidates.add(node);
                } else if (d == minDist && d < Integer.MAX_VALUE) {
                    candidates.add(node);
                }
            }

            if (minDist == Integer.MAX_VALUE || candidates.isEmpty()) {
                break;
            }

            String u = candidates.get(random.nextInt(candidates.size()));
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
                    if (random.nextBoolean()) {
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
