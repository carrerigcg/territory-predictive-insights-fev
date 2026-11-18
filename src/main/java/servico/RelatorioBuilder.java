package servico;

import dominio.Alerta;
import dominio.ComponenteTerritorial;
import dominio.DadosTerritorio;
import dominio.Limiares;
import dominio.Medicao;
import dominio.Sensor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BUILDER: monta o relatório de "status completo" passo a passo.
 *
 * Por que Builder aqui: o relatório tem muitas partes opcionais (território,
 * componentes, sensores, limiares, alertas, totais) vindas de fontes diferentes.
 * Um construtor com 8 parâmetros seria ilegível e frágil; o Builder deixa cada
 * parte ser adicionada quando estiver disponível e valida tudo no build().
 */
public class RelatorioBuilder {

    private String titulo = "Status completo do território";
    private DadosTerritorio territorio;
    private final List<String> linhasComponentes = new ArrayList<>();
    private final List<String> linhasSensores = new ArrayList<>();
    private final List<String> linhasLimiares = new ArrayList<>();
    private final List<String> linhasAlertas = new ArrayList<>();
    private final List<String> linhasEstrategias = new ArrayList<>();
    private long totalMedicoes = -1;
    private long totalAlertas = -1;

    public RelatorioBuilder comTitulo(String titulo) {
        if (titulo != null && !titulo.isBlank()) {
            this.titulo = titulo;
        }
        return this;
    }

    public RelatorioBuilder comTerritorio(DadosTerritorio territorio) {
        this.territorio = territorio;
        return this;
    }

    public RelatorioBuilder adicionarComponente(ComponenteTerritorial componente) {
        componente.verificarStatus();
        linhasComponentes.add(componente.resumo());
        return this;
    }

    public RelatorioBuilder adicionarSensor(Sensor sensor, long leiturasNoBanco, Medicao ultimaLeitura) {
        String ultima = ultimaLeitura == null ? "sem leituras"
                : String.format("última: %.1f %s em %s", ultimaLeitura.getValor(), sensor.getTipo().getUnidade(),
                ultimaLeitura.getDataHora().format(Medicao.FORMATO_DATA));
        linhasSensores.add(String.format("%-6s %-17s %-12s | %4d leituras no banco | %s",
                sensor.getCodigo(), sensor.getTipo().getDescricao(),
                sensor.verificarFuncionamento() ? "funcionando" : "DESLIGADO", leiturasNoBanco, ultima));
        return this;
    }

    public RelatorioBuilder comLimiares(Collection<Limiares> limiares) {
        for (Limiares l : limiares) {
            linhasLimiares.add(l.toString());
        }
        return this;
    }

    public RelatorioBuilder comEstrategias(Collection<String> descricoes) {
        linhasEstrategias.addAll(descricoes);
        return this;
    }

    public RelatorioBuilder comAlertasRecentes(List<Alerta> alertas) {
        for (Alerta a : alertas) {
            linhasAlertas.add(a.toString());
        }
        return this;
    }

    public RelatorioBuilder comTotais(long totalMedicoes, long totalAlertas) {
        this.totalMedicoes = totalMedicoes;
        this.totalAlertas = totalAlertas;
        return this;
    }

    public Relatorio build() {
        if (territorio == null) {
            throw new IllegalStateException("O relatório precisa dos dados do território (comTerritorio).");
        }
        List<Relatorio.Secao> secoes = new ArrayList<>();
        secoes.add(new Relatorio.Secao("Território", List.of(territorio.toString().split("\\R"))));
        secoes.add(secao("Componentes monitorados", linhasComponentes, "nenhum componente cadastrado"));
        secoes.add(secao("Sensores", linhasSensores, "nenhum sensor cadastrado"));
        secoes.add(secao("Limiares de alerta (ConfiguracaoSistema)", linhasLimiares, "limiares não informados"));
        secoes.add(secao("Estratégias de previsão ativas (FabricaPrevisao)", linhasEstrategias,
                "nenhuma estratégia informada"));
        secoes.add(secao("Alertas mais recentes", linhasAlertas, "nenhum alerta emitido até agora"));
        if (totalMedicoes >= 0) {
            secoes.add(new Relatorio.Secao("Totais no banco", List.of(
                    "Medições gravadas: " + totalMedicoes, "Alertas emitidos: " + totalAlertas)));
        }
        return new Relatorio(titulo, LocalDateTime.now(), secoes);
    }

    private static Relatorio.Secao secao(String titulo, List<String> linhas, String seVazio) {
        return new Relatorio.Secao(titulo, linhas.isEmpty() ? List.of("(" + seVazio + ")") : new ArrayList<>(linhas));
    }
}
