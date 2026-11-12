# Objetivo

Desenvolver uma aplicação que implemente um protocolo de roteamento do tipo Link State, inspirado no OSPF. Cada computador deverá atuar como um roteador, identificar seus vizinhos, divulgar o estado de seus enlaces, manter uma base de dados da topologia e calcular caminhos de menor custo pelo algoritmo de Dijkstra. A aplicação também deverá encaminhar mensagens entre os roteadores conforme as rotas calculadas.

O trabalho poderá ser desenvolvido na linguagem escolhida pelo grupo, com comunicação por sockets UDP. Não se exige compatibilidade com o OSPF real, mas as implementações de diferentes grupos deverão seguir a mesma especificação e interoperar.

## Inicialização e descoberta de vizinhos

Ao iniciar, cada roteador deverá ler o arquivo `roteadores.txt`, com um endereço IPv4 de vizinho e o custo do enlace por linha:

```text
192.168.1.2;10
192.168.1.3;5
192.168.1.4;20
```

Os custos serão inteiros positivos. Os enlaces serão bidirecionais quanto à conectividade, mas o custo poderá ser diferente em cada direção. O roteador deverá enviar HELLO a cada 5 segundos aos vizinhos configurados.

Um enlace só será considerado ativo quando houver comunicação HELLO e os dois roteadores tiverem um ao outro na lista de vizinhos configurados. Ao estabelecer ou perder uma vizinhança, o roteador deverá atualizar seu anúncio de estado de enlace.

## Banco de dados de estado de enlace (LSDB)

Cada roteador deverá manter uma LSDB com a versão válida mais recente do anúncio de cada roteador conhecido. Cada registro deverá guardar o IP de origem, o número de sequência, os enlaces anunciados e o instante de recebimento da versão válida.

A LSDB representa a topologia conhecida. A tabela de roteamento apresenta os melhores caminhos calculados a partir dela. As duas estruturas deverão ser exibidas separadamente.

## Anúncios de estado de enlace (LSA)

Cada roteador deverá gerar um LSA a cada 10 segundos e também quando houver alteração dos enlaces ativos ou de seus custos. O formato será:

`#IP_ORIGEM;SEQUENCIA*VIZINHO1;CUSTO*VIZINHO2;CUSTO`

Exemplo:
`#192.168.1.1;17*192.168.1.2;10*192.168.1.3;5`

O número de sequência começará em 1 e será incrementado a cada novo LSA, inclusive nas renovações periódicas. O último número de sequência utilizado deverá ser armazenado em arquivo, permitindo que, após uma reinicialização, o roteador continue a numeração anterior.

O número de sequência deverá ser salvo antes do envio de cada novo LSA.

## Propagação de anúncios (flooding)

Ao receber um LSA de um vizinho ativo, o roteador deverá comparar sua versão com a armazenada. Apenas anúncios novos ou mais recentes serão aceitos, registrados na LSDB e encaminhados aos demais vizinhos ativos, exceto àquele que transmitiu o datagrama.

Anúncios duplicados e antigos deverão ser descartados, sem novo flooding e sem renovação de validade. Quando uma vizinhança for estabelecida, os roteadores deverão trocar os LSAs válidos que possuem, para sincronizar suas bases de dados. Não será exigido mecanismo de confirmação ou retransmissão confiável dos LSAs. Nessa troca, cada roteador deverá enviar ao novo vizinho todos os LSAs válidos armazenados em sua LSDB, permitindo que ele conheça a topologia existente sem aguardar os anúncios periódicos dos demais roteadores.

## Cálculo e atualização das rotas

Sempre que a topologia conhecida mudar, o roteador deverá reconstruir o grafo, executar o algoritmo de Dijkstra a partir de si próprio e atualizar a tabela de roteamento. O algoritmo deverá ser implementado pelo grupo, sem biblioteca que calcule diretamente caminhos mínimos. O primeiro cálculo deverá ocorrer após a criação do LSA local, utilizando as informações disponíveis na LSDB, sem necessidade de aguardar os anúncios de todos os roteadores. À medida que novos LSAs forem recebidos, as rotas deverão ser recalculadas quando houver alteração dos enlaces ou de seus custos. A renovação periódica de um LSA sem mudança na topologia não deverá provocar nova execução do Dijkstra.

| Destino | Custo total | Próximo salto |
| :--- | :--- | :--- |
| 192.168.1.4 | 12 | 192.168.1.2 |
| 192.168.1.5 | 18 | 192.168.1.3 |

O grafo utilizará os custos anunciados em cada direção. Uma aresta só poderá ser utilizada quando as duas extremidades possuírem LSAs válidos que reconheçam o enlace. Havendo caminhos de mesmo custo, será escolhido de forma aleatória. O roteador não deverá incluir rota para si próprio. Rotas incluídas, removidas ou modificadas deverão ser destacadas na saída.

## Falhas e alterações na topologia

Se um vizinho ficar mais de 15 segundos sem enviar HELLO válido, deverá ser considerado inativo. O roteador retirará esse enlace de seu LSA, incrementará a sequência, divulgará a alteração e recalculará as rotas quando necessário.

