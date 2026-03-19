public class Elevador {

    private int andarAtual;
    private int totalAndares;
    private boolean portaAberta;

    // Metodo construtor
    public Elevador(int totalAndares) {
        this.totalAndares = totalAndares;
        this.andarAtual = 0; // térreo - andar inicial
        this.portaAberta = false;
    }

    // Subir elevador
    public void subir() {
        if (portaAberta) {
            System.out.println("A porta está aberta, não é possível subir!");
            return;
        }

        if (andarAtual < totalAndares) {
            andarAtual++;
            System.out.println("Subiu para o andar: " + andarAtual);
        } else {
            System.out.println("Voce está no último andar!");
        }
    }

    // Descer elevador
    public void descer() {
        if (portaAberta) {
            System.out.println("A porta está aberta, não é possível descer!");
            return;
        }

        if (andarAtual > 0) {
            andarAtual--;
            System.out.println("Desceu para o andar: " + andarAtual);
        } else {
            System.out.println("Voce está no térreo!");
        }
    }

    // Abrir porta
    public void abrirPorta() {
        if (!portaAberta) {
            portaAberta = true;
            System.out.println("Porta aberta.");
        } else {
            System.out.println("A porta já está aberta.");
        }
    }

    // Fechar porta
    public void fecharPorta() {
        if (portaAberta) {
            portaAberta = false;
            System.out.println("Porta fechada.");
        } else {
            System.out.println("A porta já está fechada.");
        }
    }

    // Mostrar estado da porta
    public void mostrarPorta() {
        System.out.println("Porta está " + (portaAberta ? "aberta" : "fechada"));
    }

    // Mostrar andar atual do elevador
    public void mostrarAndarAtual() {
        System.out.println("Andar atual: " + andarAtual);
    }
}