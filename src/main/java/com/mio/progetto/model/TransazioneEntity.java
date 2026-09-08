package com.mio.progetto.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity che rappresenta una transazione (spesa o entrata).
 */
public class TransazioneEntity {

    private int id;

    @NotBlank(message = "La descrizione non può essere vuota")
    @Size(max = 255, message = "La descrizione non può superare i 255 caratteri")
    private String descrizione;

    @NotNull(message = "La categoria è obbligatoria")
    private Categoria categoria;

    @NotBlank(message = "La sottocategoria è obbligatoria")
    @Size(max = 50, message = "La sottocategoria non può superare i 50 caratteri")
    private String sottocategoria;

    @NotNull(message = "Il tipo è obbligatorio")
    private TipoTransazione tipo;

    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "L'importo deve essere maggiore di zero")
    private BigDecimal importo;

    @NotNull(message = "La data è obbligatoria")
    private LocalDate data;

    private int utenteId;

    // Costruttore vuoto richiesto da Jackson per la deserializzazione JSON
    public TransazioneEntity() {
    }

    public TransazioneEntity(int id, String descrizione, Categoria categoria, String sottocategoria, TipoTransazione tipo, BigDecimal importo, LocalDate data, int utenteId) {
        this.id = id;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.sottocategoria = sottocategoria;
        this.tipo = tipo;
        this.importo = importo;
        this.data = data;
        this.utenteId = utenteId;
    }

    // Getter
    public int getId() { return id; }
    public String getDescrizione() { return descrizione; }
    public Categoria getCategoria() { return categoria; }
    public String getSottocategoria() { return sottocategoria; }
    public TipoTransazione getTipo() { return tipo; }
    public BigDecimal getImporto() { return importo; }
    public LocalDate getData() { return data; }
    public int getUtenteId() { return utenteId; }

    // Setter (richiesti da Jackson)
    public void setId(int id) { this.id = id; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public void setSottocategoria(String sottocategoria) { this.sottocategoria = sottocategoria; }
    public void setTipo(TipoTransazione tipo) { this.tipo = tipo; }
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
                ", tipo=" + tipo +
                ", importo=" + importo +
                ", data=" + data +
                '}';
    }
}
