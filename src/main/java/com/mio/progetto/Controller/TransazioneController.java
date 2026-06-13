package com.mio.progetto.controller;

import com.mio.progetto.model.TransazioneEntity;
import com.mio.progetto.service.TransazioneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mio.progetto.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController
@RequestMapping("/api/transazioni")
public class TransazioneController {

    private final TransazioneService transazioneService;

    public TransazioneController(TransazioneService transazioneService) {
        this.transazioneService = transazioneService;
    }

    @GetMapping
    public ResponseEntity<List<TransazioneEntity>> getAllTransazioni(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TransazioneEntity> transazioni = transazioneService.getAllTransazioni(userDetails.getId());
        return ResponseEntity.ok(transazioni);
    }

    @PostMapping
    public ResponseEntity<TransazioneEntity> insertTransazione(@RequestBody TransazioneEntity transazioneEntity, @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    public ResponseEntity<Void> deleteTransazioniBeforeDate(@PathVariable String data, @AuthenticationPrincipal CustomUserDetails userDetails) {
        transazioneService.deleteTransazioniBeforeDate(data, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
