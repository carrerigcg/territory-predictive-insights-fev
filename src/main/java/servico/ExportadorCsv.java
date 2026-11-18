package servico;

import dominio.Medicao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * BÔNUS: exporta o histórico consultado para CSV (separador ";" e vírgula
 * decimal, abre direto no Excel em português).
 */
public class ExportadorCsv {

    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path exportarHistorico(String codigoSensor, List<Medicao> historico, Path pastaDestino) throws IOException {
        Files.createDirectories(pastaDestino);
        Path arquivo = pastaDestino.resolve("historico_" + codigoSensor + "_"
                + LocalDateTime.now().format(CARIMBO) + ".csv");
        try (BufferedWriter escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            escritor.write('﻿'); // BOM: faz o Excel reconhecer UTF-8 (acentos)
            escritor.write("id;codigo_sensor;tipo;valor;unidade;data_hora");
            escritor.newLine();
            for (Medicao m : historico) {
                escritor.write(String.join(";",
                        String.valueOf(m.getId()),
                        m.getCodigoSensor(),
                        m.getTipo().getDescricao(),
                        String.format(Locale.forLanguageTag("pt-BR"),"%.1f", m.getValor()),
                        m.getTipo().getUnidade(),
                        m.getDataHora().format(Medicao.FORMATO_DATA)));
                escritor.newLine();
            }
        }
        return arquivo;
    }
}
