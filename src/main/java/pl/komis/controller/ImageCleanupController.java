package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.komis.model.Samochod;
import pl.komis.service.SamochodService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequestMapping("/admin/cleanup")
@RequiredArgsConstructor
public class ImageCleanupController {

    private final SamochodService samochodService;
    private final ResourceLoader resourceLoader;
    private static final String UPLOAD_DIR = "src/main/resources/static/images/samochody/";

    @GetMapping("/unused-images")
    public String showUnusedImages(Model model) {
        try {
            // 1. Pobierz wszystkie używane zdjęcia z bazy danych
            List<Samochod> wszystkieSamochody = samochodService.findAll();
            Set<String> uzywaneZdjecia = new HashSet<>();

            for (Samochod samochod : wszystkieSamochody) {
                // POPRAWIONE: zmiana z getZdjecie() na getZdjecieNazwa()
                if (samochod.getZdjecieNazwa() != null && !samochod.getZdjecieNazwa().isEmpty()) {
                    uzywaneZdjecia.add(samochod.getZdjecieNazwa());
                }
            }

            // 2. Pobierz wszystkie pliki z folderu zdjęć
            Resource resource = resourceLoader.getResource("classpath:static/images/samochody/");
            File folderZdjec = resource.getFile();
            File[] wszystkiePliki = folderZdjec.listFiles();

            List<FileInfo> nieuzywaneZdjecia = new ArrayList<>();
            List<FileInfo> wszystkieZdjecia = new ArrayList<>();

            if (wszystkiePliki != null) {
                for (File plik : wszystkiePliki) {
                    if (plik.isFile()) {
                        String nazwaPliku = plik.getName();
                        boolean jestUzywane = uzywaneZdjecia.contains(nazwaPliku);

                        FileInfo fileInfo = new FileInfo(
                                nazwaPliku,
                                plik.length(),
                                new Date(plik.lastModified()),
                                jestUzywane
                        );

                        wszystkieZdjecia.add(fileInfo);
                        if (!jestUzywane && !nazwaPliku.equals("domyslny.jpg") && !nazwaPliku.equals("fiat500.jpg")) {
                            nieuzywaneZdjecia.add(fileInfo);
                        }
                    }
                }
            }

            model.addAttribute("wszystkieZdjecia", wszystkieZdjecia);
            model.addAttribute("nieuzywaneZdjecia", nieuzywaneZdjecia);
            model.addAttribute("liczbaUzywanych", uzywaneZdjecia.size());
            model.addAttribute("liczbaNieuzywanych", nieuzywaneZdjecia.size());
            model.addAttribute("liczbaWszystkich", wszystkieZdjecia.size());

        } catch (IOException e) {
            model.addAttribute("error", "Nie można odczytać folderu zdjęć: " + e.getMessage());
            e.printStackTrace(); // Dodaj to aby zobaczyć błąd w logach
        }

        return "admin/unused-images";
    }

    @PostMapping("/delete-unused-images")
    public String deleteUnusedImages(@RequestParam(value = "images", required = false) List<String> imagesToDelete,
                                     RedirectAttributes redirectAttributes) {
        if (imagesToDelete == null || imagesToDelete.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "Nie wybrano żadnych zdjęć do usunięcia.");
            return "redirect:/admin/cleanup/unused-images";
        }

        int usuniete = 0;
        int bledy = 0;

        for (String nazwaPliku : imagesToDelete) {
            try {
                // Użyj ścieżki bezwzględnej zamiast ResourceLoader
                Path filePath = Paths.get(UPLOAD_DIR + nazwaPliku);
                if (Files.exists(filePath)) {
                    if (Files.deleteIfExists(filePath)) {
                        usuniete++;
                    } else {
                        bledy++;
                    }
                }
            } catch (Exception e) {
                bledy++;
                System.err.println("Błąd usuwania pliku " + nazwaPliku + ": " + e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("success",
                "Usunięto " + usuniete + " zdjęć. " +
                        (bledy > 0 ? " Wystąpiło " + bledy + " błędów." : ""));

        return "redirect:/admin/cleanup/unused-images";
    }

    @GetMapping("/auto-cleanup")
    public String autoCleanup(RedirectAttributes redirectAttributes) {
        try {
            // Automatyczne czyszczenie
            List<Samochod> wszystkieSamochody = samochodService.findAll();
            Set<String> uzywaneZdjecia = new HashSet<>();

            for (Samochod samochod : wszystkieSamochody) {
                // POPRAWIONE: używaj getZdjecieNazwa()
                if (samochod.getZdjecieNazwa() != null && !samochod.getZdjecieNazwa().isEmpty()) {
                    uzywaneZdjecia.add(samochod.getZdjecieNazwa());
                }
            }

            int usuniete = 0;

            // Użyj ścieżki bezwzględnej
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.exists(uploadPath)) {
                File[] wszystkiePliki = uploadPath.toFile().listFiles();

                if (wszystkiePliki != null) {
                    for (File plik : wszystkiePliki) {
                        if (plik.isFile()) {
                            String nazwaPliku = plik.getName();
                            boolean jestUzywane = uzywaneZdjecia.contains(nazwaPliku);

                            if (!jestUzywane && !nazwaPliku.equals("domyslny.jpg") &&
                                    !nazwaPliku.equals("fiat500.jpg")) {
                                if (plik.delete()) {
                                    usuniete++;
                                }
                            }
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    "Automatyczne czyszczenie: usunięto " + usuniete + " nieużywanych zdjęć.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas automatycznego czyszczenia: " + e.getMessage());
        }

        return "redirect:/admin/cleanup/unused-images";
    }

    // Klasa pomocnicza do przechowywania informacji o plikach
    public static class FileInfo {
        private String nazwa;
        private long rozmiar;
        private Date dataModyfikacji;
        private boolean uzywane;

        public FileInfo(String nazwa, long rozmiar, Date dataModyfikacji, boolean uzywane) {
            this.nazwa = nazwa;
            this.rozmiar = rozmiar;
            this.dataModyfikacji = dataModyfikacji;
            this.uzywane = uzywane;
        }

        // Gettery
        public String getNazwa() { return nazwa; }
        public long getRozmiar() { return rozmiar; }
        public Date getDataModyfikacji() { return dataModyfikacji; }
        public boolean isUzywane() { return uzywane; }
        public String getRozmiarMB() {
            return String.format("%.2f MB", rozmiar / (1024.0 * 1024.0));
        }
    }
}