package br.edu.univille.domino.model;

/** Uma jogada possível: qual peça e em qual ponta da mesa. */
public record Movimento(Peca peca, Lado lado) {
}
