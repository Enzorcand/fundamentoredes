package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registo de um LSA (Link State Advertisement) armazenado na LSDB:
 * IP de origem, número de sequência, enlaces anunciados e instante de
 * recebimento da versão válida.
 */
public class LSAEntry {

    public static final long VALIDITY_MS = 30_000L;

    private final String originIp;
    private int sequenceNumber;
    private Map<String, Integer> links;
    private long receivedAt;

    public LSAEntry(String originIp, int sequenceNumber, Map<String, Integer> links) {
        this(originIp, sequenceNumber, links, System.currentTimeMillis());
    }

    private LSAEntry(String originIp, int sequenceNumber, Map<String, Integer> links, long receivedAt) {
        this.originIp = originIp;
        this.sequenceNumber = sequenceNumber;
        this.links = new LinkedHashMap<>(links);
        this.receivedAt = receivedAt;
    }

    public String getOriginIp() { return originIp; }
    public int getSequenceNumber() { return sequenceNumber; }
    public Map<String, Integer> getLinks() { return Collections.unmodifiableMap(links); }
    public long getReceivedAt() { return receivedAt; }

    /** Só é chamado pela LSDB, sob o seu lock. */
    void update(int sequenceNumber, Map<String, Integer> links) {
        this.sequenceNumber = sequenceNumber;
        this.links = new LinkedHashMap<>(links);
        this.receivedAt = System.currentTimeMillis();
    }

    /** Cópia independente, usada pela LSDB para entregar snapshots a outras threads. */
    LSAEntry copy() {
        return new LSAEntry(originIp, sequenceNumber, links, receivedAt);
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - receivedAt) > VALIDITY_MS;
    }

    @Override
    public String toString() {
        return String.format("LSAEntry{origin='%s', seq=%d, links=%s, age=%dms}",
                originIp, sequenceNumber, links,
                System.currentTimeMillis() - receivedAt);
    }
}
