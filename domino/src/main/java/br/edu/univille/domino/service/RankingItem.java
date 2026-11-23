package br.edu.univille.domino.service;

public record RankingItem(int posicao, Long id, String nome, boolean ia, long vitorias, long partidas,
                          long aproveitamento) {
}
