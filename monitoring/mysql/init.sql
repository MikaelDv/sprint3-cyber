-- Usuário SOMENTE LEITURA para o Grafana consultar a trilha de auditoria.
-- Executado uma única vez, na criação do volume do MySQL.
CREATE USER IF NOT EXISTS 'grafana_ro'@'%' IDENTIFIED BY 'grafana_ro_2026';
GRANT SELECT ON carsearch.* TO 'grafana_ro'@'%';
FLUSH PRIVILEGES;
