package protocol;

import core.Logger;
import core.RouterState;
import java.util.List;

/**
 * Thread de verificação periódica de expiração de LSAs na LSDB (30s).
 */
public class LSAExpirationMonitor implements Runnable {

    private static final int CHECK_INTERVAL_MS = 1_000;

    private final RouterState state;
    private volatile boolean running = true;
    private Runnable onTopologyChanged = () -> {};

    public LSAExpirationMonitor(RouterState state) {
        this.state = state;
    }

    public void setOnTopologyChanged(Runnable cb) {
        this.onTopologyChanged = cb;
    }

    @Override
    public void run() {
        Logger.log("LSA_EXPIRATION", "Iniciado — validade LSA: 30s");

        while (running) {
            checkExpired();
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.log("LSA_EXPIRATION", "Encerrado.");
    }

    private void checkExpired() {
        List<String> expired = state.getLsdb().removeExpired();

        for (String ip : expired) {
            Logger.log(Logger.LSA_EXPIRADO,
                    ip + " | LSA removido da LSDB após 30s sem renovação");
            onTopologyChanged.run();
        }
    }

    public void stop() {
        running = false;
    }
}
