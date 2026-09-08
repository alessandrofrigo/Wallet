package com.mio.progetto.model;

public class Sottocategoria {
    private final String nome;
    private final Categoria categoriaPadre;

    public Sottocategoria(String nome, Categoria categoriaPadre) {
        this.nome = nome;
        this.categoriaPadre = categoriaPadre;
    }

    public String getNome() {
        return nome;
    }

    public Categoria getCategoriaPadre() {
        return categoriaPadre;
    }

    @Override
    public String toString() {
        return nome;
    }
}
