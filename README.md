# 🌊 Territory Predictive Insights — Clima Local

**Global Solution 2º/2026 – POO (Turma de Fevereiro)** · Tema: *CitizenTech*

Plataforma de monitoramento territorial em **Java 17 + JDBC + H2** que lê sensores de chuva, nível do córrego e umidade do solo, grava o histórico no banco, gera **previsões** sobre esse histórico e emite **alertas automáticos explicados** (ATENÇÃO / ALERTA / CRÍTICO).

> **Recorte escolhido:** 🌊 **Clima local — chuva intensa e risco de alagamento em pontos vulneráveis.**
> Pergunta que o sistema ajuda a responder: *"qual problema vai atingir o nosso território nos próximos dias, e o que podemos fazer hoje para evitar ou reduzir o impacto?"*

---

## 👥 Integrantes

| Nome | RM | GitHub |
|------|----|--------|
| _[Integrante 1]_ | _[RM]_ | _[@usuario]_ |
| _[Integrante 2]_ | _[RM]_ | _[@usuario]_ |
| _[Integrante 3]_ | _[RM]_ | _[@usuario]_ |

---

## 🎯 Público beneficiado

**Quem sofre o problema:** as famílias que moram nas áreas baixas e nas encostas de uma comunidade cortada por um córrego canalizado (no projeto, a fictícia *Comunidade Vale do Córrego Verde*, ~18.500 moradores, 640 domicílios em área de risco). Quando chove forte, o córrego transborda, a passagem subterrânea alaga e isola o acesso à UBS, e o solo encharcado da encosta aumenta o risco de deslizamento.

**Quem usa o sistema:** a coordenação do **Núcleo Comunitário de Defesa Civil (NUPDEC)** / associação de moradores, em contato com a Defesa Civil municipal.

**Qual decisão o sistema apoia:** *se e quando* agir **hoje** — desobstruir bueiros e revisar rotas de fuga (ATENÇÃO), avisar as famílias e preparar o abrigo (ALERTA), ou acionar a Defesa Civil e iniciar a retirada preventiva (CRÍTICO). Cada alerta diz **qual sensor**, **qual estratégia**, **qual valor previsto** e **qual limite foi atingido**, junto com a ação sugerida.

> O território é descrito apenas por **região e equipamento** (pontos, sensores). **Nenhum dado pessoal** (nome, CPF, endereço) é usado.

---

## ▶️ Como executar

### Pré-requisitos
- **JDK 17 ou superior** instalado, com a variável `JAVA_HOME` apontando para ele.
- **Não é preciso instalar Maven**: o projeto traz o *Maven Wrapper* (`mvnw` / `mvnw.cmd`), que baixa o Maven 3.9.11 na primeira execução (precisa de internet só nessa primeira vez).
- **Não é preciso instalar banco**: o H2 vem como dependência e o banco é criado sozinho.

### Windows (PowerShell ou CMD), dentro da pasta do projeto
```bash
mvnw.cmd clean package
```
```bash
java -jar target\territory-predictive-insights.jar
```

### Linux / macOS
```bash
./mvnw clean package
```
```bash
java -jar target/territory-predictive-insights.jar
```

O `package` **compila, roda os 40 testes** e gera um JAR executável com o H2 embutido.

### Só os testes
```bash
mvnw.cmd test
```

### Pela IDE (IntelliJ / Eclipse / VS Code)
Importe como **projeto Maven** e execute a classe `SistemaMonitoramento` (em `src/main/java`, pacote padrão) — ela tem o `main`.

### Roteiro rápido de demonstração
1. Opção **1** → digite `12` ciclos (gera 12 leituras por sensor; algumas falham ou são descartadas de propósito para mostrar as exceções).
2. Opção **2** → `ENTER` (todos os sensores): mostra as duas previsões, a situação e os alertas gravados.
3. Opção **3** → histórico de medições por sensor, alertas emitidos e exportação CSV.
4. Opção **5** → status completo.
5. Feche (`0`), abra de novo e veja na opção **5** que os totais continuam lá.

