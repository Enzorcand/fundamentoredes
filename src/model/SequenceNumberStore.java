package model;

import core.Logger;
import java.io.IOException;
import java.nio.file.*;

/**
 * Persiste e recupera o número de sequência do LSA local.
 */
public class SequenceNumberStore {

    private final Path filePath;
    private int currentSeq;

    public SequenceNumberStore(String myIp) {
        String safeName = myIp.replace('.', '_');
        this.filePath = Paths.get("seq_" + safeName + ".dat");
        this.currentSeq = load();
    }

    private int load() {
        if (!Files.exists(filePath)) return 0;
        try {
            String content = new String(Files.readAllBytes(filePath)).trim();
            int seq = Integer.parseInt(content);
            Logger.log("SEQ_CARREGADO", "Sequência anterior: " + seq + " (ficheiro: " + filePath + ")");
            return seq;
        } catch (Exception e) {
            Logger.log(Logger.ERRO, "Falha ao ler sequência de " + filePath + ": " + e.getMessage());
            return 0;
        }
    }

    public synchronized int nextAndSave() {
        currentSeq++;
        try {
            Files.write(filePath,
                    String.valueOf(currentSeq).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.log(Logger.ERRO, "Falha ao guardar sequência " + currentSeq
                    + " em " + filePath + ": " + e.getMessage());
        }
        return currentSeq;
    }

    public synchronized int getCurrent() {
        return currentSeq;
    }
}
