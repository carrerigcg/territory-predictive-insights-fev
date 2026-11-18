package dominio;

import excecao.DadosInvalidosException;

import java.util.List;

/**
 * Média móvel: a previsão é a média das últimas N leituras do histórico.
 * Suaviza picos isolados e responde "qual o nível típico agora?".
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    public PrevisaoMediaMovel(int janela, int minimoLeituras) throws DadosInvalidosException {
        super("Média Móvel", janela, minimoLeituras);
    }

    @Override
    public String getDescricaoMetodo() {
        return "média das últimas " + getJanela() + " leituras do banco";
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) throws DadosInvalidosException {
        ResultadoPrevisao base = super.prever(historico);   // validação + regra de silêncio
        if (!base.isConfiavel()) {
            return base;
        }
        int fim = historico.size();
        int inicio = fim - getJanela();
        double previsto = media(historico, inicio, fim);
        TipoSensor tipo = base.getTipo();
        String interpretacao = String.format(
                "média das últimas %d leituras; se o padrão recente se mantiver, espera-se cerca de %.1f %s "
                        + "de %s nas próximas horas.",
                getJanela(), previsto, tipo.getUnidade(), tipo.getDescricao().toLowerCase());
        return ResultadoPrevisao.confiavel(getNome(), base.getCodigoSensor(), tipo, previsto, getJanela(),
                getLeiturasNecessarias(), interpretacao);
    }
}
