package network;

/**
 * Tipos de mensagem do protocolo:
 *   '@' → HELLO
 *   '#' → LSA
 *   '!' → DATA
 */
public enum MessageType {
    HELLO('@'),
    LSA('#'),
    DATA('!'),
    UNKNOWN('?');

    private final char prefix;

    MessageType(char prefix) {
        this.prefix = prefix;
    }

    public char getPrefix() {
        return prefix;
    }

    public static MessageType fromRaw(String raw) {
        if (raw == null || raw.isEmpty()) return UNKNOWN;
        switch (raw.charAt(0)) {
            case '@': return HELLO;
            case '#': return LSA;
            case '!': return DATA;
            default:  return UNKNOWN;
        }
    }
}
