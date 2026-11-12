package protocol;

import core.Logger;
import core.RouterState;
import model.Neighbor;
import network.MessageParser;

/**
 * Lógica de processamento de mensagens HELLO recebidas.
 */
public class HelloHandler {

    private final RouterState state;

    public HelloHandler(RouterState state) {
        this.state = state;
    }

    public void handle(MessageParser.HelloMessage hello, String senderIp) {
        Logger.log(Logger.HELLO_RECEBIDO, hello.originIp);

        Neighbor neighbor = state.getNeighbor(hello.originIp);
        if (neighbor == null) {
            Logger.log(Logger.MENSAGEM_MALFORMADA,
                    "HELLO de " + hello.originIp
                            + " descartado — não é vizinho configurado");
            return;
        }

        if (!neighbor.isEnabled()) {
            return;
        }

        neighbor.updateLastHello();

        boolean weAreInTheirList = hello.neighbors.contains(state.getMyIp());

        if (weAreInTheirList) {
            if (!neighbor.isActive()) {
                neighbor.setActive(true);
                Logger.log(Logger.VIZINHO_ATIVO,
                        hello.originIp + " | Vizinhança estabelecida (bidirecional)");
                state.notifyNeighborStateChange();
                state.notifyNewNeighborActive(hello.originIp);
            }
        } else {
            if (neighbor.isActive()) {
                neighbor.setActive(false);
                Logger.log(Logger.VIZINHO_INATIVO,
                        hello.originIp
                                + " | Perdida bidirecionalidade (nosso IP ausente no HELLO deles)");
                state.notifyNeighborStateChange();
            }
        }
    }
}
