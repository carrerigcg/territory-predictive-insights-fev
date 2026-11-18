import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.PontoMonitoramento;
import dominio.Sensor;
import dominio.SensorNivelCorrego;
import dominio.SensorPluviometro;
import dominio.SensorUmidadeSolo;
import dominio.TipoSensor;
import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.ConexaoBanco;
import persistencia.MedicaoDAO;
import servico.AnalisePrevisao;
import servico.ConfiguracaoSistema;
import servico.FabricaPrevisao;
import servico.ServicoMonitoramento;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Menu principal (console) do Territory Predictive Insights - recorte CLIMA LOCAL:
 * chuva intensa e risco de alagamento em pontos vulneráveis.
 *
 * É aqui que as exceções customizadas são CAPTURADAS e viram mensagens claras
 * para quem está operando o sistema. Nenhum catch é vazio.
 */
public class SistemaMonitoramento {

    private static final Path PASTA_EXPORTACAO = Paths.get("exportacoes");

    private final Scanner scanner;
    private final ServicoMonitoramento servico;
    private final DadosTerritorio territorio;

    public SistemaMonitoramento(Scanner scanner, ServicoMonitoramento servico, DadosTerritorio territorio) {
        this.scanner = scanner;
        this.servico = servico;
        this.territorio = territorio;
    }

    public static void main(String[] args) {
        ServicoMonitoramento servico;
        DadosTerritorio territorio;
        ConfiguracaoSistema config;
        try {
            config = ConfiguracaoSistema.getInstancia();                       // Singleton
            ConexaoBanco conexao = new ConexaoBanco(config.getUrlBanco(), config.getUsuarioBanco(),
                    config.getSenhaBanco());
            conexao.inicializarEsquema();
            territorio = criarTerritorio(config);
            List<PontoMonitoramento> pontos = criarPontosMonitorados();
            servico = new ServicoMonitoramento(new MedicaoDAO(conexao), new AlertaDAO(conexao),
                    new FabricaPrevisao(config).criarEstrategiasConfiguradas(),   // Factory
                    config, territorio, pontos);
            System.out.println("Banco de dados conectado: " + conexao.getUrl());
        } catch (FonteIndisponivelException e) {
            System.err.println("ERRO AO INICIAR - fonte de dados indisponível: " + e.getMessage());
            System.err.println("Verifique se outra instância do sistema está aberta usando o mesmo banco.");
            return;
        } catch (DadosInvalidosException e) {
            System.err.println("ERRO AO INICIAR - dados de configuração inválidos: " + e.getMessage());
            return;
        } catch (IllegalStateException e) {
            System.err.println("ERRO AO INICIAR - " + e.getMessage());
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new SistemaMonitoramento(scanner, servico, territorio).executar();
        }
    }

    /** Dados do território (fictícios, descritos por região - nenhum dado pessoal). */
    private static DadosTerritorio criarTerritorio(ConfiguracaoSistema config) throws DadosInvalidosException {
        return new DadosTerritorio("Comunidade Vale do Córrego Verde", "Município exemplo - SP",
                18500, 2.3, 640, config.getCodigoAcessoTerritorio());
    }

    /** Pontos vulneráveis a alagamento e seus sensores. */
    private static List<PontoMonitoramento> criarPontosMonitorados() throws DadosInvalidosException {
        PontoMonitoramento margem = new PontoMonitoramento("PM-01", "Margem do córrego (trecho canalizado)",
                "Baixada Sul", "Transborda com chuva forte e alaga as ruas baixas da baixada.");
        margem.adicionarSensor(new SensorPluviometro("PLU-01"));
        margem.adicionarSensor(new SensorNivelCorrego("NIV-01"));

        PontoMonitoramento encosta = new PontoMonitoramento("PM-02", "Encosta da Vila Alta",
                "Morro Norte", "Solo encharcado aumenta a enxurrada e o risco de deslizamento.");
        encosta.adicionarSensor(new SensorPluviometro("PLU-02"));
        encosta.adicionarSensor(new SensorUmidadeSolo("UMI-02"));

        PontoMonitoramento passagem = new PontoMonitoramento("PM-03", "Passagem subterrânea da avenida",
                "Centro", "Acumula água e isola o acesso à UBS da região.");
        passagem.adicionarSensor(new SensorPluviometro("PLU-03"));
        passagem.adicionarSensor(new SensorNivelCorrego("NIV-03"));

        return List.of(margem, encosta, passagem);
    }

    // =====================================================================================
    // Loop principal
    // =====================================================================================

