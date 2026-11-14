package routing;

import model.LSAEntry;
import model.LSDB;
import model.RoutingEntry;
import java.util.*;

/**
 * Motor de roteamento: constrói o grafo a partir da LSDB e calcula os caminhos
 * mínimos com o algoritmo de Dijkstra implementado manualmente.
 *
 * Regras do enunciado:
 *  - O grafo usa os custos anunciados em cada direção.
 *  - Uma aresta só é usada quando os LSAs válidos das DUAS extremidades a reconhecem.
 *  - O próximo salto é sempre um vizinho ativo.
 *  - Havendo caminhos de mesmo custo, a escolha é aleatória.
 *  - Não há rota para o próprio roteador.
 *
 * Empates: o algoritmo guarda o CONJUNTO de próximos saltos possíveis de cada
 * destino (todos os caminhos de custo mínimo). Se o próximo salto atual da
 * tabela continua nesse conjunto, ele é mantido; caso contrário é sorteado um.
 * Isso evita que a rota "oscile" entre caminhos iguais a cada execução.
 */
public class DijkstraRouter {

    private final String myIp;
    private final LSDB lsdb;
    private final Random random = new Random();

    public DijkstraRouter(String myIp, LSDB lsdb) {
        this.myIp = myIp;
        this.lsdb = lsdb;
    }

    public Map<String, RoutingEntry> computeRoutes(Set<String> activeNeighborIps) {
        return computeRoutes(activeNeighborIps, Collections.emptyMap());
    }

    /**
     * Calcula as rotas de menor custo a partir de myIp.
     *
     * @param activeNeighborIps IPs dos vizinhos atualmente ativos (próximos saltos permitidos)
     * @param previousRoutes    tabela atual, usada para manter o próximo salto em caso de empate
     */
    public Map<String, RoutingEntry> computeRoutes(Set<String> activeNeighborIps,
                                                   Map<String, RoutingEntry> previousRoutes) {
        Map<String, Map<String, Integer>> graph = buildValidatedGraph();

        if (!graph.containsKey(myIp)) {
            return Collections.emptyMap();
        }

        // dist[v]  = menor custo conhecido de myIp até v
        // hops[v]  = conjunto de próximos saltos (vizinhos diretos) que levam a v com esse custo
        Map<String, Long> dist = new HashMap<>();
        Map<String, Set<String>> hops = new HashMap<>();
        Set<String> unvisited = new HashSet<>(graph.keySet());

        for (String node : graph.keySet()) {
            dist.put(node, Long.MAX_VALUE);
            hops.put(node, new TreeSet<>());
        }
        dist.put(myIp, 0L);

        while (!unvisited.isEmpty()) {
            // Seleciona o nó não visitado de menor distância
            String u = null;
            long best = Long.MAX_VALUE;
            for (String node : unvisited) {
                long d = dist.get(node);
                if (d < best) {
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
                    hops.put(v, new TreeSet<>(viaU));
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

            // Próximo salto tem de ser vizinho ativo
            List<String> options = new ArrayList<>();
            for (String h : hops.get(dest)) {
                if (activeNeighborIps.contains(h)) options.add(h);
            }
            if (options.isEmpty()) continue;

            String chosen;
            RoutingEntry prev = previousRoutes.get(dest);
            if (prev != null && options.contains(prev.getNextHop())) {
                chosen = prev.getNextHop();                         // mantém a escolha atual
            } else {
                chosen = options.get(random.nextInt(options.size())); // empate: aleatório
            }
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
