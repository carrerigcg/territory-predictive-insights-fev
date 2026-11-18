package servico;

import dominio.Alerta;
import dominio.ResultadoPrevisao;

import java.util.Optional;

/**
 * Uma previsão já avaliada contra os limiares:
 * o resultado, a situação em linguagem simples e o alerta (se houve).
 */
public class AnalisePrevisao {

    private final ResultadoPrevisao resultado;
    private final String situacao;
    private final Alerta alerta;

    public AnalisePrevisao(ResultadoPrevisao resultado, String situacao, Alerta alerta) {
        this.resultado = resultado;
        this.situacao = situacao;
        this.alerta = alerta;
    }

    public ResultadoPrevisao getResultado() {
        return resultado;
    }

    public String getSituacao() {
        return situacao;
    }

    public Optional<Alerta> getAlerta() {
        return Optional.ofNullable(alerta);
    }
}
