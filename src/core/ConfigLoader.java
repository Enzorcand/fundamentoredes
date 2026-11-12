package core;

import model.Neighbor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Leitura do ficheiro de configuração de vizinhos: roteadores.txt
 */
public class ConfigLoader {

    private static final String CONFIG_FILE = "roteadores.txt";

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
                    if (cost <= 0) {
                        Logger.log(Logger.MENSAGEM_MALFORMADA,
                                String.format("roteadores.txt linha %d: custo deve ser inteiro positivo (foi %d)",
                                        lineNumber, cost));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    Logger.log(Logger.MENSAGEM_MALFORMADA,
                            String.format("roteadores.txt linha %d: custo não numérico '%s'",
                                    lineNumber, costStr));
                    continue;
                }

                if (!ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
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
}
