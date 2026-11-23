package br.edu.univille.domino.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.univille.domino.model.Jogador;
import br.edu.univille.domino.model.TipoJogador;

public interface JogadorRepository extends JpaRepository<Jogador, Long> {

    Optional<Jogador> findByNomeIgnoreCase(String nome);

    Optional<Jogador> findFirstByTipo(TipoJogador tipo);
}