> Código de acesso para alterar limiares (opção 4 → 5): `DEFESA2026` (configurável em `sistema.properties`).

---

## 🗄️ Banco de dados (configuração para a correção)

- **H2 em modo arquivo**: `jdbc:h2:./dados/territorio` → arquivo `dados/territorio.mv.db` criado na pasta de onde o programa é executado. **Os dados sobrevivem ao reiniciar o programa.**
- **Script SQL:** [`src/main/resources/schema.sql`](src/main/resources/schema.sql) — executado automaticamente na inicialização por `ConexaoBanco.inicializarEsquema()` com `CREATE TABLE IF NOT EXISTS` (nunca apaga o histórico).
- Tabelas: `medicao(id, codigo_sensor, tipo_sensor, valor, data_hora)` e `alerta(id, codigo_sensor, tipo_sensor, nivel, estrategia, valor_previsto, limite, mensagem, data_hora)`.
- Configuração em [`src/main/resources/sistema.properties`](src/main/resources/sistema.properties). Para usar outro banco sem recompilar:
  ```bash
  java -Dbanco.url=jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1 -jar target/territory-predictive-insights.jar
  ```
- Para **zerar o histórico**, basta apagar a pasta `dados/`.

---

## 🗂️ Estrutura do projeto

```
territory-predictive-insights-fev/
├── pom.xml                      (H2, JUnit 6, Mockito 5, JAR executável)
├── mvnw / mvnw.cmd / .mvn/      (Maven Wrapper)
├── src/main/java/
│   ├── dominio/      ComponenteTerritorial (abstrata), PontoMonitoramento,
│   │                 Sensor (interface), SensorSimulado, SensorPluviometro,
│   │                 SensorNivelCorrego, SensorUmidadeSolo, TipoSensor,
│   │                 DadosTerritorio, Limiares, EstrategiaPrevisao (abstrata),
│   │                 PrevisaoMediaMovel, PrevisaoComparacao, ResultadoPrevisao,
│   │                 Medicao, Alerta, NivelAlerta, StatusComponente
│   ├── excecao/      DadosInvalidosException, FonteIndisponivelException,
│   │                 AcessoNegadoException
│   ├── persistencia/ ConexaoBanco, MedicaoDAO, AlertaDAO   (caminho JDBC)
│   ├── servico/      ServicoMonitoramento, FabricaPrevisao (Factory),
│   │                 ConfiguracaoSistema (Singleton), RelatorioBuilder (Builder),
│   │                 Relatorio, AnalisePrevisao, ExportadorCsv (bônus)
│   └── SistemaMonitoramento.java   (menu principal com Scanner)
├── src/main/resources/  schema.sql, sistema.properties
└── src/test/java/
    ├── dominio/      MedicaoTest, PrevisaoMediaMovelTest, PrevisaoComparacaoTest,
    │                 DadosTerritorioTest, ComponenteTerritorialTest
    ├── servico/      ServicoMonitoramentoTest (Mockito), FabricaPrevisaoTest,
    │                 RelatorioBuilderTest
    └── persistencia/ PersistenciaIntegracaoTest (H2 em memória)
```

---

## 📚 Onde está cada conceito pedido

