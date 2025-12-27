package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.komis.model.Klient;
import pl.komis.model.User;
import pl.komis.service.KlientService;
import pl.komis.service.UserService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/premia")
@RequiredArgsConstructor
public class PremiaController {

    private final UserService userService;
    private final KlientService klientService;

    @GetMapping("/moje-saldo")
    public String mojeSaldo(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        Klient klient = user.getKlient();
        if (klient != null) {
            model.addAttribute("klient", klient);
            model.addAttribute("saldo", klient.getSaldoPremii());
            model.addAttribute("procentPremii", klient.getProcentPremii());
            model.addAttribute("liczbaZakupow", klient.getLiczbaZakupow());
            model.addAttribute("totalWydane", klient.getTotalWydane());
        }

        return "premia/saldo";
    }

    @GetMapping("/regulamin")
    public String regulamin() {
        return "premia/regulamin";
    }
}