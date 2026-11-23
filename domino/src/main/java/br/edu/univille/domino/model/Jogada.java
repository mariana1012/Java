package br.edu.univille.domino.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Histórico lance a lance de uma partida. */
@Entity
public class Jogada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Partida partida;

    @ManyToOne(optional = false)
    private Jogador jogador;

    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoJogada tipo;

    private String peca;

    @Enumerated(EnumType.STRING)
    private Lado lado;

    @Column(length = 1000)
    private String comentario;

    private LocalDateTime data = LocalDateTime.now();

    protected Jogada() {
    }

    public Jogada(Partida partida, Jogador jogador, int ordem, TipoJogada tipo,
                  String peca, Lado lado, String comentario) {
        this.partida = partida;
        this.jogador = jogador;
        this.ordem = ordem;
        this.tipo = tipo;
        this.peca = peca;
        this.lado = lado;
        this.comentario = comentario;
    }

    public Long getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public int getOrdem() {
        return ordem;
    }

    public TipoJogada getTipo() {
        return tipo;
    }

    public String getPeca() {
        return peca;
    }

    public Lado getLado() {
        return lado;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getData() {
        return data;
    }
}
