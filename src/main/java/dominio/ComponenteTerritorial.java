package dominio;

import excecao.DadosInvalidosException;

/**
 * Abstração de qualquer peça do território que a comunidade monitora
 * (ponto de alagamento, encosta, passagem subterrânea...).
 *
 * Encapsula id, nome, região e status; cada filha decide COMO verificar o
 * próprio status (polimorfismo via método abstrato verificarStatus()).
 */
public abstract class ComponenteTerritorial {

    private String id;
    private String nome;
    private String regiao;
    private StatusComponente status;

    protected ComponenteTerritorial(String id, String nome, String regiao) throws DadosInvalidosException {
        setId(id);
        setNome(nome);
        setRegiao(regiao);
        this.status = StatusComponente.NAO_VERIFICADO;
    }

    /**
     * Verifica a situação atual do componente, atualiza o status interno e o devolve.
     * Cada tipo de componente tem sua própria regra.
     */
    public abstract StatusComponente verificarStatus();

    /** Descrição curta do tipo de componente (usada em relatórios). */
    public abstract String getTipoComponente();

    public String resumo() {
        return String.format("[%s] %s - %s (região %s) | status: %s",
                id, getTipoComponente(), nome, regiao, status.getDescricao());
    }

    public String getId() {
        return id;
    }

    private void setId(String id) throws DadosInvalidosException {
        this.id = exigirTexto(id, "id do componente");
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) throws DadosInvalidosException {
        this.nome = exigirTexto(nome, "nome do componente");
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) throws DadosInvalidosException {
        this.regiao = exigirTexto(regiao, "região do componente");
    }

    public StatusComponente getStatus() {
        return status;
    }

    /** Apenas as filhas atualizam o status, sempre a partir de verificarStatus(). */
    protected void setStatus(StatusComponente status) {
        this.status = status;
    }

    private static String exigirTexto(String valor, String campo) throws DadosInvalidosException {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo '" + campo + "' não pode ser vazio.");
        }
        return valor.trim();
    }
}
