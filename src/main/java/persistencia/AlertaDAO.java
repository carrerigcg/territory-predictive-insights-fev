package persistencia;

import dominio.Alerta;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.FonteIndisponivelException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso à tabela ALERTA, sempre com PreparedStatement e try-with-resources.
 */
public class AlertaDAO {

    private static final String SQL_INSERIR =
            "INSERT INTO alerta (codigo_sensor, tipo_sensor, nivel, estrategia, valor_previsto, limite, "
                    + "mensagem, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_LISTAR =
            "SELECT id, codigo_sensor, tipo_sensor, nivel, estrategia, valor_previsto, limite, mensagem, data_hora "
                    + "FROM alerta ORDER BY data_hora DESC, id DESC LIMIT ?";
    private static final String SQL_LISTAR_POR_SENSOR =
            "SELECT id, codigo_sensor, tipo_sensor, nivel, estrategia, valor_previsto, limite, mensagem, data_hora "
                    + "FROM alerta WHERE codigo_sensor = ? ORDER BY data_hora DESC, id DESC LIMIT ?";
    private static final String SQL_CONTAR =
            "SELECT COUNT(*) FROM alerta";

    private final ConexaoBanco conexaoBanco;

    public AlertaDAO(ConexaoBanco conexaoBanco) {
        this.conexaoBanco = conexaoBanco;
    }

    public Alerta salvar(Alerta alerta) throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alerta.getCodigoSensor());
            ps.setString(2, alerta.getTipo().name());
            ps.setString(3, alerta.getNivel().name());
            ps.setString(4, alerta.getEstrategia());
            ps.setDouble(5, alerta.getValorPrevisto());
            ps.setDouble(6, alerta.getLimite());
            ps.setString(7, alerta.getMensagem());
            ps.setTimestamp(8, Timestamp.valueOf(alerta.getDataHora()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    alerta.setId(chaves.getLong(1));
                }
            }
            return alerta;
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível gravar o alerta do sensor "
                    + alerta.getCodigoSensor() + ": " + e.getMessage(), e);
        }
    }

    /** Alertas já emitidos, do mais recente para o mais antigo. */
    public List<Alerta> listarEmitidos(int limite) throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR)) {
            ps.setInt(1, limite);
            return executarConsulta(ps);
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível listar os alertas: " + e.getMessage(), e);
        }
    }

    public List<Alerta> listarPorSensor(String codigoSensor, int limite) throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR_POR_SENSOR)) {
            ps.setString(1, codigoSensor);
            ps.setInt(2, limite);
            return executarConsulta(ps);
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível listar os alertas do sensor "
                    + codigoSensor + ": " + e.getMessage(), e);
        }
    }

    public long contarTodos() throws FonteIndisponivelException {
        try (Connection conexao = conexaoBanco.abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_CONTAR);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Não foi possível contar os alertas: " + e.getMessage(), e);
        }
    }

    private List<Alerta> executarConsulta(PreparedStatement ps) throws SQLException, FonteIndisponivelException {
        List<Alerta> alertas = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alertas.add(mapear(rs));
            }
        }
        return alertas;
    }

    private Alerta mapear(ResultSet rs) throws SQLException, FonteIndisponivelException {
        long id = rs.getLong("id");
        try {
            return new Alerta(id, rs.getString("codigo_sensor"),
                    TipoSensor.valueOf(rs.getString("tipo_sensor")),
                    NivelAlerta.valueOf(rs.getString("nivel")),
                    rs.getString("estrategia"),
                    rs.getDouble("valor_previsto"),
                    rs.getDouble("limite"),
                    rs.getString("mensagem"),
                    rs.getTimestamp("data_hora").toLocalDateTime());
        } catch (IllegalArgumentException e) {
            throw new FonteIndisponivelException("Registro de alerta #" + id + " corrompido no banco: "
                    + e.getMessage(), e);
        }
    }
}
