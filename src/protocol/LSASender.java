package protocol;

import core.Logger;
import core.RouterState;
import model.LSAEntry;
import model.LSDB;
import model.Neighbor;
import model.SequenceNumberStore;
import network.UDPListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/**
 * Geração e envio periódico / imediato de LSAs.
 */
public class LSASender implements Runnable {

    private static final int LSA_INTERVAL_MS = 10_000;

    private final RouterState state;
    private final SequenceNumberStore seqStore;
    private final Object lock = new Object();
    private volatile boolean running = true;

    private Runnable onTopologyChanged = () -> {};

    public LSASender(RouterState state, SequenceNumberStore seqStore) {
        this.state = state;
        this.seqStore = seqStore;
    }

    public void setOnTopologyChanged(Runnable cb) {
        this.onTopologyChanged = cb;
    }

    public void triggerNow() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void run() {
        Logger.log("LSA_SENDER", "Iniciado — intervalo " + LSA_INTERVAL_MS / 1000 + "s");

        generateAndSend();

        while (running) {
            synchronized (lock) {
                try {
                    lock.wait(LSA_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!running) break;
            }
            generateAndSend();
        }

        Logger.log("LSA_SENDER", "Encerrado.");
    }

    public void generateAndSend() {
        Map<String, Neighbor> neighbors = state.getNeighbors();

        StringBuilder sb = new StringBuilder("#");
        sb.append(state.getMyIp()).append(";");

        int seq = seqStore.nextAndSave();
        sb.append(seq);

        int activeCount = 0;
        for (Neighbor n : neighbors.values()) {
            if (n.isActive() && n.isEnabled()) {
                sb.append("*").append(n.getIp()).append(";").append(n.getCost());
                activeCount++;
            }
        }

        String lsaMsg = sb.toString();
        byte[] data   = lsaMsg.getBytes(StandardCharsets.UTF_8);

        Logger.log(Logger.LSA_GERADO,
                state.getMyIp() + " | SEQ=" + seq
                        + " | vizinhos_activos=" + activeCount
                        + " | " + lsaMsg);

        if (data.length > UDPListener.MAX_SIZE) {
            Logger.log(Logger.ERRO, "LSA excede 1200 bytes — não enviado");
            return;
        }

        java.util.Map<String, Integer> links = new java.util.LinkedHashMap<>();
        for (Neighbor n : neighbors.values()) {
            if (n.isActive() && n.isEnabled()) {
                links.put(n.getIp(), n.getCost());
            }
        }

        boolean topologyChanged = insertLocalLSA(seq, links);
        sendToActiveNeighbors(data, null);

        if (topologyChanged) {
            onTopologyChanged.run();
        }
    }

    private boolean insertLocalLSA(int seq, java.util.Map<String, Integer> links) {
        LSDB lsdb = state.getLsdb();
        return lsdb.accept(state.getMyIp(), seq, links);
    }

    public void sendToActiveNeighbors(byte[] data, String excludeIp) {
        for (Neighbor n : state.getNeighbors().values()) {
            if (!n.isActive() || !n.isEnabled()) continue;
            if (n.getIp().equals(excludeIp)) continue;

            try {
                InetAddress addr = InetAddress.getByName(n.getIp());
                DatagramPacket pkt = new DatagramPacket(
                        data, data.length, addr, UDPListener.PORT);
                state.getSocket().send(pkt);
            } catch (IOException e) {
                Logger.log(Logger.ERRO,
                        "Falha ao enviar LSA para " + n.getIp() + ": " + e.getMessage());
            }
        }
    }

    public void syncLSDBWithNeighbor(String neighborIp) {
        Collection<LSAEntry> snapshot = new java.util.ArrayList<>(state.getLsdb().getAll());

        Thread syncThread = new Thread(() -> {
            if (snapshot.isEmpty()) {
                Logger.log("LSDB_SYNC",
                        "LSDB vazia — nada a enviar para novo vizinho " + neighborIp);
                return;
            }

            Logger.log("LSDB_SYNC",
                    "Sincronização inicial com " + neighborIp
                            + " — enviando " + snapshot.size() + " LSA(s)");

            int sent = 0;
            for (LSAEntry entry : snapshot) {
                String lsaMsg = buildLSAString(entry);
                byte[] data   = lsaMsg.getBytes(StandardCharsets.UTF_8);

                if (data.length > UDPListener.MAX_SIZE) {
                    Logger.log(Logger.ERRO, "LSA de sync excede 1200 bytes — ignorado");
                    continue;
                }

                try {
                    InetAddress addr = InetAddress.getByName(neighborIp);
                    DatagramPacket pkt = new DatagramPacket(
                            data, data.length, addr, UDPListener.PORT);
                    state.getSocket().send(pkt);
                    Logger.log(Logger.LSA_ENCAMINHADO,
                            neighborIp + " | SYNC | origem=" + entry.getOriginIp()
                                    + " SEQ=" + entry.getSequenceNumber());
                    sent++;
                } catch (IOException e) {
                    Logger.log(Logger.ERRO,
                            "Falha ao sincronizar LSA com " + neighborIp + ": " + e.getMessage());
                }
            }

            Logger.log("LSDB_SYNC",
                    "Sincronização completa com " + neighborIp
                            + " — " + sent + "/" + snapshot.size() + " LSA(s) enviados");

        }, "LSDB-Sync-" + neighborIp);

        syncThread.setDaemon(true);
        syncThread.start();
    }

    private String buildLSAString(LSAEntry entry) {
        StringBuilder sb = new StringBuilder("#");
        sb.append(entry.getOriginIp()).append(";").append(entry.getSequenceNumber());
        for (Map.Entry<String, Integer> link : entry.getLinks().entrySet()) {
            sb.append("*").append(link.getKey()).append(";").append(link.getValue());
        }
        return sb.toString();
    }

    public void stop() {
        running = false;
        synchronized (lock) { lock.notifyAll(); }
    }
}
