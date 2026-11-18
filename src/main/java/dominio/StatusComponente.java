package dominio;

/** Situação operacional de um componente do território. */
public enum StatusComponente {
    OPERANTE("Operante - todos os sensores respondendo"),
    DEGRADADO("Degradado - parte dos sensores sem resposta"),
    INOPERANTE("Inoperante - nenhum sensor respondendo"),
    NAO_VERIFICADO("Ainda não verificado");

    private final String descricao;

    StatusComponente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
