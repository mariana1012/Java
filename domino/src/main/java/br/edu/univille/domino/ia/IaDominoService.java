package br.edu.univille.domino.ia;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.edu.univille.domino.model.Lado;
import br.edu.univille.domino.model.Movimento;
import br.edu.univille.domino.model.Peca;
import br.edu.univille.domino.service.Regras;

import java.util.Map;
import org.springframework.ai.openai.OpenAiChatOptions;

/** Integração com o GenAI (Spring AI): a IA escolhe a jogada entre as jogadas legais. */
@Service
public class IaDominoService {

    private static final Logger log = LoggerFactory.getLogger(IaDominoService.class);

    private static final Pattern INDICE = Pattern.compile("INDICE[^0-9]{0,10}(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMENTARIO = Pattern.compile(
            "COMENT[ÁA]RIO\\s*:?\\**\\s*(.+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);

    private static final int TENTATIVAS = 3;

    private static final String SISTEMA = """
            Você é a "Dominó-IA", uma adversária bem-humorada em uma partida de dominó duplo-seis \
            (28 peças, de 0-0 a 6-6) contra uma pessoa. Seu objetivo é ganhar a partida.
            Estratégia: jogue peças de pontuação alta cedo, mantenha variedade de números na mão, \
            prefira jogadas que o oponente provavelmente não consiga responder (números em que ele \
            comprou peça ou passou a vez) e evite ficar com números sem saída.
            Você SEMPRE responde exatamente em duas linhas, sem mais nada:
            INDICE: <número da jogada escolhida>
            COMENTARIO: <comentário curto e divertido em português, no máximo 20 palavras>
            """;

    private final ChatClient chatClient;

    public IaDominoService(ChatClient.Builder builder,
                           @Value("${spring.ai.openai.api-key:}") String chave) {
        // gpt-oss e um modelo de raciocinio: com esforco baixo ele gasta menos tokens "pensando"
        // e nao devolve resposta vazia. As opcoes precisam ser aplicadas no ChatClient (defaultOptions).
        // (nesta versao do Spring AI o defaultOptions recebe o builder das opcoes, sem o .build())
        this.chatClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .extraBody(Map.of("include_reasoning", Boolean.FALSE, "reasoning_effort", "low")))
                .defaultSystem(SISTEMA)
                .build();
        if (chave == null || chave.isBlank() || chave.equals("CHAVE_NAO_CONFIGURADA")) {
            log.warn("IA: a variável de ambiente GROQ_API_KEY NÃO foi encontrada. A IA vai jogar no modo automático.");
        } else {
            // nunca mostramos a chave inteira, só o começo e o tamanho
            log.info("IA: chave encontrada ({}..., {} caracteres).", chave.substring(0, Math.min(3, chave.length())), chave.length());
        }
    }

    /** indice é 0-based e se refere à lista de opções recebida. */
    public record Escolha(int indice, String comentario, boolean usouIA) {
    }

