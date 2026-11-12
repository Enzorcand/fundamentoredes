package core;

import model.LSDB;
import model.Neighbor;
import model.RoutingEntry;
import model.RoutingTable;
import protocol.DataHandler;
import routing.DijkstraRouter;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.Map;

/**
 * Estado partilhado do roteador entre todas as threads.
 */
public class RouterState {

    private final String myIp;
    private final DatagramSocket socket;
    private final Map<String, Neighbor> neighbors;
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
        this.neighbors      = neighbors;
        this.lsdb           = lsdb;
        this.routingTable   = new RoutingTable();
        this.dijkstraRouter = new DijkstraRouter(myIp, lsdb);
        this.dataHandler    = new DataHandler(this);
    }

    public String getMyIp() { return myIp; }
    public DatagramSocket getSocket() { return socket; }
    public Map<String, Neighbor> getNeighbors() { return Collections.unmodifiableMap(neighbors); }
    public LSDB getLsdb() { return lsdb; }
    public RoutingTable getRoutingTable() { return routingTable; }
    public DijkstraRouter getDijkstraRouter() { return dijkstraRouter; }
    public DataHandler getDataHandler() { return dataHandler; }

    public synchronized Neighbor getNeighbor(String ip) {
        return neighbors.get(ip);
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

    public synchronized void triggerDijkstra() {
        Logger.log(Logger.DIJKSTRA_EXECUTADO);
        Map<String, RoutingEntry> routes = dijkstraRouter.computeRoutes();
        routingTable.update(routes);
    }
}
