package dominio;

import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import servico.ConfiguracaoSistema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DadosTerritorioTest {

    private static final String CODIGO = "DEFESA2026";
    private DadosTerritorio territorio;

    @BeforeEach
    void prepararTerritorio() throws DadosInvalidosException {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
        territorio = new DadosTerritorio("Vale do Córrego Verde", "Município exemplo", 18500, 2.3, 640, CODIGO);
    }

    @AfterEach
    void restaurarConfiguracao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    @DisplayName("População negativa lança DadosInvalidosException e não altera o dado")
    void populacaoNegativaDeveSerRecusada() {
        // Arrange
        int populacaoNegativa = -1;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.setPopulacaoEstimada(populacaoNegativa));

        // Assert
        assertTrue(erro.getMessage().contains("População negativa"));
        assertEquals(18500, territorio.getPopulacaoEstimada());
    }

    @Test
    @DisplayName("Área zero é recusada")
    void areaZeroDeveSerRecusada() {
        // Arrange
        double areaZero = 0.0;

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> territorio.setAreaKm2(areaZero));
    }

    @Test
    @DisplayName("Código de acesso errado impede alterar limiar")
    void codigoErradoNaoAlteraLimiar() {
        // Arrange
        double limiarOriginal = ConfiguracaoSistema.getInstancia()
                .getLimiares(TipoSensor.PLUVIOMETRO).getValor(NivelAlerta.CRITICO);

        // Act
        assertThrows(AcessoNegadoException.class,
                () -> territorio.alterarLimiar("senha-errada", TipoSensor.PLUVIOMETRO, NivelAlerta.CRITICO, 999));

        // Assert
        assertEquals(limiarOriginal, ConfiguracaoSistema.getInstancia()
                .getLimiares(TipoSensor.PLUVIOMETRO).getValor(NivelAlerta.CRITICO), 0.0001);
    }

    @Test
    @DisplayName("Código correto altera o limiar no Singleton ConfiguracaoSistema")
    void codigoCorretoAlteraLimiar() throws Exception {
        // Arrange
        double novoLimiar = 60.0;

        // Act
        territorio.alterarLimiar(CODIGO, TipoSensor.PLUVIOMETRO, NivelAlerta.CRITICO, novoLimiar);

        // Assert
        assertEquals(60.0, ConfiguracaoSistema.getInstancia()
                .getLimiares(TipoSensor.PLUVIOMETRO).getValor(NivelAlerta.CRITICO), 0.0001);
    }

    @Test
    @DisplayName("Limiar que quebra a ordem ATENÇÃO < ALERTA < CRÍTICO é inválido")
    void limiarForaDeOrdemDeveSerRecusado() {
        // Arrange: ALERTA de chuva é 25; ATENÇÃO = 30 inverteria a ordem
        double atencaoInvalida = 30.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiar(CODIGO, TipoSensor.PLUVIOMETRO, NivelAlerta.ATENCAO, atencaoInvalida));

        // Assert
        assertTrue(erro.getMessage().contains("Limiar inválido"));
    }
}
