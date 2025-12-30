package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.komis.model.Klient;
import pl.komis.model.User;
import pl.komis.service.UserService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/premia")
@RequiredArgsConstructor
public class PremiaController {

    private final UserService userService;

    @GetMapping("/saldo")
    @PreAuthorize("isAuthenticated()")
    public String mojeSaldoPremii(Authentication authentication, Model model) {
        String username = authentication.getName();

        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            // Upewnij się że użytkownik ma klienta
            Klient klient = userService.ensureUserHasKlient(user.getId());

            if (klient == null) {
                model.addAttribute("errorMessage", "Nie znaleziono danych klienta");
                return "premia/saldo";
            }

            // Pobierz aktualne dane klienta
            BigDecimal saldo = klient.getSaldoPremii() != null ? klient.getSaldoPremii() : BigDecimal.ZERO;
            BigDecimal procentPremii = klient.getProcentPremii() != null ? klient.getProcentPremii() : BigDecimal.ZERO;
            Integer liczbaZakupow = klient.getLiczbaZakupow() != null ? klient.getLiczbaZakupow() : 0;
            BigDecimal totalWydane = klient.getTotalWydane() != null ? klient.getTotalWydane() : BigDecimal.ZERO;

            model.addAttribute("klient", klient);
            model.addAttribute("saldo", saldo);
            model.addAttribute("procentPremii", procentPremii);
            model.addAttribute("liczbaZakupow", liczbaZakupow);
            model.addAttribute("totalWydane", totalWydane);
            model.addAttribute("tytul",
                    "Konto premii - " + klient.getImie() + " " + klient.getNazwisko());

        } catch (Exception e) {
            System.err.println("Błąd w mojeSaldoPremii: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania danych: " + e.getMessage());
        }

        return "premia/saldo";
    }
}