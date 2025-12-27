package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.komis.model.User;
import pl.komis.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PracownikController {

    private final UserService userService;

    @GetMapping("/pracownicy")
    public String listaPracownikow(Model model) {
        // Pobierz wszystkich użytkowników z rolą ADMIN
        List<User> pracownicy = userService.findAllUsers().stream()
                .filter(user -> "ADMIN".equals(user.getRole()))
                .collect(Collectors.toList());

        model.addAttribute("pracownicy", pracownicy);
        model.addAttribute("tytul", "Lista Pracowników");
        return "pracownicy";
    }
}