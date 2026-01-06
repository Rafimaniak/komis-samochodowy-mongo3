package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.*;
import pl.komis.service.*;

import java.util.*;

@Controller
@RequestMapping("/zakupy")
@RequiredArgsConstructor
public class ZakupController {

    private final ZakupService zakupService;
    private final UserService userService;
    private final KlientService klientService;
    private final SamochodService samochodService;
    private final PracownikService pracownikService;
    private final MongoDBFunctionService mongoDBFunctionService;

    // Pomocnicza metoda do pobierania Samochodu (obsługuje Optional)
    @SuppressWarnings("unchecked")
    private Samochod getSamochodById(String id) {
        Object result = samochodService.findById(id);

        if (result instanceof Optional) {
            return ((Optional<Samochod>) result)
                    .orElseThrow(() -> new RuntimeException("Samochód nie znaleziony"));
        } else {
            Samochod samochod = (Samochod) result;
            if (samochod == null) {
                throw new RuntimeException("Samochód nie znaleziony");
            }
            return samochod;
        }
    }


    // Pomocnicza metoda do pobierania User (obsługuje Optional)
    @SuppressWarnings("unchecked")
    private User getUserByUsername(String username) {
        Object result = userService.findByUsername(username);

        if (result instanceof Optional) {
            return ((Optional<User>) result)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
        } else {
            User user = (User) result;
            if (user == null) {
                throw new RuntimeException("Użytkownik nie znaleziony");
            }
            return user;
        }
    }

