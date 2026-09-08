CREATE DATABASE IF NOT EXISTS gestione_spese;

USE gestione_spese;

CREATE TABLE IF NOT EXISTS utenti (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    ruolo VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS transazioni (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descrizione VARCHAR(255),
    importo DECIMAL(10,2),
    categoria VARCHAR(50),
    sottocategoria VARCHAR(50),
    tipo VARCHAR(10) NOT NULL DEFAULT 'USCITA',
    data DATE,
    utente_id INT NOT NULL,
    INDEX idx_transazioni_utente_id (utente_id),
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE
);

-- Migrazione idempotente: aggiunge la colonna "tipo" ai database creati con
-- una versione precedente dello schema (senza distruggere i dati esistenti).
SET @ddl := (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE transazioni ADD COLUMN tipo VARCHAR(10) NOT NULL DEFAULT ''USCITA''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transazioni'
      AND COLUMN_NAME = 'tipo');
PREPARE migrate_tipo FROM @ddl;
EXECUTE migrate_tipo;
DEALLOCATE PREPARE migrate_tipo;
