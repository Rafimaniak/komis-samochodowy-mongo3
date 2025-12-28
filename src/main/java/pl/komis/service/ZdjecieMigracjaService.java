package pl.komis.service;

import org.springframework.stereotype.Service;
import pl.komis.model.Samochod;
import pl.komis.repository.SamochodRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ZdjecieMigracjaService {

    private final SamochodRepository samochodRepository;
    private static final String UPLOAD_DIR = "src/main/resources/static/images/samochody/";

    public ZdjecieMigracjaService(SamochodRepository samochodRepository) {
        this.samochodRepository = samochodRepository;
        init();
    }

    private void init() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void migrujZdjecia() {
        List<Samochod> samochody = samochodRepository.findAll();

        for (Samochod samochod : samochody) {
            // Sprawdź czy w bazie istnieją stare URL-e (mogą być w polu zdjecieNazwa z poprzedniej wersji)
            // Lub jeśli masz dodatkowe pole tymczasowe - tutaj zakładam, że stare URL-e są w zdjecieNazwa
            if (samochod.getZdjecieNazwa() != null &&
                    samochod.getZdjecieNazwa().startsWith("http")) {
                try {
                    String url = samochod.getZdjecieNazwa();
                    String fileExtension = ".jpg";

                    if (url.contains(".jpg") || url.contains(".jpeg")) {
                        fileExtension = ".jpg";
                    } else if (url.contains(".png")) {
                        fileExtension = ".png";
                    } else if (url.contains(".gif")) {
                        fileExtension = ".gif";
                    }

                    String fileName = UUID.randomUUID().toString() + fileExtension;
                    Path destination = Paths.get(UPLOAD_DIR + fileName);

                    // Użyj URI.create() zamiast new URL() - POPRAWIONE DEPRECATION
                    try (java.io.InputStream in = URI.create(url).toURL().openStream()) {
                        Files.copy(in, destination);
                        samochod.setZdjecieNazwa(fileName);
                        samochodRepository.save(samochod);
                        System.out.println("Zmigrowano zdjęcie dla: " + samochod.getMarka() + " " + samochod.getModel());
                    }

                } catch (Exception e) {
                    System.err.println("Błąd migracji dla samochodu " + samochod.getId() + ": " + e.getMessage());
                    samochod.setZdjecieNazwa(null);
                    samochodRepository.save(samochod);
                }
            }
        }
    }
}