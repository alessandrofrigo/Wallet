package com.mio.progetto.controller;

import com.mio.progetto.model.TransazioneEntity;
import com.mio.progetto.service.TransazioneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.mio.progetto.service.CustomUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transazioni")
@Validated
public class TransazioneController {

    private final TransazioneService transazioneService;

    public TransazioneController(TransazioneService transazioneService) {
        this.transazioneService = transazioneService;
    }

    @GetMapping
    public ResponseEntity<List<TransazioneEntity>> getAllTransazioni(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TransazioneEntity> transazioni = transazioneService.getAllTransazioniPaginated(userDetails.getId(), page, size);
        return ResponseEntity.ok(transazioni);
    }

    @PostMapping
    public ResponseEntity<?> insertTransazione(@Valid @RequestBody TransazioneEntity transazioneEntity, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Validazione della coerenza tra Tipo e Categoria
        boolean isEntrata = transazioneEntity.getTipo() == com.mio.progetto.model.TipoTransazione.ENTRATA;
        boolean isCategoriaEntrate = transazioneEntity.getCategoria() == com.mio.progetto.model.Categoria.ENTRATE;
        
        if (isEntrata && !isCategoriaEntrate) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Una transazione di tipo ENTRATA deve avere la categoria ENTRATE.");
        }
        if (!isEntrata && isCategoriaEntrate) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Una transazione di tipo USCITA non può avere la categoria ENTRATE.");
        }

        // Validazione della coerenza tra categoria e sottocategoria
        boolean valida = com.mio.progetto.model.SottoCategoriaRegistry.getSottoCategorie(transazioneEntity.getCategoria()).stream()
                .anyMatch(sc -> sc.getNome().equalsIgnoreCase(transazioneEntity.getSottocategoria()));
        if (!valida) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La sottocategoria '" + transazioneEntity.getSottocategoria() + "' non è valida per la categoria " + transazioneEntity.getCategoria());
        }

        transazioneEntity.setUtenteId(userDetails.getId());
        transazioneService.insertTransazione(transazioneEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(transazioneEntity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransazioneById(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        int rows = transazioneService.deleteTransazioneById(id, userDetails.getId());
        if (rows > 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/categoria/{categoria}")
    public ResponseEntity<Void> deleteTransazioniByCategoria(@PathVariable String categoria, @AuthenticationPrincipal CustomUserDetails userDetails) {
        transazioneService.deleteTransazioniByCategoria(categoria, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/data/{data}")
    public ResponseEntity<Void> deleteTransazioniBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        transazioneService.deleteTransazioniBeforeDate(data, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
