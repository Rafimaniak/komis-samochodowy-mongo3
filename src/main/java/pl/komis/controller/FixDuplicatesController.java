package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.Klient;
import pl.komis.model.User;
import pl.komis.service.KlientService;
import pl.komis.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/fix")
@RequiredArgsConstructor
public class FixDuplicatesController {

    private final KlientService klientService;
    private final UserService userService;

    @GetMapping("/check-duplicates")
    public String checkDuplicates(Model model) {
        try {
            System.out.println("=== SPRAWDZANIE DUPLIKATÓW KLIENTÓW ===");

            List<Klient> allClients = klientService.findAll();
            System.out.println("Znaleziono " + allClients.size() + " klientów");

            // Grupuj klientów po emailu
            Map<String, List<Klient>> clientsByEmail = allClients.stream()
                    .filter(k -> k.getEmail() != null && !k.getEmail().trim().isEmpty())
                    .collect(Collectors.groupingBy(Klient::getEmail));

            System.out.println("Pogrupowano po " + clientsByEmail.size() + " unikalnych emailach");

            // Znajdź duplikaty (więcej niż 1 klient z tym samym emailem)
            Map<String, List<Klient>> duplicates = clientsByEmail.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            System.out.println("Znaleziono " + duplicates.size() + " grup duplikatów");

            model.addAttribute("duplicates", duplicates);
            model.addAttribute("totalDuplicates", duplicates.size());
            model.addAttribute("totalClients", allClients.size());

        } catch (Exception e) {
            System.err.println("BŁĄD w checkDuplicates: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd: " + e.getMessage());
        }

        return "admin/check-duplicates";
    }

    @PostMapping("/merge-duplicates")
    public String mergeDuplicates(RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== SCALANIE DUPLIKATÓW KLIENTÓW ===");

            List<Klient> allClients = klientService.findAll();

            // Grupuj klientów po emailu
            Map<String, List<Klient>> clientsByEmail = allClients.stream()
                    .filter(k -> k.getEmail() != null && !k.getEmail().trim().isEmpty())
                    .collect(Collectors.groupingBy(Klient::getEmail));

            int mergedCount = 0;
            int usersUpdated = 0;

            for (Map.Entry<String, List<Klient>> entry : clientsByEmail.entrySet()) {
                List<Klient> duplicates = entry.getValue();

                if (duplicates.size() > 1) {
                    System.out.println("Scalanie duplikatów dla email: " + entry.getKey());

                    // Wybierz pierwszego klienta jako głównego
                    Klient mainClient = duplicates.get(0);

                    // Scal dane z duplikatów
                    for (int i = 1; i < duplicates.size(); i++) {
                        Klient duplicate = duplicates.get(i);

                        System.out.println("  - Scalanie z klientem ID: " + duplicate.getId());

                        // Dodaj zakupy, saldo, etc.
                        mainClient.setLiczbaZakupow(mainClient.getLiczbaZakupow() + duplicate.getLiczbaZakupow());
                        mainClient.setSaldoPremii(mainClient.getSaldoPremii().add(
                                duplicate.getSaldoPremii() != null ? duplicate.getSaldoPremii() : BigDecimal.ZERO
                        ));
                        mainClient.setTotalWydane(mainClient.getTotalWydane().add(
                                duplicate.getTotalWydane() != null ? duplicate.getTotalWydane() : BigDecimal.ZERO
                        ));

                        // Znajdź użytkowników powiązanych z duplikatem i zaktualizuj ich
                        try {
                            List<User> usersWithDuplicate = userService.findByKlientId(duplicate.getId());
                            for (User user : usersWithDuplicate) {
                                user.setKlient(mainClient);
                                userService.saveUser(user);
                                usersUpdated++;
                            }
                        } catch (Exception e) {
                            System.err.println("  Błąd aktualizacji użytkowników: " + e.getMessage());
                        }

                        // Usuń duplikat
                        klientService.delete(duplicate.getId());
                        System.out.println("  - Usunięto duplikat ID: " + duplicate.getId());
                    }

                    // Zapisz głównego klienta
                    klientService.save(mainClient);
                    mergedCount++;
                }
            }

            String message = String.format("Scalono %d grup duplikatów klientów", mergedCount);
            if (usersUpdated > 0) {
                message += String.format(". Zaktualizowano %d użytkowników", usersUpdated);
            }

            redirectAttributes.addFlashAttribute("successMessage", message);

        } catch (Exception e) {
            System.err.println("BŁĄD w mergeDuplicates: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas scalania duplikatów: " + e.getMessage());
        }

        return "redirect:/admin/fix/check-duplicates";
    }
}