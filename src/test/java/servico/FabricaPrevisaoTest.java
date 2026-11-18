package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes dos padrões criacionais: Factory (FabricaPrevisao) e Singleton (ConfiguracaoSistema). */
class FabricaPrevisaoTest {

    private FabricaPrevisao fabrica;

    @BeforeEach
    void preparar() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
        fabrica = new FabricaPrevisao();
    }

    @Test
    @DisplayName("Factory cria a estratégia certa pela chave, com a janela da configuração")
    void deveCriarEstrategiaPelaChave() throws DadosInvalidosException {
        // Arrange
        String chave = "media_movel";

        // Act
        EstrategiaPrevisao estrategia = fabrica.criar(chave);

        // Assert
        assertInstanceOf(PrevisaoMediaMovel.class, estrategia);
        assertEquals(ConfiguracaoSistema.getInstancia().getJanelaPrevisao(), estrategia.getJanela());
    }

    @Test
    @DisplayName("Chave desconhecida lança DadosInvalidosException listando as opções")
    void chaveDesconhecidaDeveLancarExcecao() {
        // Arrange
        String chaveInvalida = "BOLA_DE_CRISTAL";

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> fabrica.criar(chaveInvalida));

        // Assert
        assertTrue(erro.getMessage().contains("MEDIA_MOVEL"));
    }

    @Test
    @DisplayName("Factory cria todas as estratégias configuradas, na ordem do arquivo")
    void deveCriarEstrategiasConfiguradas() throws DadosInvalidosException {
        // Arrange (sistema.properties: MEDIA_MOVEL,COMPARACAO)

        // Act
        List<EstrategiaPrevisao> estrategias = fabrica.criarEstrategiasConfiguradas();

        // Assert
        assertEquals(2, estrategias.size());
        assertInstanceOf(PrevisaoMediaMovel.class, estrategias.get(0));
        assertInstanceOf(PrevisaoComparacao.class, estrategias.get(1));
    }

    @Test
    @DisplayName("Singleton: sempre a mesma instância e construtor privado")
    void configuracaoDeveSerSingleton() {
        // Arrange
        Constructor<?>[] construtores = ConfiguracaoSistema.class.getDeclaredConstructors();

        // Act
        ConfiguracaoSistema primeira = ConfiguracaoSistema.getInstancia();
        ConfiguracaoSistema segunda = ConfiguracaoSistema.getInstancia();

        // Assert
        assertSame(primeira, segunda);
        assertEquals(1, construtores.length);
        assertTrue(Modifier.isPrivate(construtores[0].getModifiers()));
    }
}
