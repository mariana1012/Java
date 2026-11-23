package br.edu.univille.domino.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.univille.domino.model.Jogada;
import br.edu.univille.domino.model.Partida;

public interface JogadaRepository extends JpaRepository<Jogada, Long> {

    List<Jogada> findByPartidaOrderByOrdemAsc(Partida partida);

    long countByPartida(Partida partida);

    void deleteByPartida(Partida partida);
}
