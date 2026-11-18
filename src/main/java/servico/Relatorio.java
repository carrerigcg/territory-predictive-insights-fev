package servico;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Relatório de status completo, imutável. Só é criado pelo {@link RelatorioBuilder}.
 */
public class Relatorio {

    private final String titulo;
    private final LocalDateTime geradoEm;
    private final List<Secao> secoes;

    /** Uma seção do relatório: título + linhas de texto. */
    public static class Secao {
        private final String titulo;
        private final List<String> linhas;

        Secao(String titulo, List<String> linhas) {
            this.titulo = titulo;
            this.linhas = Collections.unmodifiableList(linhas);
        }

        public String getTitulo() {
            return titulo;
        }

        public List<String> getLinhas() {
            return linhas;
        }
    }

    Relatorio(String titulo, LocalDateTime geradoEm, List<Secao> secoes) {
        this.titulo = titulo;
        this.geradoEm = geradoEm;
        this.secoes = Collections.unmodifiableList(secoes);
    }

    public String getTitulo() {
        return titulo;
    }

    public LocalDateTime getGeradoEm() {
        return geradoEm;
    }

    public List<Secao> getSecoes() {
        return secoes;
    }

    public String formatarTexto() {
        String barra = "=".repeat(78);
        StringBuilder sb = new StringBuilder();
        sb.append(barra).append('\n')
                .append("  ").append(titulo.toUpperCase()).append('\n')
                .append("  Gerado em ").append(geradoEm.format(dominio.Medicao.FORMATO_DATA)).append('\n')
                .append(barra).append('\n');
        for (Secao secao : secoes) {
            sb.append('\n').append(">> ").append(secao.getTitulo()).append('\n');
            for (String linha : secao.getLinhas()) {
                sb.append("   ").append(linha).append('\n');
            }
        }
        sb.append(barra);
        return sb.toString();
    }

    @Override
    public String toString() {
        return formatarTexto();
    }
}
