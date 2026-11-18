-- ==============================================================
-- Script SQL do banco (H2) - executado automaticamente na
-- inicializacao por persistencia.ConexaoBanco.
-- "IF NOT EXISTS" garante que o historico NAO e apagado ao reiniciar.
-- ==============================================================

CREATE TABLE IF NOT EXISTS medicao (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_sensor  VARCHAR(30)  NOT NULL,
    tipo_sensor    VARCHAR(30)  NOT NULL,
    valor          DOUBLE       NOT NULL,
    data_hora      TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_medicao_sensor_data ON medicao (codigo_sensor, data_hora);

CREATE TABLE IF NOT EXISTS alerta (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_sensor   VARCHAR(30)   NOT NULL,
    tipo_sensor     VARCHAR(30)   NOT NULL,
    nivel           VARCHAR(10)   NOT NULL,
    estrategia      VARCHAR(60)   NOT NULL,
    valor_previsto  DOUBLE        NOT NULL,
    limite          DOUBLE        NOT NULL,
    mensagem        VARCHAR(1000) NOT NULL,
    data_hora       TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alerta_data ON alerta (data_hora);
