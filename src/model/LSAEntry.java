package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registo de um LSA (Link State Advertisement) armazenado na LSDB.
 */
public class LSAEntry {

    private final String originIp;
    private int sequenceNumber;
    private Map<String, Integer> links;
    private long receivedAt;

    public LSAEntry(String originIp, int sequenceNumber, Map<String, Integer> links) {
        this.originIp = originIp;
        this.sequenceNumber = sequenceNumber;
        this.links = new LinkedHashMap<>(links);
        this.receivedAt = System.currentTimeMillis();
    }

    public String getOriginIp() { return originIp; }
    public int getSequenceNumber() { return sequenceNumber; }
    public Map<String, Integer> getLinks() { return Collections.unmodifiableMap(links); }
    public long getReceivedAt() { return receivedAt; }

    public void update(int sequenceNumber, Map<String, Integer> links) {
        this.sequenceNumber = sequenceNumber;
        this.links = new LinkedHashMap<>(links);
        this.receivedAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - receivedAt) > 30_000L;
    }

    @Override
    public String toString() {
        return String.format("LSAEntry{origin='%s', seq=%d, links=%s, age=%dms}",
                originIp, sequenceNumber, links,
                System.currentTimeMillis() - receivedAt);
    }
}
