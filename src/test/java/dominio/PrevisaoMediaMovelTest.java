package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoMediaMovelTest {

    /** Cria um histórico cronológico (mais antigo primeiro) para o sensor informado. */
    static List<Medicao> historico(String codigo, TipoSensor tipo, double... valores) throws DadosInvalidosException {
        List<Medicao> lista = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 17, 8, 0);
        for (int i = 0; i < valores.length; i++) {
            lista.add(new Medicao((long) i + 1, codigo, tipo, valores[i], inicio.plusMinutes(10L * i)));
        }
        return lista;
    }

    @Test
    @DisplayName("Prevê a média das ÚLTIMAS 5 leituras, ignorando as mais antigas")
    void deveCalcularMediaDasUltimasCincoLeituras() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);
        List<Medicao> dados = historico("PLU-01", TipoSensor.PLUVIOMETRO, 10, 20, 30, 40, 50, 60, 70);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(dados);

        // Assert  (30+40+50+60+70)/5 = 50
        assertTrue(resultado.isConfiavel());
        assertEquals(50.0, resultado.getValorPrevisto(), 0.0001);
        assertEquals("PLU-01", resultado.getCodigoSensor());
        assertEquals(5, resultado.getLeiturasUsadas());
    }

    @Test
    @DisplayName("Com menos de 5 leituras no banco o sistema se cala (previsão não confiável)")
    void deveSeCalarComPoucasLeituras() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);
        List<Medicao> poucas = historico("PLU-01", TipoSensor.PLUVIOMETRO, 80, 90, 100);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(poucas);

        // Assert
        assertFalse(resultado.isConfiavel());
        assertTrue(Double.isNaN(resultado.getValorPrevisto()));
        assertTrue(resultado.getInterpretacao().contains("silêncio"));
    }

    @Test
    @DisplayName("Histórico nulo lança DadosInvalidosException")
    void deveLancarExcecaoParaHistoricoNulo() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> estrategia.prever(null));
    }

    @Test
    @DisplayName("Histórico misturando sensores diferentes é recusado")
    void deveRecusarHistoricoComSensoresMisturados() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);
        List<Medicao> dados = historico("PLU-01", TipoSensor.PLUVIOMETRO, 1, 2, 3, 4);
        dados.addAll(historico("PLU-02", TipoSensor.PLUVIOMETRO, 5));

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> estrategia.prever(dados));

        // Assert
        assertTrue(erro.getMessage().contains("mistura sensores"));
    }

    @Test
    @DisplayName("Janela zero é configuração inválida")
    void deveRecusarJanelaInvalida() {
        // Arrange
        int janelaInvalida = 0;

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new PrevisaoMediaMovel(janelaInvalida, 5));
    }
}
