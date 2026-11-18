package dominio;

/**
 * Níveis de alerta, do mais brando ao mais grave.
 * Cada nível já carrega a ação recomendada para HOJE ("o que podemos fazer
 * agora para evitar ou reduzir o impacto?").
 */
public enum NivelAlerta {

    ATENCAO("ATENÇÃO",
            "Acompanhar a evolução, desobstruir bueiros e revisar rotas de fuga das áreas baixas."),
    ALERTA("ALERTA",
            "Avisar as famílias das áreas baixas, preparar o abrigo comunitário e posicionar equipes."),
    CRITICO("CRÍTICO",
            "Acionar a Defesa Civil, iniciar a retirada preventiva e interditar passagens alagáveis.");

    private final String rotulo;
    private final String recomendacao;

    NivelAlerta(String rotulo, String recomendacao) {
        this.rotulo = rotulo;
        this.recomendacao = recomendacao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getRecomendacao() {
        return recomendacao;
    }
}
