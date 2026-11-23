package br.edu.univille.domino.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.edu.univille.domino.ia.IaDominoService;
import br.edu.univille.domino.model.Jogada;
import br.edu.univille.domino.model.Jogador;
import br.edu.univille.domino.model.Lado;
import br.edu.univille.domino.model.Movimento;
import br.edu.univille.domino.model.Partida;
import br.edu.univille.domino.model.Peca;
import br.edu.univille.domino.model.StatusPartida;
import br.edu.univille.domino.model.TipoJogada;
import br.edu.univille.domino.model.TipoJogador;
import br.edu.univille.domino.repository.JogadaRepository;
import br.edu.univille.domino.repository.JogadorRepository;
import br.edu.univille.domino.repository.PartidaRepository;

@Service
public class DominoService {

    public static final String NOME_IA = "Dominó-IA";
    private static final int LIMITE_HISTORICO = 20;

    private final JogadorRepository jogadorRepository;
    private final PartidaRepository partidaRepository;
    private final JogadaRepository jogadaRepository;
    private final IaDominoService iaService;

    public DominoService(JogadorRepository jogadorRepository, PartidaRepository partidaRepository,
                         JogadaRepository jogadaRepository, IaDominoService iaService) {
        this.jogadorRepository = jogadorRepository;
        this.partidaRepository = partidaRepository;
        this.jogadaRepository = jogadaRepository;
        this.iaService = iaService;
    }

    // ------------------------------------------------------------------ criação

    @Transactional
    public Partida iniciarPartida(String nomeInformado) {
        String nome = nomeInformado == null ? "" : nomeInformado.trim();
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Informe seu nome para jogar.");
        }
        if (nome.length() > 30) {
            nome = nome.substring(0, 30);
        }
        final String nomeJogador = nome;
        if (nomeJogador.equalsIgnoreCase(NOME_IA)) {
            throw new IllegalArgumentException("Esse nome é reservado para a IA. Escolha outro.");
        }

        Jogador humano = jogadorRepository.findByNomeIgnoreCase(nomeJogador)
                .orElseGet(() -> jogadorRepository.save(new Jogador(nomeJogador, TipoJogador.HUMANO)));
        if (humano.getTipo() == TipoJogador.IA) {
            throw new IllegalArgumentException("Esse nome é reservado para a IA. Escolha outro.");
        }
        Jogador ia = obterJogadorIA();

        List<Peca> pecas = Regras.todasAsPecas();
        Collections.shuffle(pecas);
        List<Peca> maoHumano = new ArrayList<>(pecas.subList(0, 7));
        List<Peca> maoIA = new ArrayList<>(pecas.subList(7, 14));
        List<Peca> monte = new ArrayList<>(pecas.subList(14, pecas.size()));

        Peca aberturaHumano = Regras.maiorPecaDeAbertura(maoHumano);
        Peca aberturaIA = Regras.maiorPecaDeAbertura(maoIA);
        boolean humanoAbre = Regras.valorDeAbertura(aberturaHumano) > Regras.valorDeAbertura(aberturaIA);

        Jogador quemAbre = humanoAbre ? humano : ia;
        Peca abertura = humanoAbre ? aberturaHumano : aberturaIA;
        Regras.remover(humanoAbre ? maoHumano : maoIA, abertura);
        List<Peca> mesa = new ArrayList<>();
        Regras.colocar(mesa, abertura, Lado.ESQUERDA);

        Partida partida = new Partida(humano, ia);
        partida.setMesa(Regras.paraTexto(mesa));
        partida.setMaoJogador1(Regras.paraTexto(maoHumano));
        partida.setMaoJogador2(Regras.paraTexto(maoIA));
        partida.setMonte(Regras.paraTexto(monte));
        partida.setVezDe(humanoAbre ? ia : humano);
        partida = partidaRepository.save(partida);

        registrarJogada(partida, quemAbre, TipoJogada.JOGOU, abertura, null,
                humanoAbre ? null : "Abri o jogo com a maior peça!");

