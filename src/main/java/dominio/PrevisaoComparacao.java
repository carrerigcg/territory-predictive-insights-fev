package dominio;

import excecao.DadosInvalidosException;

import java.util.List;

/**
 * Comparação com o período anterior: compara a média das últimas N leituras
 * com a média das N anteriores e projeta a mesma variação para frente.
 * Responde "a situação está piorando ou melhorando, e para onde vai?".
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    /** Variação (em %) abaixo da qual consideramos a tendência estável. */
    private static final double TOLERANCIA_ESTAVEL = 5.0;

    public PrevisaoComparacao(int janela, int minimoLeituras) throws DadosInvalidosException {
        super("Comparação de Períodos", janela, minimoLeituras);
    }

    @Override
    public String getDescricaoMetodo() {
        return "compara a média das últimas " + getJanela() + " leituras com as " + getJanela()
                + " anteriores e projeta a tendência";
    }

    /** Precisa de dois períodos completos para comparar. */
    @Override
    public int getLeiturasNecessarias() {
        return Math.max(2 * getJanela(), super.getLeiturasNecessarias());
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) throws DadosInvalidosException {
        ResultadoPrevisao base = super.prever(historico);   // validação + regra de silêncio
        if (!base.isConfiavel()) {
            return base;
        }
        int fim = historico.size();
        double mediaAtual = media(historico, fim - getJanela(), fim);
        double mediaAnterior = media(historico, fim - 2 * getJanela(), fim - getJanela());
        double variacao = mediaAtual - mediaAnterior;
        TipoSensor tipo = base.getTipo();
        double previsto = Math.max(tipo.getValorMinimo(), Math.min(tipo.getValorMaximo(), mediaAtual + variacao));

        double percentual = mediaAnterior == 0 ? (mediaAtual == 0 ? 0 : 100) : variacao / mediaAnterior * 100;
        String tendencia = Math.abs(percentual) < TOLERANCIA_ESTAVEL ? "ESTÁVEL"
                : (variacao > 0 ? "de ALTA" : "de QUEDA");
        String interpretacao = String.format(
                "período atual (média %.1f) vs. anterior (média %.1f): tendência %s (%+.1f %s, %+.0f%%). "
                        + "Mantida a variação, %s deve chegar a %.1f %s.",
                mediaAtual, mediaAnterior, tendencia, variacao, tipo.getUnidade(), percentual,
                tipo.getDescricao().toLowerCase(), previsto, tipo.getUnidade());
        return ResultadoPrevisao.confiavel(getNome(), base.getCodigoSensor(), tipo, previsto, 2 * getJanela(),
                getLeiturasNecessarias(), interpretacao);
    }
}
