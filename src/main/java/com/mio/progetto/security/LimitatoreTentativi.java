package com.mio.progetto.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contatore di tentativi falliti per chiave (tipicamente l'IP del client), con blocco
 * temporaneo oltre una soglia configurabile. Protegge endpoint pubblici (login, richiesta
 * di reset password) da brute-force/abuso senza dipendenze esterne.
 */
public class LimitatoreTentativi {

    private final int maxTentativi;
    private final Duration durataBlocco;
    private final ConcurrentHashMap<String, Contatore> contatori = new ConcurrentHashMap<>();

    public LimitatoreTentativi(int maxTentativi, Duration durataBlocco) {
        this.maxTentativi = maxTentativi;
        this.durataBlocco = durataBlocco;
    }

    public boolean isBloccato(String chiave) {
        return contatori.computeIfAbsent(chiave, k -> new Contatore()).isBloccato();
    }

    public void registraFallimento(String chiave) {
        contatori.computeIfAbsent(chiave, k -> new Contatore()).registraFallimento();
    }

    public void registraSuccesso(String chiave) {
        contatori.computeIfAbsent(chiave, k -> new Contatore()).registraSuccesso();
    }

    private class Contatore {
        private int numeroFallimenti = 0;
        private Instant bloccatoFino = null;

        synchronized boolean isBloccato() {
            return bloccatoFino != null && Instant.now().isBefore(bloccatoFino);
        }

        synchronized void registraFallimento() {
            numeroFallimenti++;
            if (numeroFallimenti >= maxTentativi) {
                bloccatoFino = Instant.now().plus(durataBlocco);
            }
        }

        synchronized void registraSuccesso() {
            numeroFallimenti = 0;
            bloccatoFino = null;
        }
    }
}
