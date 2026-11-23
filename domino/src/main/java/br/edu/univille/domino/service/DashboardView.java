package br.edu.univille.domino.service;

import java.util.List;

/** Dados já calculados para o painel (dashboard) da tela de ranking. */
public record DashboardView(
        long totalPartidas,
        long vitoriasIA,
        long vitoriasHumanos,
        long empates,
        long percentIA,
        long percentHumanos,
        long percentEmpates,
        String gradiente,
        List<RankingItem> top3,
        List<Barra> barras,
        List<UltimaPartida> ultimas) {

    /** Barra do gráfico "vitórias por jogador" (largura em porcentagem, de 0 a 100). */
    public record Barra(String nome, boolean ia, long vitorias, long largura) {
    }

    public record UltimaPartida(Long id, String humano, String vencedor, boolean iaVenceu, boolean empate,
                                String data) {
    }
}
