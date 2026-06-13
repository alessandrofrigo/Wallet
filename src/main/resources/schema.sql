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
    data DATE,
    utente_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE
);

-- Inserisci utente admin di default se non esiste
-- La password è 'admin' codificata con BCrypt
INSERT IGNORE INTO utenti (id, username, password, ruolo) 
VALUES (1, 'admin', '$2a$10$wT.B3z9U.R.E.RkZc3/TTeu9qC/w2S5Y9e.D7V4M.Q6L4gqE7uG/S', 'ROLE_ADMIN');
