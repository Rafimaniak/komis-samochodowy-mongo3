package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.komis.model.Samochod;
import pl.komis.service.SamochodService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class StronaController {

    private final SamochodService samochodService;

    @GetMapping("/")
    public String stronaGlowna(Model model) {
        List<Samochod> samochody = samochodService.findAll();
        model.addAttribute("samochody", samochody);
        model.addAttribute("tytul", "Komis Samochodowy - Strona Główna");

        // Sprawdź czy użytkownik jest zalogowany
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null &&
                authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("username", authentication.getName());

        return "index";
    }
}