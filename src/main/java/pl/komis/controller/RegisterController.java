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
import java.util.Optional;

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
            // Sprawdź czy klient o tym emailu już istnieje
            Optional<Klient> existingKlient = klientService.findByEmail(registerRequest.getEmail());
            Klient klient;

            if (existingKlient.isPresent()) {
                // Użyj istniejącego klienta i zaktualizuj dane
                klient = existingKlient.get();
                klient.setImie(registerRequest.getImie());
                klient.setNazwisko(registerRequest.getNazwisko());
                klient.setTelefon(registerRequest.getTelefon());
            } else {
                // Utwórz nowego klienta
                klient = new Klient();
                klient.setImie(registerRequest.getImie());
                klient.setNazwisko(registerRequest.getNazwisko());
                klient.setEmail(registerRequest.getEmail());
                klient.setTelefon(registerRequest.getTelefon());
                klient.setLiczbaZakupow(0);
                klient.setProcentPremii(0.0);
                klient.setSaldoPremii(0.0);
                klient.setTotalWydane(0.0);
            }

            // Zapisz klienta
            Klient savedKlient = klientService.save(klient);

            // Utwórz użytkownika
            User user = userService.createSimpleUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );

            // PRZYPISZ KLIENTA DO UŻYTKOWNIKA - teraz pole klient jest @DBRef
            user.setKlientId(savedKlient.getId());
            user.setCreatedAt(LocalDateTime.now());

            // Zapisz użytkownika z przypisanym klientem
            User savedUser = userService.saveUser(user);

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