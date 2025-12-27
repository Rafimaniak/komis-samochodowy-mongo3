package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.dto.RegisterRequest;
import pl.komis.model.User;
import pl.komis.model.Klient;
import pl.komis.service.UserService;
import pl.komis.service.KlientService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;
    private final KlientService klientService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("tytul", "Rejestracja - Komis Samochodowy");
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute RegisterRequest registerRequest,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        // Walidacja podstawowa
        if (bindingResult.hasErrors()) {
            model.addAttribute("tytul", "Rejestracja - Komis Samochodowy");
            return "register";
        }

        // Walidacja hasła
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registerRequest", "Hasła nie są identyczne");
            model.addAttribute("tytul", "Rejestracja - Komis Samochodowy");
            return "register";
        }

        // Sprawdź czy użytkownik już istnieje
        if (userService.usernameExists(registerRequest.getUsername())) {
            bindingResult.rejectValue("username", "error.registerRequest", "Nazwa użytkownika jest już zajęta");
            model.addAttribute("tytul", "Rejestracja - Komis Samochodowy");
            return "register";
        }

        if (userService.emailExists(registerRequest.getEmail())) {
            bindingResult.rejectValue("email", "error.registerRequest", "Email jest już zajęty");
            model.addAttribute("tytul", "Rejestracja - Komis Samochodowy");
            return "register";
        }

        try {
            // Utwórz użytkownika z timestampem
            User user = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .role("USER")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Użyj metody createSimpleUser z UserService (ona zakoduje hasło)
            User savedUser = userService.createSimpleUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );

            // Ustaw datę utworzenia
            savedUser.setCreatedAt(LocalDateTime.now());
            savedUser = userService.save(savedUser);

            // Utwórz klienta z danymi z formularza
            Klient klient = new Klient();
            klient.setImie(registerRequest.getImie());
            klient.setNazwisko(registerRequest.getNazwisko());
            klient.setEmail(registerRequest.getEmail());
            klient.setTelefon(registerRequest.getTelefon());
            klient.setLiczbaZakupow(0);
            klient.setProcentPremii(BigDecimal.ZERO);
            klient.setSaldoPremii(BigDecimal.ZERO);
            klient.setTotalWydane(BigDecimal.ZERO);

            klient = klientService.save(klient);

            // Przypisz klienta do użytkownika
            savedUser.setKlient(klient);
            userService.save(savedUser);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Konto zostało utworzone pomyślnie! Możesz się teraz zalogować.");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Wystąpił błąd podczas rejestracji: " + e.getMessage());
            model.addAttribute("tytul", "Rejestracja - Komis Samochodowy");
            return "register";
        }
    }
}