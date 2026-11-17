package protocol;

import core.Logger;
import core.RouterState;
import model.Neighbor;
import network.MessageParser;

/*
  Lógica de processamento de mensagens HELLO recebidas.
  Um enlace fica ativo quando o HELLO vem de um vizinho configurado (e habilitado)
  e o nosso IP aparece na lista de vizinhos desse HELLO (configuração mútua).
*/
public class HelloHandler {

    private final RouterState state;

    public HelloHandler(RouterState state) {
        this.state = state;
    }

    public void handle(MessageParser.HelloMessage hello, String senderIp) {
        Neighbor neighbor = state.getNeighbor(hello.originIp);
        if (neighbor == null) {
            Logger.log(Logger.HELLO_DESCARTADO, hello.originIp,
                    "Não é vizinho configurado");
            return;
        }

        if (!neighbor.isEnabled()) {
            Logger.log(Logger.HELLO_DESCARTADO, hello.originIp,
                    "Enlace lógico desativado");
            return;
        }

        Logger.log(Logger.HELLO_RECEBIDO, hello.originIp);
        neighbor.updateLastHello();

        boolean weAreInTheirList = hello.neighbors.contains(state.getMyIp());

        if (weAreInTheirList) {
            if (!neighbor.isActive()) {
                neighbor.setActive(true);
                Logger.log(Logger.VIZINHO_ATIVO, hello.originIp,
                        "Vizinhança estabelecida (bidirecional)");
                // Sincroniza LSDB com o novo vizinho antes de gerar o novo LSA local
                state.notifyNewNeighborActive(hello.originIp);
                state.notifyNeighborStateChange();
            }
        } else {
            if (neighbor.isActive()) {
                neighbor.setActive(false);
                Logger.log(Logger.VIZINHO_INATIVO, hello.originIp,
                        "Perdida bidirecionalidade (nosso IP ausente no HELLO dele)");
                state.notifyNeighborStateChange();
            }
        }
    }
}
