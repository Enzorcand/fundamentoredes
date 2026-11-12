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

### 1. HELLO
```
@IP_ORIGEM*VIZINHO1*VIZINHO2*...
@192.168.1.1*192.168.1.2*192.168.1.3
```
- Enviado a cada 5 segundos
- Usado para descoberta e manutenção de vizinhança

### 2. LSA (Link State Advertisement)
```
#IP_ORIGEM;SEQUENCIA*VIZINHO1;CUSTO*VIZINHO2;CUSTO
#192.168.1.1;25*192.168.1.2;10*192.168.1.3;5
```
- Gerado a cada 10 segundos ou quando há alteração
- Número de sequência incrementado a cada novo LSA
- Propagado por flooding

### 3. DATA
```
!IP_ORIGEM;IP_DESTINO;ID;TTL;MENSAGEM
!192.168.1.1;192.168.1.7;1001;8;Olá pessoal!
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
