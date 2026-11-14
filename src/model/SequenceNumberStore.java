package model;

import core.Logger;
import java.io.IOException;
import java.nio.file.*;

/**
 * Persiste e recupera o número de sequência do LSA local.
 * Conforme enunciado: arquivo fixo "seq.txt"; ao reiniciar, continua do valor gravado + 1.
 */
public class SequenceNumberStore {

    private final Path filePath;
    private int currentSeq;

    public SequenceNumberStore(String myIp) {
        this.filePath = Paths.get("seq.txt");
        this.currentSeq = load(myIp);
    }

    private int load(String myIp) {
        if (!Files.exists(filePath)) return 0;
        try {
            String content = new String(Files.readAllBytes(filePath)).trim();
            int seq = Integer.parseInt(content);
            // Ao reiniciar, a numeração continua a partir do valor gravado mais 1 (nextAndSave faz o incremento)
            Logger.log("SEQ_CARREGADO", "Sequência anterior: " + seq
                    + " — próxima será " + (seq + 1) + " (ficheiro: " + filePath + ")");
            return seq;
        } catch (Exception e) {
            Logger.log(Logger.ERRO, "Falha ao ler sequência de " + filePath + ": " + e.getMessage());
            return 0;
        }
    }

    public synchronized int nextAndSave() {
        currentSeq++;
        save();
        return currentSeq;
    }

    /**
     * Regra do adendo: ao receber o próprio LSA com número maior ou igual,
     * salta a sequência para superar o anúncio antigo e persiste em seq.txt.
     */
    public synchronized void advanceTo(int newSeq) {
        if (newSeq > currentSeq) {
            currentSeq = newSeq;
            save();
            Logger.log("SEQ_SALTADA", "Sequência própria avançada para " + currentSeq + " (sobrepondo LSA antigo)");
        }
    }

    private void save() {
        try {
            Files.write(filePath,
                    String.valueOf(currentSeq).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Logger.log(Logger.ERRO, "Falha ao guardar sequência " + currentSeq
                    + " em " + filePath + ": " + e.getMessage());
        }
    }

    public synchronized int getCurrent() {
        return currentSeq;
    }
}
