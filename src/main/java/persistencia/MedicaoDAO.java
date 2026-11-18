package persistencia;

import dominio.Medicao;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Acesso à tabela MEDICAO. Todo SQL usa PreparedStatement com parâmetros "?"
 * (nunca concatenação de String) e todo recurso é fechado com try-with-resources.
 */
public class MedicaoDAO {

    private static final String SQL_INSERIR =
            "INSERT INTO medicao (codigo_sensor, tipo_sensor, valor, data_hora) VALUES (?, ?, ?, ?)";
    private static final String SQL_HISTORICO_POR_SENSOR =
            "SELECT id, codigo_sensor, tipo_sensor, valor, data_hora FROM medicao "
                    + "WHERE codigo_sensor = ? ORDER BY data_hora DESC, id DESC LIMIT ?";
    private static final String SQL_CONTAR_POR_SENSOR =
            "SELECT COUNT(*) FROM medicao WHERE codigo_sensor = ?";
    private static final String SQL_CONTAR_TODAS =
            "SELECT COUNT(*) FROM medicao";

    private final ConexaoBanco conexaoBanco;

    public MedicaoDAO(ConexaoBanco conexaoBanco) {
        this.conexaoBanco = conexaoBanco;
    }

    /** Grava a medição e preenche o id gerado pelo banco. */
    public Medicao salvar(Medicao medicao) throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, medicao.getCodigoSensor());
            ps.setString(2, medicao.getTipo().name());
            ps.setDouble(3, medicao.getValor());
            ps.setTimestamp(4, Timestamp.valueOf(medicao.getDataHora()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    medicao.setId(chaves.getLong(1));
                }
            }
            return medicao;
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível gravar a medição do sensor "
                    + medicao.getCodigoSensor() + ": " + e.getMessage(), e);
        }
    }

    /** Histórico de um sensor, da leitura mais RECENTE para a mais antiga (para exibição). */
    public List<Medicao> buscarHistoricoPorSensor(String codigoSensor, int limite) throws FonteIndisponivelException {
        List<Medicao> resultado = new ArrayList<>();
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_HISTORICO_POR_SENSOR)) {
            ps.setString(1, codigoSensor);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível consultar o histórico do sensor "
                    + codigoSensor + ": " + e.getMessage(), e);
        }
    }

    /** Últimas N leituras em ORDEM CRONOLÓGICA (mais antiga primeiro): formato que as previsões esperam. */
    public List<Medicao> buscarUltimasEmOrdemCronologica(String codigoSensor, int quantidade)
            throws FonteIndisponivelException {
        List<Medicao> recentes = buscarHistoricoPorSensor(codigoSensor, quantidade);
        Collections.reverse(recentes);
        return recentes;
    }

    public Optional<Medicao> buscarUltimaPorSensor(String codigoSensor) throws FonteIndisponivelException {
        List<Medicao> ultima = buscarHistoricoPorSensor(codigoSensor, 1);
        return ultima.isEmpty() ? Optional.empty() : Optional.of(ultima.get(0));
    }

    public long contarPorSensor(String codigoSensor) throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_CONTAR_POR_SENSOR)) {
            ps.setString(1, codigoSensor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível contar as medições do sensor "
                    + codigoSensor + ": " + e.getMessage(), e);
        }
    }

    public long contarTodas() throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_CONTAR_TODAS);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível contar as medições: " + e.getMessage(), e);
        }
    }

    private Medicao mapear(ResultSet rs) throws SQLException, FonteIndisponivelException {
        long id = rs.getLong("id");
        try {
            return new Medicao(id, rs.getString("codigo_sensor"),
                    TipoSensor.valueOf(rs.getString("tipo_sensor")),
                    rs.getDouble("valor"),
                    rs.getTimestamp("data_hora").toLocalDateTime());
        } catch (DadosInvalidosException | IllegalArgumentException e) {
            throw new FonteIndisponivelException("Registro de medição #" + id
                    + " corrompido no banco: " + e.getMessage(), e);
        }
    }
}
