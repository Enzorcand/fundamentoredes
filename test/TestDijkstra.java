import model.LSDB;
import model.RoutingEntry;
import model.RoutingTable;
import routing.DijkstraRouter;

import java.util.*;

/**
 * Testes unitários do motor Dijkstra e Tabela de Roteamento.
 */
public class TestDijkstra {

    public static void main(String[] args) {
        System.out.println("=== Teste do DijkstraRouter e RoutingTable ===\n");

        String myIp = "192.168.1.1";
        LSDB lsdb = new LSDB();

        Map<String, Integer> links1 = new HashMap<>();
        links1.put("192.168.1.2", 10);
        links1.put("192.168.1.4", 30);
        links1.put("192.168.1.5", 8);
        lsdb.accept("192.168.1.1", 1, links1);

        Map<String, Integer> links2 = new HashMap<>();
        links2.put("192.168.1.1", 10);
        links2.put("192.168.1.3", 5);
        lsdb.accept("192.168.1.2", 1, links2);

        Map<String, Integer> links3 = new HashMap<>();
        links3.put("192.168.1.2", 5);
        links3.put("192.168.1.4", 2);
        lsdb.accept("192.168.1.3", 1, links3);

        Map<String, Integer> links4 = new HashMap<>();
        links4.put("192.168.1.1", 30);
        links4.put("192.168.1.3", 2);
        lsdb.accept("192.168.1.4", 1, links4);

        Map<String, Integer> links5 = new HashMap<>();
        links5.put("192.168.1.99", 50);
        lsdb.accept("192.168.1.5", 1, links5);

        DijkstraRouter router = new DijkstraRouter(myIp, lsdb);
        Map<String, RoutingEntry> routes = router.computeRoutes();

        RoutingTable table = new RoutingTable();
        table.update(routes);

        System.out.println("\n--- Asserções dos Testes ---");

        assertFalse(routes.containsKey("192.168.1.1"), "Rota para si próprio não deve existir");
        assertFalse(routes.containsKey("192.168.1.5"), "Enlace unidirecional com 192.168.1.5 deve ser ignorado");

        RoutingEntry r2 = routes.get("192.168.1.2");
        assertNotNull(r2, "Rota para 192.168.1.2 deve existir");
        assertEquals(10, r2.getTotalCost(), "Custo para 192.168.1.2 deve ser 10");
        assertEquals("192.168.1.2", r2.getNextHop(), "NextHop para 192.168.1.2 deve ser 192.168.1.2");

        RoutingEntry r3 = routes.get("192.168.1.3");
        assertNotNull(r3, "Rota para 192.168.1.3 deve existir");
        assertEquals(15, r3.getTotalCost(), "Custo para 192.168.1.3 deve ser 15");
        assertEquals("192.168.1.2", r3.getNextHop(), "NextHop para 192.168.1.3 deve ser 192.168.1.2");

        RoutingEntry r4 = routes.get("192.168.1.4");
        assertNotNull(r4, "Rota para 192.168.1.4 deve existir");
        assertEquals(17, r4.getTotalCost(), "Custo para 192.168.1.4 deve ser 17");
        assertEquals("192.168.1.2", r4.getNextHop(), "NextHop para 192.168.1.4 deve ser 192.168.1.2");

        System.out.println("\n--- Teste de Renovação Periódica sem Mudança de Topologia ---");
        boolean topoChanged = lsdb.accept("192.168.1.3", 2, links3);
        assertFalse(topoChanged, "Renovação periódica com os mesmos links NÃO deve acusar mudança na topologia");
        System.out.println("Renovação periódica detectada como SEM mudança: OK");

        System.out.println("\n--- Teste de Alteração de Custo / Modificação de Rota ---");
        Map<String, Integer> newLinks1 = new HashMap<>(links1);
        newLinks1.put("192.168.1.4", 12);
        Map<String, Integer> newLinks4 = new HashMap<>(links4);
        newLinks4.put("192.168.1.1", 12);
        lsdb.accept("192.168.1.1", 2, newLinks1);
        lsdb.accept("192.168.1.4", 2, newLinks4);

        Map<String, RoutingEntry> routesAfterCostChange = router.computeRoutes();
        table.update(routesAfterCostChange);

        RoutingEntry r4New = routesAfterCostChange.get("192.168.1.4");
        assertEquals(12, r4New.getTotalCost(), "Novo custo para 192.168.1.4 deve ser 12");
        assertEquals("192.168.1.4", r4New.getNextHop(), "Novo NextHop para 192.168.1.4 deve ser 192.168.1.4");

        System.out.println("\n--- Teste de Queda de Nó / Remoção de Rota ---");
        lsdb.remove("192.168.1.3");
        Map<String, RoutingEntry> routesAfterNodeDown = router.computeRoutes();
        table.update(routesAfterNodeDown);
        assertFalse(routesAfterNodeDown.containsKey("192.168.1.3"), "Rota para 192.168.1.3 deve ter sido removida");

        System.out.println("\n>>> TODOS OS TESTES DO DIJKSTRA PASSARAM COM SUCESSO! <<<");
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("FALHA: " + msg);
        System.out.println("  [OK] " + msg);
    }

    private static void assertFalse(boolean cond, String msg) {
        assertTrue(!cond, msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        assertTrue(obj != null, msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        assertTrue(Objects.equals(expected, actual),
                msg + " (esperado: " + expected + ", obtido: " + actual + ")");
    }
}
