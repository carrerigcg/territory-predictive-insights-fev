package dominio;

import excecao.FonteIndisponivelException;

/**
 * Contrato de qualquer sensor do território.
 * Quem consome (ServicoMonitoramento) só conhece esta interface,
 * nunca a classe concreta: novos sensores entram sem mudar o serviço.
 */
public interface Sensor {

    /** Código único do sensor (ex.: PLU-01). É a chave do histórico no banco. */
    String getCodigo();

    /** Lê o valor atual. Lança FonteIndisponivelException se o sensor não responder. */
    double lerValor() throws FonteIndisponivelException;

    /** true se o sensor está ligado e apto a responder. */
    boolean verificarFuncionamento();

    /** Tipo do sensor (define unidade e faixa física válida). */
    TipoSensor getTipo();

    /** Liga (true) ou desliga para manutenção (false). */
    void setAtivo(boolean ativo);
}
