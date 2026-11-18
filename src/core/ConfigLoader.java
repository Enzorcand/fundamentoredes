package core;

import model.Neighbor;
import network.MessageParser;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigLoader {

    private static final String CONFIG_FILE = System.getProperty("roteadores.file", "roteadores.txt");

    public static Map<String, Neighbor> loadNeighbors() {
        Map<String, Neighbor> neighbors = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length != 2) {
                    Logger.log(Logger.MENSAGEM_MALFORMADA,
                            String.format("roteadores.txt linha %d: formato inválido '%s' (esperado IP;CUSTO)",
                                    lineNumber, line));
                    continue;
                }

                String ip   = parts[0].trim();
                String costStr = parts[1].trim();

                int cost;
                try {
                    cost = Integer.parseInt(costStr);
                    if (!MessageParser.isValidCost(cost)) {
                        Logger.log(Logger.MENSAGEM_MALFORMADA,
                                String.format("roteadores.txt linha %d: custo deve estar entre %d e %d (foi %d)",
                                        lineNumber, MessageParser.MIN_COST, MessageParser.MAX_COST, cost));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    Logger.log(Logger.MENSAGEM_MALFORMADA,
                            String.format("roteadores.txt linha %d: custo não numérico '%s'",
                                    lineNumber, costStr));
                    continue;
                }

                if (!MessageParser.isValidIp(ip)) {
                    Logger.log(Logger.MENSAGEM_MALFORMADA,
                            String.format("roteadores.txt linha %d: IP inválido '%s'",
                                    lineNumber, ip));
                    continue;
                }

                neighbors.put(ip, new Neighbor(ip, cost));
                Logger.log("CONFIG_CARREGADO", String.format("Vizinho %s custo=%d", ip, cost));
            }

        } catch (IOException e) {
            Logger.log(Logger.ERRO, "Não foi possível ler roteadores.txt: " + e.getMessage());
        }

        return neighbors;
    }

    public static void saveNeighbors(Map<String, Neighbor> neighbors) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CONFIG_FILE))) {
            for (Neighbor n : neighbors.values()) {
                pw.println(n.getIp() + ";" + n.getCost());
            }
            Logger.log("CONFIG_SALVO", CONFIG_FILE + " atualizado com "
                    + neighbors.size() + " vizinho(s)");
        } catch (IOException e) {
            Logger.log(Logger.ERRO, "Falha ao salvar roteadores.txt: " + e.getMessage());
        }
    }
}
