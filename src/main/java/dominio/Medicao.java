package dominio;

import excecao.DadosInvalidosException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Uma leitura de sensor. Só existe se for válida: o construtor e o setter
 * barram valores fora da faixa física do tipo de sensor com DadosInvalidosException.
 */
public class Medicao {

    public static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Long id;
    private final String codigoSensor;
    private final TipoSensor tipo;
    private double valor;
    private final LocalDateTime dataHora;

    /** Nova medição lida agora (ainda sem id do banco). */
    public Medicao(String codigoSensor, TipoSensor tipo, double valor) throws DadosInvalidosException {
        this(null, codigoSensor, tipo, valor, LocalDateTime.now());
    }

    /** Medição completa (usada pelo DAO ao ler o histórico do banco). */
    public Medicao(Long id, String codigoSensor, TipoSensor tipo, double valor, LocalDateTime dataHora)
            throws DadosInvalidosException {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("Medição sem código de sensor.");
        }
        if (tipo == null) {
            throw new DadosInvalidosException("Medição do sensor " + codigoSensor + " sem tipo de sensor.");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("Medição do sensor " + codigoSensor + " sem data/hora.");
        }
        this.id = id;
        this.codigoSensor = codigoSensor.trim().toUpperCase();
        this.tipo = tipo;
        this.dataHora = dataHora;
        setValor(valor);
    }

    public void setValor(double valor) throws DadosInvalidosException {
        validarValor(tipo, codigoSensor, valor);
        this.valor = valor;
    }

    /** Regra de validação única, reaproveitada pelo serviço na entrada manual. */
    public static void validarValor(TipoSensor tipo, String codigoSensor, double valor) throws DadosInvalidosException {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new DadosInvalidosException("Medição de " + tipo.getDescricao().toLowerCase()
                    + " do sensor " + codigoSensor + " não é um número válido.");
        }
        if (valor < tipo.getValorMinimo()) {
            throw new DadosInvalidosException(String.format(
                    "Medição de %s negativa no sensor %s: %.1f %s (mínimo físico %.1f %s).",
                    tipo.getDescricao().toLowerCase(), codigoSensor, valor, tipo.getUnidade(),
                    tipo.getValorMinimo(), tipo.getUnidade()));
        }
        if (valor > tipo.getValorMaximo()) {
            throw new DadosInvalidosException(String.format(
                    "Medição de %s fora de faixa no sensor %s: %.1f %s (máximo físico %.1f %s).",
                    tipo.getDescricao().toLowerCase(), codigoSensor, valor, tipo.getUnidade(),
                    tipo.getValorMaximo(), tipo.getUnidade()));
        }
    }

    public Long getId() {
        return id;
    }

    /** Chamado pelo DAO depois do INSERT, com a chave gerada pelo banco. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipo() {
        return tipo;
    }

    public double getValor() {
        return valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        return String.format("#%s | %s | %-6s | %8.1f %s", id == null ? "-" : id,
                dataHora.format(FORMATO_DATA), codigoSensor, valor, tipo.getUnidade());
    }
}