    public void executar() {
        imprimirCabecalho();
        boolean continuar = true;
        while (continuar) {
            try {
                System.out.println();
                System.out.println("================ MENU PRINCIPAL ================");
                System.out.println(" 1 - Verificar sensores (ler, validar e gravar)");
                System.out.println(" 2 - Rodar previsões e alertas");
                System.out.println(" 3 - Ver histórico (medições, alertas, CSV)");
                System.out.println(" 4 - Gerenciar dados do território");
                System.out.println(" 5 - Status completo");
                System.out.println(" 0 - Sair");
                int opcao = lerInteiro("Escolha uma opção: ");
                switch (opcao) {
                    case 1 -> verificarSensores();
                    case 2 -> rodarPrevisoes();
                    case 3 -> menuHistorico();
                    case 4 -> menuTerritorio();
                    case 5 -> statusCompleto();
                    case 0 -> continuar = false;
                    default -> System.out.println("Opção inválida: digite um número de 0 a 5.");
                }
            } catch (NoSuchElementException e) {
                System.out.println("\nEntrada do teclado encerrada - finalizando o sistema.");
                continuar = false;
            }
        }
        System.out.println("Sistema encerrado. O histórico continua salvo no banco para a próxima execução.");
    }

    private void imprimirCabecalho() {
        System.out.println("==============================================================");
        System.out.println("  TERRITORY PREDICTIVE INSIGHTS - Clima local");
        System.out.println("  Chuva intensa e risco de alagamento em pontos vulneráveis");
        System.out.println("==============================================================");
        System.out.println(territorio);
    }

    // =====================================================================================
    // 1 - Verificar sensores
    // =====================================================================================

    private void verificarSensores() {
        int ciclos = lerInteiroNoIntervalo("Quantos ciclos de leitura deseja simular? (1 a 50): ", 1, 50);
        int gravadas = 0;
        int descartadas = 0;
        int falhas = 0;
        for (int ciclo = 1; ciclo <= ciclos; ciclo++) {
            System.out.println("\n--- Ciclo " + ciclo + " de " + ciclos + " ---");
            for (Sensor sensor : servico.getTodosSensores()) {
                try {
                    Medicao medicao = servico.registrarLeitura(sensor);
                    gravadas++;
                    System.out.printf("  [OK]         %-6s %8.1f %-4s gravada (id %d)%n", sensor.getCodigo(),
                            medicao.getValor(), sensor.getTipo().getUnidade(), medicao.getId());
                } catch (DadosInvalidosException e) {
                    descartadas++;
                    System.out.println("  [DESCARTADA] " + e.getMessage() + " -> leitura NÃO gravada.");
                } catch (FonteIndisponivelException e) {
                    falhas++;
                    System.out.println("  [FALHA]      " + e.getMessage());
                }
            }
        }
        System.out.printf("%nResumo: %d medições gravadas, %d descartadas por valor inválido, %d falhas de sensor.%n",
                gravadas, descartadas, falhas);
        for (PontoMonitoramento ponto : servico.getPontos()) {
            ponto.verificarStatus();
            System.out.println("  " + ponto.resumo());
        }
    }

    // =====================================================================================
    // 2 - Previsões e alertas
    // =====================================================================================

    private void rodarPrevisoes() {
        System.out.println("Sensores: " + listarCodigosSensores());
        System.out.print("Código do sensor (ENTER = todos): ");
        String codigo = scanner.nextLine().trim();
        List<Sensor> alvos = codigo.isEmpty() ? servico.getTodosSensores() : null;
        if (alvos == null) {
            try {
                alvos = List.of(servico.buscarSensorObrigatorio(codigo));
            } catch (DadosInvalidosException e) {
                System.out.println("Não foi possível rodar a previsão: " + e.getMessage());
                return;
            }
        }
        int alertasEmitidos = 0;
        for (Sensor sensor : alvos) {
            System.out.println("\n### " + sensor.getCodigo() + " - " + sensor.getTipo().getDescricao()
                    + servico.buscarPontoDoSensor(sensor.getCodigo()).map(p -> " | " + p.getNome()).orElse(""));
            try {
                for (AnalisePrevisao analise : servico.rodarPrevisoes(sensor.getCodigo())) {
                    System.out.println("  " + analise.getResultado());
                    System.out.println("     Situação: " + analise.getSituacao());
                    if (analise.getAlerta().isPresent()) {
                        Alerta alerta = analise.getAlerta().get();
                        alertasEmitidos++;
                        System.out.println("     >>> ALERTA EMITIDO E GRAVADO (#" + alerta.getId() + "): "
                                + alerta.getMensagem());
                    }
                }
            } catch (FonteIndisponivelException e) {
                System.out.println("  Previsão interrompida - fonte indisponível: " + e.getMessage());
            } catch (DadosInvalidosException e) {
                System.out.println("  Previsão interrompida - dados inválidos no histórico: " + e.getMessage());
            }
        }
        System.out.println("\nTotal de alertas emitidos nesta rodada: " + alertasEmitidos);
        System.out.println("Lembrete: o alerta APOIA a decisão da coordenação local; confirme em campo antes de agir.");
    }

