package protocol;

import core.Logger;
import core.RouterState;
import model.LSDB;
import model.Neighbor;
import model.SequenceNumberStore;
import network.MessageParser;
import network.UDPListener;
import java.util.Map;

public class LSAHandler {

    private final RouterState state;
    private Runnable onTopologyChanged = () -> {};

    public LSAHandler(RouterState state) {
        this.state = state;
    }

    public void setOnTopologyChanged(Runnable cb) {
        this.onTopologyChanged = cb;
    }

    public void handle(MessageParser.LSAMessage lsa, String senderIp) {
        LSDB lsdb = state.getLsdb();

        Neighbor sender = state.getNeighbor(senderIp);
        if (sender == null || !sender.isEnabled()) {
            Logger.log(Logger.LSA_DESCARTADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | Remetente " + senderIp + " não é vizinho configurado/habilitado");
            return;
        }

        if (lsa.originIp.equals(state.getMyIp())) {
            SequenceNumberStore seqStore = state.getSeqStore();
            if (seqStore != null && lsa.sequenceNumber >= seqStore.getCurrent()) {
                seqStore.advanceTo(lsa.sequenceNumber);
                state.notifyNeighborStateChange();
            }
            Logger.log(Logger.LSA_DESCARTADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | LSA próprio recebido — descartado sem encaminhar");
            return;
        }

        if (!lsdb.isNewer(lsa.originIp, lsa.sequenceNumber)) {
            int maxSeen = getMaxSeen(lsdb, lsa.originIp);
            Logger.log(Logger.LSA_DESCARTADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | Duplicado/Antigo — descartado (max_visto=" + maxSeen + ")");

            if (lsa.sequenceNumber < maxSeen && lsa.originIp.equals(senderIp)) {
                sendNewerCopyToOrigin(lsdb, lsa.originIp, maxSeen);
            }
            return;
        }

        boolean topologyChanged = lsdb.accept(lsa.originIp, lsa.sequenceNumber, lsa.links);
        Logger.log(Logger.LSA_ACEITO, lsa.originIp,
                "SEQ=" + lsa.sequenceNumber
                        + " | via=" + senderIp
                        + " | links=" + lsa.links.size());

        String rawLsa = buildLSAString(lsa);
        byte[] data   = rawLsa.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        if (data.length <= UDPListener.MAX_SIZE) {
            int forwarded = 0;
            for (Neighbor n : state.getNeighbors().values()) {
                if (!n.isActive() || !n.isEnabled()) continue;
                if (n.getIp().equals(senderIp)) continue;

                try {
                    java.net.InetAddress addr = java.net.InetAddress.getByName(n.getIp());
                    java.net.DatagramPacket pkt = new java.net.DatagramPacket(
                            data, data.length, addr, UDPListener.PORT);
                    state.getSocket().send(pkt);
                    forwarded++;
                } catch (java.io.IOException e) {
                    Logger.log(Logger.ERRO,
                            "Flooding LSA para " + n.getIp() + " falhou: " + e.getMessage());
                }
            }
            Logger.log(Logger.LSA_ENCAMINHADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | encaminhado para " + forwarded + " vizinho(s)");
        }

        if (topologyChanged) {
            onTopologyChanged.run();
        }
    }

    private String buildLSAString(MessageParser.LSAMessage lsa) {
        StringBuilder sb = new StringBuilder("#");
        sb.append(lsa.originIp).append(";").append(lsa.sequenceNumber);
        for (Map.Entry<String, Integer> link : lsa.links.entrySet()) {
            sb.append("*").append(link.getKey()).append(";").append(link.getValue());
        }
        return sb.toString();
    }

    private void sendNewerCopyToOrigin(LSDB lsdb, String originIp, int maxSeen) {
        StringBuilder sb = new StringBuilder("#");
        sb.append(originIp).append(";").append(maxSeen);
        model.LSAEntry stored = lsdb.get(originIp);
        if (stored != null && stored.getSequenceNumber() == maxSeen) {
            for (Map.Entry<String, Integer> link : stored.getLinks().entrySet()) {
                sb.append("*").append(link.getKey()).append(";").append(link.getValue());
            }
        }
        byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (data.length > UDPListener.MAX_SIZE) return;
        try {
            java.net.InetAddress addr = java.net.InetAddress.getByName(originIp);
            state.getSocket().send(new java.net.DatagramPacket(data, data.length, addr, UDPListener.PORT));
            Logger.log(Logger.LSA_ENCAMINHADO, originIp,
                    "SEQ=" + maxSeen + " | versão mais recente devolvida à origem (ela anunciou SEQ menor)");
        } catch (java.io.IOException e) {
            Logger.log(Logger.ERRO, "Falha ao devolver LSA a " + originIp + ": " + e.getMessage());
        }
    }

    private int getMaxSeen(LSDB lsdb, String originIp) {
        return lsdb.getMaxSeqSeen(originIp);
    }
}
