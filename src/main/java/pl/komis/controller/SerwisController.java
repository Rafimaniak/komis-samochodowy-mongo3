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
        long zarezerwowane = serwisService.countZarezerwowane();
        long zakonczone = serwisService.countZakonczone();
        BigDecimal lacznyKoszt = serwisService.getTotalKoszt();

        // DODANE: Mapa samochodów i pracowników do wyświetlania nazw
        model.addAttribute("serwisy", serwisy);
        model.addAttribute("samochodyMap", serwisService.getSamochodyMap());
        model.addAttribute("pracownicyMap", serwisService.getPracownicyMap());
        model.addAttribute("zarezerwowane", zarezerwowane);
        model.addAttribute("zakonczone", zakonczone);
        model.addAttribute("lacznyKoszt", lacznyKoszt);
        model.addAttribute("tytul", "Lista Serwisów");
        return "serwis/lista";
    }

    @GetMapping("/dodaj")
    public String newSerwisForm(Model model) {
        List<Samochod> samochody = samochodService.findAll();

        model.addAttribute("serwis", new Serwis());
        model.addAttribute("samochody", samochody);
        model.addAttribute("pracownicy", pracownikService.findAll());
        model.addAttribute("tytul", "Dodaj nowy serwis");
        return "serwis/form";
    }

    @PostMapping("/zapisz")
    public String saveSerwis(@ModelAttribute Serwis serwis,
                             @RequestParam(required = false) String id,
                             @RequestParam String samochodId,
                             @RequestParam String pracownikId,
                             RedirectAttributes redirectAttributes) {
        try {
            // USTAW ID TYLKO JEŚLI JEST PODANE (DLA EDYCJI)
            if (id != null && !id.isEmpty()) {
                serwis.setId(id);
            }
            // ZAWSZE USTAWIAJ SAMOCHOD I PRACOWNIKA
            serwis.setSamochodId(samochodId);
            serwis.setPracownikId(pracownikId);

            // UPEWNIJ SIĘ ŻE DATA JEST USTAWIONA
            if (serwis.getDataSerwisu() == null) {
                serwis.setDataSerwisu(LocalDate.now());
            }

            // ZAPISZ - ID ZOSTANIE WYGENEROWANE W SERWISIE JEŚLI POTRZEBNE
            serwisService.save(serwis);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis zapisany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
            e.printStackTrace(); // DODAJ LOGOWANIE BŁĘDÓW
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
            e.printStackTrace();
        }
        return "redirect:/serwis";
    }

    // NOWA METODA: Formularz rezerwacji
    @GetMapping("/rezerwuj")
    public String rezerwujSerwisForm(Model model) {
        List<Samochod> samochody = samochodService.findAll();
        model.addAttribute("samochody", samochody);
        model.addAttribute("pracownicy", pracownikService.findAll());
        return "serwis/rezerwacja";
    }

    // NOWA METODA: Obsługa rezerwacji
    @PostMapping("/rezerwuj")
    public String rezerwujSerwis(@RequestParam String samochodId,
                                 @RequestParam String pracownikId,
                                 @RequestParam String opisUslugi,
                                 @RequestParam LocalDate dataSerwisu,
                                 RedirectAttributes redirectAttributes) {
        try {
            Serwis serwis = new Serwis();
            serwis.setSamochodId(samochodId);
            serwis.setPracownikId(pracownikId);
            serwis.setOpisUslugi(opisUslugi);
            serwis.setDataSerwisu(dataSerwisu);
            // koszt pozostaje null -> status: ZAREZERWOWANY

            serwisService.save(serwis);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis zarezerwowany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/serwis";
    }

    // NOWA METODA: Formularz zakończenia serwisu
    @GetMapping("/zakoncz/{id}")
    public String zakonczSerwisForm(@PathVariable String id, Model model) {
        Serwis serwis = serwisService.findById(id)
                .orElseThrow(() -> new RuntimeException("Serwis nie znaleziony"));
        model.addAttribute("serwis", serwis);
        return "serwis/zakoncz";
    }

    // NOWA METODA: Obsługa zakończenia serwisu
    @PostMapping("/zakoncz/{id}")
    public String zakonczSerwis(@PathVariable String id,
                                @RequestParam BigDecimal koszt,
                                RedirectAttributes redirectAttributes) {
        try {
            serwisService.zakonczSerwis(id, koszt);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis zakończony pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/serwis";
    }

    // NOWA METODA: Anulowanie serwisu
    @PostMapping("/anuluj/{id}")
    public String anulujSerwis(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            serwisService.anulujSerwis(id);
            redirectAttributes.addFlashAttribute("successMessage", "Serwis anulowany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/serwis";
    }

    @GetMapping("/zarezerwowane")
    public String reservedServices(Model model) {
        List<Serwis> reservedServices = serwisService.findZarezerwowane();
        model.addAttribute("serwisy", reservedServices);
        model.addAttribute("samochodyMap", serwisService.getSamochodyMap());
        model.addAttribute("pracownicyMap", serwisService.getPracownicyMap());
        model.addAttribute("tytul", "Zarezerwowane serwisy");
        return "serwis/lista";
    }

    @GetMapping("/zakonczone")
    public String completedServices(Model model) {
        List<Serwis> completedServices = serwisService.findZakonczone();
        model.addAttribute("serwisy", completedServices);
        model.addAttribute("samochodyMap", serwisService.getSamochodyMap());
        model.addAttribute("pracownicyMap", serwisService.getPracownicyMap());
        model.addAttribute("tytul", "Zakończone serwisy");
        return "serwis/lista";
    }
}