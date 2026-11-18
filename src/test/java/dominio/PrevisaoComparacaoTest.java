package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dominio.PrevisaoMediaMovelTest.historico;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoComparacaoTest {

    @Test
    @DisplayName("Tendência de alta: período anterior média 10, atual média 20 -> projeta 30")
    void deveProjetarTendenciaDeAlta() throws DadosInvalidosException {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(5, 5);
        List<Medicao> dados = historico("PLU-01", TipoSensor.PLUVIOMETRO,
                10, 10, 10, 10, 10, 20, 20, 20, 20, 20);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(dados);

        // Assert
        assertTrue(resultado.isConfiavel());
        assertEquals(30.0, resultado.getValorPrevisto(), 0.0001);
        assertTrue(resultado.getInterpretacao().contains("ALTA"));
    }

    @Test
    @DisplayName("Tendência de queda nunca prevê valor abaixo do mínimo físico (0)")
    void quedaForteDeveSerLimitadaAZero() throws DadosInvalidosException {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(5, 5);
        List<Medicao> dados = historico("PLU-01", TipoSensor.PLUVIOMETRO,
                50, 50, 50, 50, 50, 10, 10, 10, 10, 10);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(dados);

        // Assert  10 + (10 - 50) = -30 -> limitado a 0
        assertEquals(0.0, resultado.getValorPrevisto(), 0.0001);
        assertTrue(resultado.getInterpretacao().contains("QUEDA"));
    }

    @Test
    @DisplayName("Precisa de 2 janelas completas (10 leituras); com 9 se cala")
    void deveExigirDoisPeriodosCompletos() throws DadosInvalidosException {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(5, 5);
        List<Medicao> noveLeituras = historico("NIV-01", TipoSensor.NIVEL_CORREGO,
                100, 110, 120, 130, 140, 150, 160, 170, 180);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(noveLeituras);

        // Assert
        assertEquals(10, estrategia.getLeiturasNecessarias());
        assertFalse(resultado.isConfiavel());
    }

    @Test
    @DisplayName("Polimorfismo: mesmo histórico, estratégias diferentes, previsões diferentes")
    void estrategiasDiferentesDevemPreverValoresDiferentes() throws DadosInvalidosException {
        // Arrange
        List<EstrategiaPrevisao> estrategias = List.of(new PrevisaoMediaMovel(5, 5), new PrevisaoComparacao(5, 5));
        List<Medicao> dados = historico("PLU-01", TipoSensor.PLUVIOMETRO,
                10, 10, 10, 10, 10, 20, 20, 20, 20, 20);

        // Act
        double mediaMovel = estrategias.get(0).prever(dados).getValorPrevisto();
        double comparacao = estrategias.get(1).prever(dados).getValorPrevisto();

        // Assert
        assertEquals(20.0, mediaMovel, 0.0001);
        assertEquals(30.0, comparacao, 0.0001);
        assertNotEquals(mediaMovel, comparacao);
    }
}
