package network;

/**
 * Contêiner imutável para uma mensagem UDP recebida.
 */
public class RawMessage {

    private final String raw;
    private final String senderIp;
    private final MessageType type;

    public RawMessage(String raw, String senderIp) {
        this.raw = raw;
        this.senderIp = senderIp;
        this.type = MessageType.fromRaw(raw);
    }

    public String getRaw() { return raw; }
    public String getSenderIp() { return senderIp; }
    public MessageType getType() { return type; }

    @Override
    public String toString() {
        return String.format("RawMessage{type=%s, sender=%s, raw='%s'}",
                type, senderIp, raw);
    }
}
