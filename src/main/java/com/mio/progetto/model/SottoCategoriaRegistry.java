package com.mio.progetto.model;

import java.util.*;

public class SottoCategoriaRegistry {

    private static final Map<Categoria, List<Sottocategoria>> mappa = new HashMap<>();

    static {

        mappa.put(Categoria.CIBO, List.of(
                new Sottocategoria("Ristorante", Categoria.CIBO),
                new Sottocategoria("Aperitivo", Categoria.CIBO),
                new Sottocategoria("Colazione", Categoria.CIBO)
//                new Sottocategoria("", Categoria.CIBO)
        ));

        mappa.put(Categoria.TRASPORTI, List.of(
                new Sottocategoria("Benzina", Categoria.TRASPORTI),
                new Sottocategoria("Bus", Categoria.TRASPORTI),
                new Sottocategoria("Treno", Categoria.TRASPORTI),
                new Sottocategoria("Taxi", Categoria.TRASPORTI)
        ));

        mappa.put(Categoria.INTRATTENIMENTO, List.of(
                new Sottocategoria("Cinema", Categoria.INTRATTENIMENTO),
                new Sottocategoria("Musica", Categoria.INTRATTENIMENTO),
                new Sottocategoria("Concerti", Categoria.INTRATTENIMENTO),
                new Sottocategoria("Streaming", Categoria.INTRATTENIMENTO)
        ));

        mappa.put(Categoria.AFFITTO, List.of(
                new Sottocategoria("Affitto Mensile", Categoria.AFFITTO),
                new Sottocategoria("Spese Condominiali", Categoria.AFFITTO)
        ));

        mappa.put(Categoria.BOLLETTE, List.of(
                new Sottocategoria("Luce", Categoria.BOLLETTE),
                new Sottocategoria("Gas", Categoria.BOLLETTE),
                new Sottocategoria("Acqua", Categoria.BOLLETTE),
                new Sottocategoria("Internet", Categoria.BOLLETTE)
        ));

        mappa.put(Categoria.SHOPPING, List.of(
                new Sottocategoria("Vestiti", Categoria.SHOPPING),
                new Sottocategoria("Scarpe", Categoria.SHOPPING),
                new Sottocategoria("Accessori", Categoria.SHOPPING)
        ));

        mappa.put(Categoria.SPESA, List.of(
                new Sottocategoria("Supermercato", Categoria.SPESA),
                new Sottocategoria("Prodotti Casa", Categoria.SPESA)
        ));

        mappa.put(Categoria.ENTRATE, List.of(
                new Sottocategoria("Stipendio", Categoria.ENTRATE),
                new Sottocategoria("Freelance", Categoria.ENTRATE),
                new Sottocategoria("Regali Ricevuti", Categoria.ENTRATE),
                new Sottocategoria("Rimborsi", Categoria.ENTRATE)
        ));

        mappa.put(Categoria.ALTRO, List.of(
                new Sottocategoria("Regali", Categoria.ALTRO),
                new Sottocategoria("Varie", Categoria.ALTRO),
                new Sottocategoria("Alcool e Tabacco", Categoria.ALTRO)
        ));
    }

    public static List<Sottocategoria> getSottoCategorie(Categoria categoria) {
        return mappa.getOrDefault(categoria, List.of());
    }
}
