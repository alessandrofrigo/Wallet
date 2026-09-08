package com.mio.progetto.controller;

import com.mio.progetto.model.Categoria;
import com.mio.progetto.model.SottoCategoriaRegistry;
import com.mio.progetto.model.Sottocategoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categorie")
public class CategoriaController {

    @GetMapping
    public ResponseEntity<List<String>> getAllCategorie() {
        List<String> categorie = Arrays.stream(Categoria.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categorie);
    }

    @GetMapping("/{categoria}/sottocategorie")
    public ResponseEntity<List<String>> getSottoCategorie(@PathVariable String categoria) {
        try {
            Categoria cat = Categoria.valueOf(categoria.toUpperCase());
            List<String> sottocategorie = SottoCategoriaRegistry.getSottoCategorie(cat).stream()
                    .map(Sottocategoria::getNome)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(sottocategorie);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
