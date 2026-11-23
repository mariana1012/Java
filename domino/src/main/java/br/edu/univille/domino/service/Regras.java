package br.edu.univille.domino.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import br.edu.univille.domino.model.Lado;
import br.edu.univille.domino.model.Movimento;
import br.edu.univille.domino.model.Peca;

/** Regras puras do dominó (sem banco de dados e sem IA). */
public final class Regras {

    private Regras() {
    }

    public static List<Peca> todasAsPecas() {
        List<Peca> pecas = new ArrayList<>();
        for (int a = 0; a <= 6; a++) {
            for (int b = a; b <= 6; b++) {
                pecas.add(new Peca(a, b));
            }
        }
        return pecas;
    }

    public static List<Peca> lerLista(String texto) {
        List<Peca> lista = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return lista;
        }
        for (String parte : texto.split(",")) {
            lista.add(Peca.parse(parte));
        }
        return lista;
    }

    public static String paraTexto(List<Peca> pecas) {
        return pecas.stream().map(Peca::toString).collect(Collectors.joining(","));
    }

    public static Integer pontaEsquerda(List<Peca> mesa) {
        return mesa.isEmpty() ? null : mesa.get(0).a();
    }

    public static Integer pontaDireita(List<Peca> mesa) {
        return mesa.isEmpty() ? null : mesa.get(mesa.size() - 1).b();
    }

    public static boolean encaixa(List<Peca> mesa, Peca peca, Lado lado) {
        if (mesa.isEmpty()) {
            return true;
        }
        int ponta = lado == Lado.ESQUERDA ? pontaEsquerda(mesa) : pontaDireita(mesa);
        return peca.temNumero(ponta);
    }

    /** Coloca a peça na mesa já orientada para encaixar na ponta escolhida. */
    public static void colocar(List<Peca> mesa, Peca peca, Lado lado) {
        if (mesa.isEmpty()) {
            mesa.add(peca);
            return;
        }
        if (lado == Lado.ESQUERDA) {
            int ponta = pontaEsquerda(mesa);
            mesa.add(0, peca.b() == ponta ? peca : peca.invertida());
        } else {
            int ponta = pontaDireita(mesa);
            mesa.add(peca.a() == ponta ? peca : peca.invertida());
        }
    }

    public static List<Movimento> jogadasLegais(List<Peca> mao, List<Peca> mesa) {
        List<Movimento> jogadas = new ArrayList<>();
        for (Peca peca : mao) {
            if (encaixa(mesa, peca, Lado.ESQUERDA)) {
                jogadas.add(new Movimento(peca, Lado.ESQUERDA));
            }
            if (encaixa(mesa, peca, Lado.DIREITA)) {
                jogadas.add(new Movimento(peca, Lado.DIREITA));
            }
        }
        return jogadas;
    }

    public static int soma(List<Peca> mao) {
        return mao.stream().mapToInt(Peca::soma).sum();
    }

    public static boolean contem(List<Peca> mao, Peca peca) {
        return mao.stream().anyMatch(p -> p.mesmaPeca(peca));
    }

    public static boolean remover(List<Peca> mao, Peca peca) {
        Iterator<Peca> it = mao.iterator();
        while (it.hasNext()) {
            if (it.next().mesmaPeca(peca)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /** Maior dupla da mão; se não houver dupla, a peça de maior soma. */
    public static Peca maiorPecaDeAbertura(List<Peca> mao) {
        Peca melhor = mao.get(0);
        for (Peca peca : mao) {
            if (valorDeAbertura(peca) > valorDeAbertura(melhor)) {
                melhor = peca;
            }
        }
        return melhor;
    }

    public static int valorDeAbertura(Peca peca) {
        return peca.ehDupla() ? 1000 + peca.a() : peca.soma() * 10 + Math.max(peca.a(), peca.b());
    }
}