        if (humanoAbre) {
            executarTurnoIA(partida);
        }
        return partidaRepository.save(partida);
    }

    // ------------------------------------------------------- ações do jogador

    @Transactional
    public Partida jogarHumano(Long partidaId, String pecaTexto, Lado lado) {
        Partida p = buscar(partidaId);
        validarVezDoHumano(p);

        Peca peca = Peca.parse(pecaTexto);
        List<Peca> mao = Regras.lerLista(p.getMaoJogador1());
        List<Peca> mesa = Regras.lerLista(p.getMesa());
        if (!Regras.contem(mao, peca)) {
            throw new IllegalArgumentException("Você não tem essa peça.");
        }
        if (!Regras.encaixa(mesa, peca, lado)) {
            throw new IllegalArgumentException("Essa peça não encaixa desse lado.");
        }

        Regras.remover(mao, peca);
        Regras.colocar(mesa, peca, lado);
        p.setMaoJogador1(Regras.paraTexto(mao));
        p.setMesa(Regras.paraTexto(mesa));
        p.setPassesSeguidos(0);
        registrarJogada(p, p.getJogador1(), TipoJogada.JOGOU, peca, lado, null);

        if (mao.isEmpty()) {
            finalizar(p, p.getJogador1());
        } else {
            p.setVezDe(p.getJogador2());
            executarTurnoIA(p);
        }
        return partidaRepository.save(p);
    }

    @Transactional
    public Partida comprarHumano(Long partidaId) {
        Partida p = buscar(partidaId);
        validarVezDoHumano(p);

        List<Peca> mao = Regras.lerLista(p.getMaoJogador1());
        List<Peca> mesa = Regras.lerLista(p.getMesa());
        if (!Regras.jogadasLegais(mao, mesa).isEmpty()) {
            throw new IllegalStateException("Você tem peça para jogar, não pode comprar.");
        }
        List<Peca> monte = Regras.lerLista(p.getMonte());
        if (monte.isEmpty()) {
            throw new IllegalStateException("O monte está vazio. Você deve passar a vez.");
        }

        mao.add(monte.remove(monte.size() - 1));
        p.setMaoJogador1(Regras.paraTexto(mao));
        p.setMonte(Regras.paraTexto(monte));
        registrarJogada(p, p.getJogador1(), TipoJogada.COMPROU, null, null,
                "Sem peça para as pontas " + Regras.pontaEsquerda(mesa) + " e " + Regras.pontaDireita(mesa) + ".");
        return partidaRepository.save(p);
    }

    @Transactional
    public Partida passarHumano(Long partidaId) {
        Partida p = buscar(partidaId);
        validarVezDoHumano(p);

        List<Peca> mao = Regras.lerLista(p.getMaoJogador1());
        List<Peca> mesa = Regras.lerLista(p.getMesa());
        if (!Regras.jogadasLegais(mao, mesa).isEmpty()) {
            throw new IllegalStateException("Você tem peça para jogar, não pode passar.");
        }
        if (!Regras.lerLista(p.getMonte()).isEmpty()) {
            throw new IllegalStateException("Ainda há peças no monte. Compre uma peça.");
        }

        registrarJogada(p, p.getJogador1(), TipoJogada.PASSOU, null, null,
                "Sem peça para as pontas " + Regras.pontaEsquerda(mesa) + " e " + Regras.pontaDireita(mesa) + ".");
        p.setPassesSeguidos(p.getPassesSeguidos() + 1);
        if (p.getPassesSeguidos() >= 2) {
            finalizarPorBloqueio(p);
        } else {
            p.setVezDe(p.getJogador2());
            executarTurnoIA(p);
        }
        return partidaRepository.save(p);
    }

    // ------------------------------------------------------------- turno da IA

    private void executarTurnoIA(Partida p) {
        Jogador ia = p.getJogador2();
        while (p.getStatus() == StatusPartida.EM_ANDAMENTO && p.getVezDe().getId().equals(ia.getId())) {
            List<Peca> mao = Regras.lerLista(p.getMaoJogador2());
            List<Peca> mesa = Regras.lerLista(p.getMesa());
            List<Movimento> opcoes = Regras.jogadasLegais(mao, mesa);

            if (opcoes.isEmpty()) {
                List<Peca> monte = Regras.lerLista(p.getMonte());
                if (!monte.isEmpty()) {
                    mao.add(monte.remove(monte.size() - 1));
                    p.setMaoJogador2(Regras.paraTexto(mao));
                    p.setMonte(Regras.paraTexto(monte));
                    registrarJogada(p, ia, TipoJogada.COMPROU, null, null, "Sem jogada, comprei uma peça.");
                } else {
                    registrarJogada(p, ia, TipoJogada.PASSOU, null, null, "Sem jogada e o monte acabou. Passo a vez.");
                    p.setPassesSeguidos(p.getPassesSeguidos() + 1);
                    if (p.getPassesSeguidos() >= 2) {
                        finalizarPorBloqueio(p);
                    } else {
                        p.setVezDe(p.getJogador1());
                    }
                }
                continue;
            }

            int pecasOponente = Regras.lerLista(p.getMaoJogador1()).size();
            int tamanhoMonte = Regras.lerLista(p.getMonte()).size();
            IaDominoService.Escolha escolha = iaService.escolher(
                    mao, mesa, pecasOponente, tamanhoMonte, opcoes, historicoParaIA(p));
            Movimento mov = opcoes.get(escolha.indice());

            Regras.remover(mao, mov.peca());
            Regras.colocar(mesa, mov.peca(), mov.lado());
            p.setMaoJogador2(Regras.paraTexto(mao));
            p.setMesa(Regras.paraTexto(mesa));
            p.setPassesSeguidos(0);
            registrarJogada(p, ia, TipoJogada.JOGOU, mov.peca(), mov.lado(), escolha.comentario());

            if (mao.isEmpty()) {
                finalizar(p, ia);
            } else {
                p.setVezDe(p.getJogador1());
            }
        }
    }

    // ------------------------------------------------------------ fim de jogo

    private void finalizar(Partida p, Jogador vencedor) {
        p.setStatus(StatusPartida.FINALIZADA);
        p.setVencedor(vencedor);
        p.setDataFim(LocalDateTime.now());
    }

    /** Jogo bloqueado: vence quem tem menos pontos na mão; se empatar, não há vencedor. */
    private void finalizarPorBloqueio(Partida p) {
        int pontos1 = Regras.soma(Regras.lerLista(p.getMaoJogador1()));
        int pontos2 = Regras.soma(Regras.lerLista(p.getMaoJogador2()));
        Jogador vencedor = null;
        if (pontos1 < pontos2) {
            vencedor = p.getJogador1();
        } else if (pontos2 < pontos1) {
            vencedor = p.getJogador2();
        }
        finalizar(p, vencedor);
    }

    // -------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public PartidaView montarView(Long id) {
        Partida p = buscar(id);
        List<Peca> mesa = Regras.lerLista(p.getMesa());
        List<Peca> mao = Regras.lerLista(p.getMaoJogador1());
        List<Peca> maoIA = Regras.lerLista(p.getMaoJogador2());
        List<Peca> monte = Regras.lerLista(p.getMonte());

        boolean emAndamento = p.getStatus() == StatusPartida.EM_ANDAMENTO;
        boolean vezDoHumano = emAndamento && p.getVezDe() != null
                && p.getVezDe().getId().equals(p.getJogador1().getId());

        List<PartidaView.PecaMao> pecasMao = new ArrayList<>();
        for (Peca peca : mao) {
            boolean esquerda = vezDoHumano && Regras.encaixa(mesa, peca, Lado.ESQUERDA);
            boolean direita = vezDoHumano && Regras.encaixa(mesa, peca, Lado.DIREITA);
            pecasMao.add(new PartidaView.PecaMao(peca, esquerda, direita));
        }
        boolean semJogada = vezDoHumano && Regras.jogadasLegais(mao, mesa).isEmpty();

        List<PartidaView.Lance> lances = new ArrayList<>();
        String ultimoComentarioIA = null;
        int indiceCentro = 0;
        for (Jogada j : jogadaRepository.findByPartidaOrderByOrdemAsc(p)) {
            if (j.getTipo() == TipoJogada.JOGOU && j.getLado() == Lado.ESQUERDA) {
                indiceCentro++;
            }
            boolean ehIA = j.getJogador().getTipo() == TipoJogador.IA;
            String comentario = ehIA ? vazioParaNulo(j.getComentario()) : null;
            if (comentario != null) {
                ultimoComentarioIA = comentario;
            }
            lances.add(new PartidaView.Lance(j.getOrdem(), j.getJogador().getNome(), ehIA, descrever(j), comentario));
        }
        Collections.reverse(lances);
        if (lances.size() > LIMITE_HISTORICO) {
            lances = new ArrayList<>(lances.subList(0, LIMITE_HISTORICO));
        }

        String resultado = null;
        if (!emAndamento) {
            if (p.getVencedor() == null) {
                resultado = "Empate! O jogo bloqueou com a mesma pontuação.";
            } else if (p.getVencedor().getId().equals(p.getJogador1().getId())) {
                resultado = "Você venceu, " + p.getJogador1().getNome() + "!";
            } else {
                resultado = "A IA venceu desta vez.";
            }
            if (!mao.isEmpty() && !maoIA.isEmpty()) {
                resultado += " (Jogo bloqueado: vence quem tem menos pontos na mão.)";
            }
        }

        return new PartidaView(
                p.getId(),
                p.getJogador1().getNome(),
                p.getJogador2().getNome(),
                emAndamento,
                vezDoHumano,
                mesa,
                Regras.pontaEsquerda(mesa),
                Regras.pontaDireita(mesa),
                pecasMao,
                maoIA.size(),
                monte.size(),
                semJogada && !monte.isEmpty(),
                semJogada && monte.isEmpty(),
                resultado,
                ultimoComentarioIA,
                lances,
                emAndamento ? List.of() : maoIA,
                p.getMesa(),
                indiceCentro);
    }

    @Transactional(readOnly = true)
    public List<RankingItem> ranking() {
        record Linha(Jogador jogador, long vitorias, long partidas) {
        }

        List<Linha> linhas = new ArrayList<>();
        for (Jogador j : jogadorRepository.findAll()) {
            long partidas = partidaRepository.contarPartidasFinalizadas(j, StatusPartida.FINALIZADA);
            if (partidas > 0) {
                linhas.add(new Linha(j, partidaRepository.countByVencedor(j), partidas));
            }
        }
        linhas.sort(Comparator.<Linha>comparingLong(Linha::vitorias).reversed()
                .thenComparingLong(Linha::partidas));

        List<RankingItem> ranking = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            Linha l = linhas.get(i);
            ranking.add(new RankingItem(i + 1, l.jogador().getId(), l.jogador().getNome(),
                    l.jogador().getTipo() == TipoJogador.IA,
                    l.vitorias(), l.partidas(), Math.round(l.vitorias() * 100.0 / l.partidas())));
        }
        return ranking;
    }

    /** Números do painel: IA contra humanos, top 3, vitórias por jogador e últimas partidas. */
    @Transactional(readOnly = true)
    public DashboardView dashboard() {
        long total = partidaRepository.countByStatus(StatusPartida.FINALIZADA);
        long vitoriasIA = partidaRepository.countByStatusAndVencedorTipo(StatusPartida.FINALIZADA, TipoJogador.IA);
        long vitoriasHumanos = partidaRepository.countByStatusAndVencedorTipo(StatusPartida.FINALIZADA, TipoJogador.HUMANO);
        long empates = Math.max(0, total - vitoriasIA - vitoriasHumanos);

        long percentIA = total == 0 ? 0 : Math.round(vitoriasIA * 100.0 / total);
        long percentHumanos = total == 0 ? 0 : Math.round(vitoriasHumanos * 100.0 / total);
        long percentEmpates = total == 0 ? 0 : Math.max(0, 100 - percentIA - percentHumanos);
        long fimHumanos = Math.min(100, percentIA + percentHumanos);
        String gradiente = "conic-gradient(#7c3aed 0% " + percentIA + "%, #ffd966 " + percentIA + "% "
                + fimHumanos + "%, #9ca3af " + fimHumanos + "% 100%)";

        List<RankingItem> ranking = ranking();
        List<RankingItem> top3 = new ArrayList<>(ranking.subList(0, Math.min(3, ranking.size())));
        List<RankingItem> top5 = ranking.subList(0, Math.min(5, ranking.size()));
        long maior = top5.stream().mapToLong(RankingItem::vitorias).max().orElse(0);
        List<DashboardView.Barra> barras = new ArrayList<>();
        for (RankingItem r : top5) {
            long largura = (maior == 0 || r.vitorias() == 0) ? 0 : Math.max(4, r.vitorias() * 100 / maior);
            barras.add(new DashboardView.Barra(r.nome(), r.ia(), r.vitorias(), largura));
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        List<DashboardView.UltimaPartida> ultimas = new ArrayList<>();
        for (Partida p : partidaRepository.findTop5ByStatusOrderByDataFimDesc(StatusPartida.FINALIZADA)) {
            boolean empate = p.getVencedor() == null;
            boolean iaVenceu = !empate && p.getVencedor().getTipo() == TipoJogador.IA;
            ultimas.add(new DashboardView.UltimaPartida(p.getId(), p.getJogador1().getNome(),
                    empate ? "Empate" : p.getVencedor().getNome(), iaVenceu, empate,
                    p.getDataFim() == null ? "" : p.getDataFim().format(formato)));
        }

        return new DashboardView(total, vitoriasIA, vitoriasHumanos, empates, percentIA, percentHumanos,
                percentEmpates, gradiente, top3, barras, ultimas);
    }

    // ------------------------------------------------- gerenciar o ranking

    /** Remove um jogador do ranking, apagando também as partidas e jogadas dele. Retorna o nome removido. */
    @Transactional
    public String removerJogador(Long jogadorId) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado."));
        if (jogador.getTipo() == TipoJogador.IA) {
            throw new IllegalArgumentException(
                    "A IA não pode ser removida sozinha, porque ela joga todas as partidas. Use \"Limpar ranking\".");
        }
        List<Partida> partidas = partidaRepository.findByJogador1OrJogador2(jogador, jogador);
        for (Partida partida : partidas) {
            jogadaRepository.deleteByPartida(partida);
        }
        jogadaRepository.flush();
        partidaRepository.deleteAll(partidas);
        partidaRepository.flush();
        jogadorRepository.delete(jogador);
        return jogador.getNome();
    }

    /** Apaga todas as jogadas, partidas e jogadores. A IA é recriada automaticamente na próxima partida. */
    @Transactional
    public void limparRanking() {
        jogadaRepository.deleteAllInBatch();
        partidaRepository.deleteAllInBatch();
        jogadorRepository.deleteAllInBatch();
    }

    // ---------------------------------------------------------------- apoio

    private Partida buscar(Long id) {
        return partidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida não encontrada."));
    }

    private Jogador obterJogadorIA() {
        return jogadorRepository.findFirstByTipo(TipoJogador.IA)
                .orElseGet(() -> jogadorRepository.save(new Jogador(NOME_IA, TipoJogador.IA)));
    }

    private void validarVezDoHumano(Partida p) {
        if (p.getStatus() != StatusPartida.EM_ANDAMENTO) {
            throw new IllegalStateException("Essa partida já terminou.");
        }
        if (!p.getVezDe().getId().equals(p.getJogador1().getId())) {
            throw new IllegalStateException("Não é a sua vez.");
        }
    }

    private void registrarJogada(Partida partida, Jogador jogador, TipoJogada tipo, Peca peca,
                                 Lado lado, String comentario) {
        int ordem = (int) jogadaRepository.countByPartida(partida) + 1;
        jogadaRepository.save(new Jogada(partida, jogador, ordem, tipo,
                peca == null ? null : peca.toString(), lado, comentario));
    }

    private String descrever(Jogada j) {
        return switch (j.getTipo()) {
            case JOGOU -> j.getLado() == null
                    ? "abriu o jogo com " + j.getPeca()
                    : "jogou " + j.getPeca() + (j.getLado() == Lado.ESQUERDA ? " na ponta esquerda" : " na ponta direita");
            case COMPROU -> "comprou uma peça do monte";
            case PASSOU -> "passou a vez";
        };
    }

    private List<String> historicoParaIA(Partida p) {
        List<Jogada> jogadas = jogadaRepository.findByPartidaOrderByOrdemAsc(p);
        int inicio = Math.max(0, jogadas.size() - 12);
        List<String> linhas = new ArrayList<>();
        for (Jogada j : jogadas.subList(inicio, jogadas.size())) {
            boolean ehIA = j.getJogador().getTipo() == TipoJogador.IA;
            String linha = (ehIA ? "IA " : "Oponente ") + descrever(j);
            if (!ehIA && j.getTipo() != TipoJogada.JOGOU && j.getComentario() != null) {
                linha += " (" + j.getComentario() + ")";
            }
            linhas.add(linha);
        }
        return linhas;
    }

    private String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
