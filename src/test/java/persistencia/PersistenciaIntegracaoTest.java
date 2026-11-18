package persistencia;

import dominio.Alerta;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de INTEGRAÇÃO dos DAOs com um H2 em memória (banco descartável, um por teste).
 * Os testes unitários da lógica (ServicoMonitoramentoTest) não dependem de banco.
 */
class PersistenciaIntegracaoTest {

    private MedicaoDAO medicaoDAO;
    private AlertaDAO alertaDAO;

    @BeforeEach
    void criarBancoEmMemoria() throws Exception {
        ConexaoBanco conexao = new ConexaoBanco("jdbc:h2:mem:teste_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa", "");
        conexao.inicializarEsquema();
        medicaoDAO = new MedicaoDAO(conexao);
        alertaDAO = new AlertaDAO(conexao);
    }

    @Test
    @DisplayName("Medições gravadas voltam do banco com id e em ordem cronológica para a previsão")
    void deveGravarEListarMedicoes() throws Exception {
        // Arrange
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 17, 8, 0);
        medicaoDAO.salvar(new Medicao(null, "PLU-01", TipoSensor.PLUVIOMETRO, 10, t0));
        medicaoDAO.salvar(new Medicao(null, "PLU-01", TipoSensor.PLUVIOMETRO, 20, t0.plusMinutes(10)));
        Medicao ultima = medicaoDAO.salvar(new Medicao(null, "PLU-01", TipoSensor.PLUVIOMETRO, 30, t0.plusMinutes(20)));
        medicaoDAO.salvar(new Medicao(null, "NIV-01", TipoSensor.NIVEL_CORREGO, 200, t0));

        // Act
        List<Medicao> recentes = medicaoDAO.buscarHistoricoPorSensor("PLU-01", 10);
        List<Medicao> cronologica = medicaoDAO.buscarUltimasEmOrdemCronologica("PLU-01", 10);

        // Assert
        assertNotNull(ultima.getId());
        assertEquals(3, medicaoDAO.contarPorSensor("PLU-01"));
        assertEquals(4, medicaoDAO.contarTodas());
        assertEquals(30.0, recentes.get(0).getValor(), 0.0001);
        assertEquals(10.0, cronologica.get(0).getValor(), 0.0001);
    }

    @Test
    @DisplayName("PreparedStatement: tentativa de SQL injection no código do sensor não retorna nada")
    void sqlInjectionNaoFunciona() throws Exception {
        // Arrange
        medicaoDAO.salvar(new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, 12));

        // Act
        List<Medicao> resultado = medicaoDAO.buscarHistoricoPorSensor("X' OR '1'='1", 10);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Alertas gravados são listados do mais recente para o mais antigo")
    void deveGravarEListarAlertas() throws Exception {
        // Arrange
        LocalDateTime agora = LocalDateTime.of(2026, 11, 17, 15, 0);
        alertaDAO.salvar(new Alerta(null, "PLU-01", TipoSensor.PLUVIOMETRO, NivelAlerta.ATENCAO, "Média Móvel",
                12, 10, "chuva moderada", agora));
        alertaDAO.salvar(new Alerta(null, "NIV-01", TipoSensor.NIVEL_CORREGO, NivelAlerta.CRITICO, "Comparação",
                380, 350, "córrego vai transbordar", agora.plusMinutes(5)));

        // Act
        List<Alerta> alertas = alertaDAO.listarEmitidos(10);

        // Assert
        assertEquals(2, alertaDAO.contarTodos());
        assertEquals(NivelAlerta.CRITICO, alertas.get(0).getNivel());
        assertEquals(1, alertaDAO.listarPorSensor("PLU-01", 10).size());
    }
}