### 1. Domínio orientado a objetos
| Requisito | Onde |
|---|---|
| Classe abstrata `ComponenteTerritorial` (id, nome, região, status) com método abstrato `verificarStatus()` | `dominio/ComponenteTerritorial.java` |
| Classe concreta que herda dela | `PontoMonitoramento` — status OPERANTE / DEGRADADO / INOPERANTE conforme quantos sensores respondem |
| Interface `Sensor` (ler valor, verificar funcionamento, retornar tipo) | `dominio/Sensor.java` — `lerValor()`, `verificarFuncionamento()`, `getTipo()` |
| ≥ 3 implementações coerentes com o recorte | `SensorPluviometro` (mm/h), `SensorNivelCorrego` (cm), `SensorUmidadeSolo` (%) — todas via `SensorSimulado`, que usa `Random` |
| `DadosTerritorio` com atributos privados e código de acesso para alterar limiares | `dominio/DadosTerritorio.java` — setters validados + `alterarLimiar(codigo, tipo, nivel, valor)` |
| Abstrata `EstrategiaPrevisao` com 2 filhos sobrescrevendo `prever()` e usando `super()` | `PrevisaoMediaMovel` e `PrevisaoComparacao`: construtores chamam `super(nome, janela, minimo)` e `prever()` chama `super.prever(historico)` (validação + regra de silêncio) antes do cálculo próprio |
| Escolha da estratégia **sem** `if tipo ==` / `instanceof` | `FabricaPrevisao` usa um catálogo `Map<String, construtor>`; o serviço percorre `List<EstrategiaPrevisao>` e chama `prever()` polimorficamente |

### 2. Exceções customizadas
- **`DadosInvalidosException`** — medição fora de faixa (ex.: *"Medição de chuva negativa no sensor PLU-01: -3,0 mm/h"*), população negativa, área ≤ 0, limiar inválido ou fora de ordem, histórico nulo/misturado, estratégia desconhecida. Lançada em `Medicao` (construtor e `setValor`), `DadosTerritorio` (setters), `Limiares`, `EstrategiaPrevisao`, `FabricaPrevisao`, `ServicoMonitoramento`.
- **`FonteIndisponivelException`** — sensor desligado ou sem resposta (`SensorSimulado.lerValor()`), banco inacessível ou registro corrompido (`ConexaoBanco`, `MedicaoDAO`, `AlertaDAO`).
- **`AcessoNegadoException`** (extra) — código de acesso errado ao alterar limiar.
- **Capturadas no menu** (`SistemaMonitoramento`) com mensagem clara: `[DESCARTADA] ... -> leitura NÃO gravada`, `[FALHA] Sensor NIV-01 ... não respondeu`, `Acesso negado - ...`, `Operação recusada - ...`. Entradas não numéricas também são tratadas (`'abc' não é um número inteiro válido`).
- **Nenhum `catch` vazio**: todo `catch` informa o usuário ou converte o erro em uma exceção com contexto.

### 3. Persistência — caminho **JDBC**
- `MedicaoDAO` e `AlertaDAO` usam **somente `PreparedStatement`** com parâmetros `?` — nenhuma concatenação de dados no SQL (há até um teste que tenta SQL injection e não retorna nada).
- Todos os `Connection`, `PreparedStatement` e `ResultSet` são fechados com **try-with-resources**.
- **Cada medição válida** lida é gravada (`ServicoMonitoramento.registrarLeitura`) e **cada alerta** emitido é gravado (`rodarPrevisoes`).
- Menu → opção 3: **histórico por sensor** (`buscarHistoricoPorSensor`) e **alertas já emitidos** (`listarEmitidos`).
- As previsões rodam **sobre o histórico do banco** (`buscarUltimasEmOrdemCronologica`), não sobre uma lista em memória.

### 4. Padrões criacionais (os três foram aplicados) — justificativa
| Padrão | Classe | Por que **ali** e não em outro lugar |
|---|---|---|
| **Factory** | `servico/FabricaPrevisao` | A estratégia a usar vem da **configuração** (`previsao.estrategias=MEDIA_MOVEL,COMPARACAO`). Sem a fábrica, o serviço precisaria de `if (tipo.equals("MEDIA_MOVEL")) new ...` — exatamente o "if espalhado" que o enunciado proíbe. Com o catálogo `Map`, adicionar uma estratégia é registrar uma linha; serviço e menu não mudam. Não usamos Factory para `Medicao` ou `Alerta` porque eles têm uma única forma de criação: uma fábrica ali seria indireção sem ganho. |
| **Singleton** | `servico/ConfiguracaoSistema` | Os **limiares** precisam ser *os mesmos* para quem emite alertas (`ServicoMonitoramento`), para quem os altera (`DadosTerritorio.alterarLimiar`) e para quem os mostra (relatório). Duas instâncias poderiam divergir e o sistema alertaria com um limite e exibiria outro. Construtor **privado** + `getInstancia()`. Não usamos Singleton nos DAOs/serviço porque eles recebem dependências pelo construtor — isso é o que permite trocá-los por **mocks** nos testes. |
| **Builder** | `servico/RelatorioBuilder` → `Relatorio` | O "status completo" junta muitas partes opcionais (território, componentes, sensores, limiares, estratégias, alertas, totais) vindas de fontes diferentes. Um construtor com 8 parâmetros seria ilegível; o Builder adiciona cada parte quando ela está disponível e valida no `build()` (sem território, não gera). |

