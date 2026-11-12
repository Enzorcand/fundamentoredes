package protocol;

import core.Logger;
import core.RouterState;
import model.Neighbor;
import network.UDPListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Thread de envio periódico de mensagens HELLO a cada 5 segundos.
 */
public class HelloSender implements Runnable {

    private static final int HELLO_INTERVAL_MS = 5_000;

    private final RouterState state;
    private volatile boolean running = true;

    public HelloSender(RouterState state) {
        this.state = state;
    }

    @Override
    public void run() {
        Logger.log("HELLO_SENDER", "Iniciado — intervalo " + HELLO_INTERVAL_MS / 1000 + "s");

        while (running) {
            sendHelloToAll();
            try {
                Thread.sleep(HELLO_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.log("HELLO_SENDER", "Encerrado.");
    }

    private void sendHelloToAll() {
        StringBuilder sb = new StringBuilder("@");
        sb.append(state.getMyIp());

        Map<String, Neighbor> neighbors = state.getNeighbors();
        for (Neighbor n : neighbors.values()) {
            if (n.isEnabled()) {
                sb.append("*").append(n.getIp());
            }
        }

        String message = sb.toString();
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        if (data.length > UDPListener.MAX_SIZE) {
            Logger.log(Logger.ERRO, "HELLO excede 1200 bytes — não enviado");
            return;
        }

        for (Neighbor neighbor : neighbors.values()) {
            if (!neighbor.isEnabled()) continue;

            try {
                InetAddress address = InetAddress.getByName(neighbor.getIp());
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, address, UDPListener.PORT);
                state.getSocket().send(packet);
                Logger.log(Logger.HELLO_ENVIADO,
                        neighbor.getIp() + " | " + message);
            } catch (IOException e) {
                Logger.log(Logger.ERRO,
                        "Falha ao enviar HELLO para " + neighbor.getIp()
                                + ": " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
    }
}
