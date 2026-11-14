package protocol;

import core.Logger;
import core.RouterState;
import model.RoutingEntry;
import network.MessageParser;
import network.UDPListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestor e encaminhador de mensagens de dados (Mensagem 3 - DATA).
 *
 * Conforme enunciado:
 * - O roteador de ORIGEM envia com o TTL inicial, SEM decrementá-lo.
 * - Roteadores INTERMEDIÁRIOS decrementam TTL antes de encaminhar.
 * - TTL é opcional ao enviar (0–255); padrão 16.
 */
public class DataHandler {

    public static final int INITIAL_TTL = 16;
    public static final int MAX_TTL     = 255;

    private final RouterState state;
    private final AtomicInteger nextId = new AtomicInteger(1001);

    public DataHandler(RouterState state) {
        this.state = state;
    }

    /** Processa mensagem DATA recebida de outro roteador. */
    public void handleReceived(MessageParser.DataMessage data, String senderIp) {
        if (data.destIp.equals(state.getMyIp())) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    NOVA MENSAGEM RECEBIDA                       ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.printf( "║ De:       %-54s ║%n", data.originIp);
            System.out.printf( "║ ID:       %-54d ║%n", data.id);
            System.out.printf( "║ TTL:      %-54d ║%n", data.ttl);
            System.out.printf( "║ Conteúdo: %-54s ║%n", truncate(data.content, 54));
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.println();

            Logger.log(Logger.DATA_RECEBIDO,
                    String.format("ID=%d | ORIGEM %s | DESTINO %s | %s",
                            data.id, data.originIp, data.destIp, data.content));
            return;
        }

        // Roteador intermediário: verificar TTL e encaminhar
        if (data.ttl <= 0) {
            Logger.log(Logger.DATA_DESCARTADA,
                    String.format("ID=%d | DESTINO %s | Motivo: TTL expirado (TTL=%d)",
                            data.id, data.destIp, data.ttl));
            return;
        }

        RoutingEntry route = state.getRoutingTable().get(data.destIp);
        if (route == null) {
            Logger.log(Logger.DATA_DESCARTADA,
                    String.format("ID=%d | DESTINO %s | Motivo: Sem rota para o destino",
                            data.id, data.destIp));
            return;
        }

        // Decrementa TTL e encaminha (apenas roteadores intermediários decrementam)
        int newTtl = data.ttl - 1;
        String nextHopIp = route.getNextHop();
        String payload = String.format("!%s;%s;%d;%d;%s",
                data.originIp, data.destIp, data.id, newTtl, data.content);

        byte[] rawBytes = payload.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length > UDPListener.MAX_SIZE) {
            Logger.log(Logger.DATA_DESCARTADA,
                    String.format("ID=%d | DESTINO %s | Motivo: Pacote excede %d bytes",
                            data.id, data.destIp, UDPListener.MAX_SIZE));
            return;
        }

        try {
            InetAddress nextHopAddr = InetAddress.getByName(nextHopIp);
            DatagramPacket packet = new DatagramPacket(
                    rawBytes, rawBytes.length, nextHopAddr, UDPListener.PORT);
            state.getSocket().send(packet);

            Logger.log(Logger.DATA_ENCAMINHADA,
                    String.format("ID=%d | DESTINO %s | VIA %s | TTL=%d | %s",
                            data.id, data.destIp, nextHopIp, newTtl, data.content));
        } catch (IOException e) {
            Logger.log(Logger.ERRO,
                    String.format("Falha ao encaminhar DATA ID=%d para %s: %s",
                            data.id, nextHopIp, e.getMessage()));
        }
    }

    /**
     * Envia mensagem DATA com TTL padrão (16).
     */
    public boolean sendMessage(String destIp, String message) {
        return sendMessage(destIp, message, INITIAL_TTL);
    }

    /**
     * Envia mensagem DATA com TTL customizado (0–255).
     * Conforme enunciado: a origem envia com o TTL inicial sem decrementá-lo.
     */
    public boolean sendMessage(String destIp, String message, int ttl) {
        if (ttl < 0 || ttl > MAX_TTL) {
            Logger.log(Logger.ERRO,
                    String.format("TTL inválido: %d (deve ser 0–255). Usando padrão %d.", ttl, INITIAL_TTL));
            ttl = INITIAL_TTL;
        }

        int id = nextId.getAndIncrement();

        if (destIp.equals(state.getMyIp())) {
            Logger.log(Logger.DATA_RECEBIDO,
                    String.format("ID=%d | ORIGEM %s | DESTINO %s (local) | %s",
                            id, state.getMyIp(), destIp, message));
            return true;
        }

        RoutingEntry route = state.getRoutingTable().get(destIp);
        if (route == null) {
            Logger.log(Logger.DATA_DESCARTADA,
                    String.format("ID=%d | DESTINO %s | Motivo: Sem rota para o destino no momento do envio",
                            id, destIp));
            return false;
        }

        String nextHopIp = route.getNextHop();
        // Origem envia com TTL inicial, sem decrementar
        String payload = String.format("!%s;%s;%d;%d;%s",
                state.getMyIp(), destIp, id, ttl, message);

        byte[] rawBytes = payload.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length > UDPListener.MAX_SIZE) {
            Logger.log(Logger.DATA_DESCARTADA,
                    String.format("ID=%d | DESTINO %s | Motivo: Pacote excede %d bytes",
                            id, destIp, UDPListener.MAX_SIZE));
            return false;
        }

        try {
            InetAddress nextHopAddr = InetAddress.getByName(nextHopIp);
            DatagramPacket packet = new DatagramPacket(
                    rawBytes, rawBytes.length, nextHopAddr, UDPListener.PORT);
            state.getSocket().send(packet);

            Logger.log(Logger.DATA_ENCAMINHADA,
                    String.format("ID=%d | DESTINO %s | VIA %s | TTL=%d | %s",
                            id, destIp, nextHopIp, ttl, message));
            return true;
        } catch (IOException e) {
            Logger.log(Logger.ERRO,
                    String.format("Falha ao enviar DATA ID=%d para %s: %s",
                            id, nextHopIp, e.getMessage()));
            return false;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