O código **chama os padrões** em vez de duplicar a criação: `SistemaMonitoramento.main` usa `ConfiguracaoSistema.getInstancia()` e `new FabricaPrevisao(config).criarEstrategiasConfiguradas()`; `ServicoMonitoramento.gerarStatusCompleto()` usa o `RelatorioBuilder`.

### 5. Testes unitários (40 testes, JUnit 6 + Mockito 5)
- **Todos no padrão AAA**, com comentários `// Arrange`, `// Act`, `// Assert`.
- **`assertThrows` das exceções customizadas**: `MedicaoTest` (chuva negativa, umidade > 100 %, NaN), `DadosTerritorioTest` (população negativa, código errado → `AcessoNegadoException`, limiar fora de ordem), `PrevisaoMediaMovelTest`, `FabricaPrevisaoTest`, `ServicoMonitoramentoTest`.
- **Mockito mockando os DAOs** em `ServicoMonitoramentoTest`: `MedicaoDAO` devolve um histórico controlado e o teste verifica com `verify`/`ArgumentCaptor` que o `AlertaDAO.salvar` foi chamado com nível **CRÍTICO**, valor 60 e limite 50 — **sem banco**. Também verifica que, com menos de 5 leituras, **nenhum** alerta é gravado, e que leitura inválida **não** chega ao `salvar`.
- **Testes de verdade**: valores esperados calculados à mão (ex.: `(30+40+50+60+70)/5 = 50`; comparação `20 + (20 − 10) = 30`). Fizemos um teste de mutação manual — trocar a divisão da média por `(n+1)` deixa **5 testes vermelhos**.
- Extra: `PersistenciaIntegracaoTest` testa os DAOs reais contra um H2 **em memória** (descartável).

---

## 📋 Funcionalidades do menu

```
1 - Verificar sensores (ler, validar e gravar)   -> N ciclos de leitura simulada
2 - Rodar previsões e alertas                    -> 2 estratégias por sensor + alertas
3 - Ver histórico                                -> medições por sensor | alertas emitidos | exportar CSV
4 - Gerenciar dados do território                -> ver dados/limiares, população, área, domicílios,
                                                    alterar limiar (código de acesso), medição manual,
                                                    ligar/desligar sensor (manutenção)
5 - Status completo                              -> relatório montado pelo RelatorioBuilder
0 - Sair
```

**Previsões (sobre o histórico do banco):**
- **Média Móvel** — média das últimas 5 leituras → "nível típico esperado se o padrão recente continuar".
- **Comparação de Períodos** — média das últimas 5 vs. as 5 anteriores; projeta a mesma variação → "está piorando ou melhorando, e até onde vai". Exige 10 leituras.

Cada resultado mostra **valor previsto + significado** (ex.: *"ALERTA - previsão 262,0 cm >= limite 250,0 cm"*).

**Limiares padrão** (em `sistema.properties`, alteráveis pelo menu com código de acesso):

| Sensor | ATENÇÃO | ALERTA | CRÍTICO |
|---|---|---|---|
| Chuva (mm/h) | 10 | 25 | 50 |
| Nível do córrego (cm) | 150 | 250 | 350 (transbordamento) |
| Umidade do solo (%) | 70 | 85 | 95 |

