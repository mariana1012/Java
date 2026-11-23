package br.edu.univille.domino.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.univille.domino.model.Jogador;
import br.edu.univille.domino.model.Partida;
import br.edu.univille.domino.model.StatusPartida;
import br.edu.univille.domino.model.TipoJogador;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    long countByVencedor(Jogador vencedor);

    List<Partida> findByJogador1OrJogador2(Jogador jogador1, Jogador jogador2);

    long countByStatus(StatusPartida status);

    long countByStatusAndVencedorTipo(StatusPartida status, TipoJogador tipo);

    List<Partida> findTop5ByStatusOrderByDataFimDesc(StatusPartida status);

    @Query("SELECT COUNT(p) FROM Partida p "
            + "WHERE p.status = :status AND (p.jogador1 = :jogador OR p.jogador2 = :jogador)")
    long contarPartidasFinalizadas(@Param("jogador") Jogador jogador, @Param("status") StatusPartida status);
}
