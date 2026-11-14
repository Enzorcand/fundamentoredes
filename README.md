# Fundamentos de Redes - Trabalho Final

## Objetivo
Desenvolver uma aplicação que implemente um protocolo de roteamento do tipo Link State, inspirado no OSPF.

## Requisitos Principais

### Funcionalidades
- Cada computador atua como um roteador
- Identificação de vizinhos
- Divulgação do estado de enlaces (LSA)
- Manutenção de base de dados da topologia (LSDB)
- Cálculo de caminhos de menor custo pelo algoritmo de Dijkstra
- Encaminhamento de mensagens entre roteadores

### Especificações Técnicas
- **Protocolo**: UDP, porta 6000
- **Codificação**: UTF-8
- **Tamanho máximo dos pacotes**: 1200 bytes
- **Identificadores**: IPv4

## Estrutura das Mensagens

<<<<<<< Updated upstream
### 1. HELLO
=======
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
>>>>>>> Stashed changes
```
@IP_ORIGEM*VIZINHO1*VIZINHO2*...
@192.168.1.1*192.168.1.2*192.168.1.3
```
- Enviado a cada 5 segundos
- Usado para descoberta e manutenção de vizinhança

<<<<<<< Updated upstream
### 2. LSA (Link State Advertisement)
```
#IP_ORIGEM;SEQUENCIA*VIZINHO1;CUSTO*VIZINHO2;CUSTO
#192.168.1.1;25*192.168.1.2;10*192.168.1.3;5
=======
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
>>>>>>> Stashed changes
```
- Gerado a cada 10 segundos ou quando há alteração
- Número de sequência incrementado a cada novo LSA
- Propagado por flooding

<<<<<<< Updated upstream
### 3. DATA
```
!IP_ORIGEM;IP_DESTINO;ID;TTL;MENSAGEM
!192.168.1.1;192.168.1.7;1001;8;Olá pessoal!
=======
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
| `send <IP> <mensagem>` | Envia DATA com TTL padrão (16) | `send 192.168.1.3 Olá vizinho!` |
| `send <IP> <TTL> <mensagem>` | Envia DATA com TTL customizado (0–255) | `send 192.168.1.3 5 Olá vizinho!` |

> O **roteador de origem** envia com o TTL informado **sem decrementá-lo**. Apenas roteadores intermediários decrementam o TTL. Um caminho com *k* saltos intermediários exige TTL ≥ *k*.

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
| `testlsa <origem> <vizinho> dup` | Reenvia o LSA armazenado de `<origem>` para `<vizinho>` com a **sequência original** (simula duplicado) | `testlsa 192.168.1.2 192.168.1.3 dup` |
| `testlsa <origem> <vizinho> old` | Reenvia o LSA armazenado de `<origem>` para `<vizinho>` com **sequência – 1** (simula antigo) | `testlsa 192.168.1.2 192.168.1.3 old` |

### Outros

| Comando | Descrição |
|---|---|
| `help` | Exibe o menu de ajuda com todos os comandos |
| `quit` / `exit` | Encerra a execução do roteador de forma limpa |

> 💡 **Dica interativa:** Se você digitar apenas `send`, `cost`, `disable`, `enable`, `addneighbor`, `removeneighbor` ou `testlsa` sem parâmetros, a CLI solicitará interativamente cada dado necessário.

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

### 2. Executar cada suíte de testes

- **Teste de Dijkstra e Grafo:** Valida caminho mínimo com múltiplos saltos, descarte de arestas unidirecionais, filtragem por vizinhos ativos, detecção de renovação sem mudança e detecção de rotas adicionadas/modificadas/removidas:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestDijkstra
  ```

- **Teste de Mensagens DATA:** Valida recebimento local, encaminhamento por salto, decremento de TTL por roteadores intermediários, descarte por TTL expirado e descarte por ausência de rota:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestData
  ```

- **Teste da CLI:** Simula comandos de alteração de custo, ativação/desativação de enlace e geração de eventos:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestCLI
  ```

- **Teste do Parser de Mensagens:** Valida decodificação sintática e descarte de formatos malformados:
  ```bash
  java -Dfile.encoding=UTF-8 -cp out TestParser
  ```

---

## 📡 Testes Manuais de Comunicação UDP

Com o roteador em execução em um terminal, você pode injetar datagramas de teste a partir de outro terminal usando `nc` (Netcat) ou Python:

### Simular recebimento de HELLO bidirecional:
```bash
# Envia HELLO de 192.168.1.2 informando que nos conhece (192.168.1.1)
echo -n "@192.168.1.2*192.168.1.1" | nc -u -w1 127.0.0.1 6000
>>>>>>> Stashed changes
```
- TTL inicial: 16
- ID único por origem
- Encaminhado conforme tabela de roteamento

## Temporizadores
- **HELLO**: 5 segundos
- **LSA periódico**: 10 segundos
- **Timeout vizinho**: 15 segundos
- **Expiração LSA**: 30 segundos

## Arquivos de Configuração
- `roteadores.txt`: Lista de vizinhos e custos (formato: IP;CUSTO)
- Arquivo de persistência: Último número de sequência

## Cenários de Teste
A. Convergência inicial da rede
B. Entrada de um novo roteador
C. Falha de um enlace
D. Falha de um roteador
E. Recuperação após falha
F. Recebimento de LSAs antigos e duplicados
G. Alteração do custo de um enlace
H. Encaminhamento de DATA com TTL insuficiente

