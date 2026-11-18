package excecao;

/**
 * Lançada quando alguém tenta alterar os limiares de alerta
 * sem informar o código de acesso correto (ver dominio.DadosTerritorio).
 */
public class AcessoNegadoException extends Exception {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