### ⭐ Bônus escolhido: exportação CSV
Menu 3 → 3 exporta o histórico consultado de um sensor para `exportacoes/historico_<SENSOR>_<data>.csv` (separador `;`, vírgula decimal e BOM UTF-8 — abre direto no Excel em português).

---

## 🧭 Decisões da equipe

1. **JDBC puro em vez de Spring Boot**: o enunciado aceita os dois com nota cheia; JDBC deixa explícito o `PreparedStatement` e o `try-with-resources`, que são critérios de avaliação, e o programa sobe em menos de 1 s com `main` simples.
2. **H2 em modo arquivo** (e não em memória): o requisito "os dados sobrevivem ao reiniciar" só é cumprido com o banco gravado em disco. Nos testes de integração usamos H2 em memória para não sujar o banco real.
3. **Exceções verificadas (checked)**: obrigam quem chama a tratar o erro — um dado inválido não consegue "escapar" calado até o banco.
4. **Validação no domínio** (`Medicao`, `DadosTerritorio`, `Limiares`): o objeto inválido nem chega a existir, então nenhum caminho do código consegue gravar lixo.
5. **Sensores com passeio aleatório** (cada leitura parte da anterior) em vez de `Math.random()` solto: gera tendências de alta/queda realistas, para a estratégia de Comparação ter o que comparar. Uma pequena probabilidade de falha e de leitura espúria foi colocada **de propósito** para demonstrar as exceções.
6. **`Random` injetável** nos sensores e **DAOs recebidos pelo construtor** no serviço: é o que permite testes determinísticos e mocks.
7. **Regra de silêncio na classe base** `EstrategiaPrevisao`: abaixo do mínimo de leituras, a previsão é marcada como não confiável e **nenhum alerta é emitido**.
8. **Limiares e alterações de dados do território ficam em memória** durante a execução (voltam ao `sistema.properties` ao reiniciar). O que precisa sobreviver — medições e alertas — está no banco. Persistir a configuração seria a próxima evolução.

---

## 🧠 Reflexão crítica

**Quando o sistema deve se calar?** Quando não há histórico suficiente para sustentar uma previsão. Com 2 ou 3 leituras, uma única pancada de chuva vira "tendência" e o sistema emitiria um alerta CRÍTICO por puro ruído. Por isso a classe base `EstrategiaPrevisao` exige **no mínimo 5 leituras** (e a Comparação exige 10, pois precisa de dois períodos completos); abaixo disso a resposta é *"Sistema em silêncio: há 3 de 5 leituras necessárias"* e **nenhum alerta é gravado** (comportamento coberto por teste). Também deve se calar sobre um sensor desligado ou em falha: a leitura não é inventada nem "completada", ela simplesmente não entra no histórico. Um alerta falso tem custo real — a comunidade deixa de confiar e ignora o próximo, que pode ser verdadeiro.

**O que os dados não representam?** Um pluviômetro por ponto mede a chuva **naquele** ponto, não na bacia inteira: uma chuva forte na cabeceira do córrego, fora da comunidade, pode encher o córrego sem que nenhum pluviômetro local registre nada. Os sensores são simulados, então os números não têm calibração real; os limiares são referências gerais e não consideram o estado dos bueiros, o lixo acumulado, obras recentes ou a capacidade real do canal. Os dados também não mostram **quem** está exposto — uma casa com idosos ou pessoas acamadas precisa de mais tempo de retirada do que o número "640 domicílios" sugere. E média móvel ou comparação de períodos não "enxergam" eventos extremos sem precedente no histórico.

**Por que o alerta apoia a decisão em vez de decidir?** Porque a decisão de retirar famílias de casa, interditar uma passagem ou abrir um abrigo envolve fatores que o sistema não vê: a situação no local, a previsão meteorológica oficial, a disponibilidade de equipes e de transporte, a confiança da comunidade. O sistema entrega **evidência organizada e explicada** — qual sensor, qual estratégia, qual valor previsto, qual limite — e uma ação **sugerida**, para que a coordenação local decida mais rápido e melhor, e possa discordar quando souber algo que os dados não mostram. A responsabilidade continua sendo humana; por isso o menu termina cada rodada lembrando: *"confirme em campo antes de agir"*.

