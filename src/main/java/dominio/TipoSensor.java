package dominio;

/**
 * Tipos de sensor do recorte "Clima local: chuva intensa e risco de alagamento".
 * Cada tipo conhece sua unidade e a FAIXA FÍSICA aceitável: um valor fora dela
 * não é "chuva forte", é um erro de leitura (DadosInvalidosException).
 */
public enum TipoSensor {

    PLUVIOMETRO("Chuva", "mm/h", 0.0, 300.0),
    NIVEL_CORREGO("Nível do córrego", "cm", 0.0, 1000.0),
    UMIDADE_SOLO("Umidade do solo", "%", 0.0, 100.0);

    private final String descricao;
    private final String unidade;
    private final double valorMinimo;
    private final double valorMaximo;

    TipoSensor(String descricao, String unidade, double valorMinimo, double valorMaximo) {
        this.descricao = descricao;
        this.unidade = unidade;
        this.valorMinimo = valorMinimo;
        this.valorMaximo = valorMaximo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public double getValorMinimo() {
        return valorMinimo;
    }

    public double getValorMaximo() {
        return valorMaximo;
    }

    /** true se o valor está dentro da faixa física possível para este tipo de sensor. */
    public boolean aceita(double valor) {
        return !Double.isNaN(valor) && !Double.isInfinite(valor)
                && valor >= valorMinimo && valor <= valorMaximo;
    }
}
