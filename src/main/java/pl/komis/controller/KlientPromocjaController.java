package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.KlientPromocja;
import pl.komis.service.KlientPromocjaService;
import pl.komis.service.KlientService;
import pl.komis.service.PromocjaService;

import java.util.List;

@Controller
@RequestMapping("/klient-promocje")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class KlientPromocjaController {

    private final KlientPromocjaService klientPromocjaService;
    private final KlientService klientService;
    private final PromocjaService promocjaService;

    @GetMapping
    public String listAll(Model model) {
        List<KlientPromocja> klientPromocje = klientPromocjaService.findAll();
        model.addAttribute("klientPromocje", klientPromocje);
        model.addAttribute("tytul", "Przypisane promocje klientów");
        return "klient-promocje/lista";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable String id, Model model) {
        KlientPromocja klientPromocja = klientPromocjaService.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono przypisania promocji"));

        model.addAttribute("klientPromocja", klientPromocja);
        model.addAttribute("tytul", "Szczegóły przypisania promocji");
        return "klient-promocje/szczegoly";
    }

    @GetMapping("/przypisz")
    public String assignForm(Model model) {
        model.addAttribute("klienci", klientService.findAll());
        model.addAttribute("promocje", promocjaService.findActivePromotions());
        model.addAttribute("tytul", "Przypisz promocję klientowi");
        return "klient-promocje/przypisz";
    }

    @PostMapping("/przypisz")
    public String assignPromotion(@RequestParam String klientId,
                                  @RequestParam String promocjaId,
                                  RedirectAttributes redirectAttributes) {
        try {
            KlientPromocja klientPromocja = klientPromocjaService.przypiszPromocjeKlientowi(klientId, promocjaId);
            redirectAttributes.addFlashAttribute("successMessage", "Promocja przypisana pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/klient-promocje";
    }

    @PostMapping("/{id}/usun")
    public String remove(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            klientPromocjaService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Przypisanie promocji usunięte");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/klient-promocje";
    }

    @GetMapping("/klient/{klientId}")
    public String promocjeKlienta(@PathVariable String klientId, Model model) {
        List<KlientPromocja> promocje = klientPromocjaService.getPromocjeForKlient(klientId);
        model.addAttribute("promocje", promocje);
        model.addAttribute("klient", klientService.findById(klientId).orElseThrow());
        model.addAttribute("tytul", "Promocje klienta");
        return "klient-promocje/lista-klient";
    }

    @GetMapping("/promocja/{promocjaId}")
    public String klienciPromocji(@PathVariable String promocjaId, Model model) {
        List<KlientPromocja> klienci = klientPromocjaService.getKlienciWithPromocja(promocjaId);
        model.addAttribute("klienci", klienci);
        model.addAttribute("promocja", promocjaService.findById(promocjaId).orElseThrow());
        model.addAttribute("tytul", "Klienci z promocją");
        return "klient-promocje/lista-promocja";
    }
}