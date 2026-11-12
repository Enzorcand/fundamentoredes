import network.MessageParser;
import network.MessageType;

/**
 * Teste isolado do MessageParser.
 */
public class TestParser {
    public static void main(String[] args) {

        System.out.println("=== Teste do MessageParser ===\n");

        // ── HELLO válido ────────────────────────────────────────────────
        String helloRaw = "@192.168.1.1*192.168.1.2*192.168.1.3";
        System.out.println("INPUT : " + helloRaw);
        System.out.println("TIPO  : " + MessageType.fromRaw(helloRaw));
        System.out.println("PARSE : " + MessageParser.parseHello(helloRaw));
        System.out.println();

        // ── LSA válido ──────────────────────────────────────────────────
        String lsaRaw = "#192.168.1.1;17*192.168.1.2;10*192.168.1.3;5";
        System.out.println("INPUT : " + lsaRaw);
        System.out.println("TIPO  : " + MessageType.fromRaw(lsaRaw));
        System.out.println("PARSE : " + MessageParser.parseLSA(lsaRaw));
        System.out.println();

        // ── DATA válido ─────────────────────────────────────────────────
        String dataRaw = "!192.168.1.1;192.168.1.7;1001;8;Olá pessoal!";
        System.out.println("INPUT : " + dataRaw);
        System.out.println("TIPO  : " + MessageType.fromRaw(dataRaw));
        System.out.println("PARSE : " + MessageParser.parseData(dataRaw));
        System.out.println();

        // ── DATA com ';' no conteúdo ────────────────────────────────────
        String dataRaw2 = "!192.168.1.1;192.168.1.7;1002;16;Mensagem; com ponto-e-vírgula!";
        System.out.println("INPUT : " + dataRaw2);
        System.out.println("PARSE : " + MessageParser.parseData(dataRaw2));
        System.out.println();

        // ── Malformados ─────────────────────────────────────────────────
        System.out.println("--- Casos malformados ---");
        MessageParser.parseHello("@");
        MessageParser.parseLSA("#192.168.1.1;0*192.168.1.2;10");
        MessageParser.parseLSA("#192.168.1.1;1*192.168.1.2;-5");
        MessageParser.parseData("!192.168.1.1;192.168.1.7;abc;8;msg");
        MessageParser.parseData("!192.168.1.1;192.168.1.7;1;-1;msg");

        System.out.println("\nTipo desconhecido: " + MessageType.fromRaw("XPTO"));
        System.out.println("\n>>> Teste do Parser concluído com sucesso! <<<");
    }
}
