package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.Promocja;
import pl.komis.service.PromocjaService;

import java.util.List;

@Controller
@RequestMapping("/promocje")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PromocjaController {

    private final PromocjaService promocjaService;

    @GetMapping
    public String listPromotions(Model model) {
        List<Promocja> promocje = promocjaService.findAll();
        model.addAttribute("promocje", promocje);
        model.addAttribute("tytul", "Lista promocji");
        return "promocje/lista";
    }

    @GetMapping("/{id}")
    public String promotionDetails(@PathVariable String id, Model model) {
        Promocja promocja = promocjaService.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocja nie znaleziona"));
        model.addAttribute("promocja", promocja);
        model.addAttribute("tytul", "Szczegóły promocji: " + promocja.getNazwa());
        return "promocje/szczegoly";
    }

    @GetMapping("/nowa")
    public String newPromotionForm(Model model) {
        model.addAttribute("promocja", new Promocja());
        model.addAttribute("tytul", "Dodaj nową promocję");
        return "promocje/form";
    }

    @PostMapping("/zapisz")
    public String savePromotion(@ModelAttribute Promocja promocja, RedirectAttributes redirectAttributes) {
        try {
            promocjaService.save(promocja);
            redirectAttributes.addFlashAttribute("successMessage", "Promocja zapisana pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/promocje";
    }

    @GetMapping("/edytuj/{id}")
    public String editPromotionForm(@PathVariable String id, Model model) {
        Promocja promocja = promocjaService.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocja nie znaleziona"));
        model.addAttribute("promocja", promocja);
        model.addAttribute("tytul", "Edytuj promocję: " + promocja.getNazwa());
        return "promocje/form";
    }

    @PostMapping("/usun/{id}")
    public String deletePromotion(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            promocjaService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Promocja usunięta pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/promocje";
    }

    @PostMapping("/{id}/toggle-status")
    public String togglePromotionStatus(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            promocjaService.togglePromotionStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Status promocji zmieniony");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/promocje";
    }

    @GetMapping("/aktywne")
    public String activePromotions(Model model) {
        List<Promocja> activePromotions = promocjaService.findActivePromotions();
        model.addAttribute("promocje", activePromotions);
        model.addAttribute("tytul", "Aktywne promocje");
        return "promocje/lista-aktywne";
    }

    @GetMapping("/wedlug-rodzaju")
    public String promotionsByType(@RequestParam(required = false) String rodzaj, Model model) {
        List<Promocja> promocje;
        if (rodzaj != null && !rodzaj.isEmpty()) {
            promocje = promocjaService.findByRodzaj(rodzaj);
            model.addAttribute("tytul", "Promocje rodzaju: " + rodzaj);
        } else {
            promocje = promocjaService.findAll();
            model.addAttribute("tytul", "Wszystkie promocje");
        }
        model.addAttribute("promocje", promocje);
        return "promocje/lista";
    }

    @GetMapping("/wazne")
    public String validPromotions(Model model) {
        List<Promocja> validPromotions = promocjaService.findValidPromotions();
        model.addAttribute("promocje", validPromotions);
        model.addAttribute("tytul", "Promocje ważne obecnie");
        return "promocje/lista-wazne";
    }
}