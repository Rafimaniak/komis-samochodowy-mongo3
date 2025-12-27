package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.Samochod;
import pl.komis.model.Serwis;
import pl.komis.service.SamochodService;
import pl.komis.service.SerwisService;
import pl.komis.service.PracownikService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/serwis")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SerwisController {

    private final SerwisService serwisService;
    private final SamochodService samochodService;
    private final PracownikService pracownikService;

    @GetMapping
    public String listSerwisy(Model model) {
        List<Serwis> serwisy = serwisService.findAll();
        model.addAttribute("serwisy", serwisy);
        model.addAttribute("tytul", "Lista serwisów");
        return "serwis/lista";
    }

    @GetMapping("/nowy")
    public String newSerwisForm(Model model) {
        List<Samochod> samochody = samochodService.findAll();
        model.addAttribute("serwis", new Serwis());
        model.addAttribute("samochody", samochody);
        model.addAttribute("pracownicy", pracownikService.findAll());
        model.addAttribute("tytul", "Dodaj nowy serwis");
        return "serwis/form";
    }

    @PostMapping("/zapisz")
    public String saveSerwis(@ModelAttribute Serwis serwis, RedirectAttributes redirectAttributes) {
        try {
            serwisService.save(serwis);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis zapisany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/serwis";
    }

    @GetMapping("/edytuj/{id}")
    public String editSerwisForm(@PathVariable String id, Model model) {
        Serwis serwis = serwisService.findById(id)
                .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));
        List<Samochod> samochody = samochodService.findAll();

        model.addAttribute("serwis", serwis);
        model.addAttribute("samochody", samochody);
        model.addAttribute("pracownicy", pracownikService.findAll());
        model.addAttribute("tytul", "Edytuj serwis");
        return "serwis/form";
    }

    @PostMapping("/usun/{id}")
    public String deleteSerwis(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            serwisService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis usunięty pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/serwis";
    }

    @GetMapping("/rezerwuj")
    public String reserveServiceForm(Model model) {
        List<Samochod> samochody = samochodService.findAvailableCars();
        model.addAttribute("samochody", samochody);
        model.addAttribute("pracownicy", pracownikService.findAll());
        model.addAttribute("tytul", "Zarezerwuj serwis");
        return "serwis/reserve-form";
    }

    @PostMapping("/rezerwuj")
    public String reserveService(@RequestParam String samochodId,
                                 @RequestParam String pracownikId,
                                 @RequestParam String opisUslugi,
                                 @RequestParam(required = false) BigDecimal szacowanyKoszt,
                                 @RequestParam LocalDate dataSerwisu,
                                 RedirectAttributes redirectAttributes) {
        try {
            Serwis serwis = new Serwis();
            serwis.setSamochod(samochodService.findById(samochodId).orElseThrow());
            serwis.setPracownik(pracownikService.findById(pracownikId).orElseThrow());
            serwis.setOpisUslugi(opisUslugi);
            serwis.setKoszt(null); // null oznacza zarezerwowany
            serwis.setDataSerwisu(dataSerwisu);

            serwisService.save(serwis);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis zarezerwowany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/serwis";
    }

    @PostMapping("/zakoncz/{id}")
    public String completeService(@PathVariable String id,
                                  @RequestParam BigDecimal rzeczywistyKoszt,
                                  @RequestParam(required = false) String dodatkoweUwagi,
                                  RedirectAttributes redirectAttributes) {
        try {
            Serwis serwis = serwisService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));

            serwis.setKoszt(rzeczywistyKoszt);
            if (dodatkoweUwagi != null && !dodatkoweUwagi.trim().isEmpty()) {
                serwis.setOpisUslugi(serwis.getOpisUslugi() + "\n" + dodatkoweUwagi);
            }

            serwisService.save(serwis);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis zakończony pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/serwis";
    }

    @PostMapping("/anuluj/{id}")
    public String cancelService(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            serwisService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis anulowany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/serwis";
    }

    @GetMapping("/statystyki")
    public String statistics(Model model) {
        long reserved = serwisService.countReservedServices();
        long completed = serwisService.countCompletedServices();
        BigDecimal totalCost = serwisService.getTotalServiceCost();

        model.addAttribute("reserved", reserved);
        model.addAttribute("completed", completed);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("tytul", "Statystyki serwisowe");
        return "serwis/statystyki";
    }

    @GetMapping("/zarezerwowane")
    public String reservedServices(Model model) {
        List<Serwis> reservedServices = serwisService.findAll().stream()
                .filter(s -> s.getKoszt() == null)
                .toList();
        model.addAttribute("serwisy", reservedServices);
        model.addAttribute("tytul", "Zarezerwowane serwisy");
        return "serwis/lista-zarezerwowane";
    }

    @GetMapping("/zakonczone")
    public String completedServices(Model model) {
        List<Serwis> completedServices = serwisService.findAll().stream()
                .filter(s -> s.getKoszt() != null)
                .toList();
        model.addAttribute("serwisy", completedServices);
        model.addAttribute("tytul", "Zakończone serwisy");
        return "serwis/lista-zakonczone";
    }
}