A aplicação deverá oferecer comandos para ativar ou desativar um enlace lógico e modificar seu custo sem reiniciar o processo. O restabelecimento do enlace dependerá de nova confirmação de vizinhança por HELLO. As alterações deverão aparecer no terminal e no log.

## Expiração de anúncios

Cada LSA remoto terá validade de 30 segundos a partir da última versão nova aceita. Sem renovação, será retirado da LSDB, provocando novo cálculo das rotas. LSAs duplicados ou antigos não renovam esse prazo.

Para impedir a aceitação posterior de anúncios antigos, o roteador deverá manter, enquanto estiver em execução, o maior número de sequência já aceito de cada origem, mesmo após a expiração do respectivo LSA. Anúncios com números de sequência iguais ou inferiores deverão ser descartados. O LSA local será renovado a cada 10 segundos.

## Protocolo de comunicação

Todos os grupos deverão usar UDP, porta 6000, mensagens codificadas em UTF-8 e identificadores IPv4. Os pacotes da aplicação terão, no máximo, 1200 bytes, sem quebra de linha ao final. Mensagens malformadas ou acima desse limite deverão ser descartadas e registradas.

### Mensagem 1 – HELLO

`@IP_ORIGEM*VIZINHO1*VIZINHO2*...`
`@192.168.1.1*192.168.1.2*192.168.1.3`

O campo IP_ORIGEM identifica o emissor. Os demais endereços são seus vizinhos configurados. HELLO será enviado a cada 5 segundos a todos os vizinhos configurados. Apenas HELLO válido, recebido de vizinho configurado, atualizará o temporizador de atividade.

### Mensagem 2 – LSA

`#IP_ORIGEM;SEQUENCIA*VIZINHO1;CUSTO*VIZINHO2;CUSTO`
`#192.168.1.1;25*192.168.1.2;10*192.168.1.3;5`

IP_ORIGEM identifica quem produziu o anúncio, não necessariamente quem o encaminhou. O datagrama LSA somente poderá ser aceito quando recebido de um vizinho ativo. Custos inválidos, anúncios malformados e versões antigas ou duplicadas deverão ser descartados.

### Mensagem 3 – DATA

A aplicação deverá permitir que o usuário envie mensagens de texto para qualquer roteador da rede, a qualquer momento durante a execução. Para isso, deverá informar o endereço IP do destino e o texto da mensagem. O encaminhamento deverá utilizar a tabela de roteamento disponível no momento do envio, mesmo que a rede ainda esteja em processo de convergência. Segue o formato na rede:

`!IP_ORIGEM;IP_DESTINO;ID;TTL;MENSAGEM`
`!192.168.1.1;192.168.1.7;1001;8;Olá pessoal!`

*   ID será único para cada origem durante uma execução.
*   TTL será um inteiro não negativo e indicará quantos encaminhamentos ainda são permitidos.

O TTL deverá ser inicializado com o valor 16 pelo roteador de origem e decrementado em uma unidade a cada encaminhamento. Ao receber uma mensagem DATA, o roteador deverá verificar se é o destino. Caso seja, exibirá a mensagem, sua origem e o ID. Caso contrário, consultará a tabela de roteamento e verificará o TTL. Se não houver rota ou o TTL for igual a zero, a mensagem deverá ser descartada, com o registro do motivo. Nos demais casos, o TTL será decrementado e a mensagem encaminhada ao próximo salto.

## Registro de eventos

Cada roteador deverá imprimir na tela de log com tipo de evento, origem quando aplicável e descrição. O log deverá permitir acompanhar a formação de vizinhanças, a aceitação e o descarte de LSAs, alterações da LSDB, execução do Dijkstra, mudanças de rota e encaminhamento ou descarte de mensagens.

```text
HELLO_RECEBIDO | 192.168.1.2
LSA_ACEITO | 192.168.1.3 | SEQ=7
DIJKSTRA_EXECUTADO
DATA_ENCAMINHADA | ID=1001 | DESTINO 192.168.1.7 | CONTEÚDO DA MENSAGEM
DATA_RECEBIDO | ID=1001 | DESTINO 192.168.1.7 | CONTEÚDO DA MENSAGEM
```

## Relatório técnico

O relatório deverá apresentar a arquitetura da solução, as estruturas de dados utilizadas (vizinhos, LSDB, tabela de roteamento e temporizadores), a implementação do Dijkstra e a topologia empregada nos testes, com custos e identificação dos roteadores.

Deverão ser documentados os cenários abaixo, com comportamento esperado, resultado observado, LSAs relevantes, alterações da LSDB e da tabela de roteamento e trechos de log:

| Cenário | Teste |
| :--- | :--- |
| A | Convergência inicial da rede |
| B | Entrada de um novo roteador |
| C | Falha de um enlace |
| D | Falha de um roteador |
| E | Recuperação após falha |
| F | Recebimento de LSAs antigos e duplicados |
| G | Alteração do custo de um enlace |
| H | Encaminhamento de DATA com TTL insuficiente |

O grupo deverá apresentar capturas realizadas com Wireshark, marcando as mensagens HELLO, LSA e DATA, endereços IP, portas UDP e conteúdo dos datagramas. Os arquivos .pcap ou .pcapng deverão acompanhar a entrega.