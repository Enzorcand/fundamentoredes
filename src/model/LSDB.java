package model;

import core.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Link State Database (LSDB).
 * Mantém a versão válida mais recente do anúncio de cada roteador conhecido.
 *
 * Todos os métodos são sincronizados e nenhum devolve vistas "vivas" das
 * estruturas internas: quem lê recebe sempre uma cópia (snapshot), para que
 * outras threads possam alterar a LSDB enquanto o Dijkstra ou a sincronização
 * percorrem os registos.
 */
public class LSDB {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Map<String, LSAEntry> entries = new TreeMap<>();
    /** Maior sequência já aceite por origem — mantida mesmo após a expiração do LSA. */
    private final Map<String, Integer> maxSeqSeen = new HashMap<>();

    public synchronized boolean isNewer(String originIp, int seq) {
        int maxSeen = maxSeqSeen.getOrDefault(originIp, 0);
        return seq > maxSeen;
    }

    public synchronized int getMaxSeqSeen(String originIp) {
        return maxSeqSeen.getOrDefault(originIp, 0);
    }

    /** Devolve cópia do registo (ou null), segura para uso fora do lock. */
    public synchronized LSAEntry get(String originIp) {
        LSAEntry e = entries.get(originIp);
        return e == null ? null : e.copy();
    }

    /** Devolve cópias de todos os registos (snapshot). */
    public synchronized List<LSAEntry> getAll() {
        List<LSAEntry> copy = new ArrayList<>(entries.size());
        for (LSAEntry e : entries.values()) copy.add(e.copy());
        return copy;
    }

    /**
     * Regista uma nova versão do LSA de originIp.
     * @return true se a topologia mudou (registo novo ou enlaces/custos diferentes).
     */
    public synchronized boolean accept(String originIp, int seq, Map<String, Integer> links) {
        maxSeqSeen.put(originIp, seq);
        LSAEntry existing = entries.get(originIp);
        if (existing == null) {
            entries.put(originIp, new LSAEntry(originIp, seq, links));
            Logger.log(Logger.LSDB_ALTERADA, originIp,
                    "Novo registo | SEQ=" + seq + " | enlaces=" + formatLinks(links));
            return true;
        }
        boolean linksChanged = !existing.getLinks().equals(links);
        existing.update(seq, links);
        if (linksChanged) {
            Logger.log(Logger.LSDB_ALTERADA, originIp,
                    "Enlaces alterados | SEQ=" + seq + " | enlaces=" + formatLinks(links));
        }
        return linksChanged;
    }

    public synchronized void remove(String originIp) {
        entries.remove(originIp);
    }

    /** Remove os LSAs expirados, exceto o LSA local (renovado pelo próprio roteador). */
    public synchronized List<String> removeExpired(String myIp) {
        List<String> expired = new ArrayList<>();
        Iterator<Map.Entry<String, LSAEntry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LSAEntry> e = it.next();
            if (e.getKey().equals(myIp)) continue;
            if (e.getValue().isExpired()) {
                expired.add(e.getKey());
                it.remove();
            }
        }
        return expired;
    }

    public synchronized void print() {
        long now = System.currentTimeMillis();
        String header = String.format("%-15s | %-10s | %-9s | %-6s | %s",
                "ORIGEM", "SEQ", "RECEBIDO", "IDADE", "ENLACES (vizinho;custo)");
        String sep = "-".repeat(Math.max(header.length(), 70));

        System.out.println(sep);
        System.out.println("LINK STATE DATABASE (LSDB)");
        System.out.println(sep);
        System.out.println(header);
        System.out.println(sep);
        if (entries.isEmpty()) {
            System.out.println("(vazia)");
        } else {
            for (LSAEntry e : entries.values()) {
                long ageSec = (now - e.getReceivedAt()) / 1000;
                System.out.printf("%-15s | %-10d | %-9s | %-6s | %s%n",
                        e.getOriginIp(),
                        e.getSequenceNumber(),
                        TIME_FMT.format(Instant.ofEpochMilli(e.getReceivedAt())),
                        ageSec + "s",
                        formatLinks(e.getLinks()));
            }
        }
        System.out.println(sep);
    }

    private static String formatLinks(Map<String, Integer> links) {
        if (links.isEmpty()) return "(nenhum)";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> l : links.entrySet()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(l.getKey()).append(";").append(l.getValue());
        }
        return sb.toString();
    }

    @Override
    public synchronized String toString() {
        return "LSDB{entries=" + entries.size() + ", maxSeqSeen=" + maxSeqSeen + "}";
    }
}
