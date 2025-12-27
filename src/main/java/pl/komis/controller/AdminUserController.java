package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.User;
import pl.komis.service.UserService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("tytul", "Zarządzanie użytkownikami");
        return "admin/users";
    }

    @GetMapping("/{id}")
    public String userDetails(@PathVariable String id, Model model) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Użytkownik nie znaleziony");
            return "error";
        }
        model.addAttribute("user", userOpt.get());
        model.addAttribute("tytul", "Szczegóły użytkownika");
        return "admin/user-details";
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

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable String id, Model model) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Użytkownik nie znaleziony");
            return "error";
        }
        model.addAttribute("user", userOpt.get());
        model.addAttribute("tytul", "Edytuj użytkownika");
        return "admin/edit-user";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable String id,
                             @ModelAttribute User user,
                             RedirectAttributes redirectAttributes) {
        try {
            user.setId(id);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Użytkownik zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("tytul", "Dodaj nowego użytkownika");
        return "admin/create-user";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user,
                             @RequestParam String password,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.createUserByAdmin(user, password, role);
            redirectAttributes.addFlashAttribute("successMessage", "Użytkownik utworzony");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}