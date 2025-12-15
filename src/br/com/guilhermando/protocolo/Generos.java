package br.com.guilhermando.protocolo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Generos{
    ACAO("Ação"),
    AVENTURA("Aventura"),
    COMEDIA("Comédia"),
    DRAMA("Drama"),
    FANTASIA("Fantasia"),
    FICCAO_CIENTIFICA("Ficção Científica"),
    TERROR("Terror"),
    ROMANCE("Romance"),
    DOCUMENTARIO("Documentário"),
    MUSICAL("Musical"),
    ANIMACAO("Animação");

    private final String nomeFormatado;

    Generos(String nome) {
        this.nomeFormatado = nome;
    }

    private static final Set<String> nomesEnums = new HashSet<>();
    static {
        for (Generos g : Generos.values()) {
            nomesEnums.add(g.name());
        }
    }

 
    public static boolean isgenerosValido(String generosInput) {
        if (generosInput == null) return false;
        String inputNormalizado = generosInput.trim();

        for (Generos g : Generos.values()) {
            if (g.name().equalsIgnoreCase(inputNormalizado) || 
                g.nomeFormatado.equalsIgnoreCase(inputNormalizado)) {
                return true;
            }
        }
        return false;
    }


    public static boolean validarListaGeneros(String[] generos) {
        if (generos == null || generos.length == 0) {
            return false;
        }
        return Arrays.stream(generos).allMatch(g -> isgenerosValido(g.trim()));
    }
}