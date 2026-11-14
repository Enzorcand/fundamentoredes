package protocol;

import core.Logger;
import core.RouterState;
import model.LSDB;
import model.Neighbor;
import network.MessageParser;
import network.UDPListener;
import java.util.Map;

/**
 * Lógica de processamento de LSAs recebidos e flooding.
 */
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

        // 1. Identificar o vizinho remetente (suporta localhost para testes locais)
        Neighbor sender = state.getNeighbor(senderIp);
        if (sender == null && ("127.0.0.1".equals(senderIp) || "0:0:0:0:0:0:0:1".equals(senderIp))) {
            sender = state.getNeighbor(lsa.originIp);
            if (sender == null) {
                for (Neighbor n : state.getNeighbors().values()) {
                    if (n.isEnabled()) {
                        sender = n;
                        break;
                    }
                }
            }
        }

        // Conforme Adendo 158: Aceitação de LSA de qualquer vizinho CONFIGURADO (e habilitado),
        // não só de vizinho ativo (elimina perda de sincronização inicial).
        // Se o enlace foi desativado logicamente (disable), descarta o LSA dele.
        if (sender == null || !sender.isEnabled()) {
            Logger.log(Logger.LSA_DESCARTADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | Remetente " + senderIp + " não é vizinho configurado/habilitado");
            return;
        }

        // 2. Verificar se o LSA tem origem no próprio roteador
        // Conforme linha 44 e Adendo 161: descartar sem encaminhar, e saltar sequência se número for maior/igual
        if (lsa.originIp.equals(state.getMyIp())) {
            if (state.getSeqStore() != null && lsa.sequenceNumber >= state.getSeqStore().getCurrent()) {
                state.getSeqStore().advanceTo(lsa.sequenceNumber);
                state.notifyNeighborStateChange(); // Dispara LSA com sequência superando o anúncio antigo
            }
            Logger.log(Logger.LSA_DESCARTADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | LSA próprio recebido — descartado sem encaminhar");
            return;
        }

        if (!lsdb.isNewer(lsa.originIp, lsa.sequenceNumber)) {
            Logger.log(Logger.LSA_DESCARTADO,
                    lsa.originIp + " | SEQ=" + lsa.sequenceNumber
                            + " | Duplicado/Antigo — descartado (max_visto="
                            + getMaxSeen(lsdb, lsa.originIp) + ")");
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

    private int getMaxSeen(LSDB lsdb, String originIp) {
        model.LSAEntry e = lsdb.get(originIp);
        return e != null ? e.getSequenceNumber() : 0;
    }
}
