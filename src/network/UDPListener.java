package network;

import core.Logger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

/**
 * Thread de escuta contínua de pacotes UDP na porta 6000.
 */
public class UDPListener implements Runnable {

    public static final int PORT     = 6000;
    public static final int MAX_SIZE = 1200;

    private final DatagramSocket socket;
    private final BlockingQueue<RawMessage> queue;
    private volatile boolean running = true;

    public UDPListener(DatagramSocket socket, BlockingQueue<RawMessage> queue) {
        this.socket = socket;
        this.queue  = queue;
    }

    @Override
    public void run() {
        byte[] buf = new byte[MAX_SIZE + 1];
        Logger.log("UDP_LISTENER", "A escutar na porta " + PORT);

        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (SocketException e) {
                if (running) {
                    Logger.log(Logger.ERRO, "Socket fechado inesperadamente: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                Logger.log(Logger.ERRO, "Erro ao receber datagrama: " + e.getMessage());
                continue;
            }

            String senderIp = packet.getAddress().getHostAddress();
            int length = packet.getLength();

            if (length > MAX_SIZE) {
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        String.format("Datagrama de %s excede %d bytes (%d bytes) — descartado",
                                senderIp, MAX_SIZE, length));
                continue;
            }

            String raw;
            try {
                raw = new String(packet.getData(), 0, length, StandardCharsets.UTF_8);
            } catch (Exception e) {
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        "Datagrama de " + senderIp + " não é UTF-8 válido — descartado");
                continue;
            }

            if (raw.endsWith("\n") || raw.endsWith("\r\n") || raw.endsWith("\r")) {
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        "Datagrama de " + senderIp + " contém quebra de linha no final — descartado");
                continue;
            }

            if (raw.isEmpty()) {
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        "Datagrama vazio de " + senderIp + " — descartado");
                continue;
            }

            MessageType type = MessageType.fromRaw(raw);
            if (type == MessageType.UNKNOWN) {
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        String.format("Tipo desconhecido (prefixo='%c') de %s — descartado",
                                raw.charAt(0), senderIp));
                continue;
            }

            RawMessage msg = new RawMessage(raw, senderIp);
            try {
                queue.put(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.log("UDP_LISTENER", "Encerrado.");
    }

    public void stop() {
        running = false;
        socket.close();
    }
}
