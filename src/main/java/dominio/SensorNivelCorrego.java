package dominio;

import java.util.Random;

/**
 * Régua/sensor ultrassônico de nível do córrego, em cm acima do leito.
 * Faixa realista simulada: 30 a ~450 cm (transbordamento por volta de 350 cm).
 */
public class SensorNivelCorrego extends SensorSimulado {

    public SensorNivelCorrego(String codigo) {
        this(codigo, new Random());
    }

    public SensorNivelCorrego(String codigo, Random random) {
        super(codigo, TipoSensor.NIVEL_CORREGO, 80.0, random, 0.04, 0.03);
    }

    @Override
    protected double gerarProximoValor(double valorAnterior) {
        // o córrego sobe mais rápido do que desce
        double variacao = random.nextDouble() < 0.55 ? Math.abs(ruido(30.0)) : -Math.abs(ruido(18.0));
        return Math.max(30.0, Math.min(450.0, valorAnterior + variacao));
    }

    @Override
    protected double gerarLeituraEspuria() {
        // eco falso do ultrassom: valor acima da altura máxima da régua
        return 1000 + random.nextDouble() * 500;
    }
}
