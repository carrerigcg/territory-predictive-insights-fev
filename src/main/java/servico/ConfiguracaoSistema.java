package servico;

import dominio.Limiares;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * SINGLETON: única instância com a configuração do sistema
 * (limiares de alerta, estratégias ativas, janela de previsão, banco).
 *
 * Por que Singleton aqui: os limiares precisam ser OS MESMOS para quem emite
 * alertas (ServicoMonitoramento), para quem os altera (DadosTerritorio) e para
 * quem os exibe (relatório). Duas cópias divergentes fariam o sistema alertar
 * com um limiar e mostrar outro. Construtor privado impede um "new" acidental.
 */
public final class ConfiguracaoSistema {

    private static final String ARQUIVO_CONFIGURACAO = "/sistema.properties";
    private static ConfiguracaoSistema instancia;

    private final Map<TipoSensor, Limiares> limiares = new EnumMap<>(TipoSensor.class);
    private final List<String> estrategiasAtivas = new ArrayList<>();
    private int janelaPrevisao;
    private int minimoLeituras;
    private String codigoAcessoTerritorio;
    private String urlBanco;
    private String usuarioBanco;
    private String senhaBanco;

    private ConfiguracaoSistema() {
        restaurarPadroes();
    }

    public static synchronized ConfiguracaoSistema getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracaoSistema();
        }
        return instancia;
    }

    /**
     * (Re)carrega os valores do arquivo sistema.properties.
     * Propriedades de sistema (-Dbanco.url=...) têm prioridade sobre o arquivo.
     */
    public synchronized void restaurarPadroes() {
        Properties propriedades = carregarArquivo();
        try {
            limiares.clear();
            for (TipoSensor tipo : TipoSensor.values()) {
                limiares.put(tipo, new Limiares(tipo,
                        lerNumero(propriedades, "limiar." + tipo.name() + "." + NivelAlerta.ATENCAO.name()),
                        lerNumero(propriedades, "limiar." + tipo.name() + "." + NivelAlerta.ALERTA.name()),
                        lerNumero(propriedades, "limiar." + tipo.name() + "." + NivelAlerta.CRITICO.name())));
            }
        } catch (DadosInvalidosException e) {
            throw new IllegalStateException("Configuração inválida em sistema.properties: " + e.getMessage(), e);
        }
        estrategiasAtivas.clear();
        for (String chave : ler(propriedades, "previsao.estrategias", "MEDIA_MOVEL,COMPARACAO").split(",")) {
            if (!chave.isBlank()) {
                estrategiasAtivas.add(chave.trim().toUpperCase());
            }
        }
        janelaPrevisao = (int) lerNumero(propriedades, "previsao.janela", 5);
        minimoLeituras = (int) lerNumero(propriedades, "previsao.minimoLeituras", 5);
        codigoAcessoTerritorio = ler(propriedades, "territorio.codigoAcesso", "DEFESA2026");
        urlBanco = ler(propriedades, "banco.url", "jdbc:h2:./dados/territorio");
        usuarioBanco = ler(propriedades, "banco.usuario", "sa");
        senhaBanco = ler(propriedades, "banco.senha", "");
    }

    private static Properties carregarArquivo() {
        Properties propriedades = new Properties();
        try (InputStream entrada = ConfiguracaoSistema.class.getResourceAsStream(ARQUIVO_CONFIGURACAO)) {
            if (entrada != null) {
                propriedades.load(entrada);
            } else {
                System.err.println("Aviso: " + ARQUIVO_CONFIGURACAO + " não encontrado; usando valores padrão.");
            }
        } catch (IOException e) {
            System.err.println("Aviso: falha ao ler " + ARQUIVO_CONFIGURACAO + " (" + e.getMessage()
                    + "); usando valores padrão.");
        }
        return propriedades;
    }

    private static String ler(Properties propriedades, String chave, String padrao) {
        return System.getProperty(chave, propriedades.getProperty(chave, padrao)).trim();
    }

    private static double lerNumero(Properties propriedades, String chave) {
        String valor = propriedades.getProperty(chave);
        if (valor == null) {
            throw new IllegalStateException("Configuração obrigatória ausente em sistema.properties: " + chave);
        }
        return converter(chave, valor);
    }

    private static double lerNumero(Properties propriedades, String chave, double padrao) {
        return converter(chave, ler(propriedades, chave, String.valueOf(padrao)));
    }

    private static double converter(String chave, String valor) {
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Valor não numérico para '" + chave + "' em sistema.properties: "
                    + valor, e);
        }
    }

    public Limiares getLimiares(TipoSensor tipo) {
        return limiares.get(tipo);
    }

    public Map<TipoSensor, Limiares> getTodosLimiares() {
        return Collections.unmodifiableMap(limiares);
    }

    public List<String> getEstrategiasAtivas() {
        return Collections.unmodifiableList(estrategiasAtivas);
    }

    public int getJanelaPrevisao() {
        return janelaPrevisao;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }

    public String getCodigoAcessoTerritorio() {
        return codigoAcessoTerritorio;
    }

    public String getUrlBanco() {
        return urlBanco;
    }

    public String getUsuarioBanco() {
        return usuarioBanco;
    }

    public String getSenhaBanco() {
        return senhaBanco;
    }
}