## Comandos Interativos
- Consultar LSDB
- Consultar tabela de roteamento
- Ativar/desativar enlaces lógicos
- Alterar custos de enlaces
- Enviar mensagens DATA

## Log de Eventos
- HELLO_RECEBIDO
- LSA_ACEITO
- DIJKSTRA_EXECUTADO
- DATA_ENCAMINHADA
- DATA_RECEBIDO

<<<<<<< Updated upstream
## Entrega
- Data: 18/11
- Código-fonte
- Relatório em PDF
- Capturas do Wireshark (.pcap/.pcapng)

## Plano de Desenvolvimento

### Fase 1: Estrutura Básica
1. Definir arquitetura e escolher linguagem (Python recomendado)
2. Criar estrutura de diretórios
3. Implementar leitura do arquivo roteadores.txt
4. Implementar comunicação UDP (porta 6000)

### Fase 2: Descoberta de Vizinhos
5. Implementar mensagem HELLO (envio a cada 5s)
6. Implementar detecção de vizinhos ativos (timeout 15s)
7. Implementar sistema de logging básico

### Fase 3: LSDB e LSAs
8. Implementar estrutura LSDB (Link State Database)
9. Implementar geração de LSA (a cada 10s e em mudanças)
10. Implementar persistência de número de sequência

### Fase 4: Flooding e Sincronização
11. Implementar flooding de LSAs
12. Implementar validação e sincronização de LSAs
13. Implementar expiração de LSAs (30s)

### Fase 5: Roteamento
14. Implementar algoritmo de Dijkstra (sem biblioteca)
15. Implementar tabela de roteamento
16. Implementar atualização de rotas

### Fase 6: Encaminhamento de Mensagens
17. Implementar mensagem DATA (encaminhamento)
18. Implementar TTL e descarte de mensagens

### Fase 7: Interface Interativa
19. Implementar comandos interativos (ativar/desativar enlaces, alterar custos)
20. Implementar comandos de consulta (LSDB, tabela de roteamento)

### Fase 8: Testes e Documentação
21. Criar testes com cenários A-H
22. Capturar tráfego com Wireshark
23. Preparar relatório técnico
=======
| Tipo | Prefixo | Formato | Descrição |
|---|---|---|---|
| **HELLO** | `@` | `@IP_ORIGEM*VIZ1*VIZ2*...` | Enviado a cada **5s** a todos os vizinhos habilitados. Adjacência estabelecida apenas com bidirecionalidade confirmada. Timeout de inatividade: **15s**. |
| **LSA** | `#` | `#IP_ORIGEM;SEQ*VIZ1;CUSTO*...` | Gerado a cada **10s** ou em mudanças topológicas. Número de sequência persistido em **`seq.txt`** (continua do valor salvo + 1 após reinício). LSA com próprio IP é descartado. Expira após **30s**. |
| **DATA** | `!` | `!IP_ORIG;IP_DEST;ID;TTL;MSG` | TTL inicial configurável pelo usuário (**0–255**, padrão **16**). A **origem envia sem decrementar**; apenas roteadores intermediários decrementam. Descartado se TTL = 0 ou sem rota. O próximo salto é sempre um **vizinho ativo**. |

Todos os pacotes trafegam sobre **UDP**, porta **6000**, codificados em **UTF-8**, com tamanho máximo de **1200 bytes** e **sem quebra de linha** ao final.

---

## 🔄 Resumo das Regras de Comportamento

| Componente | Comportamento |
|---|---|
| **HELLO** | Enviado a cada 5s. Apenas vizinhos **configurados e habilitados** recebem. |
| **Enlace ativo** | Requer HELLO bidirecional (nós estamos no HELLO do vizinho **e** ele está no nosso). |
| **Enlace desativado** | Não envia HELLO, **descarta** HELLOs e LSAs recebidos desse vizinho. |
| **LSA gerado** | A cada 10s ou em mudanças. Sequência salva em `seq.txt` antes do envio. |
| **LSA recebido** | Aceito apenas se vier de vizinho ativo e tiver sequência maior que a máxima vista. LSA com próprio IP é descartado. |
| **Flooding** | LSA novo encaminhado a todos os vizinhos ativos, exceto o remetente. Ao estabelecer nova vizinhança, sincroniza toda a LSDB. |
| **Expiração LSA** | LSAs remotos expiram após 30s sem renovação. A maior sequência vista por origem é mantida mesmo após expiração. |
| **Dijkstra** | Executado quando a topologia muda. Renovação periódica sem mudança **não** dispara novo cálculo. O próximo salto é sempre um **vizinho ativo**. |
| **DATA – origem** | Envia com TTL inicial (padrão 16, configurável 0–255) **sem decrementar**. |
| **DATA – intermediário** | Verifica TTL > 0, decrementa e encaminha. Descarta se TTL = 0 ou sem rota. |
| **addneighbor / removeneighbor** | Atualiza `roteadores.txt` em tempo real. Novo enlace só fica ativo após HELLO bidirecional. |
| **testlsa** | Reenvia LSA da LSDB para teste: `dup` (seq original) ou `old` (seq - 1). |
>>>>>>> Stashed changes
