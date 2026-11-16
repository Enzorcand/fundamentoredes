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
- Terminal compatível com Bash/Zsh (Linux, macOS) ou PowerShell/CMD (Windows).

---

## 📂 Estrutura do Projeto

```text
fundamentoredes/
├── roteadores.txt                   # Arquivo de configuração de vizinhos locais
├── seq.txt                          # Número de sequência LSA persistido (criado automaticamente)
├── .gitignore                       # Ignora arquivos de compilação (.class) e temporários
├── src/
│   ├── Main.java                    # Ponto de entrada da aplicação
│   ├── core/                        # Núcleo, estado compartilhado e interface de terminal
│   │   ├── CLI.java                 # Linha de comandos interativa
│   │   ├── ConfigLoader.java        # Leitor/escritor do roteadores.txt
│   │   ├── Logger.java              # Logger centralizado padronizado
│   │   └── RouterState.java         # Estado compartilhado entre threads
│   ├── model/                       # Estruturas de dados de topologia e rotas
│   │   ├── Neighbor.java            # Modelo de vizinho configurado/ativo
│   │   ├── LSAEntry.java            # Registro de LSA na LSDB
│   │   ├── LSDB.java                # Link State Database (Thread-safe)
│   │   ├── RoutingEntry.java        # Entrada da tabela de roteamento
│   │   ├── RoutingTable.java        # Tabela de rotas e destaque de alterações
│   │   └── SequenceNumberStore.java # Persistência do número de sequência em seq.txt
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
> - O arquivo é **atualizado automaticamente** pelos comandos `addneighbor` e `removeneighbor`.

---

## 🔨 Como Compilar

Na raiz do projeto (`fundamentoredes`):

**Linux / macOS:**
```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

**Windows (PowerShell):**
```powershell
javac -encoding UTF-8 -cp src -d out src\core\*.java src\model\*.java src\network\*.java src\routing\*.java src\protocol\*.java src\Main.java
```

---

## 🚀 Como Executar

Execute o comando passando o **endereço IPv4** deste roteador como argumento:

**Linux / macOS:**
```bash
java -cp out Main 192.168.1.1
```

**Windows:**
```powershell
java -Dfile.encoding=UTF-8 -cp out Main 192.168.1.1
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

### Envio de Mensagens

| Comando | Descrição | Exemplo |
|---|---|---|
| `send <IP> <mensagem>` | Envia DATA com TTL padrão (16) | `send 192.168.1.3 Ola vizinho!` |
| `send <IP> <TTL> <mensagem>` | Envia DATA com TTL customizado (0-255) | `send 192.168.1.3 5 Ola vizinho!` |

> O **roteador de origem** envia com o TTL informado **sem decrementá-lo**. Apenas roteadores intermediários decrementam o TTL. Um caminho com *k* saltos intermediários exige TTL >= *k*.

### Gerenciamento de Vizinhos

| Comando | Descrição | Exemplo |
|---|---|---|
| `disable <IP>` | Desativa o enlace: para o envio de HELLO e descarta HELLOs/LSAs recebidos desse vizinho | `disable 192.168.1.2` |
| `enable <IP>` | Reativa o enlace: retoma o envio de HELLO (enlace só fica ativo após nova confirmação bidirecional) | `enable 192.168.1.2` |
| `cost <IP> <custo>` | Modifica o custo de um enlace e propaga novo LSA imediatamente | `cost 192.168.1.2 25` |
| `addneighbor <IP> <custo>` | Adiciona um novo vizinho configurado e atualiza `roteadores.txt` | `addneighbor 192.168.1.5 15` |
| `removeneighbor <IP>` | Remove um vizinho configurado e atualiza `roteadores.txt` | `removeneighbor 192.168.1.5` |

### Consultas

| Comando | Descrição |
|---|---|
| `routes` | Exibe a Tabela de Roteamento atualizada |
| `lsdb` | Exibe o Banco de Dados de Estado de Enlace (LSDB) |
| `neighbors` | Exibe o status de todos os vizinhos (Ativo, Habilitado, Custo) |

### Testes de Protocolo

| Comando | Descrição | Exemplo |
|---|---|---|
| `testlsa <origem> <vizinho> dup` | Reenvia o LSA armazenado de `<origem>` para `<vizinho>` com a **sequencia original** (simula duplicado) | `testlsa 192.168.1.2 192.168.1.3 dup` |
| `testlsa <origem> <vizinho> old` | Reenvia o LSA armazenado de `<origem>` para `<vizinho>` com **sequencia - 1** (simula antigo) | `testlsa 192.168.1.2 192.168.1.3 old` |

### Outros

| Comando | Descrição |
|---|---|
| `help` | Exibe o menu de ajuda com todos os comandos |
| `quit` / `exit` | Encerra a execução do roteador de forma limpa |

> **Dica interativa:** Se você digitar apenas `send`, `cost`, `disable`, `enable`, `addneighbor`, `removeneighbor` ou `testlsa` sem parametros, a CLI solicitara interativamente cada dado necessario.

---

## 🧪 Como Executar os Testes Automatizados

### 1. Compilar os testes

**Linux / macOS:**
```bash
javac -encoding UTF-8 -cp out -d out $(find test -name "*.java")
```

**Windows (PowerShell):**
```powershell
javac -encoding UTF-8 -cp src -d out test\*.java
```

### 2. Executar cada suite de testes

- **Teste de Dijkstra e Grafo:** Valida caminho minimo com multiplos saltos, descarte de arestas unidirecionais, filtragem por vizinhos ativos, deteccao de renovacao sem mudanca e deteccao de rotas adicionadas/modificadas/removidas:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestDijkstra
  ```

