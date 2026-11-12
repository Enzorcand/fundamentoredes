import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Teste de sincronização LSDB.
 */
public class TestSync {
    public static void main(String[] args) throws Exception {

        InetAddress target = InetAddress.getByName("127.0.0.1");
        int port = 6000;

        try (DatagramSocket sock = new DatagramSocket()) {
            System.out.println("=== Teste de Sincronização LSDB ===");
            System.out.println();

            String hello = "@192.168.1.2*192.168.1.1*192.168.1.3";
            send(sock, target, port, hello);
            System.out.println("[TESTE] HELLO enviado: " + hello);
            Thread.sleep(500);

            String lsa = "#192.168.1.3;5*192.168.1.1;5*192.168.1.2;8";
            send(sock, target, port, lsa);
            System.out.println("[TESTE] LSA externo enviado: " + lsa);
            Thread.sleep(500);

            String hello2 = "@192.168.1.2*192.168.1.1*192.168.1.3";
            send(sock, target, port, hello2);
            System.out.println("[TESTE] 2º HELLO enviado: " + hello2);

            System.out.println();
            System.out.println("[TESTE] Concluído.");
        }
    }

    private static void send(DatagramSocket sock, InetAddress addr, int port, String msg)
            throws Exception {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        sock.send(new DatagramPacket(data, data.length, addr, port));
    }
}
