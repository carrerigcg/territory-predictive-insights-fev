package dominio;

/**
 * Resultado de uma estratégia de previsão: valor previsto + o que ele significa.
 * Quando não há leituras suficientes, o resultado é "não confiável" e o sistema se cala.
 */
public class ResultadoPrevisao {

    private final String estrategia;
    private final String codigoSensor;
    private final TipoSensor tipo;
    private final double valorPrevisto;
    private final int leiturasUsadas;
    private final int leiturasNecessarias;
    private final boolean confiavel;
    private final String interpretacao;

    private ResultadoPrevisao(String estrategia, String codigoSensor, TipoSensor tipo, double valorPrevisto,
                              int leiturasUsadas, int leiturasNecessarias, boolean confiavel,
                              String interpretacao) {
        this.estrategia = estrategia;
        this.codigoSensor = codigoSensor;
        this.tipo = tipo;
        this.valorPrevisto = valorPrevisto;
        this.leiturasUsadas = leiturasUsadas;
        this.leiturasNecessarias = leiturasNecessarias;
        this.confiavel = confiavel;
        this.interpretacao = interpretacao;
    }

    public static ResultadoPrevisao confiavel(String estrategia, String codigoSensor, TipoSensor tipo,
                                              double valorPrevisto, int leiturasUsadas,
                                              int leiturasNecessarias, String interpretacao) {
        return new ResultadoPrevisao(estrategia, codigoSensor, tipo, valorPrevisto, leiturasUsadas,
                leiturasNecessarias, true, interpretacao);
    }

    public static ResultadoPrevisao insuficiente(String estrategia, String codigoSensor, TipoSensor tipo,
                                                 int leiturasDisponiveis, int leiturasNecessarias) {
        String texto = String.format("Sistema em silêncio: há %d de %d leituras necessárias no banco. "
                        + "Uma previsão agora seria um chute, então nenhum alerta é emitido.",
                leiturasDisponiveis, leiturasNecessarias);
        return new ResultadoPrevisao(estrategia, codigoSensor, tipo, Double.NaN, leiturasDisponiveis,
                leiturasNecessarias, false, texto);
    }

    public String getEstrategia() {
        return estrategia;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipo() {
        return tipo;
    }

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public int getLeiturasUsadas() {
        return leiturasUsadas;
    }

    public int getLeiturasNecessarias() {
        return leiturasNecessarias;
    }

    public boolean isConfiavel() {
        return confiavel;
    }

    public String getInterpretacao() {
        return interpretacao;
    }

    @Override
    public String toString() {
        if (!confiavel) {
            return String.format("[%s] %s", estrategia, interpretacao);
        }
        return String.format("[%s] previsto: %.1f %s -> %s", estrategia, valorPrevisto,
                tipo == null ? "" : tipo.getUnidade(), interpretacao);
    }
}
