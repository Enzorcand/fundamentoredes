package core;

import model.Neighbor;
import network.MessageParser;
import java.util.Scanner;

/**
 * Interface interativa de linha de comandos (CLI) na thread principal.
 */
public class CLI implements Runnable {

    private final RouterState state;
    private volatile boolean running = true;

    public CLI(RouterState state) {
        this.state = state;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            processCommand(line, scanner);
        }
    }

    private void processCommand(String line, Scanner scanner) {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "send":
                handleSend(parts, scanner);
                break;

            case "disable":
                handleDisable(parts, scanner);
                break;

            case "enable":
                handleEnable(parts, scanner);
                break;

            case "cost":
                handleCost(parts, scanner);
                break;

            case "routes":
                state.getRoutingTable().print();
                break;

            case "lsdb":
                state.getLsdb().print();
                break;

            case "neighbors":
                printNeighbors();
                break;

            case "help":
                printHelp();
                break;

            case "exit":
            case "quit":
                System.out.println("A encerrar roteador...");
                System.exit(0);
                break;

            default:
                System.out.println("Comando desconhecido: '" + cmd + "'. Digite 'help' para ajuda.");
        }
    }

    private void handleSend(String[] parts, Scanner scanner) {
        String destIp;
        String msgText;

        if (parts.length >= 3) {
            destIp = parts[1].trim();
            msgText = parts[2].trim();
        } else if (parts.length == 2) {
            destIp = parts[1].trim();
            System.out.print("Digite o texto da mensagem: ");
            if (!scanner.hasNextLine()) return;
            msgText = scanner.nextLine().trim();
        } else {
            System.out.print("Digite o IP de destino: ");
            if (!scanner.hasNextLine()) return;
            destIp = scanner.nextLine().trim();

            System.out.print("Digite o texto da mensagem: ");
            if (!scanner.hasNextLine()) return;
            msgText = scanner.nextLine().trim();
        }

        if (!MessageParser.isValidIp(destIp)) {
            System.out.println("[ERRO] Endereço IPv4 de destino inválido: '" + destIp + "'");
            return;
        }

        if (msgText.isEmpty()) {
            System.out.println("[ERRO] Mensagem vazia não permitida.");
            return;
        }

        boolean sent = state.getDataHandler().sendMessage(destIp, msgText);
        if (sent) {
            System.out.printf("[SUCESSO] Mensagem enviada para %s.%n", destIp);
        } else {
            System.out.printf("[AVISO] Mensagem não pôde ser entregue a %s (sem rota ou descartada).%n", destIp);
        }
    }

    private void handleDisable(String[] parts, Scanner scanner) {
        String ip;
        if (parts.length >= 2) {
            ip = parts[1].trim();
        } else {
            System.out.print("Digite o IP do vizinho cujo enlace será desativado: ");
            if (!scanner.hasNextLine()) return;
            ip = scanner.nextLine().trim();
        }

        Neighbor n = state.getNeighbor(ip);
        if (n == null) {
            System.out.println("[ERRO] Vizinho " + ip + " não está na lista de vizinhos configurados.");
            return;
        }

        if (!n.isEnabled()) {
            System.out.println("[INFO] O enlace com " + ip + " já está desativado.");
            return;
        }

        n.setEnabled(false);
        boolean wasActive = n.isActive();
        n.setActive(false);

        System.out.printf("[ALTERAÇÃO] Enlace lógico com %s DESATIVADO (simulando falha).%n", ip);
        Logger.log(Logger.ENLACE_DESATIVADO, ip);

        if (wasActive) {
            state.notifyNeighborStateChange();
        }
    }

    private void handleEnable(String[] parts, Scanner scanner) {
        String ip;
        if (parts.length >= 2) {
            ip = parts[1].trim();
        } else {
            System.out.print("Digite o IP do vizinho cujo enlace será ativado: ");
            if (!scanner.hasNextLine()) return;
            ip = scanner.nextLine().trim();
        }

        Neighbor n = state.getNeighbor(ip);
        if (n == null) {
            System.out.println("[ERRO] Vizinho " + ip + " não está na lista de vizinhos configurados.");
            return;
        }

        if (n.isEnabled()) {
            System.out.println("[INFO] O enlace com " + ip + " já está ativado.");
            return;
        }

        n.setEnabled(true);
        System.out.printf("[ALTERAÇÃO] Enlace lógico com %s ATIVADO. Aguardando troca de HELLOs para restabelecimento.%n", ip);
        Logger.log(Logger.ENLACE_ATIVADO, ip + " | Aguardando confirmação HELLO bidirecional");
    }

    private void handleCost(String[] parts, Scanner scanner) {
        String ip;
        String costStr;

        if (parts.length >= 3) {
            ip = parts[1].trim();
            costStr = parts[2].trim();
        } else if (parts.length == 2) {
            ip = parts[1].trim();
            System.out.print("Digite o novo custo para o vizinho " + ip + ": ");
            if (!scanner.hasNextLine()) return;
            costStr = scanner.nextLine().trim();
        } else {
            System.out.print("Digite o IP do vizinho: ");
            if (!scanner.hasNextLine()) return;
            ip = scanner.nextLine().trim();

            System.out.print("Digite o novo custo: ");
            if (!scanner.hasNextLine()) return;
            costStr = scanner.nextLine().trim();
        }

        Neighbor n = state.getNeighbor(ip);
        if (n == null) {
            System.out.println("[ERRO] Vizinho " + ip + " não está na lista de vizinhos configurados.");
            return;
        }

        int newCost;
        try {
            newCost = Integer.parseInt(costStr);
            if (newCost <= 0) {
                System.out.println("[ERRO] Custo deve ser um inteiro positivo (> 0).");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERRO] Valor de custo inválido: '" + costStr + "'");
            return;
        }

        int oldCost = n.getCost();
        n.setCost(newCost);

        System.out.printf("[ALTERAÇÃO] Custo do enlace com %s alterado de %d para %d.%n", ip, oldCost, newCost);
        Logger.log(Logger.CUSTO_ALTERADO, String.format("%s | %d -> %d", ip, oldCost, newCost));

        if (n.isActive()) {
            state.notifyNeighborStateChange();
        }
    }

    private void printNeighbors() {
        System.out.println("┌─────────────────┬───────┬─────────┬───────────┐");
        System.out.printf( "│ %-15s │ %-5s │ %-7s │ %-9s │%n", "Vizinho", "Custo", "Ativo", "Habilitado");
        System.out.println("├─────────────────┼───────┼─────────┼───────────┤");
        for (Neighbor n : state.getNeighbors().values()) {
            System.out.printf("│ %-15s │ %-5d │ %-7b │ %-9b │%n",
                    n.getIp(), n.getCost(), n.isActive(), n.isEnabled());
        }
        System.out.println("└─────────────────┴───────┴─────────┴───────────┘");
    }

    private void printHelp() {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("                     COMANDOS DO ROTEADOR                         ");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("  send [IP] [msg]        - Envia mensagem DATA para o IP de destino");
        System.out.println("  disable [IP]           - Desativa enlace lógico (ignora HELLOs)");
        System.out.println("  enable [IP]            - Ativa enlace lógico (aguarda novo HELLO)");
        System.out.println("  cost [IP] [novo_custo] - Altera custo do enlace e atualiza LSA/rotas");
        System.out.println("  routes                 - Exibe a tabela de roteamento");
        System.out.println("  lsdb                   - Exibe a LSDB");
        System.out.println("  neighbors              - Exibe status dos vizinhos");
        System.out.println("  help                   - Exibe este menu de ajuda");
        System.out.println("  quit / exit            - Encerra o roteador");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    public void stop() {
        running = false;
    }
}