    public Escolha escolher(List<Peca> maoIA, List<Peca> mesa, int pecasOponente, int tamanhoMonte,
                            List<Movimento> opcoes, List<String> historico) {
        if (opcoes.size() == 1) {
            return new Escolha(0, "Só tinha essa jogada!", false);
        }
        String prompt = montarPrompt(maoIA, mesa, pecasOponente, tamanhoMonte, opcoes, historico);
        Exception ultimoErro = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                String resposta = chatClient.prompt().user(prompt).call().content();
                Escolha escolha = interpretar(resposta, opcoes);
                if (escolha != null) {
                    return escolha;
                }
                boolean vazia = resposta == null || resposta.isBlank();
                log.warn("Resposta da IA {} (tentativa {}/{}): [{}]",
                        vazia ? "vazia" : "fora do formato esperado", tentativa, TENTATIVAS, resposta);
                if (tentativa < TENTATIVAS) {
                    if (!pausar(400L)) {
                        break;
                    }
                    continue;
                }
                return jogadaAutomatica(opcoes, vazia ? "a IA devolveu resposta vazia" : "resposta fora do formato esperado");
            } catch (Exception e) {
                ultimoErro = e;
                if (tentativa < TENTATIVAS && erroTemporario(e)) {
                    log.warn("IA sobrecarregada (tentativa {}/{}). Tentando de novo...", tentativa, TENTATIVAS);
                    if (!pausar(1500L * tentativa)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        log.warn("Falha ao consultar a IA", ultimoErro);
        return jogadaAutomatica(opcoes, ultimoErro == null ? "erro desconhecido" : resumir(ultimoErro));
    }

    /** Espera um pouco entre tentativas. Retorna false se a thread foi interrompida. */
    private boolean pausar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Escolha interpretar(String resposta, List<Movimento> opcoes) {
        String texto = resposta == null ? "" : resposta;
        Matcher m = INDICE.matcher(texto);
        if (m.find()) {
            int escolhido = Integer.parseInt(m.group(1));
            if (escolhido >= 1 && escolhido <= opcoes.size()) {
                Matcher c = COMENTARIO.matcher(texto);
                String comentario = c.find() ? limpar(c.group(1)) : "";
                return new Escolha(escolhido - 1, comentario, true);
            }
        }
        return null;
    }

    /** Erros que costumam passar sozinhos (servidor sobrecarregado ou limite de requisições por minuto). */
    private boolean erroTemporario(Throwable e) {
        for (Throwable t = e; t != null; t = (t.getCause() == t ? null : t.getCause())) {
            String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (msg.contains("503") || msg.contains("429") || msg.contains("unavailable")
                    || msg.contains("high demand") || msg.contains("overloaded")) {
                return true;
            }
        }
        return false;
    }

    /** Resume o erro para aparecer na tela (sem expor a chave). */
    private String resumir(Exception e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String msg = causa.getMessage() == null ? "" : causa.getMessage().replace("\n", " ").trim();
        if (msg.length() > 160) {
            msg = msg.substring(0, 160) + "...";
        }
        return causa.getClass().getSimpleName() + (msg.isEmpty() ? "" : ": " + msg);
    }

    private Escolha jogadaAutomatica(List<Movimento> opcoes, String motivo) {
        int melhor = 0;
        for (int i = 1; i < opcoes.size(); i++) {
            if (opcoes.get(i).peca().soma() > opcoes.get(melhor).peca().soma()) {
                melhor = i;
            }
        }
        return new Escolha(melhor, "(IA indisponível - " + motivo + ")", false);
    }

    private String montarPrompt(List<Peca> maoIA, List<Peca> mesa, int pecasOponente, int tamanhoMonte,
                                List<Movimento> opcoes, List<String> historico) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mesa (da esquerda para a direita): ").append(Regras.paraTexto(mesa).replace(",", " ")).append("\n");
        sb.append("Ponta esquerda: ").append(Regras.pontaEsquerda(mesa))
                .append(" | Ponta direita: ").append(Regras.pontaDireita(mesa)).append("\n");
        sb.append("Sua mão: ").append(Regras.paraTexto(maoIA).replace(",", " ")).append("\n");
        sb.append("Peças do oponente: ").append(pecasOponente)
                .append(" | Peças no monte: ").append(tamanhoMonte).append("\n");
        if (!historico.isEmpty()) {
            sb.append("Últimas jogadas:\n");
            for (String linha : historico) {
                sb.append("- ").append(linha).append("\n");
            }
        }
        sb.append("\nJogadas possíveis:\n");
        for (int i = 0; i < opcoes.size(); i++) {
            Movimento mov = opcoes.get(i);
            sb.append(i + 1).append(") peça ").append(mov.peca()).append(" na ponta ")
                    .append(mov.lado() == Lado.ESQUERDA ? "esquerda" : "direita").append("\n");
        }
        sb.append("\nEscolha uma delas.");
        return sb.toString();
    }

    private String limpar(String texto) {
        String t = texto.replace("\n", " ").replace("\"", "").trim().replaceFirst("^[:*\\s]+", "").trim();
        return t.length() > 200 ? t.substring(0, 200) : t;
    }
}