package network;

import core.Logger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser de mensagens do protocolo Link State:
 *   HELLO: @IP_ORIGEM*VIZ1*VIZ2*...
 *   LSA:   #IP_ORIGEM;SEQUENCIA*VIZ1;CUSTO*VIZ2;CUSTO
 *   DATA:  !IP_ORIGEM;IP_DESTINO;ID;TTL;MENSAGEM
 */
public class MessageParser {

    public static class HelloMessage {
        public final String originIp;
        public final java.util.List<String> neighbors;

        public HelloMessage(String originIp, java.util.List<String> neighbors) {
            this.originIp = originIp;
            this.neighbors = java.util.Collections.unmodifiableList(neighbors);
        }

        @Override
        public String toString() {
            return String.format("HELLO{origin=%s, neighbors=%s}", originIp, neighbors);
        }
    }

    public static class LSAMessage {
        public final String originIp;
        public final int sequenceNumber;
        public final Map<String, Integer> links;

        public LSAMessage(String originIp, int sequenceNumber, Map<String, Integer> links) {
            this.originIp = originIp;
            this.sequenceNumber = sequenceNumber;
            this.links = java.util.Collections.unmodifiableMap(links);
        }

        @Override
        public String toString() {
            return String.format("LSA{origin=%s, seq=%d, links=%s}",
                    originIp, sequenceNumber, links);
        }
    }

    public static class DataMessage {
        public final String originIp;
        public final String destIp;
        public final int id;
        public final int ttl;
        public final String content;

        public DataMessage(String originIp, String destIp, int id, int ttl, String content) {
            this.originIp = originIp;
            this.destIp   = destIp;
            this.id       = id;
            this.ttl      = ttl;
            this.content  = content;
        }

        @Override
        public String toString() {
            return String.format("DATA{origin=%s, dest=%s, id=%d, ttl=%d, msg='%s'}",
                    originIp, destIp, id, ttl, content);
        }
    }

    public static HelloMessage parseHello(String raw) {
        String body = raw.substring(1);
        String[] parts = body.split("\\*");

        if (parts.length < 1 || parts[0].isEmpty()) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "HELLO sem IP de origem: '" + raw + "'");
            return null;
        }

        String originIp = parts[0].trim();
        if (!isValidIp(originIp)) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "HELLO com IP de origem inválido: '" + originIp + "'");
            return null;
        }

        java.util.List<String> neighbors = new java.util.ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String ip = parts[i].trim();
            if (!isValidIp(ip)) {
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        "HELLO com IP de vizinho inválido: '" + ip + "' — mensagem descartada");
                return null;
            }
            neighbors.add(ip);
        }

        return new HelloMessage(originIp, neighbors);
    }

    public static LSAMessage parseLSA(String raw) {
        String body = raw.substring(1);
        String[] parts = body.split("\\*");

        if (parts.length < 1) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA sem cabeçalho: '" + raw + "'");
            return null;
        }

        String[] header = parts[0].split(";");
        if (header.length != 2) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA cabeçalho inválido: '" + parts[0] + "'");
            return null;
        }

        String originIp = header[0].trim();
        if (!isValidIp(originIp)) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA com IP de origem inválido: '" + originIp + "'");
            return null;
        }

        int seq;
        try {
            seq = Integer.parseInt(header[1].trim());
            if (seq < 1) {
                Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA com sequência inválida: " + seq);
                return null;
            }
        } catch (NumberFormatException e) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA sequência não numérica: '" + header[1] + "'");
            return null;
        }

        Map<String, Integer> links = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String[] link = parts[i].split(";");
            if (link.length != 2) {
                Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA enlace inválido: '" + parts[i] + "'");
                return null;
            }
            String neighborIp = link[0].trim();
            if (!isValidIp(neighborIp)) {
                Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA IP de vizinho inválido: '" + neighborIp + "'");
                return null;
            }
            int cost;
            try {
                cost = Integer.parseInt(link[1].trim());
                if (cost <= 0) {
                    Logger.log(Logger.MENSAGEM_MALFORMADA,
                            "LSA custo inválido (deve ser positivo): " + cost);
                    return null;
                }
            } catch (NumberFormatException e) {
                Logger.log(Logger.MENSAGEM_MALFORMADA, "LSA custo não numérico: '" + link[1] + "'");
                return null;
            }
            links.put(neighborIp, cost);
        }

        return new LSAMessage(originIp, seq, links);
    }

    public static DataMessage parseData(String raw) {
        String body = raw.substring(1);
        String[] parts = body.split(";", 5);

        if (parts.length < 5) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "DATA campos insuficientes: '" + raw + "'");
            return null;
        }

        String originIp = parts[0].trim();
        String destIp   = parts[1].trim();

        if (!isValidIp(originIp)) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "DATA IP de origem inválido: '" + originIp + "'");
            return null;
        }
        if (!isValidIp(destIp)) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "DATA IP de destino inválido: '" + destIp + "'");
            return null;
        }

        int id;
        try {
            id = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "DATA ID não numérico: '" + parts[2] + "'");
            return null;
        }

        int ttl;
        try {
            ttl = Integer.parseInt(parts[3].trim());
            if (ttl < 0) {
                Logger.log(Logger.MENSAGEM_MALFORMADA, "DATA TTL negativo: " + ttl);
                return null;
            }
        } catch (NumberFormatException e) {
            Logger.log(Logger.MENSAGEM_MALFORMADA, "DATA TTL não numérico: '" + parts[3] + "'");
            return null;
        }

        String content = parts[4];
        return new DataMessage(originIp, destIp, id, ttl, content);
    }

    public static boolean isValidIp(String ip) {
        if (ip == null) return false;
        return ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
}
