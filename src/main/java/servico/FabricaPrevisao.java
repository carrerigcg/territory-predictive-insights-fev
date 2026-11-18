package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * FACTORY: cria as estratégias de previsão a partir da configuração
 * (chaves "MEDIA_MOVEL", "COMPARACAO" em sistema.properties).
 *
 * A escolha é feita por um catálogo (Map chave -> construtor), e NÃO por
 * if/switch/instanceof espalhados: para uma nova estratégia basta registrar
 * mais uma linha no catálogo; o serviço e o menu não mudam.
 */
public class FabricaPrevisao {

    /** Assinatura comum dos construtores das estratégias (janela, mínimo de leituras). */
    @FunctionalInterface
    private interface CriadorEstrategia {
        EstrategiaPrevisao criar(int janela, int minimoLeituras) throws DadosInvalidosException;
    }

    private static final Map<String, CriadorEstrategia> CATALOGO = Map.of(
            "MEDIA_MOVEL", PrevisaoMediaMovel::new,
            "COMPARACAO", PrevisaoComparacao::new
    );

    private final ConfiguracaoSistema configuracao;

    public FabricaPrevisao() {
        this(ConfiguracaoSistema.getInstancia());
    }

    public FabricaPrevisao(ConfiguracaoSistema configuracao) {
        this.configuracao = configuracao;
    }

    /** Cria uma estratégia pela chave, usando janela e mínimo de leituras da configuração. */
    public EstrategiaPrevisao criar(String chave) throws DadosInvalidosException {
        String chaveNormalizada = chave == null ? "" : chave.trim().toUpperCase();
        CriadorEstrategia criador = Optional.ofNullable(CATALOGO.get(chaveNormalizada))
                .orElseThrow(() -> new DadosInvalidosException("Estratégia de previsão desconhecida: '" + chave
                        + "'. Disponíveis: " + getChavesDisponiveis()));
        return criador.criar(configuracao.getJanelaPrevisao(), configuracao.getMinimoLeituras());
    }

    /** Cria todas as estratégias listadas em previsao.estrategias, na ordem configurada. */
    public List<EstrategiaPrevisao> criarEstrategiasConfiguradas() throws DadosInvalidosException {
        List<EstrategiaPrevisao> estrategias = new ArrayList<>();
        for (String chave : configuracao.getEstrategiasAtivas()) {
            estrategias.add(criar(chave));
        }
        if (estrategias.isEmpty()) {
            throw new DadosInvalidosException("Nenhuma estratégia de previsão configurada em previsao.estrategias.");
        }
        return estrategias;
    }

    public static Set<String> getChavesDisponiveis() {
        return new TreeSet<>(CATALOGO.keySet());
    }
}
