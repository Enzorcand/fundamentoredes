package core;

import model.LSDB;
import model.Neighbor;
import model.RoutingEntry;
import model.RoutingTable;
import model.SequenceNumberStore;
import protocol.DataHandler;
import routing.DijkstraRouter;
import java.net.DatagramSocket;
import java.util.*;

public class RouterState {

    private final String myIp;
    private final DatagramSocket socket;
    private final LinkedHashMap<String, Neighbor> neighbors;
    private final LSDB lsdb;
    private final RoutingTable routingTable;
    private final DijkstraRouter dijkstraRouter;
    private final DataHandler dataHandler;
    private volatile SequenceNumberStore seqStore;

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
    public SequenceNumberStore getSeqStore() { return seqStore; }
    public void setSeqStore(SequenceNumberStore seqStore) { this.seqStore = seqStore; }

    public synchronized Map<String, Neighbor> getNeighbors() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(neighbors));
    }

    public synchronized Neighbor getNeighbor(String ip) {
        return neighbors.get(ip);
    }

    public synchronized void addNeighbor(Neighbor n) {
        neighbors.put(n.getIp(), n);
    }

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

    public synchronized void triggerDijkstra() {
        Logger.log(Logger.DIJKSTRA_EXECUTADO);
        Set<String> activeNeighborIps = new HashSet<>();
        for (Neighbor n : neighbors.values()) {
            if (n.isActive()) activeNeighborIps.add(n.getIp());
        }
        Map<String, RoutingEntry> routes =
                dijkstraRouter.computeRoutes(activeNeighborIps);
        routingTable.update(routes);
    }
}
