package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {

        // Obsługa błędów logowania
        if (error != null) {
            model.addAttribute("errorMessage", "Nieprawidłowa nazwa użytkownika lub hasło!");
        }

        // Obsługa wylogowania
        if (logout != null) {
            model.addAttribute("successMessage", "Zostałeś pomyślnie wylogowany!");
        }

        return "login";
    }

    // NIE DODAWAJ metody POST! Spring Security to obsługuje
    // Usuń całą metodę @PostMapping("/login")
}