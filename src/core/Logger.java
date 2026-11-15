package core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Logger centralizado de eventos do roteador.
 * Formato: TIPO_DE_EVENTO | ORIGEM | DESCRIÇÃO
 *
 * Cada evento é impresso no terminal e gravado no ficheiro de log
 * (roteador_<IP>.log), conforme o enunciado: "As alterações deverão
 * aparecer no terminal e no log".
 *
 * O modo silencioso (comando "quiet") esconde apenas do TERMINAL os eventos
 * periódicos de HELLO; eles continuam a ser gravados no ficheiro.
 */
public class Logger {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Tipos de evento
    public static final String HELLO_RECEBIDO       = "HELLO_RECEBIDO";
    public static final String HELLO_ENVIADO        = "HELLO_ENVIADO";
    public static final String HELLO_DESCARTADO     = "HELLO_DESCARTADO";
    public static final String LSA_ACEITO           = "LSA_ACEITO";
    public static final String LSA_DESCARTADO       = "LSA_DESCARTADO";
    public static final String LSA_GERADO           = "LSA_GERADO";
    public static final String LSA_ENCAMINHADO      = "LSA_ENCAMINHADO";
    public static final String LSA_EXPIRADO         = "LSA_EXPIRADO";
    public static final String LSDB_ALTERADA        = "LSDB_ALTERADA";
    public static final String LSDB_SYNC            = "LSDB_SYNC";
    public static final String DIJKSTRA_EXECUTADO   = "DIJKSTRA_EXECUTADO";
    public static final String ROTA_ADICIONADA      = "ROTA_ADICIONADA";
    public static final String ROTA_REMOVIDA        = "ROTA_REMOVIDA";
    public static final String ROTA_MODIFICADA      = "ROTA_MODIFICADA";
    public static final String VIZINHO_ATIVO        = "VIZINHO_ATIVO";
    public static final String VIZINHO_INATIVO      = "VIZINHO_INATIVO";
    public static final String VIZINHO_ADICIONADO   = "VIZINHO_ADICIONADO";
    public static final String VIZINHO_REMOVIDO     = "VIZINHO_REMOVIDO";
    public static final String DATA_RECEBIDO        = "DATA_RECEBIDO";
    public static final String DATA_ENCAMINHADA     = "DATA_ENCAMINHADA";
    public static final String DATA_DESCARTADA      = "DATA_DESCARTADA";
    public static final String MENSAGEM_MALFORMADA  = "MENSAGEM_MALFORMADA";
    public static final String ENLACE_ATIVADO       = "ENLACE_ATIVADO";
    public static final String ENLACE_DESATIVADO    = "ENLACE_DESATIVADO";
    public static final String CUSTO_ALTERADO       = "CUSTO_ALTERADO";
    public static final String TESTE_LSA            = "TESTE_LSA";
    public static final String SEQ_SALTADA          = "SEQ_SALTADA";
    public static final String ERRO                 = "ERRO";

    /** Eventos periódicos escondidos do terminal no modo silencioso. */
    private static final Set<String> NOISY = Set.of(HELLO_ENVIADO, HELLO_RECEBIDO);

    private static PrintWriter fileOut;
    private static volatile boolean quiet = false;

    /** Abre (em modo append) o ficheiro de log deste roteador. */
    public static synchronized void init(String myIp) {
        String fileName = "roteador_" + myIp + ".log";
        try {
            fileOut = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName, true), StandardCharsets.UTF_8), true);
            fileOut.println();
            fileOut.println("===== Início da execução " + LocalDateTime.now() + " =====");
            System.out.println("Log gravado em: " + fileName);
        } catch (IOException e) {
            System.out.println("[AVISO] Não foi possível abrir o ficheiro de log "
                    + fileName + ": " + e.getMessage());
        }
    }

    public static boolean toggleQuiet() {
        quiet = !quiet;
        return quiet;
    }

    public static boolean isQuiet() {
        return quiet;
    }

    public static void log(String eventType, String details) {
        write(eventType, eventType + " | " + details);
    }

    public static void log(String eventType) {
        write(eventType, eventType);
    }

    public static void log(String eventType, String origin, String description) {
        write(eventType, eventType + " | " + origin + " | " + description);
    }

    private static synchronized void write(String eventType, String line) {
        String stamped = "[" + LocalDateTime.now().format(FORMATTER) + "] " + line;
        if (!(quiet && NOISY.contains(eventType))) {
            System.out.println(stamped);
        }
        if (fileOut != null) {
            fileOut.println(stamped);
        }
    }

    public static synchronized void close() {
        if (fileOut != null) {
            fileOut.flush();
            fileOut.close();
            fileOut = null;
        }
    }
}
