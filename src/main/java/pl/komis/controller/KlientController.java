package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.komis.model.Klient;
import pl.komis.service.KlientService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/klienci")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KlientController {

    private final KlientService klientService;

    @GetMapping
    public String listaKlientow(Model model) {
        // Pobierz wszystkich klientów (encja Klient)
        List<Klient> klienci = klientService.findAll();

        // NAPRAWIONE: Popraw wartości NaN przed użyciem
        for (Klient klient : klienci) {
            if (klient.getSaldoPremii() != null &&
                    (klient.getSaldoPremii().isNaN() || klient.getSaldoPremii().isInfinite())) {
                klient.setSaldoPremii(0.0);
            }
            if (klient.getTotalWydane() != null &&
                    (klient.getTotalWydane().isNaN() || klient.getTotalWydane().isInfinite())) {
                klient.setTotalWydane(0.0);
            }
            if (klient.getProcentPremii() != null &&
                    (klient.getProcentPremii().isNaN() || klient.getProcentPremii().isInfinite())) {
                klient.setProcentPremii(0.0);
            }
        }

        // NAPRAWIONE: Oblicz statystyki (bez NaN)
        double totalSaldo = klienci.stream()
                .filter(k -> k.getSaldoPremii() != null)
                .mapToDouble(Klient::getSaldoPremii)
                .sum();

        double totalWydane = klienci.stream()
                .filter(k -> k.getTotalWydane() != null)
                .mapToDouble(Klient::getTotalWydane)
                .sum();

        // NAPRAWIONE: Poprawne sprawdzenie czy ma premię
        long klienciZPremia = klienci.stream()
                .filter(k -> k.getProcentPremii() != null && k.getProcentPremii() > 0.0)
                .count();

        model.addAttribute("klienci", klienci);
        model.addAttribute("totalKlienci", klienci.size());
        model.addAttribute("totalSaldo", totalSaldo);
        model.addAttribute("totalWydane", totalWydane);
        model.addAttribute("klienciZPremia", klienciZPremia);
        model.addAttribute("tytul", "Lista Klientów");
        return "klienci";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String szczegolyKlienta(@PathVariable String id, Model model) {
        try {
            Optional<Klient> klientOpt = klientService.findById(id);

            if (klientOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Klient nie znaleziony");
                model.addAttribute("tytul", "Błąd");
                return "error";
            }

            Klient klient = klientOpt.get();

            // NAPRAWIONE: Popraw NaN dla pojedynczego klienta
            if (klient.getSaldoPremii() != null &&
                    (klient.getSaldoPremii().isNaN() || klient.getSaldoPremii().isInfinite())) {
                klient.setSaldoPremii(0.0);
            }
            if (klient.getTotalWydane() != null &&
                    (klient.getTotalWydane().isNaN() || klient.getTotalWydane().isInfinite())) {
                klient.setTotalWydane(0.0);
            }
            if (klient.getProcentPremii() != null &&
                    (klient.getProcentPremii().isNaN() || klient.getProcentPremii().isInfinite())) {
                klient.setProcentPremii(0.0);
            }

            model.addAttribute("klient", klient);
            model.addAttribute("tytul", "Szczegóły klienta: " + klient.getImie() + " " + klient.getNazwisko());

            return "klienci/szczegoly";

        } catch (Exception e) {
            System.err.println("BŁĄD w szczegolyKlienta: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania klienta: " + e.getMessage());
            model.addAttribute("tytul", "Błąd");
            return "error";
        }
    }
}