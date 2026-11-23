package br.edu.univille.domino.model;

/** Peça de dominó duplo-seis (0-0 até 6-6). Não é uma entidade: fica salva como texto "a-b". */
public record Peca(int a, int b) {

    public static Peca parse(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Peça inválida.");
        }
        String[] partes = texto.trim().split("-");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Peça inválida: " + texto);
        }
        try {
            int a = Integer.parseInt(partes[0].trim());
            int b = Integer.parseInt(partes[1].trim());
            if (a < 0 || a > 6 || b < 0 || b > 6) {
                throw new IllegalArgumentException("Peça inválida: " + texto);
            }
            return new Peca(a, b);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Peça inválida: " + texto);
        }
    }

    public Peca invertida() {
        return new Peca(b, a);
    }

    public boolean temNumero(int numero) {
        return a == numero || b == numero;
    }

    public boolean ehDupla() {
        return a == b;
    }

    public int soma() {
        return a + b;
    }

    /** Mesma peça independente da orientação (6-4 é igual a 4-6). */
    public boolean mesmaPeca(Peca outra) {
        return (a == outra.a && b == outra.b) || (a == outra.b && b == outra.a);
    }

    @Override
    public String toString() {
        return a + "-" + b;
    }
}