- **Teste de Mensagens DATA:** Valida recebimento local, encaminhamento por salto, decremento de TTL por roteadores intermediarios, descarte por TTL expirado e descarte por ausencia de rota:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestData
  ```

- **Teste da CLI:** Simula comandos de alteracao de custo, ativacao/desativacao de enlace e geracao de eventos:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestCLI
  ```

- **Teste do Parser de Mensagens:** Valida decodificacao sintatica e descarte de formatos malformados:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestParser
  ```

---

## 📡 Testes Manuais de Comunicação UDP

Com o roteador em execucao em um terminal, voce pode injetar datagramas de teste a partir de outro terminal usando `nc` (Netcat):

### Simular recebimento de HELLO bidirecional:
```bash
echo -n "@192.168.1.2*192.168.1.1" | nc -u -w1 127.0.0.1 6000
```

### Simular recebimento de LSA:
```bash
echo -n "#192.168.1.2;1*192.168.1.1;10" | nc -u -w1 127.0.0.1 6000
```

### Simular recebimento de mensagem DATA:
```bash
echo -n "!192.168.1.2;192.168.1.1;1001;16;Mensagem de teste" | nc -u -w1 127.0.0.1 6000
```

> **Atencao:** O parametro `-n` do `echo` e fundamental para **nao** enviar quebra de linha (`\n`), que e estritamente proibida pelo protocolo.

---

## 📑 Especificação das Mensagens

| Tipo | Prefixo | Formato | Descricao |
|---|---|---|---|
| **HELLO** | `@` | `@IP_ORIGEM*VIZ1*VIZ2*...` | Enviado a cada **5s** a todos os vizinhos habilitados. Adjacencia estabelecida apenas com bidirecionalidade confirmada. Timeout de inatividade: **15s**. |
| **LSA** | `#` | `#IP_ORIGEM;SEQ*VIZ1;CUSTO*...` | Gerado a cada **10s** ou em mudancas topologicas. Numero de sequencia persistido em **`seq.txt`** (continua do valor salvo + 1 apos reinicio). LSA com proprio IP e descartado. Expira apos **30s**. |
| **DATA** | `!` | `!IP_ORIG;IP_DEST;ID;TTL;MSG` | TTL inicial configuravel pelo usuario (**0-255**, padrao **16**). A **origem envia sem decrementar**; apenas roteadores intermediarios decrementam. Descartado se TTL = 0 ou sem rota. O proximo salto e sempre um **vizinho ativo**. |

Todos os pacotes trafegam sobre **UDP**, porta **6000**, codificados em **UTF-8**, com tamanho maximo de **1200 bytes** e **sem quebra de linha** ao final.

---

## 🔄 Resumo das Regras de Comportamento

| Componente | Comportamento |
|---|---|
| **HELLO** | Enviado a cada 5s. Apenas vizinhos **configurados e habilitados** recebem. |
| **Enlace ativo** | Requer HELLO bidirecional (nos estamos no HELLO do vizinho **e** ele esta no nosso). |
| **Enlace desativado** | Nao envia HELLO, **descarta** HELLOs e LSAs recebidos desse vizinho. |
| **LSA gerado** | A cada 10s ou em mudancas. Sequencia salva em `seq.txt` antes do envio. |
| **LSA recebido** | Aceito apenas se vier de vizinho ativo e tiver sequencia maior que a maxima vista. LSA com proprio IP e descartado. |
| **Flooding** | LSA novo encaminhado a todos os vizinhos ativos, exceto o remetente. Ao estabelecer nova vizinhanca, sincroniza toda a LSDB. |
| **Expiracao LSA** | LSAs remotos expiram apos 30s sem renovacao. A maior sequencia vista por origem e mantida mesmo apos expiracao. |
| **Dijkstra** | Executado quando a topologia muda. Renovacao periodica sem mudanca **nao** dispara novo calculo. O proximo salto e sempre um **vizinho ativo**. |
| **DATA - origem** | Envia com TTL inicial (padrao 16, configuravel 0-255) **sem decrementar**. |
| **DATA - intermediario** | Verifica TTL > 0, decrementa e encaminha. Descarta se TTL = 0 ou sem rota. |
| **addneighbor / removeneighbor** | Atualiza `roteadores.txt` em tempo real. Novo enlace so fica ativo apos HELLO bidirecional. |
| **testlsa** | Reenvia LSA da LSDB para teste: `dup` (seq original) ou `old` (seq - 1). |
