# Protocolo de Roteamento Link State (OSPF-like)

Implementação de um protocolo de roteamento distribuído do tipo **Link State** sobre **UDP (porta 6000)** em **Java**, com algoritmo de **Dijkstra** implementado manualmente para cálculo de caminhos mínimos e encaminhamento de mensagens de dados com TTL.

---

## ⚙️ Pré-requisitos e Compilação

* **Java JDK 11** ou superior instalado.

### Compilar a aplicação:
```bash
javac -encoding UTF-8 -d out $(find src test -name "*.java")
```

---

## 🚀 Como Executar

1. Configure os vizinhos no arquivo `roteadores.txt` no formato `IP;CUSTO`:
   ```text
   192.168.1.2;10
   192.168.1.3;5
   ```
   *(Linhas vazias ou iniciadas por `#` são ignoradas. O arquivo é atualizado automaticamente ao usar `addneighbor` ou `removeneighbor`).*

2. Inicie o roteador informando seu próprio endereço IPv4:
   ```bash
   java -cp out Main <SEU_IP>
   ```

---

## 🕹️ Comandos da Linha de Comandos (CLI)

Durante a execução, utilize os seguintes comandos no terminal:

| Comando | Descrição |
|---|---|
| `routes` | Exibe a Tabela de Roteamento atualizada |
| `lsdb` | Exibe o mapa da topologia (LSDB) com sequência e idade |
| `neighbors` | Exibe os vizinhos configurados e status (Ativo/Inativo) |
| `send <IP> <msg>` | Envia mensagem DATA com TTL padrão (16) |
| `send <IP> <TTL> <msg>` | Envia mensagem DATA com TTL customizado (0 a 255) |
| `disable <IP>` | Desativa o enlace lógico (para HELLO e descarta pacotes desse vizinho) |
| `enable <IP>` | Reativa o enlace (retoma envio de HELLO até confirmação bidirecional) |
| `cost <IP> <custo>` | Altera o custo do enlace e inunda novo LSA imediatamente |
| `addneighbor <IP> <custo>` | Adiciona vizinho e salva no `roteadores.txt` |
| `removeneighbor <IP>` | Remove vizinho e salva no `roteadores.txt` |
| `testlsa <origem> <viz> dup` | Reenvia LSA da base com sequência original (simula duplicado) |
| `testlsa <origem> <viz> old` | Reenvia LSA da base com sequência - 1 (simula antigo) |
| `quiet` | Liga/desliga exibição de HELLOs periódicos no terminal |
| `quit` / `exit` | Encerra o roteador |

---

## 🧪 Testes Automatizados

Para executar a simulação completa dos 8 cenários exigidos no trabalho final (Cenários A a H):

```bash
java -cp out SimuladorCenarios
```

Para executar os testes unitários individuais:
```bash
java -cp out TestDijkstra
java -cp out TestData
java -cp out TestParser
java -cp out TestCLI
```

---

## 📑 Resumo dos Protocolos e Temporizadores

* **Porta UDP:** 6000 | **Tamanho máximo:** 1200 bytes | **Codificação:** UTF-8 estrito sem quebras de linha (`\n`).
* **HELLO (`@`):** Enviado a cada **5s**. Enlace ativo somente após handshake bidirecional. Timeout de inatividade: **15s**.
* **LSA (`#`):** Enviado a cada **10s** ou por evento. Número de sequência persistido em `seq.txt`. Validade do anúncio: **30s**.
* **Dijkstra:** Recalculado sob mudança topológica real. Desempate determinístico pelo **menor IPv4 numérico** do próximo salto.
* **DATA (`!`):** Origem envia com TTL inicial sem decrementar; roteadores intermediários decrementam 1. Pacote descartado se TTL = 0 ou sem rota.
