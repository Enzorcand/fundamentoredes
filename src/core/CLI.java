package core;

import model.LSAEntry;
import model.Neighbor;
import network.MessageParser;
import network.UDPListener;
import protocol.DataHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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

            case "addneighbor":
                handleAddNeighbor(parts, scanner);
                break;

            case "removeneighbor":
                handleRemoveNeighbor(parts, scanner);
                break;

            case "testlsa":
                handleTestLsa(parts, scanner);
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

            case "quiet":
                boolean q = Logger.toggleQuiet();
                System.out.println(q
                        ? "[INFO] Modo silencioso LIGADO: HELLOs não aparecem no terminal (continuam no ficheiro de log)."
                        : "[INFO] Modo silencioso DESLIGADO: HELLOs voltam a aparecer no terminal.");
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

    // ─────────────────────────────────────────────────────────────────────────
    // send <ip> [msg]
    // send <ip> <ttl_opt> <msg>   (se 2.º token for inteiro 0-255 → é o TTL)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleSend(String[] parts, Scanner scanner) {
        String destIp;
        String msgText;
        int ttl = DataHandler.INITIAL_TTL;

        if (parts.length >= 3) {
            destIp = parts[1].trim();
            // parts[2] = "resto da linha"; verificar se começa com token numérico 0-255 (TTL opcional)
            String rest = parts[2].trim();
            int firstSpace = rest.indexOf(' ');
            if (firstSpace > 0) {
                String firstToken = rest.substring(0, firstSpace).trim();
                String remainder  = rest.substring(firstSpace + 1).trim();
                try {
                    int parsed = Integer.parseInt(firstToken);
                    if (parsed >= 0 && parsed <= DataHandler.MAX_TTL && !remainder.isEmpty()) {
                        ttl = parsed;
                        msgText = remainder;
                    } else {
                        msgText = rest;
                    }
                } catch (NumberFormatException e) {
                    msgText = rest;
                }
            } else {
                msgText = rest;
            }
        } else if (parts.length == 2) {
            destIp = parts[1].trim();
            System.out.print("Digite o TTL inicial (0-255, Enter para padrão " + DataHandler.INITIAL_TTL + "): ");
            if (!scanner.hasNextLine()) return;
            String ttlInput = scanner.nextLine().trim();
            if (!ttlInput.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(ttlInput);
                    if (parsed >= 0 && parsed <= DataHandler.MAX_TTL) {
                        ttl = parsed;
                    } else {
                        System.out.println("[AVISO] TTL fora do intervalo (0-255). Usando padrão " + DataHandler.INITIAL_TTL + ".");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[AVISO] TTL inválido. Usando padrão " + DataHandler.INITIAL_TTL + ".");
                }
            }
            System.out.print("Digite o texto da mensagem: ");
            if (!scanner.hasNextLine()) return;
            msgText = scanner.nextLine().trim();
        } else {
            System.out.print("Digite o IP de destino: ");
            if (!scanner.hasNextLine()) return;
            destIp = scanner.nextLine().trim();

            System.out.print("Digite o TTL inicial (0-255, Enter para padrão " + DataHandler.INITIAL_TTL + "): ");
            if (!scanner.hasNextLine()) return;
            String ttlInput = scanner.nextLine().trim();
            if (!ttlInput.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(ttlInput);
                    if (parsed >= 0 && parsed <= DataHandler.MAX_TTL) {
                        ttl = parsed;
                    } else {
                        System.out.println("[AVISO] TTL fora do intervalo (0-255). Usando padrão " + DataHandler.INITIAL_TTL + ".");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[AVISO] TTL inválido. Usando padrão " + DataHandler.INITIAL_TTL + ".");
                }
            }

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

        boolean sent = state.getDataHandler().sendMessage(destIp, msgText, ttl);
        if (sent) {
            System.out.printf("[SUCESSO] Mensagem enviada para %s (TTL=%d).%n", destIp, ttl);
        } else {
            System.out.printf("[AVISO] Mensagem não pôde ser entregue a %s (sem rota ou descartada).%n", destIp);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disable <ip>
    // ─────────────────────────────────────────────────────────────────────────
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

        System.out.printf("[ALTERAÇÃO] Enlace lógico com %s DESATIVADO — HELLOs e LSAs dele serão descartados.%n", ip);
        Logger.log(Logger.ENLACE_DESATIVADO, ip);

        if (wasActive) {
            state.notifyNeighborStateChange();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // enable <ip>
    // ─────────────────────────────────────────────────────────────────────────
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
        System.out.printf("[ALTERAÇÃO] Enlace lógico com %s ATIVADO — envio de HELLO retomado. Aguardando confirmação bidirecional.%n", ip);
        Logger.log(Logger.ENLACE_ATIVADO, ip + " | Aguardando confirmação HELLO bidirecional");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cost <ip> <novo_custo>
    // ─────────────────────────────────────────────────────────────────────────
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
            if (!MessageParser.isValidCost(newCost)) {
                System.out.println("[ERRO] Custo deve ser um inteiro entre "
                        + MessageParser.MIN_COST + " e " + MessageParser.MAX_COST + ".");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERRO] Valor de custo inválido: '" + costStr + "'");
            return;
        }

        int oldCost = n.getCost();
        n.setCost(newCost);
        ConfigLoader.saveNeighbors(state.getNeighbors());

        System.out.printf("[ALTERAÇÃO] Custo do enlace com %s alterado de %d para %d.%n", ip, oldCost, newCost);
        Logger.log(Logger.CUSTO_ALTERADO, String.format("%s | %d -> %d", ip, oldCost, newCost));

        if (n.isActive()) {
            state.notifyNeighborStateChange();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addneighbor <ip> <custo>
    // ─────────────────────────────────────────────────────────────────────────
    private void handleAddNeighbor(String[] parts, Scanner scanner) {
        String ip;
        String costStr;

        if (parts.length >= 3) {
            ip = parts[1].trim();
            costStr = parts[2].trim();
        } else if (parts.length == 2) {
            ip = parts[1].trim();
            System.out.print("Digite o custo do enlace com " + ip + ": ");
            if (!scanner.hasNextLine()) return;
            costStr = scanner.nextLine().trim();
        } else {
            System.out.print("Digite o IP do novo vizinho: ");
            if (!scanner.hasNextLine()) return;
            ip = scanner.nextLine().trim();

            System.out.print("Digite o custo do enlace: ");
            if (!scanner.hasNextLine()) return;
            costStr = scanner.nextLine().trim();
        }

        if (!MessageParser.isValidIp(ip)) {
            System.out.println("[ERRO] Endereço IPv4 inválido: '" + ip + "'");
            return;
        }

        if (state.getNeighbor(ip) != null) {
            System.out.println("[INFO] Vizinho " + ip + " já está configurado. Use 'cost' para alterar o custo.");
            return;
        }

        if (ip.equals(state.getMyIp())) {
            System.out.println("[ERRO] Não é possível adicionar o próprio IP como vizinho.");
            return;
        }

        int cost;
        try {
            cost = Integer.parseInt(costStr);
            if (!MessageParser.isValidCost(cost)) {
                System.out.println("[ERRO] Custo deve ser um inteiro entre "
                        + MessageParser.MIN_COST + " e " + MessageParser.MAX_COST + ".");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERRO] Valor de custo inválido: '" + costStr + "'");
            return;
        }

        Neighbor n = new Neighbor(ip, cost);
        state.addNeighbor(n);
        ConfigLoader.saveNeighbors(state.getNeighbors());

        System.out.printf("[SUCESSO] Vizinho %s (custo=%d) adicionado. Aguardando troca de HELLO para ativar enlace.%n", ip, cost);
        Logger.log(Logger.VIZINHO_ADICIONADO, ip + " | custo=" + cost);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // removeneighbor <ip>
    // ─────────────────────────────────────────────────────────────────────────
    private void handleRemoveNeighbor(String[] parts, Scanner scanner) {
        String ip;
        if (parts.length >= 2) {
            ip = parts[1].trim();
        } else {
            System.out.print("Digite o IP do vizinho a remover: ");
            if (!scanner.hasNextLine()) return;
            ip = scanner.nextLine().trim();
        }

        Neighbor n = state.getNeighbor(ip);
        if (n == null) {
            System.out.println("[ERRO] Vizinho " + ip + " não está configurado.");
            return;
        }

        boolean wasActive = n.isActive();
        state.removeNeighbor(ip);
        ConfigLoader.saveNeighbors(state.getNeighbors());

        System.out.printf("[SUCESSO] Vizinho %s removido da configuração e de roteadores.txt.%n", ip);
        Logger.log(Logger.VIZINHO_REMOVIDO, ip);

        if (wasActive) {
            state.notifyNeighborStateChange();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // testlsa <origem> <vizinho> [dup|old]
    //   dup  → reenvia com sequência original (duplicado)
    //   old  → reenvia com sequência original - 1 (antigo)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleTestLsa(String[] parts, Scanner scanner) {
        String originIp;
        String neighborIp;
        String type;

        if (parts.length >= 3) {
            originIp = parts[1].trim();
            String[] subParts = parts[2].split("\\s+", 2);
            neighborIp = subParts[0].trim();
            type = (subParts.length >= 2) ? subParts[1].trim().toLowerCase() : "dup";
        } else if (parts.length == 2) {
            originIp = parts[1].trim();
            System.out.print("IP do vizinho destinatário: ");
            if (!scanner.hasNextLine()) return;
            neighborIp = scanner.nextLine().trim();
            System.out.print("Tipo [dup=duplicado / old=antigo] (Enter para dup): ");
            if (!scanner.hasNextLine()) return;
            type = scanner.nextLine().trim().toLowerCase();
            if (type.isEmpty()) type = "dup";
        } else {
            System.out.print("IP de origem do LSA (quem gerou): ");
            if (!scanner.hasNextLine()) return;
            originIp = scanner.nextLine().trim();
            System.out.print("IP do vizinho destinatário: ");
            if (!scanner.hasNextLine()) return;
            neighborIp = scanner.nextLine().trim();
            System.out.print("Tipo [dup=duplicado / old=antigo] (Enter para dup): ");
            if (!scanner.hasNextLine()) return;
            type = scanner.nextLine().trim().toLowerCase();
            if (type.isEmpty()) type = "dup";
        }

        if (!type.equals("dup") && !type.equals("old")) {
            System.out.println("[ERRO] Tipo deve ser 'dup' ou 'old'.");
            return;
        }

        LSAEntry entry = state.getLsdb().get(originIp);
        if (entry == null) {
            System.out.println("[ERRO] Nenhum LSA de " + originIp + " encontrado na LSDB.");
            return;
        }

        Neighbor dest = state.getNeighbor(neighborIp);
        if (dest == null) {
            System.out.println("[ERRO] " + neighborIp + " não é um vizinho configurado.");
            return;
        }

        int seq = entry.getSequenceNumber();
        if ("old".equals(type)) {
            seq = Math.max(1, seq - 1);  // sequência menor = antigo
        }
        // "dup" usa a sequência original

        // Montar string do LSA
        StringBuilder sb = new StringBuilder("#");
        sb.append(entry.getOriginIp()).append(";").append(seq);
        for (Map.Entry<String, Integer> link : entry.getLinks().entrySet()) {
            sb.append("*").append(link.getKey()).append(";").append(link.getValue());
        }

        String lsaMsg = sb.toString();
        byte[] data = lsaMsg.getBytes(StandardCharsets.UTF_8);

        if (data.length > UDPListener.MAX_SIZE) {
            System.out.println("[ERRO] LSA excede 1200 bytes — não enviado.");
            return;
        }

        try {
            InetAddress addr = InetAddress.getByName(neighborIp);
            DatagramPacket pkt = new DatagramPacket(data, data.length, addr, UDPListener.PORT);
            state.getSocket().send(pkt);

            System.out.printf("[TESTE_LSA] Enviado LSA de %s (SEQ=%d, tipo=%s) para %s: %s%n",
                    originIp, seq, type, neighborIp, lsaMsg);
            Logger.log(Logger.TESTE_LSA,
                    String.format("origem=%s SEQ=%d tipo=%s destino=%s",
                            originIp, seq, type, neighborIp));
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao enviar LSA de teste: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitários
    // ─────────────────────────────────────────────────────────────────────────
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
        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.println("                       COMANDOS DO ROTEADOR                          ");
        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.println("  MENSAGENS:");
        System.out.println("    send <IP> <msg>              Envia DATA com TTL padrão (16)");
        System.out.println("    send <IP> <TTL> <msg>        Envia DATA com TTL customizado (0-255)");
        System.out.println();
        System.out.println("  VIZINHOS:");
        System.out.println("    disable <IP>                 Desativa enlace (descarta HELLOs/LSAs dele)");
        System.out.println("    enable <IP>                  Ativa enlace (aguarda novo HELLO bidirecional)");
        System.out.println("    cost <IP> <custo>            Altera custo do enlace (1-65535) e atualiza LSA/rotas");
        System.out.println("    addneighbor <IP> <custo>     Adiciona vizinho configurado (atualiza roteadores.txt)");
        System.out.println("    removeneighbor <IP>          Remove vizinho configurado (atualiza roteadores.txt)");
        System.out.println();
        System.out.println("  CONSULTAS:");
        System.out.println("    routes                       Exibe a tabela de roteamento");
        System.out.println("    lsdb                         Exibe a LSDB");
        System.out.println("    neighbors                    Exibe status dos vizinhos");
        System.out.println();
        System.out.println("  TESTES:");
        System.out.println("    testlsa <origem> <viz> dup   Reenvia LSA armazenado como duplicado (seq original)");
        System.out.println("    testlsa <origem> <viz> old   Reenvia LSA armazenado como antigo (seq - 1)");
        System.out.println();
        System.out.println("  quiet                          Liga/desliga HELLOs no terminal (o ficheiro de log guarda tudo)");
        System.out.println("  help                           Exibe este menu");
        System.out.println("  quit / exit                    Encerra o roteador");
        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    public void stop() {
        running = false;
    }
}
