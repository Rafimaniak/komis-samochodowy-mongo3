package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.User;
import pl.komis.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    private static final List<String> AVAILABLE_ROLES = Arrays.asList("USER", "ADMIN");

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("tytul", "Zarządzanie użytkownikami");
        return "admin/users";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", AVAILABLE_ROLES);
        model.addAttribute("tytul", "Dodaj nowego użytkownika");
        return "admin/user-form";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            // Walidacja hasła
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Hasła nie są identyczne");
                return "redirect:/admin/users/create";
            }

            // Ustaw domyślne wartości
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setRole(role);

            // Utwórz użytkownika
            userService.createUserByAdmin(user, password, role);

            redirectAttributes.addFlashAttribute("successMessage", "Użytkownik utworzony pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable String id, Model model) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Użytkownik nie znaleziony");
            return "error";
        }
        model.addAttribute("user", userOpt.get());
        model.addAttribute("roles", AVAILABLE_ROLES);
        model.addAttribute("tytul", "Edytuj użytkownika");
        return "admin/user-form";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable String id,
                             @ModelAttribute User user,
                             @RequestParam(required = false) String newPassword,
                             @RequestParam(required = false) String confirmNewPassword,
                             RedirectAttributes redirectAttributes) {
        try {
            // Pobierz istniejącego użytkownika
            Optional<User> existingUserOpt = userService.findById(id);
            if (existingUserOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Użytkownik nie znaleziony");
                return "redirect:/admin/users";
            }

            User existingUser = existingUserOpt.get();

            // Zaktualizuj podstawowe dane
            existingUser.setUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());
            existingUser.setRole(user.getRole());
            existingUser.setEnabled(user.getEnabled());

            // Jeśli podano nowe hasło
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (!newPassword.equals(confirmNewPassword)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Hasła nie są identyczne");
                    return "redirect:/admin/users/edit/" + id;
                }
                userService.changePassword(id, newPassword);
            }

            // Zapisz zmiany
            userService.updateUser(existingUser);

            redirectAttributes.addFlashAttribute("successMessage", "Użytkownik zaktualizowany pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/change-password/{id}")
    public String changePasswordForm(@PathVariable String id, Model model) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Użytkownik nie znaleziony");
            return "error";
        }
        model.addAttribute("user", userOpt.get());
        model.addAttribute("tytul", "Zmiana hasła");
        return "admin/change-password";
    }

    @PostMapping("/change-password/{id}")
    public String changePassword(@PathVariable String id,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Hasła nie są identyczne");
                return "redirect:/admin/users/change-password/" + id;
            }

            userService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Hasło zostało zmienione");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggleUserStatus(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Status użytkownika zmieniony");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Użytkownik usunięty");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/change-role")
    public String changeUserRole(@PathVariable String id,
                                 @RequestParam String newRole,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.changeUserRole(id, newRole);
            redirectAttributes.addFlashAttribute("successMessage", "Rola użytkownika zmieniona na: " + newRole);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}