package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.komis.service.AdminService;
import pl.komis.service.SamochodService;
import pl.komis.service.UserService;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final SamochodService samochodService;
    private final UserService userService;

    @GetMapping
    public String adminPanel(Model model) {
        // 1. Pobierz statystyki z AdminService
        Map<String, Object> stats = adminService.getDashboardStats();

        // 2. Pobierz liczbę użytkowników (to nie to samo co klienci!)
        long totalUsers = userService.count();

        // 3. Jeśli stats zawiera stare klucze, mapuj je na nowe
        // Mapowanie: liczbaSamochodow -> totalCars, liczbaDostepnych -> availableCars
        Long totalCars = (Long) stats.get("liczbaSamochodow");
        Long availableCars = (Long) stats.get("liczbaDostepnych");
        Long totalKlientow = (Long) stats.get("liczbaKlientow");
        Long totalZakupow = (Long) stats.get("liczbaZakupow");

        // 4. Dodaj wszystkie statystyki do modelu z prawidłowymi nazwami
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCars", totalCars != null ? totalCars : 0);
        model.addAttribute("availableCars", availableCars != null ? availableCars : 0);
        model.addAttribute("totalKlientow", totalKlientow != null ? totalKlientow : 0);
        model.addAttribute("totalZakupow", totalZakupow != null ? totalZakupow : 0);

        // 5. Dodaj pozostałe statystyki (zaawansowane)
        model.addAttribute("statystykiZakupow", stats.get("statystykiZakupow"));
        model.addAttribute("statystykiKlientow", stats.get("statystykiKlientow"));

        model.addAttribute("pageTitle", "Panel Administratora");
        model.addAttribute("currentPage", "panel");

        return "admin/panel";
    }
}