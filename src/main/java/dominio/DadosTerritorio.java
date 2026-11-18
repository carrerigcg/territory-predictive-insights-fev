package dominio;

import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;
import servico.ConfiguracaoSistema;

/**
 * Dados gerais do território monitorado. Todos os atributos são privados e
 * só mudam por setters que validam (DadosInvalidosException).
 *
 * Os limiares de alerta são sensíveis (um limiar alto demais "cala" o sistema),
 * por isso só podem ser alterados com o código de acesso da coordenação.
 * Nenhum dado pessoal: o território é descrito por região e equipamentos.
 */
public class DadosTerritorio {

    private String nomeTerritorio;
    private String municipio;
    private int populacaoEstimada;
    private double areaKm2;
    private int domiciliosEmAreaDeRisco;
    private final String codigoAcesso;

    public DadosTerritorio(String nomeTerritorio, String municipio, int populacaoEstimada, double areaKm2,
                           int domiciliosEmAreaDeRisco, String codigoAcesso) throws DadosInvalidosException {
        if (codigoAcesso == null || codigoAcesso.isBlank()) {
            throw new DadosInvalidosException("O código de acesso do território não pode ser vazio.");
        }
        this.codigoAcesso = codigoAcesso;
        setNomeTerritorio(nomeTerritorio);
        setMunicipio(municipio);
        setPopulacaoEstimada(populacaoEstimada);
        setAreaKm2(areaKm2);
        setDomiciliosEmAreaDeRisco(domiciliosEmAreaDeRisco);
    }

    /**
     * Altera um limiar de alerta (guardado no Singleton ConfiguracaoSistema),
     * desde que o código de acesso esteja correto e o novo valor seja válido.
     */
    public void alterarLimiar(String codigoInformado, TipoSensor tipo, NivelAlerta nivel, double novoValor)
            throws AcessoNegadoException, DadosInvalidosException {
        if (codigoInformado == null || !codigoAcesso.equals(codigoInformado.trim())) {
            throw new AcessoNegadoException("Código de acesso incorreto: limiares não foram alterados.");
        }
        if (tipo == null) {
            throw new DadosInvalidosException("Tipo de sensor não informado para alteração de limiar.");
        }
        ConfiguracaoSistema.getInstancia().getLimiares(tipo).definir(nivel, novoValor);
    }

    /** Pessoas por km² (indicador simples de exposição). */
    public double getDensidadeDemografica() {
        return populacaoEstimada / areaKm2;
    }

    public String getNomeTerritorio() {
        return nomeTerritorio;
    }

    public void setNomeTerritorio(String nomeTerritorio) throws DadosInvalidosException {
        if (nomeTerritorio == null || nomeTerritorio.isBlank()) {
            throw new DadosInvalidosException("O nome do território não pode ser vazio.");
        }
        this.nomeTerritorio = nomeTerritorio.trim();
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) throws DadosInvalidosException {
        if (municipio == null || municipio.isBlank()) {
            throw new DadosInvalidosException("O município não pode ser vazio.");
        }
        this.municipio = municipio.trim();
    }

    public int getPopulacaoEstimada() {
        return populacaoEstimada;
    }

    public void setPopulacaoEstimada(int populacaoEstimada) throws DadosInvalidosException {
        if (populacaoEstimada < 0) {
            throw new DadosInvalidosException("População negativa não é permitida: " + populacaoEstimada + ".");
        }
        this.populacaoEstimada = populacaoEstimada;
    }

    public double getAreaKm2() {
        return areaKm2;
    }

    public void setAreaKm2(double areaKm2) throws DadosInvalidosException {
        if (Double.isNaN(areaKm2) || areaKm2 <= 0) {
            throw new DadosInvalidosException("A área do território deve ser maior que zero (informado: "
                    + areaKm2 + " km²).");
        }
        this.areaKm2 = areaKm2;
    }

    public int getDomiciliosEmAreaDeRisco() {
        return domiciliosEmAreaDeRisco;
    }

    public void setDomiciliosEmAreaDeRisco(int domiciliosEmAreaDeRisco) throws DadosInvalidosException {
        if (domiciliosEmAreaDeRisco < 0) {
            throw new DadosInvalidosException("Número de domicílios em área de risco não pode ser negativo: "
                    + domiciliosEmAreaDeRisco + ".");
        }
        this.domiciliosEmAreaDeRisco = domiciliosEmAreaDeRisco;
    }

    @Override
    public String toString() {
        return String.format("Território: %s (%s)%n  População estimada: %,d pessoas | Área: %.2f km² | "
                        + "Densidade: %,.0f hab/km²%n  Domicílios em área de risco: %,d",
                nomeTerritorio, municipio, populacaoEstimada, areaKm2, getDensidadeDemografica(),
                domiciliosEmAreaDeRisco);
    }
}
