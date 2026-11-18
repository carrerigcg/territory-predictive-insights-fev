package dominio;

import java.time.LocalDateTime;

/**
 * Alerta emitido quando uma previsão ultrapassa um limiar.
 * Guarda o PORQUÊ: sensor, estratégia, valor previsto e limite estourado.
 */
public class Alerta {

    private Long id;
    private final String codigoSensor;
    private final TipoSensor tipo;
    private final NivelAlerta nivel;
    private final String estrategia;
    private final double valorPrevisto;
    private final double limite;
    private final String mensagem;
    private final LocalDateTime dataHora;

    public Alerta(Long id, String codigoSensor, TipoSensor tipo, NivelAlerta nivel, String estrategia,
                  double valorPrevisto, double limite, String mensagem, LocalDateTime dataHora) {
        if (codigoSensor == null || tipo == null || nivel == null || estrategia == null
                || mensagem == null || dataHora == null) {
            throw new IllegalArgumentException("Alerta incompleto: todos os campos são obrigatórios.");
        }
        this.id = id;
        this.codigoSensor = codigoSensor;
        this.tipo = tipo;
        this.nivel = nivel;
        this.estrategia = estrategia;
        this.valorPrevisto = valorPrevisto;
        this.limite = limite;
        this.mensagem = mensagem;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipo() {
        return tipo;
    }

    public NivelAlerta getNivel() {
        return nivel;
    }

    public String getEstrategia() {
        return estrategia;
    }

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public double getLimite() {
        return limite;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        return String.format("#%s | %s | %-8s | %s", id == null ? "-" : id,
                dataHora.format(Medicao.FORMATO_DATA), nivel.getRotulo(), mensagem);
    }
}
