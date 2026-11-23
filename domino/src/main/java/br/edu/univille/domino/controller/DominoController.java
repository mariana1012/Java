package br.edu.univille.domino.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.univille.domino.model.Lado;
import br.edu.univille.domino.model.Partida;
import br.edu.univille.domino.service.DominoService;

@Controller
public class DominoController {

    private final DominoService service;

    public DominoController(DominoService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/partidas")
    public String novaPartida(@RequestParam("nome") String nome, RedirectAttributes redirect) {
        try {
            Partida partida = service.iniciarPartida(nome);
            return "redirect:/partidas/" + partida.getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/partidas/{id}")
    public String partida(@PathVariable("id") Long id, Model model) {
        model.addAttribute("view", service.montarView(id));
        return "partida";
    }

    @PostMapping("/partidas/{id}/jogar")
    public String jogar(@PathVariable("id") Long id,
                        @RequestParam("peca") String peca,
                        @RequestParam("lado") Lado lado,
                        RedirectAttributes redirect) {
        try {
            service.jogarHumano(id, peca, lado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/partidas/" + id;
    }

    @PostMapping("/partidas/{id}/comprar")
    public String comprar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            service.comprarHumano(id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/partidas/" + id;
    }

    @PostMapping("/partidas/{id}/passar")
    public String passar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            service.passarHumano(id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/partidas/" + id;
    }

    @GetMapping("/ranking")
    public String ranking(Model model) {
        model.addAttribute("ranking", service.ranking());
        model.addAttribute("dash", service.dashboard());
        return "ranking";
    }

    @PostMapping("/ranking/limpar")
    public String limparRanking(RedirectAttributes redirect) {
        service.limparRanking();
        redirect.addFlashAttribute("mensagem", "Ranking limpo: todas as partidas e jogadores foram apagados.");
        return "redirect:/ranking";
    }

    @PostMapping("/ranking/{id}/remover")
    public String removerDoRanking(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            String nome = service.removerJogador(id);
            redirect.addFlashAttribute("mensagem", nome + " foi removido(a) do ranking.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/ranking";
    }
}
