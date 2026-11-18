package persistencia;

import excecao.FonteIndisponivelException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Fornece conexões JDBC com o H2 e cria as tabelas na primeira execução
 * (script src/main/resources/schema.sql, com CREATE TABLE IF NOT EXISTS).
 */
public class ConexaoBanco {

    private static final String SCRIPT_ESQUEMA = "/schema.sql";

    private final String url;
    private final String usuario;
    private final String senha;

    public ConexaoBanco(String url, String usuario, String senha) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL do banco não configurada (banco.url em sistema.properties).");
        }
        this.url = url;
        this.usuario = usuario;
        this.senha = senha == null ? "" : senha;
    }

    /** Abre uma conexão nova. Quem chama fecha com try-with-resources. */
    public Connection abrirConexao() throws FonteIndisponivelException {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Banco de dados indisponível (" + url + "): " + e.getMessage(), e);
        }
    }

    /** Executa o schema.sql. Seguro de rodar sempre: não apaga dados existentes. */
    public void inicializarEsquema() throws FonteIndisponivelException {
        String script = lerScript();
        try (Connection conexao = abrirConexao()) {
            for (String instrucao : script.split(";")) {
                if (!instrucao.isBlank()) {
                    // DDL fixo lido do schema.sql (nenhum dado do usuário); também via PreparedStatement
                    try (PreparedStatement comando = conexao.prepareStatement(instrucao)) {
                        comando.execute();
                    }
                }
            }
        } catch (SQLException e) {
            throw new FonteIndisponivelException("Falha ao criar as tabelas do banco: " + e.getMessage(), e);
        }
    }

    private String lerScript() throws FonteIndisponivelException {
        try (InputStream entrada = ConexaoBanco.class.getResourceAsStream(SCRIPT_ESQUEMA)) {
            if (entrada == null) {
                throw new FonteIndisponivelException("Script " + SCRIPT_ESQUEMA + " não encontrado no classpath.");
            }
            try (BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                return leitor.lines()
                        .filter(linha -> !linha.trim().startsWith("--"))
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new FonteIndisponivelException("Não foi possível ler o script do banco: " + e.getMessage(), e);
        }
    }

    public String getUrl() {
        return url;
    }
}
