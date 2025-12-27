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

import java.math.BigDecimal;
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

        // Oblicz statystyki
        BigDecimal totalSaldo = klienci.stream()
                .map(k -> k.getSaldoPremii() != null ? k.getSaldoPremii() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWydane = klienci.stream()
                .map(k -> k.getTotalWydane() != null ? k.getTotalWydane() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long klienciZPremia = klienci.stream()
                .filter(k -> k.getProcentPremii() != null && k.getProcentPremii().compareTo(BigDecimal.ZERO) > 0)
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