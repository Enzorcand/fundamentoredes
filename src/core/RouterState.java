package core;

import model.LSDB;
import model.Neighbor;
import model.RoutingEntry;
import model.RoutingTable;
import protocol.DataHandler;
import routing.DijkstraRouter;
import java.net.DatagramSocket;
import java.util.*;

/**
 * Estado partilhado do roteador entre todas as threads.
 * O mapa de vizinhos é protegido por sincronização para permitir
 * adição e remoção em tempo de execução (addneighbor/removeneighbor).
 */
public class RouterState {

    private final String myIp;
    private final DatagramSocket socket;
    private final LinkedHashMap<String, Neighbor> neighbors;
    private final LSDB lsdb;
    private final RoutingTable routingTable;
    private final DijkstraRouter dijkstraRouter;
    private final DataHandler dataHandler;

    private Runnable onNeighborStateChange = () -> {};
    private java.util.function.Consumer<String> onNewNeighborActive = ip -> {};

    public RouterState(String myIp,
                       DatagramSocket socket,
                       Map<String, Neighbor> neighbors,
                       LSDB lsdb) {
        this.myIp           = myIp;
        this.socket         = socket;
        this.neighbors      = new LinkedHashMap<>(neighbors);
        this.lsdb           = lsdb;
        this.routingTable   = new RoutingTable();
        this.dijkstraRouter = new DijkstraRouter(myIp, lsdb);
        this.dataHandler    = new DataHandler(this);
    }

    public String getMyIp() { return myIp; }
    public DatagramSocket getSocket() { return socket; }
    public LSDB getLsdb() { return lsdb; }
    public RoutingTable getRoutingTable() { return routingTable; }
    public DijkstraRouter getDijkstraRouter() { return dijkstraRouter; }
    public DataHandler getDataHandler() { return dataHandler; }

    /** Retorna snapshot imutável dos vizinhos (thread-safe para iteração). */
    public synchronized Map<String, Neighbor> getNeighbors() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(neighbors));
    }

    public synchronized Neighbor getNeighbor(String ip) {
        return neighbors.get(ip);
    }

    /** Adiciona um vizinho configurado em tempo de execução. */
    public synchronized void addNeighbor(Neighbor n) {
        neighbors.put(n.getIp(), n);
    }

    /** Remove um vizinho configurado em tempo de execução. Retorna o vizinho removido ou null. */
    public synchronized Neighbor removeNeighbor(String ip) {
        return neighbors.remove(ip);
    }

    public void setOnNeighborStateChange(Runnable callback) {
        this.onNeighborStateChange = callback;
    }

    public void notifyNeighborStateChange() {
        onNeighborStateChange.run();
    }

    public void setOnNewNeighborActive(java.util.function.Consumer<String> callback) {
        this.onNewNeighborActive = callback;
    }

    public void notifyNewNeighborActive(String neighborIp) {
        onNewNeighborActive.accept(neighborIp);
    }

    /**
     * Executa o Dijkstra e atualiza a tabela de roteamento.
     * Passa o conjunto de vizinhos ativos para garantir que o próximo salto
     * seja sempre um vizinho ativo (conforme enunciado).
     */
    public synchronized void triggerDijkstra() {
        Logger.log(Logger.DIJKSTRA_EXECUTADO);
        Set<String> activeNeighborIps = new HashSet<>();
        for (Neighbor n : neighbors.values()) {
            if (n.isActive()) activeNeighborIps.add(n.getIp());
        }
        Map<String, RoutingEntry> routes =
                dijkstraRouter.computeRoutes(activeNeighborIps, routingTable.getAll());
        routingTable.update(routes);
    }
}
