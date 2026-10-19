USE sendera;
DELETE FROM tokens_recuperacion;
ALTER TABLE tokens_recuperacion ADD COLUMN intentos INT NOT NULL DEFAULT 0 AFTER expira_en;
