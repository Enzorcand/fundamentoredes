import model.*;
import routing.DijkstraRouter;
import network.MessageParser;
import network.MessageParser.DataMessage;
import network.MessageParser.LSAMessage;
import protocol.DataHandler;

import java.util.*;

/**
 * Simulador e verificador automatizado de todos os cenários da apresentação (A até H):
 *
 * Cenário A: Convergência inicial da rede
 * Cenário B: Entrada de um novo roteador
 * Cenário C: Falha de um enlace
 * Cenário D: Falha de um roteador (expiração de LSAs)
 * Cenário E: Recuperação após falha
 * Cenário F: Recebimento de LSAs antigos e duplicados
 * Cenário G: Alteração do custo de um enlace
 * Cenário H: Encaminhamento de DATA com TTL insuficiente
 */
public class SimuladorCenarios {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.println("         SIMULAÇÃO DOS CENÁRIOS DE APRESENTAÇÃO (A até H)           ");
        System.out.println("══════════════════════════════════════════════════════════════════════\n");

        testCenarioA();
        testCenarioB();
        testCenarioC();
        testCenarioD();
        testCenarioE();
        testCenarioF();
        testCenarioG();
        testCenarioH();

        System.out.println("\n══════════════════════════════════════════════════════════════════════");
        System.out.printf(" RESULTADO GERAL: %d PASSARAM | %d FALHARAM%n", totalPassed, totalFailed);
        System.out.println("══════════════════════════════════════════════════════════════════════");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  [SUCESSO] " + description);
            totalPassed++;
        } else {
            System.err.println("  [FALHA] " + description);
            totalFailed++;
        }
    }

    // Topologia base:
    // R1 --- (10) --- R2
    //  |               |
    // (5)             (5)
    //  |               |
    // R3 --- (2)  --- R4
    private static void testCenarioA() {
        System.out.println("──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário A: Convergência inicial da rede (R1, R2, R3, R4)");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r1 = "192.168.1.1", r2 = "192.168.1.2", r3 = "192.168.1.3", r4 = "192.168.1.4";

        Map<String, Integer> l1 = new HashMap<>();
        l1.put(r2, 10);
        l1.put(r3, 5);
        lsdb.accept(r1, 1, l1);

        Map<String, Integer> l2 = new HashMap<>();
        l2.put(r1, 10);
        l2.put(r4, 5);
        lsdb.accept(r2, 1, l2);

        Map<String, Integer> l3 = new HashMap<>();
        l3.put(r1, 5);
        l3.put(r4, 2);
        lsdb.accept(r3, 1, l3);

        Map<String, Integer> l4 = new HashMap<>();
        l4.put(r2, 5);
        l4.put(r3, 2);
        lsdb.accept(r4, 1, l4);

        Set<String> activeNeighbors = new HashSet<>(Arrays.asList(r2, r3));
        DijkstraRouter router = new DijkstraRouter(r1, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes(activeNeighbors);

        check(routes.containsKey(r2), "R1 tem rota para R2");
        check(routes.get(r2).getTotalCost() == 10, "Custo para R2 é 10 (via enlace direto)");
        check(routes.get(r3).getTotalCost() == 5, "Custo para R3 é 5 (via enlace direto)");
        // R1 -> R3 (5) -> R4 (2) = 7 (menor que R1 -> R2 (10) -> R4 (5) = 15)
        check(routes.get(r4).getTotalCost() == 7, "Custo para R4 é 7 via R3");
        check(routes.get(r4).getNextHop().equals(r3), "Próximo salto para R4 é R3");
    }

    private static void testCenarioB() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário B: Entrada de um novo roteador (R5 se conecta a R4)");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r1 = "192.168.1.1", r2 = "192.168.1.2", r3 = "192.168.1.3", r4 = "192.168.1.4", r5 = "192.168.1.5";

        Map<String, Integer> l1 = Map.of(r2, 10, r3, 5);
        Map<String, Integer> l2 = Map.of(r1, 10, r4, 5);
        Map<String, Integer> l3 = Map.of(r1, 5, r4, 2);
        Map<String, Integer> l4 = new HashMap<>(Map.of(r2, 5, r3, 2));

        lsdb.accept(r1, 1, l1);
        lsdb.accept(r2, 1, l2);
        lsdb.accept(r3, 1, l3);
        lsdb.accept(r4, 1, l4);

        // Novo nó R5 entra com enlace para R4 de custo 3
        l4.put(r5, 3);
        lsdb.accept(r4, 2, l4);

        Map<String, Integer> l5 = Map.of(r4, 3);
        lsdb.accept(r5, 1, l5);

        Set<String> activeNeighbors = new HashSet<>(Arrays.asList(r2, r3));
        DijkstraRouter router = new DijkstraRouter(r1, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes(activeNeighbors);

        check(routes.containsKey(r5), "Nova rota para R5 foi incluída na tabela");
        // R1 -> R3 (5) -> R4 (2) -> R5 (3) = 10
        check(routes.get(r5).getTotalCost() == 10, "Custo total para R5 é 10");
        check(routes.get(r5).getNextHop().equals(r3), "Próximo salto para R5 é R3");
    }

    private static void testCenarioC() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário C: Falha de um enlace (Enlace R1-R3 cai)");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r1 = "192.168.1.1", r2 = "192.168.1.2", r3 = "192.168.1.3", r4 = "192.168.1.4";

        Map<String, Integer> l1 = Map.of(r2, 10); // R3 removido após falha de enlace
        Map<String, Integer> l2 = Map.of(r1, 10, r4, 5);
        Map<String, Integer> l3 = Map.of(r4, 2);   // R1 removido após falha de enlace
        Map<String, Integer> l4 = Map.of(r2, 5, r3, 2);

        lsdb.accept(r1, 2, l1);
        lsdb.accept(r2, 1, l2);
        lsdb.accept(r3, 2, l3);
        lsdb.accept(r4, 1, l4);

        // R3 não é mais vizinho ativo de R1
        Set<String> activeNeighbors = new HashSet<>(Collections.singletonList(r2));
        DijkstraRouter router = new DijkstraRouter(r1, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes(activeNeighbors);

        // Agora R4 alcançado por R1 -> R2 (10) -> R4 (5) = 15
        check(routes.get(r4).getTotalCost() == 15, "Novo custo para R4 após desvio é 15");
        check(routes.get(r4).getNextHop().equals(r2), "Próximo salto para R4 desviou para R2");
        // R3 alcançado por R1 -> R2 (10) -> R4 (5) -> R3 (2) = 17
        check(routes.get(r3).getTotalCost() == 17, "R3 ainda alcançável com custo 17 via R2");
        check(routes.get(r3).getNextHop().equals(r2), "Próximo salto para R3 agora é R2");
    }

    private static void testCenarioD() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário D: Falha de um roteador (R4 cai completamente)");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r1 = "192.168.1.1", r2 = "192.168.1.2", r4 = "192.168.1.4";

        lsdb.accept(r1, 1, Map.of(r2, 10, r4, 20));
        lsdb.accept(r2, 1, Map.of(r1, 10, r4, 5));
        lsdb.accept(r4, 1, Map.of(r1, 20, r2, 5));

        // R4 falhou: LSAs dele expiram após 30s ou são removidos
        lsdb.remove(r4);

        Set<String> activeNeighbors = new HashSet<>(Collections.singletonList(r2));
        DijkstraRouter router = new DijkstraRouter(r1, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes(activeNeighbors);

        check(!routes.containsKey(r4), "Rota para R4 foi removida da tabela");
        check(routes.containsKey(r2), "Rota para R2 permanece íntegra");
    }

    private static void testCenarioE() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário E: Recuperação após falha (R4 volta e reconverge)");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r1 = "192.168.1.1", r2 = "192.168.1.2", r4 = "192.168.1.4";

        lsdb.accept(r1, 2, Map.of(r2, 10, r4, 4));
        lsdb.accept(r2, 2, Map.of(r1, 10, r4, 5));
        lsdb.accept(r4, 2, Map.of(r1, 4, r2, 5));

        Set<String> activeNeighbors = new HashSet<>(Arrays.asList(r2, r4));
        DijkstraRouter router = new DijkstraRouter(r1, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes(activeNeighbors);

        check(routes.containsKey(r4), "Rota para R4 restabelecida");
        check(routes.get(r4).getTotalCost() == 4, "Custo para R4 atualizado para 4");
    }

    private static void testCenarioF() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário F: Recebimento de LSAs antigos e duplicados");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r2 = "192.168.1.2";

        // LSA inicial SEQ=10
        lsdb.accept(r2, 10, Map.of("192.168.1.1", 5));
        check(lsdb.get(r2).getSequenceNumber() == 10, "LSA com SEQ=10 armazenado na LSDB");

        // Chegada de LSA duplicado (SEQ=10)
        boolean acceptedDup = lsdb.accept(r2, 10, Map.of("192.168.1.1", 5));
        check(!acceptedDup, "LSA duplicado (SEQ=10) é descartado (retorna false para alteração)");

        // Chegada de LSA antigo (SEQ=9)
        boolean acceptedOld = lsdb.accept(r2, 9, Map.of("192.168.1.1", 2));
        check(!acceptedOld, "LSA antigo (SEQ=9) é sumariamente descartado");
        check(lsdb.get(r2).getSequenceNumber() == 10, "Sequência mantida em 10");

        // Chegada de LSA mais recente (SEQ=11)
        boolean acceptedNew = lsdb.accept(r2, 11, Map.of("192.168.1.1", 8));
        check(acceptedNew, "LSA novo (SEQ=11) é aceito");
        check(lsdb.get(r2).getSequenceNumber() == 11, "Sequência atualizada para 11");
    }

    private static void testCenarioG() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário G: Alteração do custo de um enlace");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        LSDB lsdb = new LSDB();
        String r1 = "192.168.1.1", r2 = "192.168.1.2", r3 = "192.168.1.3";

        // Inicial: R1-R2=10, R1-R3=5, R3-R2=2 -> R1 para R2 via R3 custa 7
        lsdb.accept(r1, 1, Map.of(r2, 10, r3, 5));
        lsdb.accept(r2, 1, Map.of(r1, 10, r3, 2));
        lsdb.accept(r3, 1, Map.of(r1, 5, r2, 2));

        Set<String> activeNeighbors = new HashSet<>(Arrays.asList(r2, r3));
        DijkstraRouter router = new DijkstraRouter(r1, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes(activeNeighbors);
        check(routes.get(r2).getNextHop().equals(r3), "Inicialmente caminho R1 -> R2 é via R3 (custo 7)");

        // Altera custo de R1-R2 de 10 para 1
        lsdb.accept(r1, 2, Map.of(r2, 1, r3, 5));
        lsdb.accept(r2, 2, Map.of(r1, 1, r3, 2));

        routes = router.computeRoutes(activeNeighbors);
        check(routes.get(r2).getTotalCost() == 1, "Novo custo direto para R2 é 1");
        check(routes.get(r2).getNextHop().equals(r2), "Próximo salto mudou para R2 diretamente");
    }

    private static void testCenarioH() {
        System.out.println("\n──────────────────────────────────────────────────────────────────────");
        System.out.println("Cenário H: Encaminhamento de DATA com TTL insuficiente");
        System.out.println("──────────────────────────────────────────────────────────────────────");

        // DATA com TTL=0 que chega para ser encaminhado a outro nó
        DataMessage msgZeroTtl = MessageParser.parseData("!192.168.1.10;192.168.1.20;999;0;Mensagem teste");
        check(msgZeroTtl != null, "Mensagem DATA parseada com sucesso (TTL=0)");
        check(msgZeroTtl.ttl == 0, "TTL recebido é 0");

        // Validação de limites (Adendo 160)
        DataMessage msgExceededTtl = MessageParser.parseData("!192.168.1.10;192.168.1.20;999;256;Mensagem teste");
        check(msgExceededTtl == null, "Mensagem com TTL 256 é rejeitada como malformada (limite 0-255)");

        LSAMessage lsaExceededCost = MessageParser.parseLSA("#192.168.1.1;1*192.168.1.2;65536");
        check(lsaExceededCost == null, "LSA com custo 65536 é rejeitado (limite 1-65535)");

        LSAMessage lsaExceededSeq = MessageParser.parseLSA("#192.168.1.1;2147483648*192.168.1.2;10");
        check(lsaExceededSeq == null, "LSA com sequência 2147483648 é rejeitado (limite até 2147483647)");
    }
}

