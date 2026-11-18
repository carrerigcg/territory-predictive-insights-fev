package dominio;

import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ponto vulnerável a alagamento monitorado por um conjunto de sensores.
 * Seu status depende de quantos sensores estão respondendo.
 */
public class PontoMonitoramento extends ComponenteTerritorial {

    private final String descricaoRisco;
    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(String id, String nome, String regiao, String descricaoRisco)
            throws DadosInvalidosException {
        super(id, nome, regiao);
        this.descricaoRisco = descricaoRisco == null ? "" : descricaoRisco.trim();
    }

    public void adicionarSensor(Sensor sensor) throws DadosInvalidosException {
        if (sensor == null) {
            throw new DadosInvalidosException("Não é possível instalar um sensor nulo no ponto " + getId() + ".");
        }
        for (Sensor existente : sensores) {
            if (existente.getCodigo().equalsIgnoreCase(sensor.getCodigo())) {
                throw new DadosInvalidosException("O sensor " + sensor.getCodigo()
                        + " já está instalado no ponto " + getId() + ".");
            }
        }
        sensores.add(sensor);
    }

    /**
     * OPERANTE se todos os sensores respondem, DEGRADADO se só parte responde,
     * INOPERANTE se nenhum responde (ou se não há sensores instalados).
     */
    @Override
    public StatusComponente verificarStatus() {
        long funcionando = sensores.stream().filter(Sensor::verificarFuncionamento).count();
        StatusComponente novoStatus;
        if (sensores.isEmpty() || funcionando == 0) {
            novoStatus = StatusComponente.INOPERANTE;
        } else if (funcionando == sensores.size()) {
            novoStatus = StatusComponente.OPERANTE;
        } else {
            novoStatus = StatusComponente.DEGRADADO;
        }
        setStatus(novoStatus);
        return novoStatus;
    }

    @Override
    public String getTipoComponente() {
        return "Ponto de monitoramento";
    }

    public String getDescricaoRisco() {
        return descricaoRisco;
    }

    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }
}
