package model;

import java.util.*;

/**
 * Link State Database (LSDB).
 * Mantém a versão válida mais recente do anúncio de cada roteador conhecido.
 */
public class LSDB {

    private final Map<String, LSAEntry> entries = new HashMap<>();
    private final Map<String, Integer> maxSeqSeen = new HashMap<>();

    public synchronized boolean isNewer(String originIp, int seq) {
        int maxSeen = maxSeqSeen.getOrDefault(originIp, 0);
        return seq > maxSeen;
    }

    public synchronized LSAEntry get(String originIp) {
        return entries.get(originIp);
    }

    public synchronized Collection<LSAEntry> getAll() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public synchronized boolean accept(String originIp, int seq, Map<String, Integer> links) {
        maxSeqSeen.put(originIp, seq);
        LSAEntry existing = entries.get(originIp);
        if (existing == null) {
            entries.put(originIp, new LSAEntry(originIp, seq, links));
            return true;
        } else {
            boolean linksChanged = !existing.getLinks().equals(links);
            existing.update(seq, links);
            return linksChanged;
        }
    }

    public synchronized void remove(String originIp) {
        entries.remove(originIp);
    }

    public synchronized List<String> removeExpired() {
        List<String> expired = new ArrayList<>();
        Iterator<Map.Entry<String, LSAEntry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LSAEntry> e = it.next();
            if (e.getValue().isExpired()) {
                expired.add(e.getKey());
                it.remove();
            }
        }
        return expired;
    }

    public synchronized void print() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║               LINK STATE DATABASE (LSDB)            ║");
        System.out.println("╠══════════╦════════╦═══════════════════════════════╗");
        System.out.printf( "║ %-8s ║ %-6s ║ %-29s ║%n", "ORIGEM", "SEQ", "ENLACES (vizinho;custo)");
        System.out.println("╠══════════╬════════╬═══════════════════════════════╣");
        if (entries.isEmpty()) {
            System.out.println("║                   (vazia)                           ║");
        } else {
            for (LSAEntry e : entries.values()) {
                StringBuilder links = new StringBuilder();
                for (Map.Entry<String, Integer> l : e.getLinks().entrySet()) {
                    if (links.length() > 0) links.append(" ");
                    links.append(l.getKey()).append(";").append(l.getValue());
                }
                System.out.printf("║ %-8s ║ %-6d ║ %-29s ║%n",
                        e.getOriginIp(), e.getSequenceNumber(), links);
            }
        }
        System.out.println("╚══════════╩════════╩═══════════════════════════════╝");
    }

    @Override
    public synchronized String toString() {
        return "LSDB{entries=" + entries.size() + ", maxSeqSeen=" + maxSeqSeen + "}";
    }
}
