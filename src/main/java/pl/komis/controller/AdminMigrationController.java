package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.service.KlientMigrationService;

@Controller
@RequestMapping("/admin/migration")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMigrationController {

    private final KlientMigrationService migrationService;

    @GetMapping("/check-duplicates")
    public String checkDuplicates(Model model) {
        String result = migrationService.checkForDuplicates();
        model.addAttribute("result", result);
        model.addAttribute("tytul", "Sprawdzanie duplikatów klientów");
        return "admin/migration-result";
    }

    @PostMapping("/merge-duplicates")
    public String mergeDuplicates(RedirectAttributes redirectAttributes) {
        try {
            String result = migrationService.mergeDuplicateClients();
            redirectAttributes.addFlashAttribute("successMessage", "Scalono duplikaty klientów");
            redirectAttributes.addFlashAttribute("result", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd podczas scalania: " + e.getMessage());
        }
        return "redirect:/admin/migration/result";
    }

    @PostMapping("/create-index")
    public String createIndex(RedirectAttributes redirectAttributes) {
        try {
            String result = migrationService.createUniqueEmailIndex();
            redirectAttributes.addFlashAttribute("successMessage", "Utworzono indeks");
            redirectAttributes.addFlashAttribute("result", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd tworzenia indeksu: " + e.getMessage());
        }
        return "redirect:/admin/migration/result";
    }

    @PostMapping("/fix-names")
    public String fixNames(RedirectAttributes redirectAttributes) {
        try {
            String result = migrationService.fixMissingNames();
            redirectAttributes.addFlashAttribute("successMessage", "Naprawiono dane klientów");
            redirectAttributes.addFlashAttribute("result", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd naprawiania danych: " + e.getMessage());
        }
        return "redirect:/admin/migration/result";
    }

    @GetMapping("/result")
    public String showResult(Model model) {
        model.addAttribute("tytul", "Wynik migracji");
        return "admin/migration-result";
    }
}