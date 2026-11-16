package network;

import core.Logger;
import protocol.DataHandler;
import protocol.HelloHandler;
import protocol.LSAHandler;
import java.util.concurrent.BlockingQueue;

/**
 * Thread de processamento e despacho de mensagens recebidas.
 */
public class MessageProcessor implements Runnable {

    private final BlockingQueue<RawMessage> queue;
    private final HelloHandler helloHandler;
    private final LSAHandler   lsaHandler;
    private final DataHandler  dataHandler;
    private volatile boolean   running = true;

    public MessageProcessor(BlockingQueue<RawMessage> queue,
                            HelloHandler helloHandler,
                            LSAHandler lsaHandler,
                            DataHandler dataHandler) {
        this.queue        = queue;
        this.helloHandler = helloHandler;
        this.lsaHandler   = lsaHandler;
        this.dataHandler  = dataHandler;
    }

    @Override
    public void run() {
        Logger.log("MSG_PROCESSOR", "Pronto a processar mensagens.");

        while (running) {
            RawMessage msg;
            try {
                msg = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            dispatch(msg);
        }

        Logger.log("MSG_PROCESSOR", "Encerrado.");
    }

    private void dispatch(RawMessage msg) {
        switch (msg.getType()) {
            case HELLO: handleHello(msg); break;
            case LSA:   handleLSA(msg);   break;
            case DATA:  handleData(msg);  break;
            default:
                Logger.log(Logger.MENSAGEM_MALFORMADA,
                        "Tipo desconhecido no processador: " + msg.getRaw());
        }
    }

    private void handleHello(RawMessage msg) {
        MessageParser.HelloMessage hello = MessageParser.parseHello(msg.getRaw());
        if (hello == null) return;
        helloHandler.handle(hello, msg.getSenderIp());
    }

    private void handleLSA(RawMessage msg) {
        MessageParser.LSAMessage lsa = MessageParser.parseLSA(msg.getRaw());
        if (lsa == null) return;
        lsaHandler.handle(lsa, msg.getSenderIp());
    }

    private void handleData(RawMessage msg) {
        MessageParser.DataMessage data = MessageParser.parseData(msg.getRaw());
        if (data == null) return;
        dataHandler.handleReceived(data, msg.getSenderIp());
    }

    public void stop() {
        running = false;
    }
}
