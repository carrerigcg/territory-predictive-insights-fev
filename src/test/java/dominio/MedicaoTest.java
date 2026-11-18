package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicaoTest {

    @Test
    @DisplayName("Medição dentro da faixa física é criada com os dados informados")
    void deveCriarMedicaoValida() throws DadosInvalidosException {
        // Arrange
        String codigo = "plu-01";
        double valor = 32.5;

        // Act
        Medicao medicao = new Medicao(codigo, TipoSensor.PLUVIOMETRO, valor);

        // Assert
        assertEquals("PLU-01", medicao.getCodigoSensor());
        assertEquals(TipoSensor.PLUVIOMETRO, medicao.getTipo());
        assertEquals(32.5, medicao.getValor(), 0.0001);
        assertNotNull(medicao.getDataHora());
    }

    @Test
    @DisplayName("Chuva negativa lança DadosInvalidosException com mensagem explicativa")
    void deveLancarExcecaoParaChuvaNegativa() {
        // Arrange
        double chuvaNegativa = -3.2;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, chuvaNegativa));

        // Assert
        assertTrue(erro.getMessage().contains("chuva negativa"), erro.getMessage());
        assertTrue(erro.getMessage().contains("PLU-01"), erro.getMessage());
    }

    @Test
    @DisplayName("Umidade do solo acima de 100% é rejeitada como fora de faixa")
    void deveLancarExcecaoParaUmidadeAcimaDeCemPorCento() {
        // Arrange
        double umidadeImpossivel = 130.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("UMI-02", TipoSensor.UMIDADE_SOLO, umidadeImpossivel));

        // Assert
        assertTrue(erro.getMessage().contains("fora de faixa"), erro.getMessage());
    }

    @Test
    @DisplayName("Valor NaN (sensor com defeito) é rejeitado")
    void deveLancarExcecaoParaValorNaoNumerico() {
        // Arrange
        double valorCorrompido = Double.NaN;

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, valorCorrompido));
    }

    @Test
    @DisplayName("Setter recusa valor inválido e preserva o valor anterior")
    void setterInvalidoNaoAlteraValor() throws DadosInvalidosException {
        // Arrange
        Medicao medicao = new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, 120.0);

        // Act
        assertThrows(DadosInvalidosException.class, () -> medicao.setValor(-10));

        // Assert
        assertEquals(120.0, medicao.getValor(), 0.0001);
    }
}