---

## 🤖 Uso de IA

> ⚠️ **Preencham/ajustem esta seção com a verdade do que a equipe fez.**

- **Onde usamos:** assistente de IA (Claude Code, da Anthropic) para estruturar o projeto a partir do enunciado, gerar uma primeira versão das classes, dos testes e deste README, e explicar conceitos (Factory com `Map` de construtores, Mockito com `ArgumentCaptor`, try-with-resources no JDBC).
- **Como revisamos:** _[descrevam aqui — ex.: cada integrante leu e explicou para os outros as classes da sua parte; rodamos `mvnw test` e o menu completo; alteramos X, Y e Z; conferimos cada item do enunciado na seção "Onde está cada conceito pedido"]_.
- Todos os integrantes devem ser capazes de explicar qualquer trecho do código na conversa de correção.

---

## 🧩 Divisão de tarefas

| Integrante | Responsabilidade | Principais arquivos |
|---|---|---|
| _[Integrante 1]_ | **Domínio e exceções** | `dominio/*` (ComponenteTerritorial, PontoMonitoramento, Sensor e implementações, DadosTerritorio, Limiares, Medicao, Alerta), `excecao/*` |
| _[Integrante 2]_ | **Persistência** | `persistencia/*` (ConexaoBanco, MedicaoDAO, AlertaDAO), `schema.sql`, `sistema.properties`, `PersistenciaIntegracaoTest` |
| _[Integrante 3]_ | **Previsões, alertas e testes** | `EstrategiaPrevisao`, `PrevisaoMediaMovel`, `PrevisaoComparacao`, `servico/*` (ServicoMonitoramento, FabricaPrevisao, ConfiguracaoSistema, RelatorioBuilder), testes unitários |
| Todos | Menu, README, revisão cruzada | `SistemaMonitoramento.java`, `README.md` |

Todos os integrantes commitam no GitHub com mensagens claras.

---

## ✅ Checklist do enunciado

- [x] `ComponenteTerritorial` abstrata + `verificarStatus()` abstrato + filha concreta `PontoMonitoramento`
- [x] Interface `Sensor` com 3 implementações (`SensorPluviometro`, `SensorNivelCorrego`, `SensorUmidadeSolo`)
- [x] `DadosTerritorio` com atributos privados e código de acesso para alterar limiares
- [x] `EstrategiaPrevisao` abstrata + `PrevisaoMediaMovel` + `PrevisaoComparacao` sobrescrevendo `prever()` e usando `super`
- [x] Escolha da estratégia sem `if tipo ==` / `instanceof` (Factory com catálogo)
- [x] `DadosInvalidosException` e `FonteIndisponivelException` lançadas em setters e serviços, capturadas no menu com mensagem clara; nenhum `catch` vazio
- [x] JDBC: `MedicaoDAO` e `AlertaDAO` com `PreparedStatement` e try-with-resources
- [x] Cada medição válida e cada alerta gravados; histórico por sensor e alertas listados no menu; dados sobrevivem ao reiniciar
- [x] Factory, Singleton **e** Builder, justificados acima
- [x] ≥ 5 testes AAA (40), ≥ 1 `assertThrows` (vários), ≥ 1 Mockito mockando DAO (8 testes)
- [x] Previsões com valor + significado; níveis ATENÇÃO / ALERTA / CRÍTICO explicados; limiares do `ConfiguracaoSistema`
- [x] Menu com Scanner: verificar sensores · rodar previsões · ver histórico · gerenciar dados do território · status completo
- [x] Reflexão crítica, público beneficiado, decisões, uso de IA e divisão de tarefas neste README
- [x] Configuração do banco (H2 + `schema.sql`) incluída
- [x] Bônus: exportação do histórico em CSV
