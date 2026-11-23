package br.edu.univille.domino.service;

import java.util.List;

import br.edu.univille.domino.model.Peca;

/** Dados já prontos para a tela da partida. */
public record PartidaView(
        Long id,
        String nomeJogador,
        String nomeIA,
        boolean emAndamento,
        boolean vezDoHumano,
        List<Peca> mesa,
        Integer pontaEsquerda,
        Integer pontaDireita,
        List<PecaMao> mao,
        int pecasIA,
        int pecasMonte,
        boolean podeComprar,
        boolean podePassar,
        String resultado,
        String ultimoComentarioIA,
        List<Lance> historico,
        List<Peca> maoIAFinal,
        String mesaTexto,
        int indiceCentro) {

    public record PecaMao(Peca peca, boolean podeEsquerda, boolean podeDireita) {
    }

    public record Lance(int ordem, String jogador, boolean ia, String descricao, String comentario) {
    }
}