    // =====================================================================================
    // 3 - Histórico
    // =====================================================================================

    private void menuHistorico() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println("\n------- HISTÓRICO (banco de dados) -------");
            System.out.println(" 1 - Medições por sensor");
            System.out.println(" 2 - Alertas já emitidos");
            System.out.println(" 3 - Exportar histórico de um sensor para CSV (bônus)");
            System.out.println(" 0 - Voltar");
            switch (lerInteiro("Escolha: ")) {
                case 1 -> listarMedicoesPorSensor();
                case 2 -> listarAlertas();
                case 3 -> exportarCsv();
                case 0 -> voltar = true;
                default -> System.out.println("Opção inválida: digite um número de 0 a 3.");
            }
        }
    }

    private void listarMedicoesPorSensor() {
        System.out.println("Sensores: " + listarCodigosSensores());
        System.out.print("Código do sensor: ");
        String codigo = scanner.nextLine().trim();
        int limite = lerInteiroNoIntervalo("Quantas medições mais recentes? (1 a 500): ", 1, 500);
        try {
            List<Medicao> historico = servico.buscarHistoricoPorSensor(codigo, limite);
            if (historico.isEmpty()) {
                System.out.println("Nenhuma medição gravada para " + codigo.toUpperCase()
                        + ". Use a opção 1 do menu principal para ler os sensores.");
                return;
            }
            System.out.println("\nHistórico de " + codigo.toUpperCase() + " (mais recente primeiro):");
            historico.forEach(m -> System.out.println("  " + m));
        } catch (DadosInvalidosException e) {
            System.out.println("Consulta inválida: " + e.getMessage());
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível consultar o banco: " + e.getMessage());
        }
    }

    private void listarAlertas() {
        int limite = lerInteiroNoIntervalo("Quantos alertas mais recentes? (1 a 500): ", 1, 500);
        try {
            List<Alerta> alertas = servico.listarAlertasEmitidos(limite);
            if (alertas.isEmpty()) {
                System.out.println("Nenhum alerta emitido até agora.");
                return;
            }
            System.out.println("\nAlertas emitidos (mais recente primeiro):");
            alertas.forEach(a -> System.out.println("  " + a));
        } catch (DadosInvalidosException e) {
            System.out.println("Consulta inválida: " + e.getMessage());
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível consultar o banco: " + e.getMessage());
        }
    }

    private void exportarCsv() {
        System.out.println("Sensores: " + listarCodigosSensores());
        System.out.print("Código do sensor: ");
        String codigo = scanner.nextLine().trim();
        int limite = lerInteiroNoIntervalo("Quantas medições mais recentes exportar? (1 a 5000): ", 1, 5000);
        try {
            Path arquivo = servico.exportarHistoricoCsv(codigo, limite, PASTA_EXPORTACAO);
            System.out.println("Histórico exportado para: " + arquivo.toAbsolutePath());
        } catch (DadosInvalidosException e) {
            System.out.println("Exportação não realizada: " + e.getMessage());
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível ler o histórico no banco: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Não foi possível gravar o arquivo CSV: " + e.getMessage());
        }
    }

    // =====================================================================================
    // 4 - Dados do território
    // =====================================================================================

    private void menuTerritorio() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println("\n------- DADOS DO TERRITÓRIO -------");
            System.out.println(" 1 - Ver dados do território e limiares");
            System.out.println(" 2 - Alterar população estimada");
            System.out.println(" 3 - Alterar área (km²)");
            System.out.println(" 4 - Alterar domicílios em área de risco");
            System.out.println(" 5 - Alterar limiar de alerta (exige código de acesso)");
            System.out.println(" 6 - Registrar medição manual (leitura feita em campo)");
            System.out.println(" 7 - Ligar/desligar sensor (manutenção)");
            System.out.println(" 0 - Voltar");
            int opcao = lerInteiro("Escolha: ");
            try {
                switch (opcao) {
                    case 1 -> exibirDadosTerritorio();
                    case 2 -> {
                        territorio.setPopulacaoEstimada(lerInteiro("Nova população estimada: "));
                        System.out.println("População atualizada.");
                    }
                    case 3 -> {
                        territorio.setAreaKm2(lerDecimal("Nova área em km²: "));
                        System.out.println("Área atualizada.");
                    }
                    case 4 -> {
                        territorio.setDomiciliosEmAreaDeRisco(lerInteiro("Domicílios em área de risco: "));
                        System.out.println("Número de domicílios atualizado.");
                    }
                    case 5 -> alterarLimiar();
                    case 6 -> registrarMedicaoManual();
                    case 7 -> alternarSensor();
                    case 0 -> voltar = true;
                    default -> System.out.println("Opção inválida: digite um número de 0 a 7.");
                }
            } catch (DadosInvalidosException e) {
                System.out.println("Operação recusada - " + e.getMessage());
            }
        }
    }

    private void exibirDadosTerritorio() {
        System.out.println(territorio);
        System.out.println("Limiares atuais:");
        ConfiguracaoSistema.getInstancia().getTodosLimiares().values()
                .forEach(l -> System.out.println("  " + l));
    }

    private void alterarLimiar() {
        TipoSensor[] tipos = TipoSensor.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.printf(" %d - %s (%s)%n", i + 1, tipos[i].getDescricao(), tipos[i].getUnidade());
        }
        TipoSensor tipo = tipos[lerInteiroNoIntervalo("Tipo de sensor: ", 1, tipos.length) - 1];
        NivelAlerta[] niveis = NivelAlerta.values();
        for (int i = 0; i < niveis.length; i++) {
            System.out.printf(" %d - %s (atual: %.1f %s)%n", i + 1, niveis[i].getRotulo(),
                    ConfiguracaoSistema.getInstancia().getLimiares(tipo).getValor(niveis[i]), tipo.getUnidade());
        }
        NivelAlerta nivel = niveis[lerInteiroNoIntervalo("Nível: ", 1, niveis.length) - 1];
        double valor = lerDecimal("Novo limiar (" + tipo.getUnidade() + "): ");
        System.out.print("Código de acesso da coordenação: ");
        String codigo = scanner.nextLine();
        try {
            territorio.alterarLimiar(codigo, tipo, nivel, valor);
            System.out.println("Limiar atualizado: " + ConfiguracaoSistema.getInstancia().getLimiares(tipo));
        } catch (AcessoNegadoException e) {
            System.out.println("Acesso negado - " + e.getMessage());
        } catch (DadosInvalidosException e) {
            System.out.println("Limiar recusado - " + e.getMessage());
        }
    }

    private void registrarMedicaoManual() throws DadosInvalidosException {
        System.out.println("Sensores: " + listarCodigosSensores());
        System.out.print("Código do sensor: ");
        String codigo = scanner.nextLine().trim();
        Sensor sensor = servico.buscarSensorObrigatorio(codigo);
        double valor = lerDecimal("Valor medido (" + sensor.getTipo().getUnidade() + "): ");
        try {
            Medicao medicao = servico.registrarMedicaoManual(codigo, valor);
            System.out.println("Medição manual gravada: " + medicao);
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível gravar no banco: " + e.getMessage());
        }
    }

    private void alternarSensor() throws DadosInvalidosException {
        System.out.println("Sensores: " + listarCodigosSensores());
        System.out.print("Código do sensor: ");
        Sensor sensor = servico.buscarSensorObrigatorio(scanner.nextLine().trim());
        sensor.setAtivo(!sensor.verificarFuncionamento());
        System.out.println("Sensor " + sensor.getCodigo() + " agora está "
                + (sensor.verificarFuncionamento() ? "LIGADO." : "DESLIGADO (manutenção)."));
    }

    // =====================================================================================
    // 5 - Status completo
    // =====================================================================================

    private void statusCompleto() {
        try {
            System.out.println(servico.gerarStatusCompleto().formatarTexto());
        } catch (FonteIndisponivelException e) {
            System.out.println("Status incompleto - banco indisponível: " + e.getMessage());
        }
    }

    // =====================================================================================
    // Utilitários de entrada
    // =====================================================================================

    private String listarCodigosSensores() {
        StringBuilder sb = new StringBuilder();
        for (Sensor s : servico.getTodosSensores()) {
            sb.append(s.getCodigo()).append(" (").append(s.getTipo().getDescricao()).append(")  ");
        }
        return sb.toString().trim();
    }

    private int lerInteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim();
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("'" + entrada + "' não é um número inteiro válido. Tente novamente.");
            }
        }
    }

    private int lerInteiroNoIntervalo(String mensagem, int minimo, int maximo) {
        while (true) {
            int valor = lerInteiro(mensagem);
            if (valor >= minimo && valor <= maximo) {
                return valor;
            }
            System.out.println("Valor fora do intervalo permitido (" + minimo + " a " + maximo + ").");
        }
    }

    private double lerDecimal(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim().replace(',', '.');
            try {
                return Double.parseDouble(entrada);
            } catch (NumberFormatException e) {
                System.out.println("'" + entrada + "' não é um número válido (use, por exemplo, 12,5). Tente novamente.");
            }
        }
    }
}
