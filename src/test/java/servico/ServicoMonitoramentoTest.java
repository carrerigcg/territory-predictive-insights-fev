package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.PontoMonitoramento;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import dominio.Sensor;
import dominio.SensorPluviometro;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa a LÓGICA DE ALERTA sem banco: MedicaoDAO e AlertaDAO são mocks do Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    @Mock
    private MedicaoDAO medicaoDAO;

    @Mock
    private AlertaDAO alertaDAO;

    private ServicoMonitoramento servico;

    @BeforeEach
    void montarServico() throws DadosInvalidosException {
        ConfiguracaoSistema config = ConfiguracaoSistema.getInstancia();
        config.restaurarPadroes();   // chuva: ATENÇÃO 10, ALERTA 25, CRÍTICO 50 mm/h
        PontoMonitoramento ponto = new PontoMonitoramento("PM-01", "Margem do córrego", "Baixada Sul", "alaga");
        ponto.adicionarSensor(new SensorPluviometro("PLU-01", new Random(7)));
        DadosTerritorio territorio = new DadosTerritorio("Vale do Córrego Verde", "Município exemplo",
                18500, 2.3, 640, "DEFESA2026");
        servico = new ServicoMonitoramento(medicaoDAO, alertaDAO,
                List.of(new PrevisaoMediaMovel(5, 5), new PrevisaoComparacao(5, 5)),
                config, territorio, List.of(ponto));
    }

    private static List<Medicao> chuva(double... valores) throws DadosInvalidosException {
        List<Medicao> lista = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 17, 14, 0);
        for (int i = 0; i < valores.length; i++) {
            lista.add(new Medicao((long) i + 1, "PLU-01", TipoSensor.PLUVIOMETRO, valores[i], inicio.plusMinutes(i)));
        }
        return lista;
    }

    @Test
    @DisplayName("Chuva prevista de 60 mm/h gera e GRAVA alerta CRÍTICO explicado")
    void deveEmitirEGravarAlertaCritico() throws Exception {
        // Arrange: 5 leituras de 60 mm/h -> média móvel 60 >= limite CRÍTICO 50
        when(medicaoDAO.buscarUltimasEmOrdemCronologica(eq("PLU-01"), anyInt()))
                .thenReturn(chuva(60, 60, 60, 60, 60));

        // Act
        List<AnalisePrevisao> analises = servico.rodarPrevisoes("PLU-01");

        // Assert
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaDAO, times(1)).salvar(captor.capture());   // Comparação se calou (precisa de 10)
        Alerta alerta = captor.getValue();
        assertEquals(NivelAlerta.CRITICO, alerta.getNivel());
        assertEquals(60.0, alerta.getValorPrevisto(), 0.0001);
        assertEquals(50.0, alerta.getLimite(), 0.0001);
        assertTrue(alerta.getMensagem().contains("PLU-01"));
        assertTrue(alerta.getMensagem().contains("Média Móvel"));
        assertEquals(2, analises.size());
    }

    @Test
    @DisplayName("Com menos de 5 leituras no banco nenhum alerta é gravado (sistema se cala)")
    void naoDeveAlertarComHistoricoInsuficiente() throws Exception {
        // Arrange: valores altíssimos, mas só 3 leituras
        when(medicaoDAO.buscarUltimasEmOrdemCronologica(eq("PLU-01"), anyInt()))
                .thenReturn(chuva(110, 115, 120));

        // Act
        List<AnalisePrevisao> analises = servico.rodarPrevisoes("PLU-01");

        // Assert
        verify(alertaDAO, never()).salvar(any());
        assertFalse(analises.get(0).getResultado().isConfiavel());
        assertTrue(analises.get(0).getSituacao().startsWith("SEM PREVISÃO"));
    }

    @Test
    @DisplayName("Chuva fraca prevista fica NORMAL e não gera alerta")
    void naoDeveAlertarAbaixoDoLimiar() throws Exception {
        // Arrange
        when(medicaoDAO.buscarUltimasEmOrdemCronologica(eq("PLU-01"), anyInt()))
                .thenReturn(chuva(2, 3, 2, 3, 2, 3, 2, 3, 2, 3));

        // Act
        List<AnalisePrevisao> analises = servico.rodarPrevisoes("PLU-01");

        // Assert
        verify(alertaDAO, never()).salvar(any());
        assertTrue(analises.stream().allMatch(a -> a.getSituacao().startsWith("NORMAL")));
    }

    @Test
    @DisplayName("Tendência de alta (10 -> 20) faz a Comparação prever 30 e emitir ALERTA")
    void comparacaoDeveEmitirAlertaPorTendencia() throws Exception {
        // Arrange: média móvel = 20 (ATENÇÃO); comparação = 20 + (20 - 10) = 30 (ALERTA)
        when(medicaoDAO.buscarUltimasEmOrdemCronologica(eq("PLU-01"), anyInt()))
                .thenReturn(chuva(10, 10, 10, 10, 10, 20, 20, 20, 20, 20));

        // Act
        List<AnalisePrevisao> analises = servico.rodarPrevisoes("PLU-01");

        // Assert
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaDAO, times(2)).salvar(captor.capture());
        assertEquals(NivelAlerta.ATENCAO, captor.getAllValues().get(0).getNivel());
        assertEquals(NivelAlerta.ALERTA, captor.getAllValues().get(1).getNivel());
        assertEquals(30.0, analises.get(1).getResultado().getValorPrevisto(), 0.0001);
    }

    @Test
    @DisplayName("Leitura inválida do sensor lança DadosInvalidosException e NÃO é gravada")
    void leituraInvalidaNaoDeveSerGravada() throws Exception {
        // Arrange
        Sensor sensorComDefeito = mock(Sensor.class);
        when(sensorComDefeito.getCodigo()).thenReturn("PLU-01");
        when(sensorComDefeito.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(sensorComDefeito.lerValor()).thenReturn(-5.0);

        // Act
        assertThrows(DadosInvalidosException.class, () -> servico.registrarLeitura(sensorComDefeito));

        // Assert
        verify(medicaoDAO, never()).salvar(any());
    }

    @Test
    @DisplayName("Sensor sem resposta propaga FonteIndisponivelException e nada é gravado")
    void sensorIndisponivelNaoDeveGravar() throws Exception {
        // Arrange
        Sensor sensorFora = mock(Sensor.class);
        when(sensorFora.lerValor()).thenThrow(new FonteIndisponivelException("timeout"));

        // Act
        assertThrows(FonteIndisponivelException.class, () -> servico.registrarLeitura(sensorFora));

        // Assert
        verify(medicaoDAO, never()).salvar(any());
    }

    @Test
    @DisplayName("Leitura válida é gravada exatamente uma vez com o valor lido")
    void leituraValidaDeveSerGravada() throws Exception {
        // Arrange
        Sensor sensor = mock(Sensor.class);
        when(sensor.getCodigo()).thenReturn("PLU-01");
        when(sensor.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(sensor.lerValor()).thenReturn(42.0);
        when(medicaoDAO.salvar(any(Medicao.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Medicao gravada = servico.registrarLeitura(sensor);

        // Assert
        verify(medicaoDAO, times(1)).salvar(any(Medicao.class));
        assertEquals(42.0, gravada.getValor(), 0.0001);
        assertEquals("PLU-01", gravada.getCodigoSensor());
    }

    @Test
    @DisplayName("Previsão para sensor inexistente é recusada antes de consultar o banco")
    void sensorInexistenteDeveSerRecusado() throws Exception {
        // Arrange
        String codigoInexistente = "XYZ-99";

        // Act
        assertThrows(DadosInvalidosException.class, () -> servico.rodarPrevisoes(codigoInexistente));

        // Assert
        verify(medicaoDAO, never()).buscarUltimasEmOrdemCronologica(any(), anyInt());
    }
}
