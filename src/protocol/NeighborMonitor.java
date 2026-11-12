package protocol;

import core.Logger;
import core.RouterState;
import model.Neighbor;
import java.util.Map;

/**
 * Thread que monitoriza os timeouts de HELLO dos vizinhos (15 segundos).
 */
public class NeighborMonitor implements Runnable {

    private static final int CHECK_INTERVAL_MS = 1_000;

    private final RouterState state;
    private volatile boolean running = true;

    public NeighborMonitor(RouterState state) {
        this.state = state;
    }

    @Override
    public void run() {
        Logger.log("NEIGHBOR_MONITOR", "Iniciado — timeout HELLO: 15s");

        while (running) {
            checkTimeouts();
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.log("NEIGHBOR_MONITOR", "Encerrado.");
    }

    private void checkTimeouts() {
        Map<String, Neighbor> neighbors = state.getNeighbors();

        for (Neighbor neighbor : neighbors.values()) {
            if (!neighbor.isActive()) continue;

            if (neighbor.isHelloTimedOut()) {
                neighbor.setActive(false);
                Logger.log(Logger.VIZINHO_INATIVO,
                        neighbor.getIp() + " | Sem HELLO há mais de 15s");
                state.notifyNeighborStateChange();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
