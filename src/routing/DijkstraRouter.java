package routing;

import model.LSAEntry;
import model.LSDB;
import model.RoutingEntry;
import java.util.*;

/**
 * Motor de roteamento: constrói o grafo a partir da LSDB e calcula os caminhos
 * mínimos com o algoritmo de Dijkstra implementado manualmente.
 *
 * Regras do enunciado e adendos:
 *  - O grafo usa os custos anunciados em cada direção.
 *  - Uma aresta só é usada quando os LSAs válidos das DUAS extremidades a reconhecem.
 *  - O próximo salto é sempre um vizinho ativo.
 *  - Desempate determinístico: menor IP de próximo salto (adendo, no lugar do aleatório).
 *  - Não há rota para o próprio roteador.
 *
 * Empates: o algoritmo guarda o CONJUNTO de próximos saltos possíveis de cada
 * destino (todos os caminhos de custo mínimo) e, no fim, escolhe o de menor IP
 * entre os que são vizinhos ativos.
 */
public class DijkstraRouter {

    private final String myIp;
    private final LSDB lsdb;

    public DijkstraRouter(String myIp, LSDB lsdb) {
        this.myIp = myIp;
        this.lsdb = lsdb;
    }

    /** Compara dois endereços IPv4 numericamente, octeto a octeto. */
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
     * @param activeNeighborIps IPs dos vizinhos atualmente ativos (próximos saltos permitidos)
     */
    public Map<String, RoutingEntry> computeRoutes(Set<String> activeNeighborIps) {
        Map<String, Map<String, Integer>> graph = buildValidatedGraph();

        if (!graph.containsKey(myIp)) {
            return Collections.emptyMap();
        }

        // dist[v] = menor custo conhecido de myIp até v
        // hops[v] = conjunto de próximos saltos (vizinhos diretos) que levam a v com esse custo
        Map<String, Long> dist = new HashMap<>();
        Map<String, Set<String>> hops = new HashMap<>();
        Set<String> unvisited = new HashSet<>(graph.keySet());

        for (String node : graph.keySet()) {
            dist.put(node, Long.MAX_VALUE);
            hops.put(node, new HashSet<>());
        }
        dist.put(myIp, 0L);

        while (!unvisited.isEmpty()) {
            // Seleciona o nó não visitado de menor distância (empate: menor IP, para ser determinístico)
            String u = null;
            long best = Long.MAX_VALUE;
            for (String node : unvisited) {
                long d = dist.get(node);
                if (d < best || (d == best && d != Long.MAX_VALUE && compareIps(node, u) < 0)) {
                    best = d;
                    u = node;
                }
            }
            if (u == null) break; // restantes são inalcançáveis
            unvisited.remove(u);

            for (Map.Entry<String, Integer> edge : graph.get(u).entrySet()) {
                String v = edge.getKey();
                if (!unvisited.contains(v)) continue;

                long alt = best + edge.getValue();
                long current = dist.get(v);

                // Se u é a própria origem, o próximo salto para v é o próprio v;
                // senão, v herda os próximos saltos de u.
                Set<String> viaU = u.equals(myIp)
                        ? Collections.singleton(v)
                        : hops.get(u);

                if (alt < current) {
                    dist.put(v, alt);
                    hops.put(v, new HashSet<>(viaU));
                } else if (alt == current) {
                    hops.get(v).addAll(viaU); // caminho de mesmo custo: acumula opções
                }
            }
        }

        Map<String, RoutingEntry> routes = new TreeMap<>();
        for (String dest : graph.keySet()) {
            if (dest.equals(myIp)) continue;
            long totalCost = dist.get(dest);
            if (totalCost == Long.MAX_VALUE) continue;

            // Próximo salto: vizinho ativo; havendo empate, o de menor IP
            String chosen = null;
            for (String h : hops.get(dest)) {
                if (!activeNeighborIps.contains(h)) continue;
                if (chosen == null || compareIps(h, chosen) < 0) chosen = h;
            }
            if (chosen == null) continue;

            routes.put(dest, new RoutingEntry(dest, (int) totalCost, chosen));
        }

        return routes;
    }

    /**
     * Grafo dirigido: aresta u→v com o custo anunciado por u, incluída apenas
     * se o LSA de v também anunciar o enlace com u.
     */
    private Map<String, Map<String, Integer>> buildValidatedGraph() {
        Map<String, LSAEntry> byOrigin = new HashMap<>();
        for (LSAEntry e : lsdb.getAll()) { // snapshot consistente
            byOrigin.put(e.getOriginIp(), e);
        }

        Map<String, Map<String, Integer>> graph = new HashMap<>();
        for (String origin : byOrigin.keySet()) {
            graph.put(origin, new HashMap<>());
        }

        for (LSAEntry lsaU : byOrigin.values()) {
            String u = lsaU.getOriginIp();
            for (Map.Entry<String, Integer> linkU : lsaU.getLinks().entrySet()) {
                String v = linkU.getKey();
                LSAEntry lsaV = byOrigin.get(v);
                if (lsaV == null) continue;
                if (lsaV.getLinks().containsKey(u)) {
                    graph.get(u).put(v, linkU.getValue());
                }
            }
        }
        return graph;
    }
}
