package excecao;

/**
 * Lançada quando um dado do território não pode ser aceito:
 * medição fora da faixa física do sensor, população negativa,
 * limiar inválido, histórico nulo, estratégia desconhecida etc.
 *
 * É uma exceção verificada (checked): quem chama é obrigado a tratar,
 * o que impede que um dado ruim chegue ao banco "em silêncio".
 */
public class DadosInvalidosException extends Exception {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }

    public DadosInvalidosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
