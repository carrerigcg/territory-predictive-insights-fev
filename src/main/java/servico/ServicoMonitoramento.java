package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.EstrategiaPrevisao;
import dominio.Limiares;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.PontoMonitoramento;
import dominio.ResultadoPrevisao;
import dominio.Sensor;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio do monitoramento: ler sensores, validar e gravar medições,
 * rodar as previsões sobre o histórico do banco, comparar com os limiares e
 * emitir/gravar alertas explicados. Não lê teclado nem imprime nada: o menu
 * (SistemaMonitoramento) é quem conversa com o usuário e captura as exceções.
 */
public class ServicoMonitoramento {

    /** Quantas leituras recentes do banco alimentam as previsões. */
    public static final int LEITURAS_PARA_PREVISAO = 50;

    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final List<EstrategiaPrevisao> estrategias;
    private final ConfiguracaoSistema configuracao;
    private final DadosTerritorio territorio;
    private final List<PontoMonitoramento> pontos;

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, List<EstrategiaPrevisao> estrategias,
                                ConfiguracaoSistema configuracao, DadosTerritorio territorio,
                                List<PontoMonitoramento> pontos) {
        if (medicaoDAO == null || alertaDAO == null || configuracao == null) {
            throw new IllegalArgumentException("Serviço precisa dos DAOs e da configuração.");
        }
        if (estrategias == null || estrategias.isEmpty()) {
            throw new IllegalArgumentException("Serviço precisa de pelo menos uma estratégia de previsão.");
        }
        this.medicaoDAO = medicaoDAO;
        this.alertaDAO = alertaDAO;
        this.estrategias = List.copyOf(estrategias);
        this.configuracao = configuracao;
        this.territorio = territorio;
        this.pontos = pontos == null ? List.of() : List.copyOf(pontos);
    }

    // ------------------------------------------------------------------ sensores e leituras

    /**
     * Lê o sensor, valida a medição e grava no banco.
     *
     * @throws FonteIndisponivelException sensor sem resposta ou banco fora do ar
     * @throws DadosInvalidosException    valor lido fora da faixa física (não é gravado)
     */
    public Medicao registrarLeitura(Sensor sensor) throws FonteIndisponivelException, DadosInvalidosException {
        if (sensor == null) {
            throw new DadosInvalidosException("Sensor não informado para leitura.");
        }
        double valor = sensor.lerValor();
        Medicao medicao = new Medicao(sensor.getCodigo(), sensor.getTipo(), valor);  // valida aqui
        return medicaoDAO.salvar(medicao);
    }

    /** Registro manual (ex.: agente comunitário leu a régua do córrego). Mesma validação da leitura automática. */
    public Medicao registrarMedicaoManual(String codigoSensor, double valor)
            throws DadosInvalidosException, FonteIndisponivelException {
        Sensor sensor = buscarSensorObrigatorio(codigoSensor);
        Medicao medicao = new Medicao(sensor.getCodigo(), sensor.getTipo(), valor);
        return medicaoDAO.salvar(medicao);
    }

    public List<Sensor> getTodosSensores() {
        List<Sensor> sensores = new ArrayList<>();
        for (PontoMonitoramento ponto : pontos) {
            sensores.addAll(ponto.getSensores());
        }
        return Collections.unmodifiableList(sensores);
    }

    public Optional<Sensor> buscarSensor(String codigoSensor) {
        if (codigoSensor == null) {
            return Optional.empty();
        }
        return getTodosSensores().stream()
                .filter(s -> s.getCodigo().equalsIgnoreCase(codigoSensor.trim()))
                .findFirst();
    }

    public Sensor buscarSensorObrigatorio(String codigoSensor) throws DadosInvalidosException {
        return buscarSensor(codigoSensor).orElseThrow(() -> new DadosInvalidosException(
                "Sensor '" + codigoSensor + "' não existe neste território."));
    }

    public Optional<PontoMonitoramento> buscarPontoDoSensor(String codigoSensor) {
        return pontos.stream()
                .filter(p -> p.getSensores().stream().anyMatch(s -> s.getCodigo().equalsIgnoreCase(codigoSensor)))
                .findFirst();
    }

    // ------------------------------------------------------------------ previsões e alertas

    /**
     * Roda TODAS as estratégias configuradas sobre o histórico do banco do sensor,
     * compara cada previsão com os limiares e grava os alertas emitidos.
     */
    public List<AnalisePrevisao> rodarPrevisoes(String codigoSensor)
            throws FonteIndisponivelException, DadosInvalidosException {
        Sensor sensor = buscarSensorObrigatorio(codigoSensor);
        List<Medicao> historico = medicaoDAO.buscarUltimasEmOrdemCronologica(sensor.getCodigo(),
                LEITURAS_PARA_PREVISAO);
        List<AnalisePrevisao> analises = new ArrayList<>();
        for (EstrategiaPrevisao estrategia : estrategias) {
            ResultadoPrevisao resultado = estrategia.prever(historico);     // polimorfismo
            Optional<Alerta> alerta = avaliarAlerta(resultado, sensor);
            if (alerta.isPresent()) {
                alertaDAO.salvar(alerta.get());
            }
            analises.add(new AnalisePrevisao(resultado, descreverSituacao(resultado, sensor.getTipo()),
                    alerta.orElse(null)));
        }
        return analises;
    }

    /**
     * Compara a previsão com os limiares do ConfiguracaoSistema.
     * Sem previsão confiável (poucas leituras) o sistema se cala: nenhum alerta.
     */
    public Optional<Alerta> avaliarAlerta(ResultadoPrevisao resultado, Sensor sensor) {
        if (resultado == null || !resultado.isConfiavel()) {
            return Optional.empty();
        }
        Limiares limiares = configuracao.getLimiares(sensor.getTipo());
        return limiares.classificar(resultado.getValorPrevisto())
                .map(nivel -> criarAlerta(resultado, sensor, nivel, limiares.getValor(nivel)));
    }

    private Alerta criarAlerta(ResultadoPrevisao resultado, Sensor sensor, NivelAlerta nivel, double limite) {
        TipoSensor tipo = sensor.getTipo();
        String local = buscarPontoDoSensor(sensor.getCodigo())
                .map(p -> p.getNome() + " / região " + p.getRegiao())
                .orElse("local não cadastrado");
        String mensagem = String.format(
                "%s: sensor %s (%s, %s) - a estratégia %s prevê %.1f %s, atingindo o limite de %s (%.1f %s). "
                        + "Ação sugerida: %s",
                nivel.getRotulo(), sensor.getCodigo(), tipo.getDescricao(), local, resultado.getEstrategia(),
                resultado.getValorPrevisto(), tipo.getUnidade(), nivel.getRotulo(), limite, tipo.getUnidade(),
                nivel.getRecomendacao());
        return new Alerta(null, sensor.getCodigo(), tipo, nivel, resultado.getEstrategia(),
                resultado.getValorPrevisto(), limite, mensagem, LocalDateTime.now());
    }

    /** Traduz a previsão em linguagem simples: "o que esse número significa?". */
    public String descreverSituacao(ResultadoPrevisao resultado, TipoSensor tipo) {
        if (!resultado.isConfiavel()) {
            return "SEM PREVISÃO - dados insuficientes; o sistema não emite alerta com base em chute.";
        }
        Limiares limiares = configuracao.getLimiares(tipo);
        double previsto = resultado.getValorPrevisto();
        return limiares.classificar(previsto)
                .map(nivel -> String.format("%s - previsão %.1f %s >= limite %.1f %s.", nivel.getRotulo(),
                        previsto, tipo.getUnidade(), limiares.getValor(nivel), tipo.getUnidade()))
                .orElse(String.format("NORMAL - previsão %.1f %s abaixo do limite de ATENÇÃO (%.1f %s).",
                        previsto, tipo.getUnidade(), limiares.getValor(NivelAlerta.ATENCAO), tipo.getUnidade()));
    }

    // ------------------------------------------------------------------ histórico

    public List<Medicao> buscarHistoricoPorSensor(String codigoSensor, int limite)
            throws FonteIndisponivelException, DadosInvalidosException {
        if (limite < 1) {
            throw new DadosInvalidosException("A quantidade de registros deve ser pelo menos 1.");
        }
        Sensor sensor = buscarSensorObrigatorio(codigoSensor);
        return medicaoDAO.buscarHistoricoPorSensor(sensor.getCodigo(), limite);
    }

    public List<Alerta> listarAlertasEmitidos(int limite) throws FonteIndisponivelException, DadosInvalidosException {
        if (limite < 1) {
            throw new DadosInvalidosException("A quantidade de registros deve ser pelo menos 1.");
        }
        return alertaDAO.listarEmitidos(limite);
    }

    /** BÔNUS: exporta o histórico consultado do sensor para CSV. */
    public Path exportarHistoricoCsv(String codigoSensor, int limite, Path pastaDestino)
            throws FonteIndisponivelException, DadosInvalidosException, IOException {
        List<Medicao> historico = buscarHistoricoPorSensor(codigoSensor, limite);
        if (historico.isEmpty()) {
            throw new DadosInvalidosException("O sensor " + codigoSensor.toUpperCase()
                    + " ainda não tem medições gravadas para exportar.");
        }
        return new ExportadorCsv().exportarHistorico(codigoSensor.toUpperCase(), historico, pastaDestino);
    }

    // ------------------------------------------------------------------ status completo

    /** Monta o relatório de status completo usando o RelatorioBuilder. */
    public Relatorio gerarStatusCompleto() throws FonteIndisponivelException {
        RelatorioBuilder builder = new RelatorioBuilder()
                .comTitulo("Status completo - Territory Predictive Insights")
                .comTerritorio(territorio)
                .comLimiares(configuracao.getTodosLimiares().values());
        for (PontoMonitoramento ponto : pontos) {
            builder.adicionarComponente(ponto);
        }
        for (Sensor sensor : getTodosSensores()) {
            builder.adicionarSensor(sensor, medicaoDAO.contarPorSensor(sensor.getCodigo()),
                    medicaoDAO.buscarUltimaPorSensor(sensor.getCodigo()).orElse(null));
        }
        List<String> descricoes = new ArrayList<>();
        for (EstrategiaPrevisao e : estrategias) {
            descricoes.add(e + " (exige " + e.getLeiturasNecessarias() + " leituras)");
        }
        return builder.comEstrategias(descricoes)
                .comAlertasRecentes(alertaDAO.listarEmitidos(5))
                .comTotais(medicaoDAO.contarTodas(), alertaDAO.contarTodos())
                .build();
    }

    public List<EstrategiaPrevisao> getEstrategias() {
        return estrategias;
    }

    public List<PontoMonitoramento> getPontos() {
        return pontos;
    }

    public DadosTerritorio getTerritorio() {
        return territorio;
    }
}
