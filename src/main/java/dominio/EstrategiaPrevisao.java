package dominio;

import excecao.DadosInvalidosException;

import java.util.List;

/**
 * Estratégia de previsão aplicada sobre o histórico de UM sensor
 * (lista em ordem cronológica: da leitura mais antiga para a mais recente).
 *
 * A classe base concentra o que é comum a toda estratégia:
 *  - valida o histórico (nulo, sensores misturados);
 *  - aplica a regra de silêncio (poucas leituras = sem previsão);
 *  - devolve uma previsão ingênua (repetir a última leitura).
 * As filhas sobrescrevem prever(), chamam super.prever() para reaproveitar
 * essas regras e refinam o cálculo com o próprio método.
 */
public abstract class EstrategiaPrevisao {

    private final String nome;
    private final int janela;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String nome, int janela, int minimoLeituras) throws DadosInvalidosException {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("A estratégia de previsão precisa de um nome.");
        }
        if (janela < 1) {
            throw new DadosInvalidosException("Janela de previsão inválida: " + janela + " (mínimo 1 leitura).");
        }
        if (minimoLeituras < 1) {
            throw new DadosInvalidosException("Mínimo de leituras inválido: " + minimoLeituras + ".");
        }
        this.nome = nome;
        this.janela = janela;
        this.minimoLeituras = minimoLeituras;
    }

    /** Explica em uma frase como a estratégia calcula a previsão. */
    public abstract String getDescricaoMetodo();

    /** Quantas leituras a estratégia exige para não se calar. */
    public int getLeiturasNecessarias() {
        return Math.max(janela, minimoLeituras);
    }

    public ResultadoPrevisao prever(List<Medicao> historico) throws DadosInvalidosException {
        if (historico == null) {
            throw new DadosInvalidosException("Histórico de medições nulo: nada a prever.");
        }
        String codigo = historico.isEmpty() ? null : historico.get(0).getCodigoSensor();
        TipoSensor tipo = historico.isEmpty() ? null : historico.get(0).getTipo();
        for (Medicao m : historico) {
            if (!m.getCodigoSensor().equals(codigo)) {
                throw new DadosInvalidosException("Histórico mistura sensores (" + codigo + " e "
                        + m.getCodigoSensor() + "): a previsão deve usar um sensor por vez.");
            }
        }
        if (historico.size() < getLeiturasNecessarias()) {
            return ResultadoPrevisao.insuficiente(nome, codigo, tipo, historico.size(), getLeiturasNecessarias());
        }
        double ultima = historico.get(historico.size() - 1).getValor();
        return ResultadoPrevisao.confiavel(nome, codigo, tipo, ultima, 1, getLeiturasNecessarias(),
                "Previsão ingênua: repete a última leitura.");
    }

    /** Média das leituras no intervalo [inicio, fim) do histórico. */
    protected double media(List<Medicao> historico, int inicio, int fim) {
        double soma = 0;
        for (int i = inicio; i < fim; i++) {
            soma += historico.get(i).getValor();
        }
        return soma / (fim - inicio);
    }

    public String getNome() {
        return nome;
    }

    public int getJanela() {
        return janela;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }

    @Override
    public String toString() {
        return nome + " - " + getDescricaoMetodo();
    }
}
