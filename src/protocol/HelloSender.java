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

    private static String buildHello(RouterState state, Map<String, Neighbor> neighbors) {
        StringBuilder sb = new StringBuilder("@");
        sb.append(state.getMyIp());
        for (Neighbor n : neighbors.values()) {
            if (n.isEnabled()) {
                sb.append("*").append(n.getIp());
            }
        }
        return sb.toString();
    }

    private void sendHelloToAll() {
        Map<String, Neighbor> neighbors = state.getNeighbors();
        String message = buildHello(state, neighbors);

        for (Neighbor neighbor : neighbors.values()) {
            if (!neighbor.isEnabled()) continue;
            send(state, neighbor.getIp(), message);
        }
    }

    public static void sendHelloTo(RouterState state, String neighborIp) {
        Neighbor n = state.getNeighbor(neighborIp);
        if (n == null || !n.isEnabled()) return;
        send(state, neighborIp, buildHello(state, state.getNeighbors()));
    }

    private static void send(RouterState state, String ip, String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        if (data.length > UDPListener.MAX_SIZE) {
            Logger.log(Logger.ERRO, "HELLO excede " + UDPListener.MAX_SIZE + " bytes — não enviado");
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, address, UDPListener.PORT);
            state.getSocket().send(packet);
            Logger.log(Logger.HELLO_ENVIADO, ip + " | " + message);
        } catch (IOException e) {
            Logger.log(Logger.ERRO, "Falha ao enviar HELLO para " + ip + ": " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
