package dominio;

import excecao.FonteIndisponivelException;

import java.util.Random;

/**
 * Base comum dos sensores simulados com {@link Random}.
 *
 * Os valores seguem um "passeio aleatório" (cada leitura parte da anterior),
 * o que gera tendências de alta/queda realistas. Para exercitar as exceções,
 * o sensor pode, raramente:
 *  - não responder (FonteIndisponivelException);
 *  - devolver uma leitura espúria fora da faixa física (que o serviço barra com
 *    DadosInvalidosException ao criar a Medicao).
 */
public abstract class SensorSimulado implements Sensor {

    private final String codigo;
    private final TipoSensor tipo;
    protected final Random random;
    private final double probabilidadeFalha;
    private final double probabilidadeLeituraEspuria;
    private boolean ativo = true;
    private double ultimoValor;

    protected SensorSimulado(String codigo, TipoSensor tipo, double valorInicial, Random random,
                             double probabilidadeFalha, double probabilidadeLeituraEspuria) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do sensor não pode ser vazio.");
        }
        if (tipo == null || random == null) {
            throw new IllegalArgumentException("Tipo e gerador aleatório do sensor são obrigatórios.");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.tipo = tipo;
        this.ultimoValor = valorInicial;
        this.random = random;
        this.probabilidadeFalha = probabilidadeFalha;
        this.probabilidadeLeituraEspuria = probabilidadeLeituraEspuria;
    }

    /** Calcula a próxima leitura "realista" a partir da anterior. */
    protected abstract double gerarProximoValor(double valorAnterior);

    /** Leitura espúria típica do equipamento (ex.: valor negativo por descalibração). */
    protected double gerarLeituraEspuria() {
        return -(1 + random.nextDouble() * 10);
    }

    @Override
    public double lerValor() throws FonteIndisponivelException {
        if (!verificarFuncionamento()) {
            throw new FonteIndisponivelException("Sensor " + codigo + " (" + tipo.getDescricao()
                    + ") está desligado/em manutenção e não pode ser lido.");
        }
        if (random.nextDouble() < probabilidadeFalha) {
            throw new FonteIndisponivelException("Sensor " + codigo + " (" + tipo.getDescricao()
                    + ") não respondeu à leitura (timeout de comunicação).");
        }
        if (random.nextDouble() < probabilidadeLeituraEspuria) {
            return arredondar(gerarLeituraEspuria());
        }
        ultimoValor = limitar(gerarProximoValor(ultimoValor));
        return arredondar(ultimoValor);
    }

    @Override
    public boolean verificarFuncionamento() {
        return ativo;
    }

    /** Liga/desliga o sensor (simula manutenção). */
    @Override
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    @Override
    public TipoSensor getTipo() {
        return tipo;
    }

    /** Gaussiana com média 0 e desvio informado. */
    protected double ruido(double desvio) {
        return random.nextGaussian() * desvio;
    }

    private double limitar(double valor) {
        return Math.max(tipo.getValorMinimo(), Math.min(tipo.getValorMaximo(), valor));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    @Override
    public String toString() {
        return codigo + " (" + tipo.getDescricao() + ", " + tipo.getUnidade() + ")"
                + (ativo ? "" : " [DESLIGADO]");
    }
}
