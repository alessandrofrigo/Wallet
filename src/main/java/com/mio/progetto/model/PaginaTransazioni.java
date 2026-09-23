package com.mio.progetto.model;

import java.util.List;

public record PaginaTransazioni(
        List<TransazioneEntity> contenuto,
        int page,
        int size,
        long totale,
        int totalePagine
) {
    public PaginaTransazioni(List<TransazioneEntity> contenuto, int page, int size, long totale) {
        this(contenuto, page, size, totale, (int) Math.ceil((double) totale / size));
    }
}
