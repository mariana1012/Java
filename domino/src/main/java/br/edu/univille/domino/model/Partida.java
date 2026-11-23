package br.edu.univille.domino.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Uma partida entre um jogador humano (jogador1) e a IA (jogador2).
 * O estado do jogo (mesa, mãos e monte) fica salvo como texto, no formato "6-6,6-4,4-2".
 */
@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Jogador jogador1;

    @ManyToOne(optional = false)
    private Jogador jogador2;

    @ManyToOne
    private Jogador vencedor;

    @ManyToOne
    private Jogador vezDe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPartida status = StatusPartida.EM_ANDAMENTO;

    @Column(length = 500)
    private String mesa = "";

    @Column(length = 500)
    private String maoJogador1 = "";

    @Column(length = 500)
    private String maoJogador2 = "";

    @Column(length = 500)
    private String monte = "";

    private int passesSeguidos;

    private LocalDateTime dataInicio = LocalDateTime.now();

    private LocalDateTime dataFim;

    protected Partida() {
    }

    public Partida(Jogador jogador1, Jogador jogador2) {
        this.jogador1 = jogador1;
        this.jogador2 = jogador2;
    }

    public Long getId() {
        return id;
    }

    public Jogador getJogador1() {
        return jogador1;
    }

    public Jogador getJogador2() {
        return jogador2;
    }

    public Jogador getVencedor() {
        return vencedor;
    }

    public void setVencedor(Jogador vencedor) {
        this.vencedor = vencedor;
    }

    public Jogador getVezDe() {
        return vezDe;
    }

    public void setVezDe(Jogador vezDe) {
        this.vezDe = vezDe;
    }

    public StatusPartida getStatus() {
        return status;
    }

    public void setStatus(StatusPartida status) {
        this.status = status;
    }

    public String getMesa() {
        return mesa;
    }

    public void setMesa(String mesa) {
        this.mesa = mesa;
    }

    public String getMaoJogador1() {
        return maoJogador1;
    }

    public void setMaoJogador1(String maoJogador1) {
        this.maoJogador1 = maoJogador1;
    }

    public String getMaoJogador2() {
        return maoJogador2;
    }

    public void setMaoJogador2(String maoJogador2) {
        this.maoJogador2 = maoJogador2;
    }

    public String getMonte() {
        return monte;
    }

    public void setMonte(String monte) {
        this.monte = monte;
    }

    public int getPassesSeguidos() {
        return passesSeguidos;
    }

    public void setPassesSeguidos(int passesSeguidos) {
        this.passesSeguidos = passesSeguidos;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }
}
