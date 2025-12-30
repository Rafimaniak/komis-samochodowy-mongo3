package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.*;
import pl.komis.service.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/zakupy")
@RequiredArgsConstructor
public class ZakupController {

    private final ZakupService zakupService;
    private final UserService userService;
    private final KlientService klientService;
    private final SamochodService samochodService;
    private final PracownikService pracownikService;

    @GetMapping("/rabaty")
    @PreAuthorize("hasRole('ADMIN')")
    public String listaRabatow(Model model) {
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
        model.addAttribute("totalSaldo", totalSaldo);
        model.addAttribute("totalWydane", totalWydane);
        model.addAttribute("klienciZPremia", klienciZPremia);
        model.addAttribute("tytul", "System premii i rabatów");
        return "zakupy/rabaty";
    }

    // Dla admina: pełna lista zakupów
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listZakupy(Model model) {
        List<Zakup> zakupy = zakupService.findAll();
        System.out.println("DEBUG: Pobrano " + zakupy.size() + " zakupów");
        model.addAttribute("zakupy", zakupy);
        model.addAttribute("tytul", "Lista Wszystkich Zakupów");
        return "zakupy/lista-admin";
    }

    @GetMapping("/wykorzystaj-saldo")
    @PreAuthorize("isAuthenticated()")
    public String wykorzystajSaldoForm(@RequestParam String samochodId,
                                       @RequestParam BigDecimal cena,
                                       Authentication authentication,
                                       Model model) {
        String username = authentication.getName();

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        if (user.getKlient() == null) {
            throw new RuntimeException("Nie masz powiązanego klienta");
        }

        Klient klient = klientService.findById(user.getKlient().getId())
                .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));

        // Pobierz samochód
        Samochod samochod = samochodService.findById(samochodId)
                .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

        // SPRAWDŹ CZY SAMOCHÓD JEST ZAREZERWOWANY
        if ("ZAREZERWOWANY".equals(samochod.getStatus())) {
            if (samochod.getZarezerwowanyPrzez() == null) {
                throw new RuntimeException("Samochód jest zarezerwowany, ale brak informacji o kliencie");
            } else if (!samochod.getZarezerwowanyPrzez().getId().equals(klient.getId())) {
                throw new RuntimeException("Ten samochód jest zarezerwowany przez innego klienta. " +
                        "Tylko osoba rezerwująca może go kupić.");
            }
        }

        // Sprawdź czy samochód jest dostępny
        if (!"DOSTEPNY".equals(samochod.getStatus()) && !"ZAREZERWOWANY".equals(samochod.getStatus())) {
            throw new RuntimeException("Samochód nie jest dostępny do zakupu. Status: " + samochod.getStatus());
        }

        // Reszta metody pozostaje bez zmian...
        model.addAttribute("samochodId", samochodId);
        model.addAttribute("cenaBazowa", cena);
        model.addAttribute("klient", klient);
        model.addAttribute("saldoKlienta", klient.getSaldoPremii());
        model.addAttribute("maksWykorzystanie", cena.min(klient.getSaldoPremii()));

        // Dodaj informację o samochodzie do modelu
        model.addAttribute("samochod", samochod);

        // Dodaj info o premii
        model.addAttribute("procentPremii", klient.getProcentPremii());
        if (klient.getProcentPremii().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal premia = cena.multiply(klient.getProcentPremii())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            model.addAttribute("premiaOdZakupu", premia);
        }

        // Dodaj listę pracowników
        List<Pracownik> pracownicy = pracownikService.findAll();
        model.addAttribute("pracownicy", pracownicy);

        return "zakupy/wykorzystaj-saldo";
    }

    // POST-wykonanie zakupu z saldem - UŻYWAMY TERAZ PROCEDURY Z BAZY
    @PostMapping("/wykorzystaj-saldo")
    @PreAuthorize("isAuthenticated()")
    public String wykonajZakupZSaldem(@RequestParam String samochodId,
                                      @RequestParam BigDecimal cenaBazowa,
                                      @RequestParam(required = false) BigDecimal wykorzystaneSaldo,
                                      @RequestParam String pracownikId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {

        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            Klient klient = user.getKlient();
            if (klient == null) {
                throw new RuntimeException("Nie masz powiązanego klienta");
            }

            // Pobierz samochód
            Samochod samochod = samochodService.findById(samochodId)
                    .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));

            // Sprawdź dostępność samochodu
            if (!samochod.getStatus().equals("DOSTEPNY") &&
                    !samochod.getStatus().equals("ZAREZERWOWANY")) {
                throw new RuntimeException("Samochód nie jest dostępny do zakupu. Status: " + samochod.getStatus());
            }

            // Jeśli klient zarezerwował ten samochód, to tylko on może go kupić
            if (samochod.getStatus().equals("ZAREZERWOWANY") &&
                    (samochod.getZarezerwowanyPrzez() == null ||
                            !samochod.getZarezerwowanyPrzez().getId().equals(klient.getId()))) {
                throw new RuntimeException("Ten samochód jest zarezerwowany przez innego klienta. " +
                        "Tylko osoba rezerwująca może go kupić.");
            }

            // Pobierz pracownika
            Pracownik pracownik = pracownikService.findById(pracownikId)
                    .orElseThrow(() -> new RuntimeException("Pracownik nie znaleziony"));

            // Obsłuż wykorzystanie salda (jeśli podano)
            BigDecimal faktycznieWykorzystaneSaldo = BigDecimal.ZERO;
            if (wykorzystaneSaldo != null && wykorzystaneSaldo.compareTo(BigDecimal.ZERO) > 0) {
                // Sprawdź czy klient ma wystarczające saldo
                if (klient.getSaldoPremii().compareTo(wykorzystaneSaldo) < 0) {
                    throw new RuntimeException("Niewystarczające saldo premii");
                }
                faktycznieWykorzystaneSaldo = wykorzystaneSaldo.min(cenaBazowa);
            }

            // UŻYCIE PROCEDURY Z BAZY DANYCH zamiast ręcznego tworzenia Zakupu
            String zakupId = String.valueOf(zakupService.createZakupZSaldem(
                    samochodId,
                    user.getId(),
                    pracownikId,
                    cenaBazowa,
                    faktycznieWykorzystaneSaldo
            ));

            // Pobierz zaktualizowane dane klienta po akcji triggera
            Klient zaktualizowanyKlient = klientService.findById(klient.getId())
                    .orElseThrow(() -> new RuntimeException("Klient nie znaleziony po zakupie"));

            // Przygotuj komunikat sukcesu z aktualnymi danymi
            String message;
            if (faktycznieWykorzystaneSaldo.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal zaoszczedzone = faktycznieWykorzystaneSaldo;
                message = String.format(
                        "Samochód kupiony z wykorzystaniem salda!<br>" +
                                "Wykorzystano saldo: <strong>%.2f zł</strong><br>" +
                                "Nowe saldo: <strong>%.2f zł</strong>",
                        zaoszczedzone, zaktualizowanyKlient.getSaldoPremii()
                );
            } else {
                message = String.format(
                        "Samochód kupiony pomyślnie!<br>" +
                                "Twoje nowe saldo premii: <strong>%.2f zł</strong>",
                        zaktualizowanyKlient.getSaldoPremii()
                );
            }

            redirectAttributes.addFlashAttribute("successMessage", message);

            return "redirect:/zakupy/moje";

        } catch (Exception e) {
            System.err.println("Błąd podczas zakupu z wykorzystaniem salda: " + e.getMessage());
            e.printStackTrace();

            // Przekieruj z błędem
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas zakupu: " + e.getMessage());
            return "redirect:/samochody/szczegoly?id=" + samochodId;
        }
    }

    // Dla użytkownika: tylko jego zakupy
    @GetMapping("/moje")
    @PreAuthorize("isAuthenticated()")
    public String mojeZakupy(Authentication authentication, Model model) {
        String username = authentication.getName();

        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            List<Zakup> zakupy = new ArrayList<>();
            Klient klient = null;

            // ZAWSZE upewnij się że użytkownik ma klienta
            klient = userService.ensureUserHasKlient(user.getId());

            if (klient == null) {
                throw new RuntimeException("Nie można utworzyć/pobrać klienta");
            }

            // Pobierz zakupy tylko jeśli klient istnieje
            zakupy = zakupService.findByKlientId(klient.getId());

            System.out.println("DEBUG: Dla klienta ID: " + klient.getId() +
                    " znaleziono " + zakupy.size() + " zakupów");

            // Oblicz statystyki
            BigDecimal sumaCenaBazowa = zakupy.stream()
                    .map(z -> z.getCenaBazowa() != null ? z.getCenaBazowa() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaWykorzystaneSaldo = zakupy.stream()
                    .map(z -> z.getWykorzystaneSaldo() != null ? z.getWykorzystaneSaldo() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaNaliczonaPremia = zakupy.stream()
                    .map(z -> z.getNaliczonaPremia() != null ? z.getNaliczonaPremia() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaCenaZakupu = zakupy.stream()
                    .map(z -> z.getCenaZakupu() != null ? z.getCenaZakupu() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("zakupy", zakupy);
            model.addAttribute("klient", klient);
            model.addAttribute("sumaCenaBazowa", sumaCenaBazowa);
            model.addAttribute("sumaWykorzystaneSaldo", sumaWykorzystaneSaldo);
            model.addAttribute("sumaNaliczonaPremia", sumaNaliczonaPremia);
            model.addAttribute("sumaCenaZakupu", sumaCenaZakupu);
            model.addAttribute("tytul", "Moje Zakupy");

            return "zakupy/lista-klient";

        } catch (Exception e) {
            System.err.println("BŁĄD w mojeZakupy: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania zakupów: " + e.getMessage());
            return "error";
        }
    }

    // GET - szczegóły zakupów klienta (dla admina)
    @GetMapping("/moje/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String zakupyKlienta(@PathVariable String id, Model model) {
        try {
            // Znajdź klienta
            Klient klient = klientService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));

            // Pobierz zakupy klienta
            List<Zakup> zakupy = zakupService.findByKlientId(id);

            // Oblicz statystyki
            BigDecimal sumaCenaBazowa = zakupy.stream()
                    .map(z -> z.getCenaBazowa() != null ? z.getCenaBazowa() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaWykorzystaneSaldo = zakupy.stream()
                    .map(z -> z.getWykorzystaneSaldo() != null ? z.getWykorzystaneSaldo() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaNaliczonaPremia = zakupy.stream()
                    .map(z -> z.getNaliczonaPremia() != null ? z.getNaliczonaPremia() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaCenaZakupu = zakupy.stream()
                    .map(z -> z.getCenaZakupu() != null ? z.getCenaZakupu() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Dodaj do modelu
            model.addAttribute("zakupy", zakupy);
            model.addAttribute("klient", klient);
            model.addAttribute("sumaCenaBazowa", sumaCenaBazowa);
            model.addAttribute("sumaWykorzystaneSaldo", sumaWykorzystaneSaldo);
            model.addAttribute("sumaNaliczonaPremia", sumaNaliczonaPremia);
            model.addAttribute("sumaCenaZakupu", sumaCenaZakupu);
            model.addAttribute("tytul", "Zakupy klienta: " + klient.getImie() + " " + klient.getNazwisko());

            return "zakupy/lista-klient";

        } catch (Exception e) {
            System.err.println("BŁĄD w zakupyKlienta: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania zakupów: " + e.getMessage());
            return "error";
        }
    }

    // GET - szczegóły konkretnego zakupu (dla admina)
    @GetMapping("/szczegoly/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String szczegolyZakupu(@PathVariable String id, Model model) {
        try {
            Zakup zakup = zakupService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Zakup nie znaleziony"));

            model.addAttribute("zakup", zakup);
            model.addAttribute("tytul", "Szczegóły zakupu #" + zakup.getId());

            return "zakupy/szczegoly";
        } catch (Exception e) {
            System.err.println("BŁĄD w szczegolyZakupu: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania zakupu: " + e.getMessage());
            return "error";
        }

    }

    // Pomocnicza metoda do znajdowania zakupów po emailu klienta
    private List<Zakup> findZakupyByUserEmail(String email) {
        // Najpierw znajdź klienta po emailu
        return klientService.findByEmail(email)
                .map(klient -> zakupService.findByKlientId(klient.getId()))
                .orElse(List.of());
    }

    // Usuwanie tylko dla admina - UŻYWAMY TERAZ PROCEDURY Z BAZY
    @PostMapping("/usun/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteZakup(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            zakupService.remove(id);
            redirectAttributes.addFlashAttribute("successMessage", "Zakup #" + id + " został usunięty.");
        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania zakupu: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas usuwania zakupu: " + e.getMessage());
        }
        return "redirect:/zakupy";
    }
    // Dodaj tę metodę do debugowania
    @GetMapping("/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public String debugZakupy(Model model) {
        try {
            // Pobierz wszystkich klientów
            List<Klient> klienci = klientService.findAll();

            // Dla każdego klienta sprawdź zakupy
            Map<String, Object> debugInfo = new HashMap<>();
            for (Klient klient : klienci) {
                List<Zakup> zakupy = zakupService.findByKlientId(klient.getId());

                Map<String, Object> klientInfo = new HashMap<>();
                klientInfo.put("klient", klient);
                klientInfo.put("liczbaZakupow", zakupy.size());
                klientInfo.put("zakupy", zakupy);

                debugInfo.put(klient.getId(), klientInfo);
            }

            model.addAttribute("debugInfo", debugInfo);
            model.addAttribute("totalClients", klienci.size());

            return "admin/debug-zakupy";

        } catch (Exception e) {
            System.err.println("Błąd w debugZakupy: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd debugowania: " + e.getMessage());
            return "error";
        }
    }
}