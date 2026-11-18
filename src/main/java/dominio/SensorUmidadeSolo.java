package dominio;

import java.util.Random;

/**
 * Sensor capacitivo de umidade do solo (%), instalado em encostas.
 * Solo saturado não absorve a chuva: aumenta enxurrada e risco de deslizamento.
 */
public class SensorUmidadeSolo extends SensorSimulado {

    public SensorUmidadeSolo(String codigo) {
        this(codigo, new Random());
    }

    public SensorUmidadeSolo(String codigo, Random random) {
        super(codigo, TipoSensor.UMIDADE_SOLO, 45.0, random, 0.04, 0.03);
    }

    @Override
    protected double gerarProximoValor(double valorAnterior) {
        // o solo tende a acumular umidade e seca devagar
        double variacao = random.nextDouble() < 0.6 ? Math.abs(ruido(6.0)) : -Math.abs(ruido(3.0));
        return Math.max(20.0, Math.min(100.0, valorAnterior + variacao));
    }

    @Override
    protected double gerarLeituraEspuria() {
        // curto-circuito no sensor: mais de 100% de umidade
        return 100.5 + random.nextDouble() * 50;
    }
}
