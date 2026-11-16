import core.RouterState;
import model.LSDB;
import model.Neighbor;
import model.RoutingEntry;
import network.MessageParser;
import protocol.DataHandler;

import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * Teste unitário e de integração para a lógica de dados.
 */
public class TestData {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Teste do Encaminhamento de Mensagens DATA ===\n");

        String myIp = "192.168.1.1";
        DatagramSocket dummySocket = new DatagramSocket();
        Map<String, Neighbor> neighbors = new HashMap<>();
        neighbors.put("192.168.1.2", new Neighbor("192.168.1.2", 10));
        LSDB lsdb = new LSDB();

        RouterState state = new RouterState(myIp, dummySocket, neighbors, lsdb);

        Map<String, RoutingEntry> routes = new HashMap<>();
        routes.put("192.168.1.7", new RoutingEntry("192.168.1.7", 12, "192.168.1.2"));
        state.getRoutingTable().update(routes);

        DataHandler dataHandler = state.getDataHandler();

        System.out.println("--- Teste 1: Mensagem destinada ao próprio nó (192.168.1.1) ---");
        MessageParser.DataMessage msgForMe =
                new MessageParser.DataMessage("192.168.1.3", "192.168.1.1", 100, 15, "Olá R1!");
        dataHandler.handleReceived(msgForMe, "192.168.1.2");
        System.out.println("[OK] Teste 1 concluído.");

        System.out.println("\n--- Teste 2: Encaminhamento com rota válida e decremento de TTL ---");
        MessageParser.DataMessage msgToForward =
                new MessageParser.DataMessage("192.168.1.3", "192.168.1.7", 101, 8, "Mensagem em trânsito");
        dataHandler.handleReceived(msgToForward, "192.168.1.3");
        System.out.println("[OK] Teste 2 concluído.");

        System.out.println("\n--- Teste 3: Descarte por TTL esgotado (TTL=0) ---");
        MessageParser.DataMessage msgZeroTtl =
                new MessageParser.DataMessage("192.168.1.3", "192.168.1.7", 102, 0, "Mensagem moribunda");
        dataHandler.handleReceived(msgZeroTtl, "192.168.1.3");
        System.out.println("[OK] Teste 3 concluído.");

        System.out.println("\n--- Teste 4: Descarte por inexistência de rota ---");
        MessageParser.DataMessage msgNoRoute =
                new MessageParser.DataMessage("192.168.1.3", "10.0.0.99", 103, 10, "Destino desconhecido");
        dataHandler.handleReceived(msgNoRoute, "192.168.1.3");
        System.out.println("[OK] Teste 4 concluído.");

        System.out.println("\n--- Teste 5: Envio de nova mensagem gerada localmente ---");
        boolean sentSuccess = dataHandler.sendMessage("192.168.1.7", "Primeira mensagem originada em R1!");
        if (!sentSuccess) throw new AssertionError("Deveria ter enviado com sucesso");

        boolean sentFail = dataHandler.sendMessage("10.0.0.99", "Mensagem sem rota");
        if (sentFail) throw new AssertionError("Deveria ter falhado por falta de rota");
        System.out.println("[OK] Teste 5 concluído.");

        dummySocket.close();
        System.out.println("\n>>> TODOS OS TESTES DE DADOS PASSARAM COM SUCESSO! <<<");
    }
}
