import core.CLI;
import core.RouterState;
import model.LSDB;
import model.Neighbor;

import java.io.ByteArrayInputStream;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Teste unitário para a CLI interativa.
 */
public class TestCLI {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Teste da Interface Interativa CLI ===\n");

        String myIp = "192.168.1.1";
        DatagramSocket dummySocket = new DatagramSocket();
        Map<String, Neighbor> neighbors = new HashMap<>();
        Neighbor n2 = new Neighbor("192.168.1.2", 10);
        neighbors.put("192.168.1.2", n2);
        LSDB lsdb = new LSDB();

        RouterState state = new RouterState(myIp, dummySocket, neighbors, lsdb);

        final boolean[] lsaTriggered = {false};
        state.setOnNeighborStateChange(() -> {
            lsaTriggered[0] = true;
            System.out.println("  [TRIGGER] Callback de alteração de estado/LSA acionado com sucesso!");
        });

        n2.setActive(true);

        String simulatedInput = String.join("\n",
                "neighbors",
                "cost 192.168.1.2 25",
                "disable 192.168.1.2",
                "enable 192.168.1.2",
                "send 192.168.1.2 Olá mundo CLI",
                "routes",
                "lsdb",
                "help"
        ) + "\n";

        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));

        CLI cli = new CLI(state);
        cli.run();

        System.out.println("\n--- Asserções dos Comandos CLI ---");

        if (n2.getCost() != 25) {
            throw new AssertionError("Custo deveria ser 25, obtido: " + n2.getCost());
        }
        System.out.println("  [OK] Custo alterado dinamicamente para 25.");

        if (!lsaTriggered[0]) {
            throw new AssertionError("Deveria ter disparado trigger de LSA ao mudar custo/estado.");
        }
        System.out.println("  [OK] Geração imediata de LSA e Dijkstra disparada.");

        if (!n2.isEnabled()) {
            throw new AssertionError("Enlace deveria estar habilitado após 'enable'.");
        }
        System.out.println("  [OK] Enlace habilitado aguardando HELLO.");

        dummySocket.close();
        System.out.println("\n>>> TODOS OS TESTES DA CLI PASSARAM COM SUCESSO! <<<");
    }
}
