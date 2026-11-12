# Protocolo de Roteamento Link State (OSPF-like)

Implementação de um protocolo de roteamento distribuído do tipo **Link State** inspirado no OSPF, desenvolvido em **Java** com comunicação via **sockets UDP na porta 6000**.

Cada nó atua como um roteador autônomo: descobre vizinhos via mensagens periódicas, dissemina o estado dos seus enlaces (flooding de LSAs), mantém uma base de topologia global (LSDB), calcula caminhos mínimos pelo algoritmo de **Dijkstra** (implementado manualmente) e encaminha mensagens de dados (DATA).

---

## 📋 Sumário
- [Pré-requisitos](#-pré-requisitos)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Configuração de Vizinhos (`roteadores.txt`)](#-configuração-de-vizinhos-roteadorestxt)
- [Como Compilar](#-como-compilar)
- [Como Executar](#-como-executar)
- [Comandos da Interface Interativa (CLI)](#-comandos-da-interface-interativa-cli)
- [Como Executar os Testes Automatizados](#-como-executar-os-testes-automatizados)
- [Testes Manuais de Comunicação UDP](#-testes-manuais-de-comunicação-udp)
- [Especificação das Mensagens](#-especificação-das-mensagens)

---

## 💻 Pré-requisitos

- **Java JDK 11** ou superior instalado (`javac -version` e `java -version`).
- Terminal compatível com Bash/Zsh (Linux, macOS ou WSL no Windows).

---

## 📂 Estrutura do Projeto

```text
T2redes/
├── roteadores.txt                   # Arquivo de configuração de vizinhos locais
├── .gitignore                       # Ignora arquivos de compilação (.class) e temporários
├── src/
│   ├── Main.java                    # Ponto de entrada da aplicação
│   ├── core/                        # Núcleo, estado compartilhado e interface de terminal
│   │   ├── CLI.java                 # Linha de comandos interativa
│   │   ├── ConfigLoader.java        # Leitor do roteadores.txt
│   │   ├── Logger.java              # Logger centralizado padronizado
│   │   └── RouterState.java         # Estado compartilhado entre threads
│   ├── model/                       # Estruturas de dados de topologia e rotas
│   │   ├── Neighbor.java            # Modelo de vizinho configurado/ativo
│   │   ├── LSAEntry.java            # Registro de LSA na LSDB
│   │   ├── LSDB.java                # Link State Database (Thread-safe)
│   │   ├── RoutingEntry.java        # Entrada da tabela de roteamento
│   │   ├── RoutingTable.java        # Tabela de rotas e destaque de alterações
│   │   └── SequenceNumberStore.java # Persistência do número de sequência LSA
│   ├── network/                     # Camada de rede UDP e decodificação
│   │   ├── MessageType.java         # Enum dos prefixos (@, #, !)
│   │   ├── RawMessage.java          # Contêiner imutável de pacote UDP
│   │   ├── MessageParser.java       # Decodificador sintático e semântico
│   │   ├── UDPListener.java         # Thread de escuta UDP (porta 6000)
│   │   └── MessageProcessor.java    # Fila de processamento assíncrono
│   ├── protocol/                    # Lógica dos protocolos de enlace e dados
│   │   ├── HelloSender.java         # Envio de HELLO a cada 5s
│   │   ├── HelloHandler.java        # Verificação de bidirecionalidade HELLO
│   │   ├── NeighborMonitor.java     # Timeout de 15s de vizinhos inativos
│   │   ├── LSASender.java           # Geração e flooding de LSA a cada 10s ou eventos
│   │   ├── LSAHandler.java          # Validação de versão e reencaminhamento LSA
│   │   ├── LSAExpirationMonitor.java# Expiração de LSAs remotos após 30s
│   │   └── DataHandler.java         # Roteamento e envio de mensagens DATA
│   └── routing/                     # Algoritmo de roteamento
│       └── DijkstraRouter.java      # Dijkstra do zero com custos assimétricos e empates
└── test/                            # Bateria de testes unitários e de integração
    ├── TestParser.java              # Validação de sintaxe e rejeição de malformados
    ├── TestDijkstra.java            # Cálculo de caminhos mínimos e desempates
    ├── TestData.java                # Roteamento de DATA, decremento de TTL e descartes
    ├── TestCLI.java                 # Simulação de comandos da CLI
    └── TestSync.java                # Teste de sincronização inicial da LSDB
```

---

## ⚙️ Configuração de Vizinhos (`roteadores.txt`)

Na inicialização, o roteador lê o arquivo `roteadores.txt` na raiz da pasta. Cada linha define o IP de um vizinho e o custo do enlace para alcançá-lo (separados por ponto e vírgula):

```text
192.168.1.2;10
192.168.1.3;5
192.168.1.4;20
```

> **Regras:**
> - Os custos são inteiros positivos (`> 0`).
> - Linhas em branco ou iniciadas por `#` são ignoradas.
> - Os custos podem ser assimétricos (diferentes em cada sentido).

---

## 🔨 Como Compilar

No terminal, na raiz do projeto (`T2redes`):

```bash
# Cria o diretório out e compila todas as classes do código-fonte
javac -d out $(find src -name "*.java")
```

---

## 🚀 Como Executar

Execute o comando passando o **endereço IPv4** deste roteador como argumento:

```bash
java -cp out Main 192.168.1.1
```

*(Se não passar nenhum argumento, o roteador tentará descobrir o IP local da máquina automaticamente).*

Ao iniciar, você verá:
1. O IP configurado do roteador.
2. A leitura e carregamento dos vizinhos de `roteadores.txt`.
3. A abertura do socket UDP na **porta 6000**.
4. A exibição da **LSDB** inicial (vazia) e da **Tabela de Roteamento**.
5. O menu de comandos interativos da **CLI**.

---

## 🕹️ Comandos da Interface Interativa (CLI)

Durante a execução do roteador, você pode digitar comandos diretamente no terminal:

| Comando | Descrição | Exemplo |
|---|---|---|
| `send [IP] [mensagem]` | Envia uma mensagem DATA para o IP de destino | `send 192.168.1.3 Olá vizinho!` |
| `disable <IP>` | Desativa logicamente o enlace (ignora HELLOs desse vizinho) | `disable 192.168.1.2` |
| `enable <IP>` | Reativa o enlace lógico (aguarda novo HELLO bidirecional) | `enable 192.168.1.2` |
| `cost <IP> <custo>` | Modifica o custo de um enlace e propaga novo LSA imediatamente | `cost 192.168.1.2 25` |
| `routes` | Imprime a Tabela de Roteamento atualizada no terminal | `routes` |
| `lsdb` | Imprime o Banco de Dados de Estado de Enlace (LSDB) | `lsdb` |
| `neighbors` | Exibe o status de todos os vizinhos (Ativo, Habilitado, Custo) | `neighbors` |
| `help` | Exibe o menu de ajuda com a lista de comandos | `help` |
| `quit` / `exit` | Encerra a execução do roteador de forma limpa | `quit` |

> 💡 **Dica interativa:** Se você digitar apenas `send`, `cost`, `disable` ou `enable` sem parâmetros, a CLI solicitará interativamente cada dado necessário.

---

## 🧪 Como Executar os Testes Automatizados

O projeto conta com baterias completas de testes para cada componente:

### 1. Compilar os testes
```bash
javac -cp out -d out $(find test -name "*.java")
```

### 2. Executar cada suíte de testes

- **Teste de Dijkstra e Grafo:** Valida caminho mínimo com múltiplos saltos, descarte de arestas unidirecionais, detecção de renovação sem mudança e detecção de rotas adicionadas/modificadas/removidas:
  ```bash
  java -cp out TestDijkstra
  ```

- **Teste de Mensagens DATA:** Valida recebimento local, encaminhamento por salto, decremento de TTL, descarte por TTL expirado e descarte por ausência de rota:
  ```bash
  java -cp out TestData
  ```

- **Teste da CLI:** Simula comandos de alteração de custo, ativação/desativação de enlace e geração de eventos:
  ```bash
  java -cp out TestCLI
  ```

- **Teste do Parser de Mensagens:** Valida decodificação sintática e descarte de formatos malformados:
  ```bash
  java -cp out TestParser
  ```

---

## 📡 Testes Manuais de Comunicação UDP

Com o roteador em execução em um terminal, você pode injetar datagramas de teste a partir de outro terminal usando `nc` (Netcat) ou Python:

### Simular recebimento de HELLO bidirecional:
```bash
# Envia HELLO de 192.168.1.2 informando que nos conhece (192.168.1.1)
echo -n "@192.168.1.2*192.168.1.1" | nc -u -w1 127.0.0.1 6000
```

### Simular recebimento de LSA:
```bash
# LSA gerado por 192.168.1.2 anunciando enlace para 192.168.1.1 com custo 10
echo -n "#192.168.1.2;1*192.168.1.1;10" | nc -u -w1 127.0.0.1 6000
```

### Simular recebimento de mensagem DATA:
```bash
# DATA de 192.168.1.2 destinado ao próprio roteador (192.168.1.1)
echo -n "!192.168.1.2;192.168.1.1;1001;16;Mensagem de teste" | nc -u -w1 127.0.0.1 6000
```

> ⚠️ **Atenção:** O parâmetro `-n` do `echo` é fundamental para **não** enviar quebra de linha (`\n`), que é estritamente proibida pelo protocolo.

---

## 📑 Especificação das Mensagens

| Tipo | Prefixo | Formato | Descrição |
|---|---|---|---|
| **HELLO** | `@` | `@IP_ORIGEM*VIZ1*VIZ2*...` | Enviado a cada **5s**. Estabelece adjacência apenas se o IP local estiver na lista do outro roteador (bidirecionalidade). Timeout de inatividade: **15s**. |
| **LSA** | `#` | `#IP_ORIGEM;SEQ*VIZ1;CUSTO*...` | Gerado a cada **10s** ou em mudanças topológicas. Sequência persiste em arquivo (`seq_*.dat`). Expira após **30s**. |
| **DATA** | `!` | `!IP_ORIG;IP_DEST;ID;TTL;MSG` | TTL inicial = **16**, decrementado a cada salto. Descartado se TTL = 0 ou sem rota. |

Todos os pacotes trafegam sobre **UDP**, porta **6000**, codificados em **UTF-8**, com tamanho máximo de **1200 bytes** e **sem quebra de linha** ao final.
