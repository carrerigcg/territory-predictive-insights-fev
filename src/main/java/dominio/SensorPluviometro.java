package dominio;

import java.util.Random;

/**
 * Pluviômetro: intensidade de chuva em mm/h.
 * Faixa realista simulada: 0 a ~120 mm/h, com pancadas ocasionais.
 */
public class SensorPluviometro extends SensorSimulado {

    public SensorPluviometro(String codigo) {
        this(codigo, new Random());
    }

    public SensorPluviometro(String codigo, Random random) {
        super(codigo, TipoSensor.PLUVIOMETRO, 5.0, random, 0.04, 0.03);
    }

    @Override
    protected double gerarProximoValor(double valorAnterior) {
        double proximo = valorAnterior + ruido(6.0);
        if (random.nextDouble() < 0.12) {
            proximo += 15 + random.nextDouble() * 25;   // pancada de chuva
        }
        if (random.nextDouble() < 0.10) {
            proximo *= 0.4;                              // estiagem repentina
        }
        return Math.min(proximo, 120.0);
    }
}
