package dominio;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponenteTerritorialTest {

    @Test
    @DisplayName("Ponto com um sensor desligado fica DEGRADADO; todos desligados, INOPERANTE")
    void statusDependeDosSensoresFuncionando() throws DadosInvalidosException {
        // Arrange
        PontoMonitoramento ponto = new PontoMonitoramento("PM-01", "Margem do córrego", "Baixada Sul", "alaga");
        SensorPluviometro chuva = new SensorPluviometro("PLU-01", new Random(1));
        SensorNivelCorrego nivel = new SensorNivelCorrego("NIV-01", new Random(2));
        ponto.adicionarSensor(chuva);
        ponto.adicionarSensor(nivel);

        // Act
        StatusComponente todosLigados = ponto.verificarStatus();
        nivel.setAtivo(false);
        StatusComponente umDesligado = ponto.verificarStatus();
        chuva.setAtivo(false);
        StatusComponente todosDesligados = ponto.verificarStatus();

        // Assert
        assertEquals(StatusComponente.OPERANTE, todosLigados);
        assertEquals(StatusComponente.DEGRADADO, umDesligado);
        assertEquals(StatusComponente.INOPERANTE, todosDesligados);
    }

    @Test
    @DisplayName("Sensor desligado lança FonteIndisponivelException ao ser lido")
    void sensorDesligadoLancaFonteIndisponivel() {
        // Arrange
        Sensor sensor = new SensorUmidadeSolo("UMI-02", new Random(3));
        sensor.setAtivo(false);

        // Act + Assert
        assertThrows(FonteIndisponivelException.class, sensor::lerValor);
    }

    @Test
    @DisplayName("Componente sem nome é recusado")
    void componenteSemNomeEhRecusado() {
        // Arrange
        String nomeVazio = "  ";

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new PontoMonitoramento("PM-09", nomeVazio, "Centro", ""));
    }

    @Test
    @DisplayName("Limiares classificam no nível MAIS GRAVE atingido")
    void limiaresClassificamNivelMaisGrave() throws DadosInvalidosException {
        // Arrange
        Limiares limiares = new Limiares(TipoSensor.PLUVIOMETRO, 10, 25, 50);

        // Act
        Optional<NivelAlerta> normal = limiares.classificar(5);
        Optional<NivelAlerta> atencao = limiares.classificar(10);
        Optional<NivelAlerta> critico = limiares.classificar(80);

        // Assert
        assertTrue(normal.isEmpty());
        assertEquals(NivelAlerta.ATENCAO, atencao.orElseThrow());
        assertEquals(NivelAlerta.CRITICO, critico.orElseThrow());
    }
}
