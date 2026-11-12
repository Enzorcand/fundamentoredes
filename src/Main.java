import core.CLI;
import core.ConfigLoader;
import core.Logger;
import core.RouterState;
import model.LSDB;
import model.Neighbor;
import model.SequenceNumberStore;
import network.MessageParser;
import network.MessageProcessor;
import network.RawMessage;
import network.UDPListener;
import protocol.HelloHandler;
import protocol.HelloSender;
import protocol.LSAExpirationMonitor;
import protocol.LSAHandler;
import protocol.LSASender;
import protocol.NeighborMonitor;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Ponto de entrada da aplicação de roteamento Link State (OSPF-like).
 *
 * Uso: java -cp out Main [IP_DO_ROTEADOR]
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("=== Roteador Link State (OSPF-like) ===");
        System.out.println();

        // 1. Determinar IP próprio
        String myIp = resolveMyIp(args);
        if (myIp == null) {
            Logger.log(Logger.ERRO, "Não foi possível determinar o IP do roteador. "
                    + "Passe-o como argumento: java -cp out Main <IP>");
            System.exit(1);
        }
        Logger.log("INICIALIZADO", "IP do roteador: " + myIp);

        // 2. Carregar vizinhos do ficheiro roteadores.txt
        Map<String, Neighbor> neighbors = ConfigLoader.loadNeighbors();
        if (neighbors.isEmpty()) {
            Logger.log(Logger.ERRO, "Nenhum vizinho carregado. Verifique roteadores.txt.");
            System.exit(1);
        }
        Logger.log("INICIALIZADO", neighbors.size() + " vizinho(s) configurado(s)");

        // 3. Inicializar LSDB
        LSDB lsdb = new LSDB();

        // 4. Abrir socket UDP na porta 6000
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(UDPListener.PORT);
            Logger.log("SOCKET_ABERTO", "UDP porta " + UDPListener.PORT);
        } catch (SocketException e) {
            Logger.log(Logger.ERRO, "Não foi possível abrir socket UDP na porta "
                    + UDPListener.PORT + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        // 5. Estado partilhado (RouterState)
        RouterState state = new RouterState(myIp, socket, neighbors, lsdb);

        // 6. Número de sequência persistente
        SequenceNumberStore seqStore = new SequenceNumberStore(myIp);

        // 7. Handlers e emissores do protocolo
        LSASender lsaSender = new LSASender(state, seqStore);
        LSAHandler lsaHandler = new LSAHandler(state);
        LSAExpirationMonitor expirationMonitor = new LSAExpirationMonitor(state);
        HelloHandler helloHandler = new HelloHandler(state);

        // 8. Ligação de callbacks e gatilhos de eventos
        state.setOnNeighborStateChange(lsaSender::triggerNow);
        state.setOnNewNeighborActive(lsaSender::syncLSDBWithNeighbor);
        lsaHandler.setOnTopologyChanged(state::triggerDijkstra);
        expirationMonitor.setOnTopologyChanged(state::triggerDijkstra);
        lsaSender.setOnTopologyChanged(state::triggerDijkstra);

        // 9. Fila assíncrona e threads de execução
        BlockingQueue<RawMessage> messageQueue = new LinkedBlockingQueue<>();

        UDPListener      listener   = new UDPListener(socket, messageQueue);
        MessageProcessor processor  = new MessageProcessor(messageQueue, helloHandler, lsaHandler, state.getDataHandler());
        HelloSender      helloSnd   = new HelloSender(state);
        NeighborMonitor  monitor    = new NeighborMonitor(state);

        Thread listenerThread  = new Thread(listener,          "UDP-Listener");
        Thread processorThread = new Thread(processor,         "Msg-Processor");
        Thread helloThread     = new Thread(helloSnd,          "Hello-Sender");
        Thread monitorThread   = new Thread(monitor,           "Neighbor-Monitor");
        Thread lsaThread       = new Thread(lsaSender,         "LSA-Sender");
        Thread expiryThread    = new Thread(expirationMonitor, "LSA-Expiry");

        for (Thread t : new Thread[]{listenerThread, processorThread,
                helloThread, monitorThread, lsaThread, expiryThread}) {
            t.setDaemon(true);
            t.start();
        }

        // 10. Hook de encerramento
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.log("SHUTDOWN", "A encerrar roteador " + myIp + "...");
            listener.stop();
            processor.stop();
            helloSnd.stop();
            monitor.stop();
            lsaSender.stop();
            expirationMonitor.stop();
        }, "Shutdown-Hook"));

        Logger.log("PRONTO", "Roteador " + myIp
                + " activo. Aguardando mensagens na porta " + UDPListener.PORT);
        System.out.println();
        lsdb.print();
        System.out.println();
        state.getRoutingTable().print();

        // 11. Interface interativa na thread principal
        CLI cli = new CLI(state);
        cli.run();

        try {
            listenerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String resolveMyIp(String[] args) {
        if (args.length >= 1 && MessageParser.isValidIp(args[0])) {
            return args[0];
        }
        try {
            String detected = InetAddress.getLocalHost().getHostAddress();
            Logger.log("AUTO_IP", "IP detectado automaticamente: " + detected);
            return detected;
        } catch (Exception e) {
            return null;
        }
    }
}
