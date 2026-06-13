package com.mio.progetto.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity che rappresenta una transazione (spesa o entrata).
 */
public class TransazioneEntity {

    private int id;
    private String descrizione;
    private Categoria categoria;
    private String sottocategoria;
    private BigDecimal importo;
    private LocalDate data;
    private int utenteId;

    // Costruttore vuoto richiesto da Jackson per la deserializzazione JSON
    public TransazioneEntity() {
    }

    public TransazioneEntity(int id, String descrizione, Categoria categoria, String sottocategoria, BigDecimal importo, LocalDate data) {
        this.id = id;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.sottocategoria = sottocategoria;
        this.importo = importo;
        this.data = data;
        this.utenteId = utenteId;
    }

    // Getter
    public int getId() { return id; }
    public String getDescrizione() { return descrizione; }
    public Categoria getCategoria() { return categoria; }
    public String getSottocategoria() { return sottocategoria; }
    public BigDecimal getImporto() { return importo; }
    public LocalDate getData() { return data; }
    public int getUtenteId() { return utenteId; }

    // Setter (richiesti da Jackson)
    public void setId(int id) { this.id = id; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public void setSottocategoria(String sottocategoria) { this.sottocategoria = sottocategoria; }
    public void setImporto(BigDecimal importo) { this.importo = importo; }
    public void setData(LocalDate data) { this.data = data; }
    public void setUtenteId(int utenteId) { this.utenteId = utenteId; }

    @Override
    public String toString() {
        return "TransazioneEntity{" +
                "id=" + id +
                ", descrizione='" + descrizione + '\'' +
                ", categoria=" + categoria +
                ", sottocategoria='" + sottocategoria + '\'' +
                ", importo=" + importo +
                ", data=" + data +
                '}';
    }
}
