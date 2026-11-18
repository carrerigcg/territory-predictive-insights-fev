package dominio;

import excecao.DadosInvalidosException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Limiares de um tipo de sensor para cada nível de alerta.
 * Invariante protegida: 0 <= ATENÇÃO < ALERTA < CRÍTICO <= máximo físico do sensor.
 */
public class Limiares {

    private final TipoSensor tipo;
    private final Map<NivelAlerta, Double> valores = new EnumMap<>(NivelAlerta.class);

    public Limiares(TipoSensor tipo, double atencao, double alerta, double critico) throws DadosInvalidosException {
        if (tipo == null) {
            throw new DadosInvalidosException("Limiares precisam de um tipo de sensor.");
        }
        this.tipo = tipo;
        validarConjunto(atencao, alerta, critico);
        valores.put(NivelAlerta.ATENCAO, atencao);
        valores.put(NivelAlerta.ALERTA, alerta);
        valores.put(NivelAlerta.CRITICO, critico);
    }

    public double getValor(NivelAlerta nivel) {
        return valores.get(nivel);
    }

    /** Altera um único limiar, mantendo a ordem ATENÇÃO < ALERTA < CRÍTICO. */
    public void definir(NivelAlerta nivel, double novoValor) throws DadosInvalidosException {
        if (nivel == null) {
            throw new DadosInvalidosException("Nível de alerta não informado.");
        }
        Map<NivelAlerta, Double> simulacao = new EnumMap<>(valores);
        simulacao.put(nivel, novoValor);
        validarConjunto(simulacao.get(NivelAlerta.ATENCAO), simulacao.get(NivelAlerta.ALERTA),
                simulacao.get(NivelAlerta.CRITICO));
        valores.put(nivel, novoValor);
    }

    /**
     * Devolve o nível MAIS GRAVE cujo limiar foi atingido pelo valor,
     * ou vazio se o valor está abaixo de ATENÇÃO.
     */
    public Optional<NivelAlerta> classificar(double valor) {
        NivelAlerta[] niveis = NivelAlerta.values();
        for (int i = niveis.length - 1; i >= 0; i--) {
            if (valor >= valores.get(niveis[i])) {
                return Optional.of(niveis[i]);
            }
        }
        return Optional.empty();
    }

    public TipoSensor getTipo() {
        return tipo;
    }

    private void validarConjunto(double atencao, double alerta, double critico) throws DadosInvalidosException {
        validarUnico(atencao, NivelAlerta.ATENCAO);
        validarUnico(alerta, NivelAlerta.ALERTA);
        validarUnico(critico, NivelAlerta.CRITICO);
        if (!(atencao < alerta && alerta < critico)) {
            throw new DadosInvalidosException(String.format(
                    "Limiar inválido para %s: é preciso ATENÇÃO (%.1f) < ALERTA (%.1f) < CRÍTICO (%.1f).",
                    tipo.getDescricao(), atencao, alerta, critico));
        }
    }

    private void validarUnico(double valor, NivelAlerta nivel) throws DadosInvalidosException {
        if (Double.isNaN(valor) || valor < 0 || valor > tipo.getValorMaximo()) {
            throw new DadosInvalidosException(String.format(
                    "Limiar inválido: %s de %s = %.1f %s (deve ficar entre 0 e %.1f %s).",
                    nivel.getRotulo(), tipo.getDescricao(), valor, tipo.getUnidade(),
                    tipo.getValorMaximo(), tipo.getUnidade()));
        }
    }

    @Override
    public String toString() {
        String u = tipo.getUnidade();
        return String.format("%-17s ATENÇÃO >= %6.1f %-4s | ALERTA >= %6.1f %-4s | CRÍTICO >= %6.1f %s",
                tipo.getDescricao(), getValor(NivelAlerta.ATENCAO), u, getValor(NivelAlerta.ALERTA), u,
                getValor(NivelAlerta.CRITICO), u);
    }
}