    // Pomocnicza metoda do pobierania Klient (obsługuje Optional)
    @SuppressWarnings("unchecked")
    private Klient getKlientById(String id) {
        Object result = klientService.findById(id);

        if (result instanceof Optional) {
            return ((Optional<Klient>) result)
                    .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));
        } else {
            Klient klient = (Klient) result;
            if (klient == null) {
                throw new RuntimeException("Klient nie znaleziony");
            }
            return klient;
        }
    }

    // Pomocnicza metoda do pobierania Pracownik (obsługuje Optional)
    @SuppressWarnings("unchecked")
    private Pracownik getPracownikById(String id) {
        Object result = pracownikService.findById(id);

        if (result instanceof Optional) {
            return ((Optional<Pracownik>) result)
                    .orElseThrow(() -> new RuntimeException("Pracownik nie znaleziony"));
        } else {
            Pracownik pracownik = (Pracownik) result;
            if (pracownik == null) {
                throw new RuntimeException("Pracownik nie znaleziony");
            }
            return pracownik;
        }
    }

    // Pomocnicza metoda do pobierania Zakup (obsługuje Optional)
    @SuppressWarnings("unchecked")
    private Zakup getZakupById(String id) {
        Object result = zakupService.findById(id);

        if (result instanceof Optional) {
            return ((Optional<Zakup>) result)
                    .orElseThrow(() -> new RuntimeException("Zakup nie znaleziony"));
        } else {
            Zakup zakup = (Zakup) result;
            if (zakup == null) {
                throw new RuntimeException("Zakup nie znaleziony");
            }
            return zakup;
        }
    }

    @GetMapping("/rabaty")
    @PreAuthorize("hasRole('ADMIN')")
    public String listaRabatow(Model model) {
        List<Klient> klienci = klientService.findAll();

        Double totalSaldo = klienci.stream()
                .map(k -> k.getSaldoPremii() != null ? k.getSaldoPremii() : 0.0)
                .reduce(0.0, Double::sum);

        Double totalWydane = klienci.stream()
                .map(k -> k.getTotalWydane() != null ? k.getTotalWydane() : 0.0)
                .reduce(0.0, Double::sum);

        long klienciZPremia = klienci.stream()
                .filter(k -> k.getProcentPremii() != null && k.getProcentPremii() > 0)
                .count();

        model.addAttribute("klienci", klienci);
        model.addAttribute("totalSaldo", totalSaldo);
        model.addAttribute("totalWydane", totalWydane);
        model.addAttribute("klienciZPremia", klienciZPremia);
        model.addAttribute("tytul", "System premii i rabatów");
        return "zakupy/rabaty";
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listZakupy(Model model) {
        List<Zakup> zakupy = zakupService.findAll();
        System.out.println("DEBUG: Pobrano " + zakupy.size() + " zakupów");

        Double sumaCenaZakupu = 0.0;
        Double sumaWykorzystaneSaldo = 0.0;
        Double sumaRabatow = 0.0;
        int liczbaRabatow = 0;

        for (Zakup zakup : zakupy) {
            if (zakup.getCenaZakupu() != null) {
                sumaCenaZakupu += zakup.getCenaZakupu();
            }
            if (zakup.getWykorzystaneSaldo() != null) {
                sumaWykorzystaneSaldo += zakup.getWykorzystaneSaldo();
            }
            if (zakup.getZastosowanyRabat() != null && zakup.getZastosowanyRabat() > 0) {
                sumaRabatow += zakup.getZastosowanyRabat();
                liczbaRabatow++;
            }
        }

        Double sredniRabat = 0.0;
        if (liczbaRabatow > 0) {
            sredniRabat = sumaRabatow / liczbaRabatow;
            sredniRabat = Math.round(sredniRabat * 100.0) / 100.0; // Zaokrąglenie do 2 miejsc
        }

        boolean brakZakupow = zakupy.isEmpty();

        model.addAttribute("zakupy", zakupy);
        model.addAttribute("sumaCenaZakupu", brakZakupow ? 0.0 : sumaCenaZakupu);
        model.addAttribute("sumaWykorzystaneSaldo", brakZakupow ? 0.0 : sumaWykorzystaneSaldo);
        model.addAttribute("sredniRabat", brakZakupow ? 0.0 : sredniRabat);
        model.addAttribute("tytul", "Lista Wszystkich Zakupów");
        model.addAttribute("brakZakupow", brakZakupow);
        return "zakupy/lista-admin";
    }

    @GetMapping("/wykorzystaj-saldo")
    @PreAuthorize("isAuthenticated()")
    public String wykorzystajSaldoForm(@RequestParam String samochodId,
                                       @RequestParam Double cena,
                                       Authentication authentication,
                                       Model model) {
        String username = authentication.getName();

        User user = getUserByUsername(username);

        String klientId = user.getKlientId();
        if (klientId == null) {
            throw new RuntimeException("Nie masz powiązanego klienta");
        }

        Klient klient = getKlientById(klientId);

        Samochod samochod = getSamochodById(samochodId);

        if ("ZAREZERWOWANY".equals(samochod.getStatus())) {
            if (samochod.getZarezerwowanyPrzezKlientId() == null) {
                throw new RuntimeException("Samochód jest zarezerwowany, ale brak informacji o kliencie");
            } else if (!samochod.getZarezerwowanyPrzezKlientId().equals(klient.getId())) {
                throw new RuntimeException("Ten samochód jest zarezerwowany przez innego klienta. " +
                        "Tylko osoba rezerwująca może go kupić.");
            }
        }

        if (!"DOSTEPNY".equals(samochod.getStatus()) && !"ZAREZERWOWANY".equals(samochod.getStatus())) {
            throw new RuntimeException("Samochód nie jest dostępny do zakupu. Status: " + samochod.getStatus());
        }

        model.addAttribute("samochodId", samochodId);
        model.addAttribute("cenaBazowa", cena);
        model.addAttribute("klient", klient);
        model.addAttribute("saldoKlienta", klient.getSaldoPremii());
        // Maksymalne wykorzystanie to minimum z ceny i salda
        model.addAttribute("maksWykorzystanie", Math.min(cena, klient.getSaldoPremii()));
        model.addAttribute("samochod", samochod);
        model.addAttribute("procentPremii", klient.getProcentPremii());

        if (klient.getProcentPremii() != null && klient.getProcentPremii() > 0) {
            Double premia = cena * (klient.getProcentPremii() / 100.0);
            premia = Math.round(premia * 100.0) / 100.0; // Zaokrąglenie do 2 miejsc
            model.addAttribute("premiaOdZakupu", premia);
        }

        List<Pracownik> pracownicy = pracownikService.findAll();
        model.addAttribute("pracownicy", pracownicy);

        return "zakupy/wykorzystaj-saldo";
    }

    @PostMapping("/wykorzystaj-saldo")
    @PreAuthorize("isAuthenticated()")
    public String wykonajZakupZSaldem(@RequestParam String samochodId,
                                      @RequestParam Double cenaBazowa,
                                      @RequestParam(required = false) Double wykorzystaneSaldo,
                                      @RequestParam String pracownikId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {

        try {
            String username = authentication.getName();
            User user = getUserByUsername(username);

            Klient klient = getKlientById(user.getKlientId());

            if (klient == null) {
                throw new RuntimeException("Nie masz powiązanego klienta");
            }

            Samochod samochod = getSamochodById(samochodId);

            if (!samochod.getStatus().equals("DOSTEPNY") &&
                    !samochod.getStatus().equals("ZAREZERWOWANY")) {
                throw new RuntimeException("Samochód nie jest dostępny do zakupu. Status: " + samochod.getStatus());
            }

            if (samochod.getStatus().equals("ZAREZERWOWANY") &&
                    (samochod.getZarezerwowanyPrzezKlientId() == null ||
                            !samochod.getZarezerwowanyPrzezKlientId().equals(klient.getId()))) {
                throw new RuntimeException("Ten samochód jest zarezerwowany przez innego klienta. " +
                        "Tylko osoba rezerwująca może go kupić.");
            }

            Pracownik pracownik = getPracownikById(pracownikId);

            Double faktycznieWykorzystaneSaldo = 0.0;
            if (wykorzystaneSaldo != null && wykorzystaneSaldo > 0) {
                if (klient.getSaldoPremii() < wykorzystaneSaldo) {
                    throw new RuntimeException("Niewystarczające saldo premii");
                }
                faktycznieWykorzystaneSaldo = Math.min(wykorzystaneSaldo, cenaBazowa);
            }

            String zakupId = String.valueOf(zakupService.createZakupZSaldem(
                    samochodId,
                    klient.getId(),
                    pracownikId,
                    cenaBazowa,
                    faktycznieWykorzystaneSaldo
            ));

            Klient zaktualizowanyKlient = getKlientById(klient.getId());

            String message;
            if (faktycznieWykorzystaneSaldo > 0) {
                Double zaoszczedzone = faktycznieWykorzystaneSaldo;
                message = String.format(
                        "Samochód kupiony z wykorzystaniem salda!<br>" +
                                "Wykorzystano saldo: <strong>%.2f zł</strong><br>" +
                                "Nowe saldo: <strong>%.2f zł</strong>",
                        zaoszczedzone, zaktualizowanyKlient.getSaldoPremii()
                );
            } else {
                message = String.format(
                        "Samochód kupiony pomyślnie!<br>" +
                                "Twoje nowe saldo premii: <strong>%.2f zł</strong>",
                        zaktualizowanyKlient.getSaldoPremii()
                );
            }

            redirectAttributes.addFlashAttribute("successMessage", message);

            return "redirect:/zakupy/moje";

        } catch (Exception e) {
            System.err.println("Błąd podczas zakupu z wykorzystaniem salda: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas zakupu: " + e.getMessage());
            return "redirect:/samochody/szczegoly?id=" + samochodId;
        }
    }

    @GetMapping("/moje")
    @PreAuthorize("isAuthenticated()")
    public String mojeZakupy(Authentication authentication, Model model) {
        String username = authentication.getName();

        try {
            User user = getUserByUsername(username);

            List<Zakup> zakupy = new ArrayList<>();
            Klient klient = null;

            klient = userService.ensureUserHasKlient(user.getId());

            if (klient == null) {
                throw new RuntimeException("Nie można utworzyć/pobrać klienta");
            }

            zakupy = zakupService.findByKlientId(klient.getId());

            System.out.println("DEBUG: Dla klienta ID: " + klient.getId() +
                    " znaleziono " + zakupy.size() + " zakupów");

            Double sumaCenaBazowa = zakupy.stream()
                    .map(z -> z.getCenaBazowa() != null ? z.getCenaBazowa() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaWykorzystaneSaldo = zakupy.stream()
                    .map(z -> z.getWykorzystaneSaldo() != null ? z.getWykorzystaneSaldo() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaNaliczonaPremia = zakupy.stream()
                    .map(z -> z.getNaliczonaPremia() != null ? z.getNaliczonaPremia() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaCenaZakupu = zakupy.stream()
                    .map(z -> z.getCenaZakupu() != null ? z.getCenaZakupu() : 0.0)
                    .reduce(0.0, Double::sum);

            model.addAttribute("zakupy", zakupy);
            model.addAttribute("klient", klient);
            model.addAttribute("sumaCenaBazowa", sumaCenaBazowa);
            model.addAttribute("sumaWykorzystaneSaldo", sumaWykorzystaneSaldo);
            model.addAttribute("sumaNaliczonaPremia", sumaNaliczonaPremia);
            model.addAttribute("sumaCenaZakupu", sumaCenaZakupu);
            model.addAttribute("tytul", "Moje Zakupy");

            return "zakupy/lista-klient";

        } catch (Exception e) {
            System.err.println("BŁĄD w mojeZakupy: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania zakupów: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/moje/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String zakupyKlienta(@PathVariable String id, Model model) {
        try {
            Klient klient = getKlientById(id);

            List<Zakup> zakupy = zakupService.findByKlientId(id);

            Double sumaCenaBazowa = zakupy.stream()
                    .map(z -> z.getCenaBazowa() != null ? z.getCenaBazowa() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaWykorzystaneSaldo = zakupy.stream()
                    .map(z -> z.getWykorzystaneSaldo() != null ? z.getWykorzystaneSaldo() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaNaliczonaPremia = zakupy.stream()
                    .map(z -> z.getNaliczonaPremia() != null ? z.getNaliczonaPremia() : 0.0)
                    .reduce(0.0, Double::sum);

            Double sumaCenaZakupu = zakupy.stream()
                    .map(z -> z.getCenaZakupu() != null ? z.getCenaZakupu() : 0.0)
                    .reduce(0.0, Double::sum);

            model.addAttribute("zakupy", zakupy);
            model.addAttribute("klient", klient);
            model.addAttribute("sumaCenaBazowa", sumaCenaBazowa);
            model.addAttribute("sumaWykorzystaneSaldo", sumaWykorzystaneSaldo);
            model.addAttribute("sumaNaliczonaPremia", sumaNaliczonaPremia);
            model.addAttribute("sumaCenaZakupu", sumaCenaZakupu);
            model.addAttribute("tytul", "Zakupy klienta: " + klient.getImie() + " " + klient.getNazwisko());

            return "zakupy/lista-klient";

        } catch (Exception e) {
            System.err.println("BŁĄD w zakupyKlienta: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd podczas ładowania zakupów: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/szczegoly/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String szczegolyZakupu(@PathVariable String id, Model model) {
        try {
            Zakup zakup = getZakupById(id);

            Klient klient = null;
            if (zakup.getKlientId() != null) {
                klient = klientService.findById(zakup.getKlientId())
                        .orElse(null);
            }

            List<Zakup> zakupy = Collections.singletonList(zakup);

            Double sumaCenaBazowa = zakup.getCenaBazowa() != null ? zakup.getCenaBazowa() : 0.0;
            Double sumaWykorzystaneSaldo = zakup.getWykorzystaneSaldo() != null ? zakup.getWykorzystaneSaldo() : 0.0;
            Double sumaNaliczonaPremia = zakup.getNaliczonaPremia() != null ? zakup.getNaliczonaPremia() : 0.0;
            Double sumaCenaZakupu = zakup.getCenaZakupu() != null ? zakup.getCenaZakupu() : 0.0;

            model.addAttribute("zakupy", zakupy);
            model.addAttribute("klient", klient);
            model.addAttribute("sumaCenaBazowa", sumaCenaBazowa);
            model.addAttribute("sumaWykorzystaneSaldo", sumaWykorzystaneSaldo);
            model.addAttribute("sumaNaliczonaPremia", sumaNaliczonaPremia);
            model.addAttribute("sumaCenaZakupu", sumaCenaZakupu);
            model.addAttribute("tytul", "Szczegóły zakupu #" + zakup.getId());

            return "zakupy/lista-klient";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Błąd podczas ładowania zakupu: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/usun/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteZakup(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            zakupService.remove(id);
            redirectAttributes.addFlashAttribute("successMessage", "Zakup #" + id + " został usunięty.");
        } catch (Exception e) {
            System.err.println("Błąd podczas usuwania zakupu: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas usuwania zakupu: " + e.getMessage());
        }
        return "redirect:/zakupy";
    }

    @GetMapping("/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public String debugZakupy(Model model) {
        try {
            List<Klient> klienci = klientService.findAll();

            Map<String, Object> debugInfo = new HashMap<>();
            for (Klient klient : klienci) {
                List<Zakup> zakupy = zakupService.findByKlientId(klient.getId());

                Map<String, Object> klientInfo = new HashMap<>();
                klientInfo.put("klient", klient);
                klientInfo.put("liczbaZakupow", zakupy.size());
                klientInfo.put("zakupy", zakupy);

                debugInfo.put(klient.getId(), klientInfo);
            }

            model.addAttribute("debugInfo", debugInfo);
            model.addAttribute("totalClients", klienci.size());

            return "admin/debug-zakupy";

        } catch (Exception e) {
            System.err.println("Błąd w debugZakupy: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Błąd debugowania: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/napraw-dane")
    @PreAuthorize("hasRole('ADMIN')")
    public String naprawDaneZakupow(RedirectAttributes redirectAttributes) {
        try {
            List<Zakup> zakupy = zakupService.findAll();
            int naprawione = 0;

            for (Zakup zakup : zakupy) {
                boolean zmieniony = false;

                if ((zakup.getSamochodMarka() == null || zakup.getSamochodModel() == null)
                        && zakup.getSamochodId() != null) {
                    try {
                        Samochod samochod = getSamochodById(zakup.getSamochodId());

                        zakup.setSamochodMarka(samochod.getMarka());
                        zakup.setSamochodModel(samochod.getModel());
                        zmieniony = true;
                    } catch (Exception e) {
                        System.err.println("Błąd przy naprawie samochodu dla zakupu " + zakup.getId() + ": " + e.getMessage());
                    }
                }

                if (zakup.getKlientImieNazwisko() == null && zakup.getKlientId() != null) {
                    try {
                        Klient klient = getKlientById(zakup.getKlientId());

                        zakup.setKlientImieNazwisko(klient.getImie() + " " + klient.getNazwisko());
                        zmieniony = true;
                    } catch (Exception e) {
                        System.err.println("Błąd przy naprawie klienta dla zakupu " + zakup.getId() + ": " + e.getMessage());
                    }
                }

                if (zakup.getPracownikImieNazwisko() == null && zakup.getPracownikId() != null) {
                    try {
                        Pracownik pracownik = getPracownikById(zakup.getPracownikId());

                        zakup.setPracownikImieNazwisko(pracownik.getImie() + " " + pracownik.getNazwisko());
                        zmieniony = true;
                    } catch (Exception e) {
                        System.err.println("Błąd przy naprawie pracownika dla zakupu " + zakup.getId() + ": " + e.getMessage());
                    }
                }

                if (zmieniony) {
                    zakupService.save(zakup);
                    naprawione++;
                    System.out.println("Naprawiono zakup: " + zakup.getId());
                }
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Naprawiono " + naprawione + " z " + zakupy.size() + " zakupów.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd podczas naprawy danych: " + e.getMessage());
        }

        return "redirect:/zakupy";
    }

    @GetMapping("/funkcje/test")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Object> testFunkcjiMongoDB() {
        Map<String, Object> result = new HashMap<>();
        result.put("statystykiZakupow", mongoDBFunctionService.getStatystykiZakupow());
        result.put("miesieczneStatystyki", zakupService.getMiesieczneStatystyki());
        result.put("czyscRezerwacje", mongoDBFunctionService.czyscPrzeterminowaneRezerwacje());
        return result;
    }

    @GetMapping("/funkcje/klienci/statystyki")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Object> statystykiKlientow() {
        return mongoDBFunctionService.getStatystykiKlientow();
    }

    @GetMapping("/funkcje/klienci/top")
    @PreAuthorize("hasRole('ADMIN')")
    public String topKlienci(Model model) {
        // Zmień na używanie metody z MongoDBFunctionService
        List<Map<String, Object>> topKlienci = mongoDBFunctionService.getTopKlientow(10);
        model.addAttribute("topKlienci", topKlienci);
        model.addAttribute("tytul", "Top 10 klientów");
        return "admin/top-klienci";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('PRACOWNIK')")
    public String saveZakup(@ModelAttribute Zakup zakup, RedirectAttributes redirectAttributes) {
        zakupService.save(zakup);
        redirectAttributes.addFlashAttribute("success", "Zakup zapisany!");
        return "redirect:/zakupy/list";
    }
}