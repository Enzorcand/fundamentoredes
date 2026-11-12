package core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger centralizado de eventos do roteador.
 * Formato obrigatório: TIPO_DE_EVENTO | DETALHES
 * Conforme enunciado, seção "Registro de eventos".
 */
public class Logger {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Tipos de evento definidos no enunciado
    public static final String HELLO_RECEBIDO       = "HELLO_RECEBIDO";
    public static final String HELLO_ENVIADO        = "HELLO_ENVIADO";
    public static final String LSA_ACEITO           = "LSA_ACEITO";
    public static final String LSA_DESCARTADO       = "LSA_DESCARTADO";
    public static final String LSA_GERADO           = "LSA_GERADO";
    public static final String LSA_ENCAMINHADO      = "LSA_ENCAMINHADO";
    public static final String LSA_EXPIRADO         = "LSA_EXPIRADO";
    public static final String DIJKSTRA_EXECUTADO   = "DIJKSTRA_EXECUTADO";
    public static final String ROTA_ADICIONADA      = "ROTA_ADICIONADA";
    public static final String ROTA_REMOVIDA        = "ROTA_REMOVIDA";
    public static final String ROTA_MODIFICADA      = "ROTA_MODIFICADA";
    public static final String VIZINHO_ATIVO        = "VIZINHO_ATIVO";
    public static final String VIZINHO_INATIVO      = "VIZINHO_INATIVO";
    public static final String DATA_RECEBIDO        = "DATA_RECEBIDO";
    public static final String DATA_ENCAMINHADA     = "DATA_ENCAMINHADA";
    public static final String DATA_DESCARTADA      = "DATA_DESCARTADA";
    public static final String MENSAGEM_MALFORMADA  = "MENSAGEM_MALFORMADA";
    public static final String ENLACE_ATIVADO       = "ENLACE_ATIVADO";
    public static final String ENLACE_DESATIVADO    = "ENLACE_DESATIVADO";
    public static final String CUSTO_ALTERADO       = "CUSTO_ALTERADO";
    public static final String ERRO                 = "ERRO";

    public static void log(String eventType, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.printf("[%s] %s | %s%n", timestamp, eventType, details);
    }

    public static void log(String eventType) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.printf("[%s] %s%n", timestamp, eventType);
    }

    public static void log(String eventType, String origin, String description) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.printf("[%s] %s | %s | %s%n", timestamp, eventType, origin, description);
    }
}
