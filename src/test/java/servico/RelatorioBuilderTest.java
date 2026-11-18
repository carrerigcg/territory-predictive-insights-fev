package servico;

import dominio.DadosTerritorio;
import dominio.PontoMonitoramento;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioBuilderTest {

    @Test
    @DisplayName("Builder monta o relatório passo a passo com as seções informadas")
    void deveMontarRelatorioPassoAPasso() throws DadosInvalidosException {
        // Arrange
        DadosTerritorio territorio = new DadosTerritorio("Vale do Córrego Verde", "Município exemplo",
                18500, 2.3, 640, "DEFESA2026");
        PontoMonitoramento ponto = new PontoMonitoramento("PM-01", "Margem do córrego", "Baixada Sul", "alaga");

        // Act
        Relatorio relatorio = new RelatorioBuilder()
                .comTitulo("Teste")
                .comTerritorio(territorio)
                .adicionarComponente(ponto)
                .comAlertasRecentes(List.of())
                .comTotais(12, 3)
                .build();

        // Assert
        String texto = relatorio.formatarTexto();
        assertEquals("Teste", relatorio.getTitulo());
        assertTrue(texto.contains("Vale do Córrego Verde"));
        assertTrue(texto.contains("PM-01"));
        assertTrue(texto.contains("Medições gravadas: 12"));
        assertTrue(texto.contains("nenhum alerta emitido"));
    }

    @Test
    @DisplayName("Builder sem território é recusado no build()")
    void buildSemTerritorioDeveFalhar() {
        // Arrange
        RelatorioBuilder builder = new RelatorioBuilder().comTitulo("Incompleto");

        // Act + Assert
        assertThrows(IllegalStateException.class, builder::build);
    }
}
