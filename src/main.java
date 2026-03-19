public class main {
    public static void Main(String[] args) {

        Elevador elevador = new Elevador(5);

        elevador.mostrarAndarAtual();
        elevador.subir();
        elevador.subir();

        elevador.abrirPorta();
        elevador.subir(); // Restringe o elevador de subir

        elevador.fecharPorta();
        elevador.subir();

        elevador.descer();

        elevador.mostrarPorta();
        elevador.mostrarAndarAtual();
    }
}

