package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.komis.service.SamochodService;
import pl.komis.service.UserService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final SamochodService samochodService;

    @GetMapping
    public String adminPanel(Model model) {
        long totalUsers = userService.count();
        long totalCars = samochodService.count();
        long availableCars = samochodService.countByStatus("DOSTEPNY");

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCars", totalCars);
        model.addAttribute("availableCars", availableCars);
        model.addAttribute("tytul", "Panel Administracyjny");

        return "admin/panel";
    }
}